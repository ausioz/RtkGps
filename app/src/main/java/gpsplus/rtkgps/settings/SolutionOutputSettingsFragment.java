package gpsplus.rtkgps.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtklib.SolutionOptions;
import gpsplus.rtklib.constants.GeoidModel;
import gpsplus.rtklib.constants.TimeSystem;

public class SolutionOutputSettingsFragment extends PreferenceFragmentCompat {

    @SuppressWarnings("unused")
    private static final boolean DBG = BuildConfig.DEBUG;
    static final String TAG = SolutionOutputSettingsFragment.class.getSimpleName();

    public static final String SHARED_PREFS_NAME = "SolutionOutputSettings";

    public static final String KEY_OUTPUT_HEADER = "output_header";
    public static final String KEY_TIME_FORMAT = "time_format";
    public static final String KEY_LAT_LON_FORMAT = "lat_lon_format";
    public static final String KEY_FIELD_SEPARATOR = "field_separator";
    public static final String KEY_HEIGHT = "height";
    public static final String KEY_GEOID_MODEL = "geoid_model";
    public static final String KEY_NMEA_INTERVAL_RMC_GGA = "nmea_interval_rmc_gga";
    public static final String KEY_NMEA_INTERVAL_GSA_GSV = "nmea_interval_gsa_gsv";
    public static final String KEY_OUTPUT_SOLUTION_STATUS = "output_solution_status";
    public static final String KEY_DEBUG_TRACE = "debug_trace";
    public static final String KEY_OUTPUT_MOCK_LOCATION = "output_mocklocation";
    public static final String KEY_ENABLE_TEST_MODE = "enable_testmode";
    public static final String KEY_CUSTOM_PROJ4 = "customproj4";

    private CheckBoxPreference mOutputHeaderPref;
    private ListPreference mTimeFormatPref;
    private ListPreference mLatLonFormatPref;
    private EditTextPreference mFieldSeparatorPref;
    private ListPreference mHeightPref;
    private ListPreference mGeoidModelPref;
    private EditTextPreference mNmeaIntervalRmcPref;
    private EditTextPreference mNmeaIntervalGsaPref;
    private ListPreference mOutputSolutionStatusPref;
    private ListPreference mDebugTracePref;


