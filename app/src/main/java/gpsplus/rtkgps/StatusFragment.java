package gpsplus.rtkgps;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.util.Timer;
import java.util.TimerTask;

import gpsplus.rtkgps.databinding.FragmentStatusBinding;
import gpsplus.rtkgps.databinding.IncludeStatusViewWidgetBinding;
import gpsplus.rtkgps.view.GTimeView;
import gpsplus.rtkgps.view.GpsSkyView;
import gpsplus.rtkgps.view.SnrView;
import gpsplus.rtkgps.view.SolutionView;
import gpsplus.rtkgps.view.SolutionView.Format;
import gpsplus.rtklib.GTime;
import gpsplus.rtklib.RtkControlResult;
import gpsplus.rtklib.RtkServerObservationStatus;
import gpsplus.rtklib.RtkServerStreamStatus;

public class StatusFragment extends Fragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = StatusFragment.class.getSimpleName();

    public static final String PREF_TIME_FORMAT = "StatusFragment.PREF_TIME_FORMAT";
    public static final String PREF_SOLUTION_FORMAT = "StatusFragment.PREF_SOLUTION_FORMAT";
    private static final String KEY_CURRENT_STATUS_VIEW = "StatusFragment.currentStatusView";

    private Timer mStreamStatusUpdateTimer;
    private final RtkServerStreamStatus mStreamStatus = new RtkServerStreamStatus();
    private final RtkServerObservationStatus mRoverObservationStatus = new RtkServerObservationStatus();
    private final RtkServerObservationStatus mBaseObservationStatus = new RtkServerObservationStatus();
    private final RtkControlResult mRtkStatus = new RtkControlResult();

    private ArrayAdapter<StatusView> mStatusViewSpinnerAdapter;
    private StatusView mCurrentStatusView;

    private FragmentStatusBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.fragment_status, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.menu_select_solution_format) {
                    showSelectSolutionViewDialog();
                    return true;
                } else if (id == R.id.menu_select_gtime_format) {
                    showSelectTimeFormatDialog();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());


        binding.gtimeView.setOnLongClickListener(v -> {
            showSelectTimeFormatDialog();
            return true;
        });

        binding.solutionView.setOnLongClickListener(v -> {
            showSelectSolutionViewDialog();
            return true;
        });

        mStatusViewSpinnerAdapter = new ArrayAdapter<>(requireContext(), R.layout.select_solution_view_item);
        mStatusViewSpinnerAdapter.addAll(StatusView.values());
        mStatusViewSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        IncludeStatusViewWidgetBinding statusViewWidget = binding.statusViewWidget;
        if (statusViewWidget != null) {
            statusViewWidget.statusViewSpinner.setAdapter(mStatusViewSpinnerAdapter);
            statusViewWidget.statusViewSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (DBG) Log.v(TAG, "onItemSelected() " + position);
                    StatusView newView = mStatusViewSpinnerAdapter.getItem(position);
                    if (mCurrentStatusView != newView) {
                        assert newView != null;
                        setStatusView(newView);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing
                }
            });
        }

        if (savedInstanceState == null) {
            setStatusView(StatusView.SNR);
        } else {
            mCurrentStatusView = StatusView.valueOf(savedInstanceState.getString(KEY_CURRENT_STATUS_VIEW));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mStreamStatusUpdateTimer = new Timer();
        mStreamStatusUpdateTimer.schedule(new TimerTask() {
            final Runnable updateStatusRunnable = StatusFragment.this::updateStatus;

            @Override
            public void run() {
                Activity a = getActivity();
                if (a != null) a.runOnUiThread(updateStatusRunnable);
            }
        }, 200, 250);
    }

    @Override
    public void onResume() {
        super.onResume();
        final SharedPreferences prefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        updateGTimeFormat(prefs);
        updateSolutionFormat(prefs);
        prefs.registerOnSharedPreferenceChangeListener(mPrefsChangedListener);
    }

    @Override
    public void onPause() {
        final SharedPreferences prefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        prefs.unregisterOnSharedPreferenceChangeListener(mPrefsChangedListener);
        super.onPause();
    }

    @Override
    public void onStop() {
        mStreamStatusUpdateTimer.cancel();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_STATUS_VIEW, mCurrentStatusView.name());
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefsChangedListener = (sharedPreferences, key) -> {
        if (PREF_TIME_FORMAT.equals(key)) {
            updateGTimeFormat(sharedPreferences);
        } else if (PREF_SOLUTION_FORMAT.equals(key)) {
            updateSolutionFormat(sharedPreferences);
        }
    };

    private void showSelectSolutionViewDialog() {
        SelectSolutionViewFormatDialog.newInstance(binding.solutionView.getFormat())
                .show(getParentFragmentManager(), "Select Solution View Format Dialog");
    }

    private void showSelectTimeFormatDialog() {
        SelectTimeFormatDialog.newInstance(binding.gtimeView.getTimeFormat())
                .show(getParentFragmentManager(), "Select Time Format Dialog");
    }

    private void setStatusView(StatusView statusView) {
        mCurrentStatusView = statusView;
        IncludeStatusViewWidgetBinding statusViewWidget = binding.statusViewWidget;
        assert statusViewWidget != null;
        switch (statusView) {
            case SKYPLOT_BASE_L1:
            case SKYPLOT_BASE_L2:
            case SKYPLOT_BASE_L5:
            case SKYPLOT_ROVER_L1:
            case SKYPLOT_ROVER_L2:
            case SKYPLOT_ROVER_L5:
                statusViewWidget.Sky.setVisibility(View.VISIBLE);
                statusViewWidget.Snr1.setVisibility(View.GONE);
                statusViewWidget.Snr2.setVisibility(View.GONE);
                break;
            case SNR:
            case SNR_L1:
            case SNR_L2:
            case SNR_L5:
                statusViewWidget.Sky.setVisibility(View.GONE);
                statusViewWidget.Snr1.setVisibility(View.VISIBLE);
                statusViewWidget.Snr2.setVisibility(View.VISIBLE);
                break;
            default:
                throw new IllegalStateException("Unknown status view: " + statusView);
        }

        switch (statusView) {
            case SKYPLOT_BASE_L1:
            case SKYPLOT_ROVER_L1:
                statusViewWidget.Sky.setFreqBand(GpsSkyView.BAND_L1);
                break;
            case SKYPLOT_BASE_L2:
            case SKYPLOT_ROVER_L2:
                statusViewWidget.Sky.setFreqBand(GpsSkyView.BAND_L2);
                break;
            case SKYPLOT_BASE_L5:
            case SKYPLOT_ROVER_L5:
                statusViewWidget.Sky.setFreqBand(GpsSkyView.BAND_L5);
                break;
            case SNR:
                statusViewWidget.Snr1.setFreqBand(SnrView.BAND_ANY);
                statusViewWidget.Snr2.setFreqBand(SnrView.BAND_ANY);
                break;
            case SNR_L1:
                statusViewWidget.Snr1.setFreqBand(SnrView.BAND_L1);
                statusViewWidget.Snr2.setFreqBand(SnrView.BAND_L1);
                break;
            case SNR_L2:
                statusViewWidget.Snr1.setFreqBand(SnrView.BAND_L2);
                statusViewWidget.Snr2.setFreqBand(SnrView.BAND_L2);
                break;
            case SNR_L5:
                statusViewWidget.Snr1.setFreqBand(SnrView.BAND_L5);
                statusViewWidget.Snr2.setFreqBand(SnrView.BAND_L5);
                break;
        }
    }

    private void updateStatus() {
        if (binding == null) return;

        MainActivity ma = (MainActivity) getActivity();
        IncludeStatusViewWidgetBinding statusViewWidget = binding.statusViewWidget;
        if (ma == null) return;

        RtkNaviService rtks = ma.getRtkService();
        int serverStatus;

        if (rtks == null) {
            serverStatus = RtkServerStreamStatus.STATE_CLOSE;
            mStreamStatus.clear();
            clearStatusUI();
        } else {
            serverStatus = rtks.getServerStatus();

            if (rtks.isServiceStarted()) {
                try {
                    rtks.getStreamStatus(mStreamStatus);
                    rtks.getRoverObservationStatus(mRoverObservationStatus);
                    rtks.getBaseObservationStatus(mBaseObservationStatus);
                    rtks.getRtkStatus(mRtkStatus);

                    binding.streamStatus.setText(mStreamStatus.mMsg);
                    binding.streamIndicatorsView.setStats(mStreamStatus, serverStatus);
                    binding.solutionView.setStats(mRtkStatus);
                    binding.gtimeView.setTime(mRoverObservationStatus.getTime());
                } catch (Exception e) {
                    Log.e(TAG, "Error getting RTK status: " + e.getMessage(), e);
                    serverStatus = RtkServerStreamStatus.STATE_CLOSE;
                    mStreamStatus.clear();
                    clearStatusUI();
                }
            } else {
                mStreamStatus.clear();
                clearStatusUI();
            }
        }

        switch (mCurrentStatusView) {
            case SKYPLOT_BASE_L1:
            case SKYPLOT_BASE_L2:
            case SKYPLOT_BASE_L5:
                assert statusViewWidget != null;
                statusViewWidget.Sky.setStats(mBaseObservationStatus);
                break;
            case SKYPLOT_ROVER_L1:
            case SKYPLOT_ROVER_L2:
            case SKYPLOT_ROVER_L5:
                assert statusViewWidget != null;
                statusViewWidget.Sky.setStats(mRoverObservationStatus);
                break;
            case SNR:
            case SNR_L1:
            case SNR_L2:
            case SNR_L5:
                assert statusViewWidget != null;
                statusViewWidget.Snr1.setStats(mRoverObservationStatus);
                statusViewWidget.Snr2.setStats(mBaseObservationStatus);
                break;
            default:
                throw new IllegalStateException("Unknown status view: " + mCurrentStatusView);
        }
    }

    private void clearStatusUI() {
        binding.streamStatus.setText("");
        binding.streamIndicatorsView.setStats(mStreamStatus, RtkServerStreamStatus.STATE_CLOSE);
        binding.solutionView.setStats(new RtkControlResult());
        binding.gtimeView.setTime(new GTime());

        mRoverObservationStatus.clear();
        mBaseObservationStatus.clear();
    }

    private void updateGTimeFormat(SharedPreferences prefs) {
        try {
            String timeFormat = prefs.getString(PREF_TIME_FORMAT, null);
            if (timeFormat != null) {
                binding.gtimeView.setTimeFormat(GTimeView.Format.valueOf(timeFormat));
            }
        } catch (Exception e) {
            Log.e(TAG, "updateGTimeFormat: " + e.getMessage());
        }
    }

    private void updateSolutionFormat(SharedPreferences prefs) {
        try {
            String solutionFormat = prefs.getString(PREF_SOLUTION_FORMAT, null);
            if (solutionFormat != null) {
                binding.solutionView.setFormat(Format.valueOf(solutionFormat));
            }
        } catch (Exception e) {
            Log.e(TAG, "updateSolutionFormat: " + e.getMessage());
        }
    }

    public enum StatusView {

        SNR(R.string.status_view_snr),

        SNR_L1(R.string.status_view_snr_l1),

        SNR_L2(R.string.status_view_snr_l2),

        SNR_L5(R.string.status_view_snr_l5),

        SKYPLOT_ROVER_L1(R.string.status_view_skyplot_rover_l1),

        SKYPLOT_ROVER_L2(R.string.status_view_skyplot_rover_l2),

        SKYPLOT_ROVER_L5(R.string.status_view_skyplot_rover_l5),

        SKYPLOT_BASE_L1(R.string.status_view_skyplot_base_l1),

        SKYPLOT_BASE_L2(R.string.status_view_skyplot_base_l2),

        SKYPLOT_BASE_L5(R.string.status_view_skyplot_base_l5),

        //BASELINE(R.string.status_view_baseline)

        ;

        final int mTitleId;

        StatusView(int titleId) {
            mTitleId = titleId;
        }
    }

    public static class SelectTimeFormatDialog extends DialogFragment {

        public static SelectTimeFormatDialog newInstance(GTimeView.Format selectedFormat) {
            SelectTimeFormatDialog f = new SelectTimeFormatDialog();

            Bundle args = new Bundle();
            args.putString("selectedFormat", selectedFormat.name());
            f.setArguments(args);

            return f;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final GTimeView.Format[] formatValues;
            final CharSequence[] items;
            final AlertDialog.Builder builder;
            final GTimeView.Format selected;

            formatValues = GTimeView.Format.values();
            items = new CharSequence[formatValues.length];

            for (int i=0; i<formatValues.length; ++i) {
                items[i] = getString(formatValues[i].getDescriptionResId());
            }

            assert getArguments() != null;
            selected = GTimeView.Format.valueOf(getArguments().getString("selectedFormat"));

            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.menu_select_gtime_format);
            builder.setSingleChoiceItems(items, selected.ordinal(),
                    (dialog, which) -> setNewFormat(formatValues[which]));

            return builder.create();
        }

        void setNewFormat(GTimeView.Format newFormat) {
            final SharedPreferences prefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_TIME_FORMAT, newFormat.name());
            editor.apply();
            dismiss();
        }
    }

    public static class SelectSolutionViewFormatDialog extends DialogFragment {

        public static SelectSolutionViewFormatDialog newInstance(SolutionView.Format selectedFormat) {
            SelectSolutionViewFormatDialog f = new SelectSolutionViewFormatDialog();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putString("selectedFormat", selectedFormat.name());
            f.setArguments(args);

            return f;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SolutionView.Format[] formatValues;
            final CharSequence[] items;
            final AlertDialog.Builder builder;
            final SolutionView.Format selected;

            formatValues = SolutionView.Format.values();
            items = new CharSequence[formatValues.length];

            for (int i=0; i<formatValues.length; ++i) {
                items[i] = getString(formatValues[i].getDescriptionResId());
            }

            assert getArguments() != null;
            selected = SolutionView.Format.valueOf(getArguments().getString("selectedFormat"));

            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.menu_select_solution_format);
            builder.setSingleChoiceItems(items, selected.ordinal(),
                    (dialog, which) -> setNewFormat(formatValues[which]));

            return builder.create();
        }

        void setNewFormat(SolutionView.Format newFormat) {
            final SharedPreferences prefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_SOLUTION_FORMAT, newFormat.name());
            editor.apply();
            dismiss();
        }
    }

}