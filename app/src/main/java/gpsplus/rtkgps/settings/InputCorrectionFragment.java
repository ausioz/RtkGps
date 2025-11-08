package gpsplus.rtkgps.settings;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtkgps.settings.widget.StreamFormatPreference;
import gpsplus.rtkgps.settings.widget.StreamTypePreference;
import gpsplus.rtklib.RtkServerSettings.InputStream;
import gpsplus.rtklib.constants.StreamFormat;
import gpsplus.rtklib.constants.StreamType;

import javax.annotation.Nonnull;


public class InputCorrectionFragment extends InputRoverFragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = InputCorrectionFragment.class.getSimpleName();

    public static final String SHARED_PREFS_NAME = "InputCorrection";

    private static final StreamType[] CORRECTION_STREAM_TYPES = new StreamType[]{
            StreamType.TCPCLI, StreamType.NTRIPCLI, StreamType.FILE
    };

    private static final StreamType DEFAULT_STREAM_TYPE = StreamType.NTRIPCLI;

    private static final StreamFormat[] CORRECTION_STREAM_FORMATS = new StreamFormat[]{
            StreamFormat.RTCM2, StreamFormat.RTCM3, StreamFormat.OEM3, StreamFormat.OEM4,
            StreamFormat.UBX, StreamFormat.RT17, StreamFormat.SS2, StreamFormat.LEXR,
            StreamFormat.SEPT, StreamFormat.CRES, StreamFormat.STQ, StreamFormat.GW10,
            StreamFormat.JAVAD, StreamFormat.NVS, StreamFormat.BINEX, StreamFormat.SP3
    };

    private static final StreamFormat DEFAULT_STREAM_FORMAT = StreamFormat.RTCM3;

    private static final SettingsHelper.InputStreamDefaults DEFAULTS = new SettingsHelper.InputStreamDefaults();

    static {
        DEFAULTS.setEnabled(false)
                .setFileClientDefaults(
                        new StreamFileClientFragment.Value().setFilename("input_correction.log")
                );
    }

    public InputCorrectionFragment() {
        super();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.input_stream_settings_correction, rootKey);
        initPreferenceScreen();
        if (DBG) Log.v(TAG, "onCreatePreferences()");

        Preference enablePref = findPreference(KEY_ENABLE);
        if (enablePref != null) {
            enablePref.setTitle(R.string.input_streams_settings_enable_correction_title);
        }

        StreamTypePreference typePref = findPreference(KEY_TYPE);
        if (typePref != null) {
            typePref.setTitle(R.string.input_streams_settings_correction_tab_title);
            typePref.setValues(CORRECTION_STREAM_TYPES);
            typePref.setDefaultValue(DEFAULT_STREAM_TYPE);
        }

        StreamFormatPreference formatPref = findPreference(KEY_FORMAT);
        if (formatPref != null) {
            formatPref.setValues(CORRECTION_STREAM_FORMATS);
            formatPref.setDefaultValue(DEFAULT_STREAM_FORMAT);
        }
    }

    @Override
    protected String getSharedPreferenceName() {
        return SHARED_PREFS_NAME;
    }

    @Nonnull
    public static InputStream readPrefs(Context ctx) {
        return SettingsHelper.readInputStreamPrefs(ctx, SHARED_PREFS_NAME);
    }

    public static void setDefaultValues(Context ctx, boolean force) {
        SettingsHelper.setInputStreamDefaultValues(ctx, SHARED_PREFS_NAME, force, DEFAULTS);
    }
}
