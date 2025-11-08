package gpsplus.rtkgps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

import gpsplus.rtkgps.databinding.ActivityMainBinding;
import gpsplus.rtkgps.databinding.NavigationDrawerBinding;
import gpsplus.rtkgps.settings.NTRIPCasterSettingsFragment;
import gpsplus.rtkgps.settings.ProcessingOptions1Fragment;
import gpsplus.rtkgps.settings.SettingsActivity;
import gpsplus.rtkgps.settings.SettingsHelper;
import gpsplus.rtkgps.settings.SolutionOutputSettingsFragment;
import gpsplus.rtkgps.settings.StreamSettingsActivity;
import gpsplus.rtkgps.settings.StreamSettingsPagerAdapter;
import gpsplus.rtkgps.utils.ChangeLog;
import gpsplus.rtkgps.utils.GpsTime;
import gpsplus.rtkgps.utils.ZipHelper;

public class MainActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    private static final boolean DBG = BuildConfig.DEBUG;
    //    public static final int REQUEST_LINK_TO_DBX = 2654;
    static final String TAG = MainActivity.class.getSimpleName();

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    public static final String APP_KEY = "6ffqsgh47v9y5dc";
    public static final String APP_SECRET = "hfmsbkv4ktyl60h";
    public static final String RTKGPS_CHILD_DIRECTORY = "RtkGps/";
//    private DbxAccountManager mDbxAcctMgr;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    RtkNaviService mRtkService;
    boolean mRtkServiceBound = false;
    private static DemoModeLocation mDemoModeLocation;
    private String mSessionCode;
    String m_PointName = "POINT";
    boolean m_bRet_pointName = false;

    private ActivityMainBinding binding;
    private ActionBarDrawerToggle drawerToggle;

    private int mNavDraverSelectedItem;
    private static String mApplicationDirectory = "";
    NavigationDrawerBinding navBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MultiplePermissionsListener dialogMultiplePermissionsListener =
                DialogOnAnyDeniedMultiplePermissionsListener.Builder
                        .withContext(this)
                        .withTitle(R.string.permissions_request_title)
                        .withMessage(R.string.permissions_request_message)
                        .withButtonText(android.R.string.ok)
                        .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            Dexter.withContext(this)
                    .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.POST_NOTIFICATIONS
                    )
                    .withListener(dialogMultiplePermissionsListener)
                    .check();

            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            Dexter.withContext(this)
                    .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH_CONNECT
                    )
                    .withListener(dialogMultiplePermissionsListener)
                    .check();

            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }

        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            // Android 11 (API 30)
            Dexter.withContext(this)
                    .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH
                    )
                    .withListener(dialogMultiplePermissionsListener)
                    .check();

            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }

        } else {
            // Android 8–10 (API 26–29)
            Dexter.withContext(this)
                    .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    .withListener(dialogMultiplePermissionsListener)
                    .check();
        }

        PackageManager m = getPackageManager();
        String s = getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            assert p.applicationInfo != null;
            MainActivity.mApplicationDirectory = p.applicationInfo.dataDir;
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Error Package name not found ", e);
        }

        // copy assets/data in background thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                copyAssetsToApplicationDirectory();
                copyAssetsToWorkingDirectory();
            } catch (IOException e) {
                Log.e(TAG, "Background file copy error: " + e.getMessage());
            }
        });
        executor.shutdown();

