package gpsplus.rtkgps.settings;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.MainActivity;
import gpsplus.rtkgps.R;
import gpsplus.rtklib.RtkServerSettings.TransportSettings;
import gpsplus.rtklib.constants.StreamType;

public class StreamBluetoothFragment extends PreferenceFragmentCompat {

    private static final boolean DBG = BuildConfig.DEBUG;

    private static final String KEY_DEVICE_ADDRESS = "stream_bluetooth_address";
    private static final String KEY_DEVICE_NAME = "stream_bluetooth_name";

    private String mSharedPrefsName;

    private BluetoothAdapter mBluetoothAdapter;

    private ListPreference deviceSelectorPref;

    // Permissions launcher for Android 12+ runtime Bluetooth permissions
    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    reloadSummaries();
                } else {
                    if (deviceSelectorPref != null) {
                        deviceSelectorPref.setEnabled(false);
                        deviceSelectorPref.setSummary("Bluetooth permissions are required.");
                    }
                }
            });

    public static final class Value implements TransportSettings {

        public static final String ADDRESS_DEVICE_IS_NOT_SELECTED = "";

        private String address;
        private String name;
        private String mPath;

        public Value() {
            address = ADDRESS_DEVICE_IS_NOT_SELECTED;
            name = ADDRESS_DEVICE_IS_NOT_SELECTED;
        }

        Value(String address, String name) {
            this.address = address;
            this.name = name;
        }

        @NonNull
        public static String bluetoothLocalSocketName(@NonNull String ignoredAddress, String stream) {
            return "bt_" + stream; // + "_" + address.replaceAll("\\W", "_");
        }

        public Value setAddress(@NonNull String address) {
            this.address = address.toUpperCase();
            this.name = address;
            this.mPath = null;
            return this;
        }

        @Override
        public StreamType getType() {
            return StreamType.BLUETOOTH;
        }

        public String getAddress() {
            return address.toUpperCase();
        }

        public String getName() {
            return name;
        }

        @Override
        public String getPath() {
            if (mPath == null) throw new IllegalStateException("Path not initialized. Call updatePath()");
            return mPath;
        }

        public void updatePath(Context context, String sharedPrefsName) {
            mPath = MainActivity.getLocalSocketPath(context,
                    bluetoothLocalSocketName(address, sharedPrefsName)).getAbsolutePath();
        }

        @Override
        public Value copy() {
            Value v = new Value();
            v.address = address;
            v.name = name;
            v.mPath = mPath;
            return v;
        }
    }

    public StreamBluetoothFragment() {
        super();
        mSharedPrefsName = StreamBluetoothFragment.class.getSimpleName();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DBG) Log.v(mSharedPrefsName, "onCreate()");

        Bundle arguments = getArguments();
        if (arguments == null || !arguments.containsKey(StreamDialogActivity.ARG_SHARED_PREFS_NAME)) {
            throw new IllegalArgumentException("ARG_SHARED_PREFS_NAME argument not defined");
        }

        mSharedPrefsName = arguments.getString(StreamDialogActivity.ARG_SHARED_PREFS_NAME);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        getPreferenceManager().setSharedPreferencesName(mSharedPrefsName);

        if (savedInstanceState == null) {
            askToTurnOnBluetooth();
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.stream_bluetooth_settings, rootKey);

        deviceSelectorPref = findPreference(KEY_DEVICE_ADDRESS);
        if (deviceSelectorPref == null) {
            if (DBG) Log.e(mSharedPrefsName, "Preference with key " + KEY_DEVICE_ADDRESS + " not found");
            return;
        }

        deviceSelectorPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (mBluetoothAdapter == null) return false;

            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(newValue.toString());
            String name = newValue.toString();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
                    String deviceName = device.getName();
                    if (deviceName != null) name = deviceName;
                } else {
                    Log.w(mSharedPrefsName, "BLUETOOTH_CONNECT permission not granted; cannot read device name");
                }
            } else {
                String deviceName = device.getName();
                if (deviceName != null) name = deviceName;
            }

            SharedPreferences.Editor editor = Objects.requireNonNull(preference.getSharedPreferences()).edit();
            editor.putString(KEY_DEVICE_NAME, name);
            editor.apply();

            return true;
        });

        checkPermissionsAndLoadDevices();
    }

    private void askToTurnOnBluetooth() {
        if (mBluetoothAdapter == null) return;
        if (mBluetoothAdapter.isEnabled()) return;

        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivity(intent);
    }

    private void checkPermissionsAndLoadDevices() {
        if (mBluetoothAdapter == null) {
            if (deviceSelectorPref != null) {
                deviceSelectorPref.setEnabled(false);
                deviceSelectorPref.setSummary("Bluetooth is not supported on this device.");
            }
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            if (deviceSelectorPref != null) {
                deviceSelectorPref.setEnabled(false);
                deviceSelectorPref.setSummary(getString(R.string.bluetooth_disabled_summary));
            }
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires runtime permissions for Bluetooth
            List<String> neededPermissions = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }

            if (!neededPermissions.isEmpty()) {
                requestPermissionsLauncher.launch(neededPermissions.toArray(new String[0]));
                return;
            }
        }

        reloadSummaries();
    }

    void reloadSummaries() {
        if (deviceSelectorPref == null) return;

        Set<BluetoothDevice> pairedDevicesSet;

        boolean hasBluetoothConnectPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED;

        if (!mBluetoothAdapter.isEnabled() || !hasBluetoothConnectPermission) {
            pairedDevicesSet = null;
        } else {
            pairedDevicesSet = mBluetoothAdapter.getBondedDevices();
        }

        if (pairedDevicesSet == null || pairedDevicesSet.isEmpty()) {
            deviceSelectorPref.setEnabled(false);
            deviceSelectorPref.setSummary(getString(R.string.bluetooth_device_not_selected));
            return;
        }

        List<BluetoothDevice> pairedDevices = new ArrayList<>(pairedDevicesSet);
        pairedDevices.sort(new BluetoothDeviceComparator(requireContext()));

        String[] entries = new String[pairedDevices.size()];
        String[] values = new String[pairedDevices.size()];

        for (int i = 0; i < pairedDevices.size(); ++i) {
            BluetoothDevice dev = pairedDevices.get(i);
            String nameOrAddress;

            nameOrAddress = dev.getName();

            entries[i] = nameOrAddress != null ? nameOrAddress : dev.getAddress();
            values[i] = dev.getAddress();
        }

        deviceSelectorPref.setEnabled(true);
        deviceSelectorPref.setEntries(entries);
        deviceSelectorPref.setEntryValues(values);

        CharSequence currentEntry = deviceSelectorPref.getEntry();
        if (TextUtils.isEmpty(currentEntry)) {
            deviceSelectorPref.setSummary(getString(R.string.bluetooth_device_not_selected));
        } else {
            deviceSelectorPref.setSummary(currentEntry);
        }
    }


    private static class BluetoothDeviceComparator implements Comparator<BluetoothDevice> {

        private final Context context;

        BluetoothDeviceComparator(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public int compare(BluetoothDevice d1, BluetoothDevice d2) {
            boolean hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

            String name1 = hasPermission ? d1.getName() : null;
            String name2 = hasPermission ? d2.getName() : null;

            if (name1 == null) name1 = "";
            if (name2 == null) name2 = "";

            return name1.compareToIgnoreCase(name2);
        }
    }


    public static void setDefaultValue(Context ctx, String sharedPrefsName, Value value) {
        SharedPreferences prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_DEVICE_NAME, value.name)
                .putString(KEY_DEVICE_ADDRESS, value.address)
                .apply();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener =
            (sharedPreferences, key) -> reloadSummaries();

    private final BroadcastReceiver mBluetoothChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadSummaries();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (DBG) Log.v(mSharedPrefsName, "onResume()");
        reloadSummaries();

        Objects.requireNonNull(getPreferenceManager().getSharedPreferences())
                .registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);

        requireActivity().registerReceiver(mBluetoothChangeListener,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public void onPause() {
        if (DBG) Log.v(mSharedPrefsName, "onPause()");
        requireActivity().unregisterReceiver(mBluetoothChangeListener);
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences())
                .unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        super.onPause();
    }

    @NonNull
    public static Value readSettings(Context context, SharedPreferences prefs, String sharedPrefsName) {
        String address = prefs.getString(KEY_DEVICE_ADDRESS, null);
        if (address == null) throw new IllegalStateException("setDefaultValues() must be called");

        Value v = new Value(address, prefs.getString(KEY_DEVICE_NAME, ""));
        v.updatePath(context, sharedPrefsName);
        return v;
    }

    private static String getSummary(Resources r, @Nullable CharSequence deviceName) {
        if (TextUtils.isEmpty(deviceName)) {
            deviceName = r.getString(R.string.bluetooth_device_not_selected);
        }
        return "Bluetooth: " + deviceName;
    }

    public static String readSummary(Resources r, SharedPreferences prefs) {
        String name = prefs.getString(KEY_DEVICE_NAME, "");
        return getSummary(r, name);
    }
}