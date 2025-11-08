package gpsplus.rtkgps.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import javax.annotation.Nonnull;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtklib.RtkServerSettings.TransportSettings;
import gpsplus.rtklib.constants.StreamType;


public class StreamUdpClientFragment extends PreferenceFragmentCompat {

    private static final boolean DBG = BuildConfig.DEBUG;

    private static final String KEY_HOST = "stream_udp_client_host";
    private static final String KEY_PORT = "stream_udp_client_port";

    private final PreferenceChangeListener mPreferenceChangeListener;

    private String mSharedPrefsName;

    public static final class Value implements TransportSettings, Cloneable {
        private String host;
        private int port;

        public static final String DEFAULT_HOST = "localhost";
        public static final int DEFAULT_PORT = 46434;

        public Value() {
            host = DEFAULT_HOST;
            port = DEFAULT_PORT;
        }

        public Value setHost(@Nonnull String host) {
            this.host = host;
            return this;
        }

        public Value setPort(int port) {
            if (port <= 0 || port > 65535) throw new IllegalArgumentException();
            this.port = port;
            return this;
        }

        @Override
        public StreamType getType() {
            return StreamType.UDPCLI;
        }

        @Override
        public String getPath() {
            return SettingsHelper.encodeNtripTcpPath(
                    null,
                    null,
                    host,
                    String.valueOf(port),
                    null,
                    null
            );
        }

        @NonNull
        @Override
        protected Value clone() {
            try {
                return (Value)super.clone();
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Value copy() {
            return clone();
        }
    }

    public StreamUdpClientFragment() {
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
        addPreferencesFromResource(R.xml.stream_udp_client_settings);
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName, Value value) {
        final SharedPreferences prefs;
        prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

        prefs.edit()
            .putString(KEY_HOST, value.host)
            .putString(KEY_PORT, String.valueOf(value.port))
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
        };

        for (String key : keys) {
            Preference pref = findPreference(key);
            if (pref instanceof EditTextPreference) {
                EditTextPreference etp = (EditTextPreference) pref;
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
        String portStr = prefs.getString(KEY_PORT, String.valueOf(Value.DEFAULT_PORT));
        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) {
                port = Value.DEFAULT_PORT;
            }
        } catch (NumberFormatException e) {
            port = Value.DEFAULT_PORT;
        }

        String host = prefs.getString(KEY_HOST, Value.DEFAULT_HOST);
        if (host.isEmpty()) {
            host = Value.DEFAULT_HOST;
        }

        return new Value()
                .setHost(host)
                .setPort(port);
    }

    public static String readSummary(SharedPreferences prefs) {
        return "udp:" + readSettings(prefs).getPath();
    }


}