//        mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);

        if (checkLocationPermission()) {
            initDemoModeLocation();
        } else {
            requestLocationPermission();
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        navBinding = NavigationDrawerBinding.bind(binding.navigationDrawer.getRoot());
        //toggleCasterSwitch();

        createDrawerToggle();

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        initNavDrawer();

        if (savedInstanceState == null) {
            SettingsHelper.setDefaultValues(this, false);
            proxyIfUsbAttached(getIntent());
            selectDrawerItem(R.id.navdraw_item_status);
        }

        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun())
            cl.getLogDialog().show();

    }

    private void initNavDrawer() {
        navBinding.navdrawItemMap.setOnClickListener(this::onNavDrawerItemClicked);
        navBinding.navdrawItemStatus.setOnClickListener(this::onNavDrawerItemClicked);
        navBinding.navdrawItemInputStreams.setOnClickListener(this::onNavDrawerItemClicked);
        navBinding.navdrawItemOutputStreams.setOnClickListener(this::onNavDrawerItemClicked);
        navBinding.navdrawItemLogStreams.setOnClickListener(this::onNavDrawerItemClicked);
        navBinding.navdrawItemProcessingOptions.setOnClickListener(this::onNavDrawerItemClicked);
        navBinding.navdrawItemSolutionOptions.setOnClickListener(this::onNavDrawerItemClicked);
        navBinding.navdrawItemSaveSettings.setOnClickListener(this::onNavDrawerItemClicked);
        navBinding.navdrawItemLoadSettings.setOnClickListener(this::onNavDrawerItemClicked);

        SwitchCompat serverSwitch = navBinding.navdrawServerSwitch;
        serverSwitch.setChecked(false);
        serverSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            binding.drawerLayout.closeDrawer(GravityCompat.START);

            if (isChecked) {
                startRtkService();
            } else {
                stopRtkService();
            }

            invalidateOptionsMenu();
        });

        /*mNavDrawerCasterSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            private NTRIPCaster mCaster = null;

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mDrawerLayout.closeDrawer(mNavDrawer);
                if (isChecked) {
                    if (mCaster == null)
                    {
                        mCaster = new NTRIPCaster(getFileStorageDirectory()+"/ntripcaster/conf");
                    }
                    mCaster.start(2101, "none");
                    //TEST
                }else {
                    if (getCasterBrutalEnding())
                    {
                        stopRtkService();
                        int ret = mCaster.stop(1);
                        android.os.Process.killProcess(android.os.Process.myPid()); //in case of not stopping
                    }else{
                        int ret = mCaster.stop(0);
                        Log.v(TAG, "NTRIPCaster.stop(0)="+ret);

                    }
                }
                invalidateOptionsMenu();
            }
        });*/
    }


    private void onNavDrawerItemClicked(View view) {
        int id = view.getId();
        selectDrawerItem(id);
    }

    private void selectDrawerItem(int itemId) {
        if (itemId == R.id.navdraw_item_status || itemId == R.id.navdraw_item_map) {
            setNavDrawerItemFragment(itemId);
        } else if (itemId == R.id.navdraw_item_input_streams) {
            showInputStreamSettings();
        } else if (itemId == R.id.navdraw_item_output_streams) {
            showOutputStreamSettings();
        } else if (itemId == R.id.navdraw_item_log_streams) {
            showLogStreamSettings();
        } else if (itemId == R.id.navdraw_item_processing_options
                || itemId == R.id.navdraw_item_solution_options)
//                || itemId == R.id.navdraw_item_ntripcaster_options) 
        {
            showSettings(itemId);
        } else if (itemId == R.id.navdraw_item_save_settings) {
            showSettingsSaveToFile();
        } else if (itemId == R.id.navdraw_item_load_settings) {
            showSettingsLoadFromFile();
        } else {
            throw new IllegalStateException("Unknown drawer item ID: " + itemId);
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }

   /* private void toggleCasterSwitch() {
        SharedPreferences casterSolution = getSharedPreferences(NTRIPCasterSettingsFragment.SHARED_PREFS_NAME, 0);
        boolean bIsCasterEnabled = casterSolution.getBoolean(NTRIPCasterSettingsFragment.KEY_ENABLE_CASTER, false);
        mNavDrawerCasterSwitch.setEnabled(bIsCasterEnabled);
    }

    private boolean getCasterBrutalEnding() {
        SharedPreferences casterSolution = getSharedPreferences(NTRIPCasterSettingsFragment.SHARED_PREFS_NAME, 0);
        return casterSolution.getBoolean(NTRIPCasterSettingsFragment.KEY_BRUTAL_ENDING_CASTER, true);
    }*/

    private void setNavDrawerItemFragment(int itemId) {
        if (mNavDraverSelectedItem == itemId) return;

        Fragment fragment;
        if (itemId == R.id.navdraw_item_status) {
            fragment = new StatusFragment();
        } else if (itemId == R.id.navdraw_item_map) {
            fragment = new MapFragment();
        } else {
            throw new IllegalArgumentException("Unsupported fragment ID: " + itemId);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();

        mNavDraverSelectedItem = itemId;
        setNavDrawerItemChecked(itemId);
    }

    private void setNavDrawerItemChecked(int itemId) {
        navBinding.navdrawItemStatus.setSelected(false);
        navBinding.navdrawItemMap.setSelected(false);
        navBinding.navdrawItemSaveSettings.setSelected(false);

        View selected = navBinding.getRoot().findViewById(itemId);
        if (selected != null) {
            selected.setSelected(true);
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    @SuppressLint("MissingPermission")
    private void initDemoModeLocation() {
        mDemoModeLocation = new DemoModeLocation(getApplicationContext());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initDemoModeLocation();
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static DemoModeLocation getDemoModeLocation() {
        return mDemoModeLocation;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mRtkServiceBound) {
            final Intent intent = new Intent(this, RtkNaviService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        proxyIfUsbAttached(intent);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerToggle != null) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service
        if (mRtkServiceBound) {
            unbindService(mConnection);
            mRtkServiceBound = false;
            mRtkService = null;
        }

    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            mNavDraverSelectedItem = savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM);
            setNavDrawerItemChecked(mNavDraverSelectedItem);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mNavDraverSelectedItem != 0) {
            outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, mNavDraverSelectedItem);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean serviceActive = navBinding.navdrawServerSwitch.isChecked();

        menu.findItem(R.id.menu_start_service).setVisible(!serviceActive);
        menu.findItem(R.id.menu_stop_service).setVisible(serviceActive);
        menu.findItem(R.id.menu_add_point).setVisible(serviceActive);
        menu.findItem(R.id.menu_tools).setVisible(true);

        // Uncomment if using Dropbox
        // if (mDbxAcctMgr.hasLinkedAccount()) {
        //     menu.findItem(R.id.menu_dropbox).setVisible(false);
        // }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();
        if (id == R.id.menu_start_service) {
            startRtkService();
        } else if (id == R.id.menu_stop_service) {
            stopRtkService();
        } else if (id == R.id.menu_add_point) {
            askToAddPointToCrw();
        } else if (id == R.id.menu_tools) {
            startActivity(new Intent(this, ToolsActivity.class));
        } else if (id == R.id.menu_settings) {
            binding.drawerLayout.openDrawer(GravityCompat.START);
        } else if (id == R.id.menu_about) {
            startActivity(new Intent(this, AboutActivity.class));
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private boolean askForPointName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.PointNameAlertDialogStyle);
        builder.setTitle(R.string.point_name_input_title);


        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(R.string.button_ok, (dialog, which) -> {
            m_PointName = input.getText().toString();
            m_bRet_pointName = true;
        });
        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> {
            m_bRet_pointName = false;
            dialog.cancel();
        });

        builder.show();
        return m_bRet_pointName;
    }

    private void askToAddPointToCrw() {
        if (askForPointName()) {
            final Intent intent = new Intent(RtkNaviService.ACTION_STORE_POINT);
            intent.setClass(this, RtkNaviService.class);
            intent.putExtra(RtkNaviService.EXTRA_POINT_NAME, m_PointName);
            startService(intent);
        }
    }

    private void copyAssetsDirToApplicationDirectory(String sourceDir, File destDir) throws IOException {
        String[] files = getAssets().list(sourceDir);
        if (files == null) return;

        for (String fileName : files) {
            String assetPath = sourceDir + "/" + fileName;
            File destFile = new File(destDir, assetPath);

            if (destFile.exists()) continue;

            File parentDir = destFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
                }
            }

            try (InputStream in = getAssets().open(assetPath);
                 OutputStream out = new BufferedOutputStream(Files.newOutputStream(destFile.toPath()))) {
                byte[] buffer = new byte[1024];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.flush();
            } catch (IOException e) {
                Log.e(TAG, "Failed to copy asset: " + assetPath + " to " + destFile.getAbsolutePath(), e);
                if (destFile.exists() && !destFile.delete()) {
                    Log.w(TAG, "Failed to delete partial file: " + destFile.getAbsolutePath());
                }
                throw e;
            }
        }
    }


    private void copyAssetsToApplicationDirectory() throws IOException {
        copyAssetsDirToApplicationDirectory("data", this.getFilesDir());
        copyAssetsDirToApplicationDirectory("proj4", this.getFilesDir());
    }

    private void copyAssetsToWorkingDirectory() throws IOException {
        copyAssetsDirToApplicationDirectory("ntripcaster", getFileStorageDirectory());
    }

    private void proxyIfUsbAttached(Intent intent) {

        if (intent == null) return;

        if (!UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) return;

        if (DBG) Log.v(TAG, "usb device attached");

        final Intent proxyIntent = new Intent(UsbToRtklib.ACTION_USB_DEVICE_ATTACHED);
        proxyIntent.putExtras(Objects.requireNonNull(intent.getExtras()));
        sendBroadcast(proxyIntent);
    }

    private void createDrawerToggle() {
        drawerToggle = new ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                binding.toolbar,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        binding.drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    private void refreshServiceSwitchStatus() {
        boolean serviceActive = mRtkServiceBound && (mRtkService != null && mRtkService.isServiceStarted());
        navBinding.navdrawServerSwitch.setChecked(serviceActive);
    }

    private void startRtkService() {
        GpsTime gpsTime = new GpsTime();
        gpsTime.setTime(System.currentTimeMillis());
        mSessionCode = String.format("%s_%s", gpsTime.getStringGpsWeek(), gpsTime.getStringGpsTOW());
        final Intent rtkServiceIntent = new Intent(RtkNaviService.ACTION_START);
        rtkServiceIntent.putExtra(RtkNaviService.EXTRA_SESSION_CODE, mSessionCode);
        rtkServiceIntent.setClass(this, RtkNaviService.class);
        startService(rtkServiceIntent);
        navBinding.navdrawServerSwitch.setChecked(true);
    }

    public String getSessionCode() {
        return mSessionCode;
    }

    private void stopRtkService() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm stop");
        builder.setMessage("Do you really want to stop service?");
        builder.setIcon(R.drawable.ic_launcher);
        builder.setPositiveButton("Yes", (dialog, id) -> {
            dialog.dismiss();
            stopRtkServiceConfirmed();
        });
        builder.setNegativeButton("No", (dialog, id) -> {
            dialog.dismiss();
            if (!navBinding.navdrawServerSwitch.isChecked()) {
                navBinding.navdrawServerSwitch.setChecked(true);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void stopRtkServiceConfirmed() {
        final Intent intent = new Intent(RtkNaviService.ACTION_STOP);
        intent.setClass(this, RtkNaviService.class);
        startService(intent);
        navBinding.navdrawServerSwitch.setChecked(false);
    }

    public RtkNaviService getRtkService() {
        return mRtkService;
    }

    private void showSettings(int itemId) {
        final Intent intent = new Intent(this, SettingsActivity.class);

        if (itemId == R.id.navdraw_item_processing_options) {
            intent.putExtra(SettingsActivity.EXTRA_FRAGMENT_NAME,
                    ProcessingOptions1Fragment.class.getName());
        } else if (itemId == R.id.navdraw_item_solution_options) {
            intent.putExtra(SettingsActivity.EXTRA_FRAGMENT_NAME,
                    SolutionOutputSettingsFragment.class.getName());
        } 
        /*else if (itemId == R.id.navdraw_item_ntripcaster_options) {
            intent.putExtra(SettingsActivity.EXTRA_FRAGMENT_NAME,
                    NTRIPCasterSettingsFragment.class.getName());
        } */
        else {
            throw new IllegalStateException("Unexpected item ID: " + itemId);
        }
        startActivity(intent);
    }

    private void showInputStreamSettings() {
        final Intent intent = new Intent(this, StreamSettingsActivity.class);
        intent.putExtra(StreamSettingsActivity.ARG_STEAM,
                StreamSettingsPagerAdapter.STREAM_INPUT_SETTINGS);
        startActivity(intent);
    }

    private void showOutputStreamSettings() {
        final Intent intent = new Intent(this, StreamSettingsActivity.class);
        intent.putExtra(StreamSettingsActivity.ARG_STEAM,
                StreamSettingsPagerAdapter.STREAM_OUTPUT_SETTINGS);
        startActivity(intent);
    }

    private void showLogStreamSettings() {
        final Intent intent = new Intent(this, StreamSettingsActivity.class);
        intent.putExtra(StreamSettingsActivity.ARG_STEAM,
                StreamSettingsPagerAdapter.STREAM_LOG_SETTINGS);
        startActivity(intent);
    }

    private void showSettingsSaveToFile() {
        File[] files = new File(getSharedPreferencesDirectoryname()).listFiles();
        assert files != null;
        String[] filenames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            filenames[i] = files[i].getAbsolutePath();
        }
        String zipFileName = getSettingsZipFileName();
        String message;
        try {
            ZipHelper.zip(filenames, zipFileName);
            message = String.format(
                    getResources().getString(R.string.navdraw_item_save_settings_finished),
                    zipFileName
            );
        } catch (IOException e) {
            message = String.format(
                    getResources().getString(R.string.navdraw_item_save_settings_failed),
                    zipFileName
            );
        }
        displayToast(message);
    }

    private void showSettingsLoadFromFile() {
        String zipFileName = getSettingsZipFileName();
        String message;
        try {
            ZipHelper.unzip(zipFileName, getSharedPreferencesDirectoryname());
            message = String.format(
                    getResources().getString(R.string.navdraw_item_load_settings_finished),
                    zipFileName
            );
        } catch (IOException e) {
            message = String.format(
                    getResources().getString(R.string.navdraw_item_load_settings_failed),
                    zipFileName
            );
        }
        displayToast(message);
    }

    private String getSharedPreferencesDirectoryname() {
        return new File(getApplicationInfo().dataDir, "shared_prefs").getAbsolutePath();
    }

    private String getSettingsZipFileName() {
        return new File(getExternalFilesDir(null), "settings.zip").getAbsolutePath();
    }

    private void displayToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void onNavDrawevItemClicked(View v) {
        selectDrawerItem(v.getId());
    }

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            RtkNaviService.RtkNaviServiceBinder binder = (RtkNaviService.RtkNaviServiceBinder) service;
            mRtkService = binder.getService();
            mRtkServiceBound = true;
            refreshServiceSwitchStatus();
            invalidateOptionsMenu();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mRtkServiceBound = false;
            mRtkService = null;
            refreshServiceSwitchStatus();
            invalidateOptionsMenu();
        }
    };


    @Nonnull
    public static File getFileStorageDirectory() {
        File externalLocation = new File(Environment.getExternalStorageDirectory(), RTKGPS_CHILD_DIRECTORY);

        if (!externalLocation.exists() && !externalLocation.mkdirs()) {
            Log.e(TAG, "Failed to create dir: " + externalLocation.getAbsolutePath());
        }

        return externalLocation;
    }

    @Nonnull
    public static File getFileInStorageDirectory(String nameWithExtension) {
        return new File(Environment.getExternalStorageDirectory(), RTKGPS_CHILD_DIRECTORY + nameWithExtension);
    }

    public static String getAndCheckSessionDirectory(String code) {
        File sessionDir = new File(MainActivity.getFileStorageDirectory(), code);
        if (!sessionDir.exists()) {
            if (!sessionDir.mkdirs()) {
                throw new RuntimeException("Failed to create session directory: " + sessionDir.getAbsolutePath());
            }
        }
        return sessionDir.getAbsolutePath();
    }


    public static File getFileInStorageSessionDirectory(String code, String nameWithExtension) {
        String szSessionDirectory = MainActivity.getAndCheckSessionDirectory(code);
        return new File(szSessionDirectory + File.separator + nameWithExtension);
    }


    @Nonnull
    public static File getLocalSocketPath(Context ctx, String socketName) {
        return ctx.getFileStreamPath(socketName);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == REQUEST_LINK_TO_DBX) {
//            if (resultCode == Activity.RESULT_OK) {
        // ... Start using Dropbox files.
//            } else {
        // ... Link failed or was cancelled by the user.
        //           }
        //       } else {
        super.onActivityResult(requestCode, resultCode, data);
        //       }
    }

    public static String getApplicationDirectory() {
        return MainActivity.mApplicationDirectory;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        assert key != null;
        /*if (key.equalsIgnoreCase(NTRIPCasterSettingsFragment.KEY_ENABLE_CASTER))
        {
            toggleCasterSwitch();
        }*/

    }

    @Override
    protected void onPause() {
        super.onPause();
        getSharedPreferences(NTRIPCasterSettingsFragment.SHARED_PREFS_NAME, MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSharedPreferences(NTRIPCasterSettingsFragment.SHARED_PREFS_NAME, MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this);
    }
}
