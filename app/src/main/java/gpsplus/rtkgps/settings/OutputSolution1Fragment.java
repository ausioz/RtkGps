package gpsplus.rtkgps.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtkgps.settings.widget.SolutionFormatPreference;
import gpsplus.rtkgps.settings.widget.StreamTypePreference;
import gpsplus.rtklib.RtkServerSettings;
import gpsplus.rtklib.SolutionOptions;
import gpsplus.rtklib.constants.SolutionFormat;
import gpsplus.rtklib.constants.StreamFormat;
import gpsplus.rtklib.constants.StreamType;

public class OutputSolution1Fragment extends PreferenceFragmentCompat {

    private static final boolean DBG = BuildConfig.DEBUG;

    public static final String SHARED_PREFS_NAME = "OutputSolution1";

    protected static final String KEY_ENABLE = "enable";
    protected static final String KEY_TYPE = "type";
    protected static final String KEY_FORMAT = "format";
    protected static final String KEY_STREAM_SETTINGS_BUTTON = "stream_settings_button";

    private final PreferenceChangeListener mPreferenceChangeListener = new PreferenceChangeListener();

    private final StreamType[] INPUT_STREAM_TYPES = new StreamType[]{
            StreamType.TCPCLI,
            StreamType.NTRIPSVR,
            StreamType.FILE
    };

    private static final StreamType DEFAULT_STREAM_TYPE = StreamType.FILE;

    private static final SolutionFormat[] SOLUTION_FORMATS = new SolutionFormat[]{
            SolutionFormat.LLH,
            SolutionFormat.XYZ,
            SolutionFormat.ENU,
            SolutionFormat.NMEA
    };

    private static final SolutionFormat DEFAULT_SOLUTION_FORMAT = SolutionFormat.LLH;

    private static final SettingsHelper.OutputStreamDefaults DEFAULTS = new SettingsHelper.OutputStreamDefaults();

    static {
        DEFAULTS.setFileClientDefaults(
                new StreamFileClientFragment.Value()
                        .setFilename("solution1_%Y%m%d%h%M%S.pos")
        );
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (DBG) Log.v(getSharedPreferenceName(), "onCreatePreferences() bundle: " + savedInstanceState);

        // Set shared preferences name
        getPreferenceManager().setSharedPreferencesName(getSharedPreferenceName());

        // Load preferences from resource
        setPreferencesFromResource(R.xml.output_stream_settings, rootKey);

        initPreferenceScreen();

        Preference streamSettingsButton = findPreference(KEY_STREAM_SETTINGS_BUTTON);
        if (streamSettingsButton != null) {
            streamSettingsButton.setOnPreferenceClickListener(preference -> {
                streamSettingsButtonClicked();
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DBG) Log.v(getSharedPreferenceName(), "onResume()");
        refresh();
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        if (DBG) Log.v(getSharedPreferenceName(), "onPause()");
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        super.onPause();
    }

    protected String getSharedPreferenceName() {
        return SHARED_PREFS_NAME;
    }

    protected void initPreferenceScreen() {
        if (DBG) Log.v(getSharedPreferenceName(), "initPreferenceScreen()");

        StreamTypePreference typePref = findPreference(KEY_TYPE);
        if (typePref != null) {
            typePref.setValues(INPUT_STREAM_TYPES);
            typePref.setDefaultValue(DEFAULT_STREAM_TYPE);
        }

        SolutionFormatPreference formatPref = findPreference(KEY_FORMAT);
        if (formatPref != null) {
            formatPref.setValues(SOLUTION_FORMATS);
            formatPref.setDefaultValue(DEFAULT_SOLUTION_FORMAT);
        }
    }

    protected void streamSettingsButtonClicked() {
        StreamTypePreference typePref = findPreference(KEY_TYPE);
        if (typePref == null || typePref.getValueT() == null) return;

        Intent intent = new Intent(requireContext(), StreamDialogActivity.class);

        Bundle fragmentArgs = new Bundle(1);
        fragmentArgs.putString(StreamDialogActivity.ARG_SHARED_PREFS_NAME, getSharedPreferenceName());

        intent.putExtra(StreamDialogActivity.ARG_FRAGMENT_ARGUMENTS, fragmentArgs);
        intent.putExtra(StreamDialogActivity.ARG_FRAGMENT_TYPE, typePref.getValueT().name());

        startActivity(intent);
    }

    private void refresh() {
        if (DBG) Log.v(getSharedPreferenceName(), "refresh()");

        StreamTypePreference typePref = findPreference(KEY_TYPE);
        SolutionFormatPreference formatPref = findPreference(KEY_FORMAT);
        Preference settingsPref = findPreference(KEY_STREAM_SETTINGS_BUTTON);

        if (typePref != null && typePref.getValueT() != null) {
            typePref.setSummary(getString(typePref.getValueT().getNameResId()));
        }
        if (formatPref != null && formatPref.getValueT() != null) {
            formatPref.setSummary(getString(formatPref.getValueT().getNameResId()));
        }
        if (settingsPref != null) {
            settingsPref.setSummary(SettingsHelper.readOutputStreamSumary(
                    getResources(),
                    Objects.requireNonNull(getPreferenceManager().getSharedPreferences())
            ));
        }
    }

    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            refresh();
        }
    }

    protected void setDefaultValues(boolean force) {
        if (DBG) Log.v(getSharedPreferenceName(), "setDefaultValues()");

        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

        boolean needUpdate = force || !Objects.requireNonNull(prefs).contains(KEY_ENABLE);

        if (needUpdate) {
            assert prefs != null;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_ENABLE, false)
                    .putString(KEY_TYPE, StreamType.NTRIPSVR.name())
                    .putString(KEY_FORMAT, StreamFormat.RTCM3.name());
            editor.apply();
        }
    }

    @NonNull
    public static RtkServerSettings.OutputStream readPrefs(Context ctx, @NonNull SolutionOptions base) {
        return SettingsHelper.readOutputStreamPrefs(ctx, SHARED_PREFS_NAME, base);
    }

    public static void setDefaultValues(Context ctx, boolean force) {
        SettingsHelper.setOutputStreamDefaultValues(ctx, SHARED_PREFS_NAME, force, DEFAULTS);
    }
}