    private SolutionOptions mSolutionOptions;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);

        addPreferencesFromResource(R.xml.solution_output_settings);

        mSolutionOptions = readPrefs(requireActivity());
        requireActivity().setTitle(R.string.navdraw_item_solution_options);
        initSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadSummaries();
    }

    public static SolutionOptions readPrefs(Context ctx) {
        final SolutionOptions opts;
        final SharedPreferences prefs;
        String v;
        mSzEllipsoidal = ctx.getResources().getStringArray(R.array.solopt_height_entries)[0];
        mSzGeodetic = ctx.getResources().getStringArray(R.array.solopt_height_entries)[1];

        opts = new SolutionOptions();
        prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Activity.MODE_PRIVATE);

        try {
            opts.setOutHead(prefs.getBoolean(KEY_OUTPUT_HEADER, opts.getOutHead()));
        } catch (ClassCastException cce) {
            Log.e(TAG, "readPrefs: " + cce.getMessage());
        }

        try {
            v = prefs.getString(KEY_TIME_FORMAT, null);
            if (v != null) opts.setTimeSystem(TimeSystem.valueOf(v));
        } catch (IllegalArgumentException | ClassCastException iae) {
            Log.e(TAG, "readPrefs: " + iae.getMessage());
        }

        try {
            v = prefs.getString(KEY_LAT_LON_FORMAT, null);
            if (v != null) {
                if (!"degree".equals(v) && !"dms".equals(v)) {
                    throw new IllegalArgumentException("Wrong lat_lon format");
                }
                opts.setLatLonFormat("degree".equals(v) ? 0 : 1);
            }
        } catch (IllegalArgumentException | ClassCastException iae) {
            Log.e(TAG, "readPrefs: " + iae.getMessage());
        }

        try {
            v = prefs.getString(KEY_FIELD_SEPARATOR, null);
            if (v != null) opts.setFieldSeparator(v);
        } catch (IllegalArgumentException | ClassCastException iae) {
            Log.e(TAG, "readPrefs: " + iae.getMessage());
        }

        try {
            v = prefs.getString(KEY_HEIGHT, null);
            if (v != null) {

                if (!mSzEllipsoidal.equals(v) && !mSzGeodetic.equals(v)) {
                    throw new IllegalArgumentException("Wrong height");
                }
                opts.setIsEllipsoidalHeight(mSzEllipsoidal.equals(v));
            }
        } catch (IllegalArgumentException | ClassCastException iae) {
            Log.e(TAG, "readPrefs: " + iae.getMessage());
        }

        try {
            v = prefs.getString(KEY_GEOID_MODEL, null);
            if (v != null) opts.setGeoidModel(GeoidModel.valueOf(v));
        } catch (IllegalArgumentException | ClassCastException iae) {
            Log.e(TAG, "readPrefs: " + iae.getMessage());
        }

        try {
            v = prefs.getString(KEY_NMEA_INTERVAL_RMC_GGA, null);
            if (v != null) opts.setNmeaIntervalRmcGga(Double.parseDouble(v));
        } catch (ClassCastException | IllegalArgumentException cce) {
            Log.e(TAG, "readPrefs: " + cce.getMessage());
        }

        try {
            v = prefs.getString(KEY_NMEA_INTERVAL_GSA_GSV, null);
            if (v != null) opts.setNmeaIntervalGsv(Double.parseDouble(v));
        } catch (ClassCastException | IllegalArgumentException cce) {
            Log.e(TAG, "readPrefs: " + cce.getMessage());
        }

        try {
            v = prefs.getString(KEY_OUTPUT_SOLUTION_STATUS, null);
            if (v != null) opts.setSolutionStatsLevel(Integer.parseInt(v));
        } catch (ClassCastException | IllegalArgumentException cce) {
            Log.e(TAG, "readPrefs: " + cce.getMessage());
        }

        try {
            v = prefs.getString(KEY_DEBUG_TRACE, null);
            if (v != null) opts.setDebugTraceLevel(Integer.parseInt(v));
        } catch (ClassCastException | IllegalArgumentException cce) {
            Log.e(TAG, "readPrefs: " + cce.getMessage());
        }


        return opts;
    }

    private void initSettings() {
        final Resources r = getResources();

        mOutputHeaderPref = findPreference(KEY_OUTPUT_HEADER);
        assert mOutputHeaderPref != null;
        mOutputHeaderPref.setChecked(mSolutionOptions.getOutHead());
        mOutputHeaderPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mTimeFormatPref = findPreference(KEY_TIME_FORMAT);
        assert mTimeFormatPref != null;
        mTimeFormatPref.setEntries(TimeSystem.getEntries(r));
        mTimeFormatPref.setEntryValues(TimeSystem.getEntryValues());
        mTimeFormatPref.setValue(mSolutionOptions.getTimeSystem().name());
        mTimeFormatPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mLatLonFormatPref = findPreference(KEY_LAT_LON_FORMAT);
        assert mLatLonFormatPref != null;
        mLatLonFormatPref.setValue(mSolutionOptions.getLatLonFormat() == 0 ? "degree" : "dms");
        mLatLonFormatPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mFieldSeparatorPref = findPreference(KEY_FIELD_SEPARATOR);
        assert mFieldSeparatorPref != null;
        mFieldSeparatorPref.setText(mSolutionOptions.getFieldSeparator());
        mFieldSeparatorPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mHeightPref = findPreference(KEY_HEIGHT);
        assert mHeightPref != null;
        mHeightPref.setValue(mSolutionOptions.isEllipsoidalHeight() ? mSzEllipsoidal : mSzGeodetic);
        mHeightPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mGeoidModelPref = findPreference(KEY_GEOID_MODEL);
        assert mGeoidModelPref != null;
        mGeoidModelPref.setEntries(GeoidModel.getEntries(r));
        mGeoidModelPref.setEntryValues(GeoidModel.getEntryValues());
        mGeoidModelPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        // TODO
        mGeoidModelPref.setEnabled(true);

        mNmeaIntervalGsaPref = findPreference(KEY_NMEA_INTERVAL_GSA_GSV);
        assert mNmeaIntervalGsaPref != null;
        mNmeaIntervalGsaPref.setText(String.valueOf(mSolutionOptions.getNmeaIntervalGsv()));
        mNmeaIntervalGsaPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mNmeaIntervalRmcPref = findPreference(KEY_NMEA_INTERVAL_RMC_GGA);
        assert mNmeaIntervalRmcPref != null;
        mNmeaIntervalRmcPref.setText(String.valueOf(mSolutionOptions.getNmeaIntervalRmcGga()));
        mNmeaIntervalRmcPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mOutputSolutionStatusPref = findPreference(KEY_OUTPUT_SOLUTION_STATUS);
        assert mOutputSolutionStatusPref != null;
        mOutputSolutionStatusPref.setValue(String.valueOf(mSolutionOptions.getSolutionStatsLevel()));
        mOutputSolutionStatusPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

        mDebugTracePref = findPreference(KEY_DEBUG_TRACE);
        assert mDebugTracePref != null;
        mDebugTracePref.setValue(String.valueOf(mSolutionOptions.getDebugTraceLevel()));
        mDebugTracePref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

    }

    private void reloadSummaries() {
        final Resources r = getResources();
        CharSequence summary;

        summary = r.getString(mSolutionOptions.getTimeSystem().getNameResId());
        mTimeFormatPref.setSummary(summary);

        summary = r.getStringArray(R.array.solopt_lat_lon_format_entries)[mSolutionOptions.getLatLonFormat()];
        mLatLonFormatPref.setSummary(summary);

        mFieldSeparatorPref.setSummary(mSolutionOptions.getFieldSeparator());

        summary = r.getStringArray(R.array.solopt_height_entry_values)[mSolutionOptions.isEllipsoidalHeight() ? 0 : 1];
        mHeightPref.setSummary(summary);

        summary = r.getString(mSolutionOptions.getGeoidModel().getNameResId());
        mGeoidModelPref.setSummary(summary);

        mNmeaIntervalGsaPref.setSummary(String.valueOf(mSolutionOptions.getNmeaIntervalGsv()));

        mNmeaIntervalRmcPref.setSummary(String.valueOf(mSolutionOptions.getNmeaIntervalRmcGga()));

        summary = r.getStringArray(R.array.solopt_output_solution_status_entries)[mSolutionOptions.getSolutionStatsLevel()];
        mOutputSolutionStatusPref.setSummary(summary);

        summary = r.getStringArray(R.array.solopt_debug_trace_entries)[mSolutionOptions.getDebugTraceLevel()];
        mDebugTracePref.setSummary(summary);

    }

    private final OnPreferenceChangeListener mOnPreferenceChangeListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {

            if (mOutputHeaderPref.equals(preference)) {
                mSolutionOptions.setOutHead((Boolean) newValue);
            } else if (mTimeFormatPref.equals(preference)) {
                mSolutionOptions.setTimeSystem(TimeSystem.valueOf(String.valueOf(newValue)));
            } else if (mLatLonFormatPref.equals(preference)) {
                mSolutionOptions.setLatLonFormat(TextUtils.equals((CharSequence) newValue, "dms") ? 1 : 0);
            } else if (mFieldSeparatorPref.equals(preference)) {
                mSolutionOptions.setFieldSeparator(newValue.toString());
            } else if (mHeightPref.equals(preference)) {
                mSolutionOptions.setIsEllipsoidalHeight(TextUtils.equals((CharSequence) newValue, mSzEllipsoidal));
            } else if (mGeoidModelPref.equals(preference)) {
                mSolutionOptions.setGeoidModel(GeoidModel.valueOf(newValue.toString()));
            } else if (mNmeaIntervalRmcPref.equals(preference)) {
                mSolutionOptions.setNmeaIntervalRmcGga(Double.parseDouble(newValue.toString()));
            } else if (mNmeaIntervalGsaPref.equals(preference)) {
                mSolutionOptions.setNmeaIntervalGsv(Double.parseDouble(newValue.toString()));
            } else if (mOutputSolutionStatusPref.equals(preference)) {
                mSolutionOptions.setSolutionStatsLevel(Integer.parseInt(newValue.toString()));
            } else if (mDebugTracePref.equals(preference)) {
                mSolutionOptions.setDebugTraceLevel(Integer.parseInt(newValue.toString()));
            }
            reloadSummaries();
            return true;
        }
    };
    private static String mSzEllipsoidal;
    private static String mSzGeodetic;
}
