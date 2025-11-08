package gpsplus.rtkgps.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import android.util.Log;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtkgps.settings.widget.StreamFormatPreference;
import gpsplus.rtkgps.settings.widget.StreamTypePreference;
import gpsplus.rtklib.RtkCommon;
import gpsplus.rtklib.RtkServerSettings.InputStream;

import javax.annotation.Nonnull;


public class InputBaseFragment extends InputRoverFragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = InputBaseFragment.class.getSimpleName();

    public static final String SHARED_PREFS_NAME = "InputBase";

    protected static final String KEY_TRANSMIT_GPGGA_TO_BASE = "transmit_gpgga_to_base";
    protected static final String KEY_TRANSMIT_GPGGA_LAT = "transmit_gpgga_latitude";
    protected static final String KEY_TRANSMIT_GPGGA_LON = "transmit_gpgga_longitude";

    private static final SettingsHelper.InputStreamDefaults DEFAULTS = new SettingsHelper.InputStreamDefaults();

    static {
        DEFAULTS.setEnabled(false).setFileClientDefaults(new StreamFileClientFragment.Value().setFilename("input_base.rtcm3"));
    }

    public InputBaseFragment() {
        super();
    }

    @Override
    protected String getSharedPreferenceName() {
        return SHARED_PREFS_NAME;
    }

    @Override
    protected void initPreferenceScreen() {
        if (DBG) Log.v(getSharedPreferenceName(), "initPreferenceScreen()");

        addPreferencesFromResource(R.xml.input_stream_settings_base);

        Preference enablePref = findPreference(KEY_ENABLE);
        if (enablePref != null) {
            enablePref.setTitle(R.string.input_streams_settings_enable_base_title);
        }

        initStreamTypePref();

        StreamTypePreference typePref = findPreference(KEY_TYPE);
        if (typePref != null) {
            typePref.setTitle(R.string.input_streams_settings_base_tab_title);
        }

        StreamFormatPreference formatPref = findPreference(KEY_FORMAT);
        if (formatPref != null) {
            formatPref.setValues(INPUT_STREAM_FORMATS);
            formatPref.setDefaultValue(DEFAULT_STREAM_FORMAT);
        }
    }

    @Override
    protected void refresh() {
        super.refresh();

        ListPreference transmitPref = findPreference(KEY_TRANSMIT_GPGGA_TO_BASE);
        EditTextPreference gpggaLatPref = findPreference(KEY_TRANSMIT_GPGGA_LAT);
        EditTextPreference gpggaLonPref = findPreference(KEY_TRANSMIT_GPGGA_LON);
        TwoStatePreference enablePref = findPreference(KEY_ENABLE);

        if (transmitPref == null || gpggaLatPref == null || gpggaLonPref == null || enablePref == null) {
            if (DBG) Log.w(TAG, "One or more preferences are missing in refresh()");
            return;
        }

        boolean enabled = enablePref.isChecked();
        boolean transmitLatLon = "1".equals(transmitPref.getValue());

        gpggaLatPref.setEnabled(transmitLatLon && enabled);
        gpggaLonPref.setEnabled(transmitLatLon && enabled);

        transmitPref.setSummary(transmitPref.getEntry());
        gpggaLatPref.setSummary(gpggaLatPref.getText());
        gpggaLonPref.setSummary(gpggaLonPref.getText());
    }

    @Override
    protected int stationPositionButtonDisabledCause() {
        return mProcessingOptions.getPositioningMode().isRelative() ? 0 : R.string.station_position_for_relative_mode;
    }

    @Override
    protected void stationPositionButtonClicked() {
        Intent intent = new Intent(requireActivity(), StationPositionActivity.class);
        intent.putExtra(StationPositionActivity.ARG_SHARED_PREFS_NAME, getSharedPreferenceName());
        intent.putExtra(StationPositionActivity.ARG_HIDE_USE_RTCM, false);
        startActivity(intent);
    }

    @Nonnull
    public static InputStream readPrefs(Context ctx) {
        InputStream stream = SettingsHelper.readInputStreamPrefs(ctx, SHARED_PREFS_NAME);
        SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        int type = Integer.parseInt(prefs.getString(KEY_TRANSMIT_GPGGA_TO_BASE, "0"));
        double lat = Double.parseDouble(prefs.getString(KEY_TRANSMIT_GPGGA_LAT, "0"));
        double lon = Double.parseDouble(prefs.getString(KEY_TRANSMIT_GPGGA_LON, "0"));

        stream.setTransmitNmeaPosition(type, RtkCommon.pos2ecef(Math.toRadians(lat), Math.toRadians(lon), 0, null));
        return stream;
    }

    public static void setDefaultValues(Context ctx, boolean force) {
        SettingsHelper.setInputStreamDefaultValues(ctx, SHARED_PREFS_NAME, force, DEFAULTS);

        SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        boolean needUpdate = force || !prefs.contains(KEY_TRANSMIT_GPGGA_TO_BASE);

        if (needUpdate) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_TRANSMIT_GPGGA_TO_BASE, "0");
            editor.putString(KEY_TRANSMIT_GPGGA_LAT, "0.0");
            editor.putString(KEY_TRANSMIT_GPGGA_LON, "0.0");
            editor.apply();
        }
    }
}

