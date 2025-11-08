package gpsplus.rtkgps.settings;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.EditTextPreference;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtklib.RtkServerSettings.TransportSettings;
import gpsplus.rtklib.constants.StreamType;

import javax.annotation.Nonnull;


public class StreamNtripClientFragment extends PreferenceFragmentCompat {

    private static final boolean DBG = BuildConfig.DEBUG;

    private static final String KEY_HOST = "stream_ntrip_client_host";
    private static final String KEY_PORT = "stream_ntrip_client_port";
    private static final String KEY_MOUNTPOINT = "stream_ntrip_client_mountpoint";
    private static final String KEY_USER = "stream_ntrip_client_user";
    private static final String KEY_PASSWORD = "stream_ntrip_client_password";

    private final PreferenceChangeListener mPreferenceChangeListener;

    private String mSharedPrefsName;

    public static final class Value implements TransportSettings, Cloneable {
        private @Nonnull String host;
        private int port;
        private @Nonnull String mountpoint;
        private @Nonnull String user;
        private @Nonnull String password;

        public static final String DEFAULT_HOST = "rt.igs-ip.net";
        public static final int DEFAULT_PORT = 2101;
        public static final String DEFAULT_MOUNTPOUNT = "BRST0";
        public static final String DEFAULT_USER = "request-user";
        public static final String DEFAULT_PASSWORD = "password";

        public Value() {
            host = DEFAULT_HOST;
            port = DEFAULT_PORT;
            mountpoint = DEFAULT_MOUNTPOUNT;
            user = DEFAULT_USER;
            password = DEFAULT_PASSWORD;
        }

        public Value setHost(@Nonnull String host) {
            this.host = host;
            return this;
        }

        public Value setPort(int port) {
            if (port <= 0 || port > 65535) {port=DEFAULT_PORT;}
            this.port = port;
            return this;
        }

        public Value setMountpoint(@Nonnull String mountpoint) {
            this.mountpoint = mountpoint;
            return this;
        }

        public Value setUser(@Nonnull String user) {
            this.user = user;
            return this;
        }

        public Value setPassword(@Nonnull String password) {
            this.password = password;
            return this;
        }

        @Override
        public StreamType getType() {
            return StreamType.NTRIPCLI;
        }

        @Override
        public String getPath() {
            return SettingsHelper.encodeNtripTcpPath(user, password, host,
                    String.valueOf(port), mountpoint, null);
        }

        public String getSummary() {
            return SettingsHelper.encodeNtripTcpPath(user,
                    password.isEmpty() ? null : "xxx",
                    host,
                    String.valueOf(port),
                    mountpoint,
                    null);
        }

        @NonNull
        @Override
        protected Value clone() {
            try {
                return (Value) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Value copy() {
            return clone();
        }

    }

    public StreamNtripClientFragment() {
        super();
        mPreferenceChangeListener = new PreferenceChangeListener();
        mSharedPrefsName = StreamNtripClientFragment.class.getSimpleName();
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        final Bundle arguments;

        arguments = getArguments();
        if (arguments == null || !arguments.containsKey(StreamDialogActivity.ARG_SHARED_PREFS_NAME)) {
            throw new IllegalArgumentException("ARG_SHARED_PREFFS_NAME argument not defined");
        }

        mSharedPrefsName = arguments.getString(StreamDialogActivity.ARG_SHARED_PREFS_NAME);

        if (DBG) Log.v(mSharedPrefsName, "onCreate()");

        getPreferenceManager().setSharedPreferencesName(mSharedPrefsName);

        initPreferenceScreen();
    }

    protected void initPreferenceScreen() {
        if (DBG) Log.v(mSharedPrefsName, "initPreferenceScreen()");
        addPreferencesFromResource(R.xml.stream_ntrip_client_settings);
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName, Value value) {
        final SharedPreferences prefs;
        prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

        prefs.edit()
            .putString(KEY_HOST, value.host)
            .putString(KEY_PORT, String.valueOf(value.port))
            .putString(KEY_MOUNTPOINT, value.mountpoint)
            .putString(KEY_USER, value.user)
            .putString(KEY_PASSWORD, value.password)
            .apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DBG) Log.v(mSharedPrefsName, "onResume()");
        reloadSummaries();
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        if (DBG) Log.v(mSharedPrefsName, "onPause()");
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        super.onPause();
    }

    void reloadSummaries() {
        String[] keys = {
                KEY_HOST,
                KEY_PORT,
                KEY_MOUNTPOINT,
                KEY_USER,
                KEY_PASSWORD
        };

        for (String key : keys) {
            EditTextPreference etp = findPreference(key);
            if (etp != null) {
                etp.setSummary(etp.getText());
            }
        }
    }

    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            reloadSummaries();
        }
    }


    public static Value readSettings(SharedPreferences prefs) {
        return new Value()
            .setUser(prefs.getString(KEY_USER, ""))
            .setPassword(prefs.getString(KEY_PASSWORD, ""))
            .setHost(prefs.getString(KEY_HOST, ""))
            .setPort(Integer.parseInt(prefs.getString(KEY_PORT, "0")))
            .setMountpoint(prefs.getString(KEY_MOUNTPOINT, ""));
    }

    public static String readSummary(SharedPreferences prefs) {
        return "ntrip://" + readSettings(prefs).getPath();
    }

}
