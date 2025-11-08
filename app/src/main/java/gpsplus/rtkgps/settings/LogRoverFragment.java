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

import javax.annotation.Nonnull;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtkgps.settings.widget.StreamTypePreference;
import gpsplus.rtklib.RtkServerSettings.LogStream;
import gpsplus.rtklib.constants.StreamType;


public class LogRoverFragment extends PreferenceFragmentCompat {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = LogRoverFragment.class.getSimpleName();

    public static final String SHARED_PREFS_NAME = "LogRover";

    public static final String KEY_ENABLE = "enable";
    public static final String KEY_TYPE = "type";
    private static final String KEY_STREAM_SETTINGS_BUTTON = "stream_settings_button";

    private static final StreamType[] INPUT_STREAM_TYPES = {
            StreamType.TCPCLI,
            StreamType.NTRIPSVR,
            StreamType.FILE
    };

    private static final StreamType DEFAULT_STREAM_TYPE = StreamType.FILE;

    private static final SettingsHelper.LogStreamDefaults DEFAULTS = new SettingsHelper.LogStreamDefaults();

    static {
        DEFAULTS.setFileClientDefaults(
                new StreamFileClientFragment.Value().setFilename("rover_%Y%m%d%h%M%S.log")
        );
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            (sharedPreferences, key) -> refresh();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.log_stream_settings, rootKey);
        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
        initPreferenceScreen();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DBG) Log.v(TAG, "onResume()");
        refresh();
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences())
                .registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onPause() {
        if (DBG) Log.v(TAG, "onPause()");
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences())
                .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        super.onPause();
    }

    protected void initPreferenceScreen() {
        if (DBG) Log.v(TAG, "initPreferenceScreen()");

        StreamTypePreference typePref = findPreference(KEY_TYPE);
        if (typePref != null) {
            typePref.setValues(INPUT_STREAM_TYPES);
            typePref.setDefaultValue(DEFAULT_STREAM_TYPE);
        }

        Preference streamSettingsButton = findPreference(KEY_STREAM_SETTINGS_BUTTON);
        if (streamSettingsButton != null) {
            streamSettingsButton.setOnPreferenceClickListener(preference -> {
                streamSettingsButtonClicked();
                return true;
            });
        }
    }

    protected String getSharedPreferenceName() {
        return SHARED_PREFS_NAME;
    }

    protected void streamSettingsButtonClicked() {
        StreamTypePreference typePref = findPreference(KEY_TYPE);
        if (typePref == null || typePref.getValueT() == null) return;

        Intent intent = new Intent(requireContext(), StreamDialogActivity.class);

        Bundle args = new Bundle();
        args.putString(StreamDialogActivity.ARG_SHARED_PREFS_NAME, getSharedPreferenceName());

        intent.putExtra(StreamDialogActivity.ARG_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(StreamDialogActivity.ARG_FRAGMENT_TYPE, typePref.getValueT().name());

        startActivity(intent);
    }

    private void refresh() {
        if (DBG) Log.v(TAG, "refresh()");

        StreamTypePreference typePref = findPreference(KEY_TYPE);
        if (typePref != null && typePref.getValueT() != null) {
            typePref.setSummary(getString(typePref.getValueT().getNameResId()));
        }

        Preference settingsPref = findPreference(KEY_STREAM_SETTINGS_BUTTON);
        if (settingsPref != null) {
            settingsPref.setSummary(
                    SettingsHelper.readLogStreamSumary(
                            getResources(),
                            Objects.requireNonNull(getPreferenceManager().getSharedPreferences())
                    )
            );
        }
    }

    @Nonnull
    public static LogStream readPrefs(@NonNull Context ctx) {
        return SettingsHelper.readLogStreamPrefs(ctx, SHARED_PREFS_NAME);
    }

    public static void setDefaultValues(@NonNull Context ctx, boolean force) {
        SettingsHelper.setLogStreamDefaultValues(ctx, SHARED_PREFS_NAME, force, DEFAULTS);
    }
}