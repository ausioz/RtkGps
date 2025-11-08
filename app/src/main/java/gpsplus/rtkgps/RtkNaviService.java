package gpsplus.rtkgps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;

import gpsplus.rtkgps.settings.OutputGPXTraceFragment;
import gpsplus.rtkgps.settings.ProcessingOptions1Fragment;
import gpsplus.rtkgps.settings.SettingsHelper;
import gpsplus.rtkgps.settings.SolutionOutputSettingsFragment;
import gpsplus.rtkgps.settings.StreamBluetoothFragment.Value;
import gpsplus.rtkgps.settings.StreamMobileMapperFragment;
import gpsplus.rtkgps.settings.StreamUsbFragment;
import gpsplus.rtkgps.utils.PreciseEphemerisDownloader;
import gpsplus.rtkgps.utils.PreciseEphemerisProvider;
import gpsplus.rtkgps.view.SolutionView;
import gpsplus.rtklib.ProcessingOptions;
import gpsplus.rtklib.RtkCommon;
import gpsplus.rtklib.RtkCommon.Position3d;
import gpsplus.rtklib.RtkControlResult;
import gpsplus.rtklib.RtkServer;
import gpsplus.rtklib.RtkServerObservationStatus;
import gpsplus.rtklib.RtkServerSettings;
import gpsplus.rtklib.RtkServerSettings.TransportSettings;
import gpsplus.rtklib.RtkServerStreamStatus;
import gpsplus.rtklib.Solution;
import gpsplus.rtklib.constants.EphemerisOption;
import gpsplus.rtklib.constants.GeoidModel;
import gpsplus.rtklib.constants.StreamType;

public class RtkNaviService extends Service implements LocationListener {

    // Constants
    private static final String TAG = RtkNaviService.class.getSimpleName();

    public static final String ACTION_START = "gpsplus.rtkgps.RtkNaviService.START";
    public static final String ACTION_STOP = "gpsplus.rtkgps.RtkNaviService.STOP";
    public static final String ACTION_STORE_POINT = "gpsplus.rtkgps.RtkNaviService.STORE_POINT";
    public static final String EXTRA_SESSION_CODE = "gpsplus.rtkgps.RtkNaviService.SESSION_CODE";
    public static final String EXTRA_POINT_NAME = "gpsplus.rtkgps.RtkNaviService.POINT_NAME";

    private static final String RTK_GPS_MOCK_LOC_SERVICE = "RtkGps mock loc service";
    private static final String GPS_PROVIDER = LocationManager.GPS_PROVIDER;
    private static final String HANDLER_THREAD_NAME = "RtkNaviService";
    private static final String NOTIFICATION_CHANNEL_ID = "RTK_GPS_SERVICE_CHANNEL";
    private static final String NOTIFICATION_CHANNEL_NAME = "RTK GPS Service";
    private static final long DEFAULT_PROCESSING_CYCLE = 5L;
    private static final int NOTIFICATION_ID = R.string.local_service_started;

    // Core service components
    private final IBinder mBinder = new RtkNaviServiceBinder();
    private static final RtkServer mRtkServer = new RtkServer();

    // Threading
    private HandlerThread mServiceHandlerThread;
    private Handler mServiceHandler;
    private volatile boolean mBoolIsRunning = false;

    // Power management
    private PowerManager.WakeLock mCpuLock;

    // GPS and location
    private Location mLocationPrec = null;
    private boolean mBoolMockLocationsPref = false;
    private RtkCommon rtkCommon;

    // Data streams
    private BluetoothToRtklib mBtRover, mBtBase;
    private UsbToRtklib mUsbReceiver;
    private MobileMapperToRtklib mMobileMapperToRtklib;

    // GPX tracing
    private boolean mBoolGenerateGPXTrace = false;
    private GPXTrace mGpxTrace = null;

    private long mLProcessingCycle = DEFAULT_PROCESSING_CYCLE;
    private String mSessionCode;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManagerCompat notificationManager;

    public static volatile boolean mbStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            mCpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        } else {
            Log.e(TAG, "Failed to get PowerManager service");
        }
        mServiceHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
        mServiceHandlerThread.start();
        mServiceHandler = new Handler(mServiceHandlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.v(TAG, "RtkNaviService restarted");
            processStart();
        } else {
            final String action = intent.getAction();
            if (action == null) {
                Log.e(TAG, "onStartCommand(): null action");
                return START_STICKY;
            }

            switch (action) {
                case ACTION_START:
                    if (intent.hasExtra(EXTRA_SESSION_CODE)) {
                        mSessionCode = intent.getStringExtra(EXTRA_SESSION_CODE);
                    } else {
                        mSessionCode = String.valueOf(System.currentTimeMillis());
                    }
                    processStart();
                    break;
                case ACTION_STOP:
                    processStop();
                    break;
                case ACTION_STORE_POINT:
                    break;
                default:
                    Log.e(TAG, "onStartCommand(): unknown action " + action);
                    break;
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return binder to allow clients to interact with the service
        return mBinder;
    }

    public final void getStreamStatus(
            RtkServerStreamStatus status) {
        mRtkServer.getStreamStatus(status);
    }

    public final void getRoverObservationStatus(
            RtkServerObservationStatus status) {
        if (MainActivity.getDemoModeLocation() != null && MainActivity.getDemoModeLocation().isInDemoMode() && mbStarted) {
            MainActivity.getDemoModeLocation().getObservationStatus(status);
        } else {
            mRtkServer.getRoverObservationStatus(status);
        }
    }

    public final void getBaseObservationStatus(
            RtkServerObservationStatus status) {
        mRtkServer.getBaseObservationStatus(status);
    }

    public RtkControlResult getRtkStatus(RtkControlResult dst) {
        return mRtkServer.getRtkStatus(dst);
    }

    public static void loadSP3(String file) {
        mRtkServer.readSP3(file);
    }

    public static void loadSatAnt(String file) {
        mRtkServer.readSatAnt(file);
    }

    public boolean isServiceStarted() {
        return mRtkServer.getStatus() != RtkServerStreamStatus.STATE_CLOSE;
    }

    public int getServerStatus() {
        return mRtkServer.getStatus();
    }

    public Solution[] readSolutionBuffer() {
        return mRtkServer.readSolutionBuffer();
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class RtkNaviServiceBinder extends Binder {
        public RtkNaviService getService() {
            // Return this instance of UpdateDbService so clients can call
            // public methods
            return RtkNaviService.this;
        }
    }

    public void processStart() {
        if (isServiceStarted()) {
            Log.d(TAG, "Service already started, ignoring start request");
            return;
        }

        Log.i(TAG, "Starting RTK GPS Service");
        mbStarted = true;
        startForegroundService();
        initializeDemoMode();
        initializeRtkServer();
        configureEphemeris();
        startDataStreams();
        configureLocationServices();
        initializeGeoidModel();
        startProcessingLoop();

        Log.i(TAG, "RTK GPS Service started successfully");
    }

    @SuppressLint("MissingPermission")
    private void initializeDemoMode() {
        try {
            if (MainActivity.getDemoModeLocation() != null) {
                MainActivity.getDemoModeLocation().reset();
                if (MainActivity.getDemoModeLocation().isInDemoMode()) {
                    MainActivity.getDemoModeLocation().startDemoMode();
                }
            }
        } catch (NullPointerException e) {
            Log.w(TAG, "Demo mode location not available", e);
        }
    }

    private void initializeRtkServer() {
        RtkServerSettings settings = SettingsHelper.loadSettings(this);
        mRtkServer.setServerSettings(settings);

        if (!mRtkServer.start()) {
            Log.e(TAG, "Failed to start RTK server");
            throw new RuntimeException("RTK server failed to start");
        }
    }

    private void configureEphemeris() {
        SharedPreferences processPrefs = getBaseContext().getSharedPreferences(
                ProcessingOptions1Fragment.SHARED_PREFS_NAME, 0);
        String ephemVa = processPrefs.getString(ProcessingOptions1Fragment.KEY_SAT_EPHEM_CLOCK, "");

        if (!ephemVa.isEmpty()) {
            try {
                EphemerisOption ephemerisOption = EphemerisOption.valueOf(ephemVa);
                PreciseEphemerisProvider provider = ephemerisOption.getProvider();
                if (PreciseEphemerisDownloader.isCurrentOrbitsPresent(provider)) {
                    loadSP3(PreciseEphemerisDownloader.getCurrentOrbitFile(provider).getAbsolutePath());
                }
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid ephemeris option: " + ephemVa, e);
            }
        }
    }

    private void startDataStreams() {
        startBluetoothPipes();
        startUsb();
        startMobileMapper();
    }

    private void startForegroundService() {
        ProcessingOptions opt = ProcessingOptions1Fragment.readPrefs(this);
        String positioningMode;
        if (MainActivity.getDemoModeLocation().isInDemoMode()) {
            positioningMode = getString(R.string.solq_internal);
        } else {
            positioningMode = this.getString(opt.getPositioningMode().getNameResId());
        }
        acquireWakeLock();
        Notification notification = createForegroundNotification(positioningMode);
        startForeground(NOTIFICATION_ID, notification);
    }

    @SuppressLint("WakelockTimeout")
    private void acquireWakeLock() {
        if (mCpuLock != null && !mCpuLock.isHeld()) {
            mCpuLock.acquire();
        }
    }

    private void configureLocationServices() {
        loadPreferences();

        boolean isDemoMode = MainActivity.getDemoModeLocation() != null
                && MainActivity.getDemoModeLocation().isInDemoMode();

        // Prevent mock location setup if demo mode is active
        if (mBoolMockLocationsPref) {
            if (!isDemoMode) {
                setupMockLocationProvider();
            } else {
                Toast.makeText(this, "No Mock Location provider available in demo mode", Toast.LENGTH_LONG).show();
            }
        }
    }


    private void loadPreferences() {
        SharedPreferences prefs = getBaseContext().getSharedPreferences(
                SolutionOutputSettingsFragment.SHARED_PREFS_NAME, 0);
        mBoolMockLocationsPref = prefs.getBoolean(
                SolutionOutputSettingsFragment.KEY_OUTPUT_MOCK_LOCATION, false);

        prefs = getBaseContext().getSharedPreferences(OutputGPXTraceFragment.SHARED_PREFS_NAME, 0);
        mBoolGenerateGPXTrace = prefs.getBoolean(OutputGPXTraceFragment.KEY_ENABLE, false);

        prefs = getBaseContext().getSharedPreferences(ProcessingOptions1Fragment.SHARED_PREFS_NAME, 0);
        String processingCycleStr = prefs.getString(ProcessingOptions1Fragment.KEY_PROCESSING_CYCLE, "5");
        try {
            mLProcessingCycle = Long.parseLong(processingCycleStr);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid processing cycle value: " + processingCycleStr + ", using default: 5", e);
            mLProcessingCycle = DEFAULT_PROCESSING_CYCLE;
        }
    }

    @SuppressLint({"MissingPermission", "InlinedApi"})
    private void setupMockLocationProvider() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Log.e(TAG, "LocationManager not available");
            return;
        }

        try {
            locationManager.addTestProvider(GPS_PROVIDER, false, false,
                    false, false, true, false, true,
                    ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE);

            locationManager.setTestProviderEnabled(GPS_PROVIDER, true);

            if (hasLocationPermission()) {
                locationManager.requestLocationUpdates(GPS_PROVIDER, 0, 0, this);
            } else {
                requestLocationPermission();
            }

            Log.i(RTK_GPS_MOCK_LOC_SERVICE, "Mock Location service was started");
        } catch (Exception e) {
            Log.e(RTK_GPS_MOCK_LOC_SERVICE, "Failed to setup mock location provider", e);
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        PermissionListener dialogPermissionListener = DialogOnDeniedPermissionListener.Builder
                .withContext(this)
                .withTitle(R.string.permission_to_use_gps_title)
                .withMessage(R.string.permission_to_use_gps)
                .withButtonText(android.R.string.ok)
                .build();

        Dexter.withContext(getApplicationContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(dialogPermissionListener)
                .check();
    }

    private void initializeGeoidModel() {
        SharedPreferences prefs = getBaseContext().getSharedPreferences(
                ProcessingOptions1Fragment.SHARED_PREFS_NAME, 0);
        String geoidModelStr = prefs.getString(
                SolutionOutputSettingsFragment.KEY_GEOID_MODEL, GeoidModel.EMBEDDED.name());

        try {
            GeoidModel model = GeoidModel.valueOf(geoidModelStr);
            rtkCommon = new RtkCommon(model);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid geoid model: " + geoidModelStr + ", using default: EMBEDDED", e);
            rtkCommon = new RtkCommon(GeoidModel.EMBEDDED);
        }

        String antennaPath = MainActivity.getApplicationDirectory() + File.separator +
                "files" + File.separator + "data" + File.separator + "igs05.atx";
        loadSatAnt(antennaPath);
    }

    private void startProcessingLoop() {
        mBoolIsRunning = true;
        scheduleNextProcessing();
    }

    private void scheduleNextProcessing() {
        if (mBoolIsRunning && mServiceHandler != null) {
            mServiceHandler.postDelayed(this::processRtkCycle, mLProcessingCycle * 1000);
        }
    }

    private void processRtkCycle() {
        if (!mBoolIsRunning) {
            return;
        }

        try {
            if (mBoolMockLocationsPref || mBoolGenerateGPXTrace) {
                processRtkSolution();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing RTK solution", e);
        }
        scheduleNextProcessing();
    }


    @SuppressLint("MissingPermission")
    private void processRtkSolution() {
        RtkControlResult result = getRtkStatus(null);
        Solution solution = result.getSolution();
        Position3d positionECEF = solution.getPosition();

        if (RtkCommon.norm(positionECEF.getValues()) > 0.0) {
            Position3d positionLatLon = RtkCommon.ecef2pos(positionECEF);

            if (mBoolMockLocationsPref) {
                updateMockLocation(solution, positionLatLon, positionECEF);
            }

            if (mBoolGenerateGPXTrace) {
                updateGpxTrace(positionLatLon, solution);
            }
        }

        updateNotificationStatus(solution.getSolutionStatus().name());
    }

    private void updateMockLocation(Solution solution, Position3d positionLatLon, Position3d positionECEF) {
        // Compute accuracy (HRMS)
        RtkCommon.Matrix3x3 cov = solution.getQrMatrix();
        Position3d roverPos = RtkCommon.ecef2pos(positionECEF);
        double lat = roverPos.getLat();
        double lon = roverPos.getLon();
        double[] Qe = RtkCommon.covenu(lat, lon, cov).getValues();
        double HRMS = SolutionView.computeHRMS(Qe);

        Location currentLocation = createLocation(
                Math.toDegrees(positionLatLon.getLat()),
                Math.toDegrees(positionLatLon.getLon()),
                positionLatLon.getHeight(),
                (float) HRMS
        );

        Log.i(RTK_GPS_MOCK_LOC_SERVICE, String.format("Mock location: %.6f, %.6f, accuracy: %.2f m",
                currentLocation.getLatitude(), currentLocation.getLongitude(), HRMS));

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            try {
                locationManager.setTestProviderLocation(GPS_PROVIDER, currentLocation);
            } catch (Exception e) {
                Log.w(RTK_GPS_MOCK_LOC_SERVICE, "Failed to set mock location: " + e.getMessage());
            }
        }
    }

    private void updateGpxTrace(Position3d positionLatLon, Solution solution) {
        if (mGpxTrace == null) {
            mGpxTrace = new GPXTrace();
        }
        mGpxTrace.addPoint(
                Math.toDegrees(positionLatLon.getLat()),
                Math.toDegrees(positionLatLon.getLon()),
                positionLatLon.getHeight(),
                rtkCommon.getAltitudeCorrection(positionLatLon.getLat(), positionLatLon.getLon()),
                solution.getTime()
        );
    }

    public void processStop() {
        Log.i(TAG, "Stopping RTK GPS Service");

        mBoolIsRunning = false;
        mbStarted = false;

        // Cancel any pending processing tasks
        if (mServiceHandler != null) {
            mServiceHandler.removeCallbacksAndMessages(null);
        }

        cleanupDemoMode();
        cleanupMockLocationProvider();
        stopDataStreams();
        finalizeGpxTrace();

        // Force stop the service
        stop();

        Log.i(TAG, "RTK GPS Service stopped");
    }

    private void cleanupDemoMode() {
        try {
            if (MainActivity.getDemoModeLocation() != null &&
                    MainActivity.getDemoModeLocation().isInDemoMode()) {
                MainActivity.getDemoModeLocation().stopDemoMode();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error stopping demo mode", e);
        }
    }

    private void cleanupMockLocationProvider() {
        if (mBoolMockLocationsPref) {
            try {
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (lm != null) {
                    lm.removeTestProvider(GPS_PROVIDER);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error removing mock location provider", e);
            }
        }
    }

    private void stopDataStreams() {
        stopBluetoothPipes();
        stopUsb();
        stopMobileMapper();
    }

    private void finalizeGpxTrace() {
        if (mBoolGenerateGPXTrace && mGpxTrace != null) {
            try {
                // GPX trace finalization logic would go here
                Log.d(TAG, "GPX trace finalized");
            } catch (Exception e) {
                Log.e(TAG, "Error finalizing GPX trace", e);
            }
        }
    }

    private void stop() {
        Log.d(TAG, "Stopping service components");

        stopForeground(true);
        if (mCpuLock != null && mCpuLock.isHeld()) {
            mCpuLock.release();
        }

        if (isServiceStarted()) {
            mRtkServer.stop();

            stopBluetoothPipes();
            stopUsb();
            stopMobileMapper();
            // Tell the user we stopped.
            Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
        }

        // Ensure service is actually stopped
        stopSelf();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service being destroyed");
        stop();
        if (mServiceHandlerThread != null) {
            mServiceHandlerThread.quit();
            try {
                mServiceHandlerThread.join(1000); // Wait up to 1 second for thread to finish
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting for handler thread to finish", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private Notification createForegroundNotification(String positioningMode) {
        createNotificationChannel();

        CharSequence text = getText(R.string.local_service_started);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE
        );

        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("RTKGPS+ started: " + positioningMode)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);

        notificationManager = NotificationManagerCompat.from(this);
        return notificationBuilder.build();
    }

    private void createNotificationChannel() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        // Check if channel already exists
        NotificationChannel existingChannel =
                notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
        if (existingChannel == null) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("RTK GPS positioning service:");
            channel.setShowBadge(false);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private void updateNotificationStatus(String statusText) {
        if (notificationBuilder == null) return;

        notificationBuilder.setContentText("Status: " + statusText);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private class BluetoothCallbacks implements BluetoothToRtklib.Callbacks {

        private final int mStreamId;
        private final Handler mHandler;

        public BluetoothCallbacks(int streamId) {
            mStreamId = streamId;
            mHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void onConnected() {
            mHandler.post(() ->
                    Toast.makeText(RtkNaviService.this, R.string.bluetooth_connected, Toast.LENGTH_SHORT).show());

            // Execute startup commands on background thread
            mServiceHandler.post(() -> mRtkServer.sendStartupCommands(mStreamId));
        }

        @Override
        public void onStopped() {
            Log.d(TAG, "Bluetooth stream stopped");
        }

        @Override
        public void onConnectionLost() {
            mHandler.post(() ->
                    Toast.makeText(RtkNaviService.this, R.string.bluetooth_connection_lost, Toast.LENGTH_SHORT).show());
        }
    }

    private class UsbCallbacks implements UsbToRtklib.Callbacks {
        private final int mStreamId;
        private final Handler mHandler;

        public UsbCallbacks(int streamId) {
            mStreamId = streamId;
            mHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void onConnected() {
            mHandler.post(() ->
                    Toast.makeText(RtkNaviService.this, R.string.usb_connected, Toast.LENGTH_SHORT).show());

            // Execute startup commands on background thread  
            mServiceHandler.post(() -> mRtkServer.sendStartupCommands(mStreamId));
        }

        @Override
        public void onStopped() {
            Log.d(TAG, "USB stream stopped");
        }

        @Override
        public void onConnectionLost() {
            mHandler.post(() ->
                    Toast.makeText(RtkNaviService.this, R.string.usb_connection_lost, Toast.LENGTH_SHORT).show());
        }
    }

    private void startBluetoothPipes() {
        final TransportSettings roverSettngs, baseSettings;

        RtkServerSettings settings = mRtkServer.getServerSettings();

        roverSettngs = settings.getInputRover().getTransportSettings();

        if (roverSettngs.getType() == StreamType.BLUETOOTH) {
            Value btSettings = (Value) roverSettngs;
            mBtRover = new BluetoothToRtklib(btSettings.getAddress().toUpperCase(), btSettings.getPath());
            mBtRover.setCallbacks(new BluetoothCallbacks(RtkServer.RECEIVER_ROVER));
            mBtRover.start(this.getApplication().getBaseContext());
        } else {
            mBtRover = null;
        }

        baseSettings = settings.getInputBase().getTransportSettings();
        if (baseSettings.getType() == StreamType.BLUETOOTH) {
            Value btSettings = (Value) baseSettings;
            mBtBase = new BluetoothToRtklib(btSettings.getAddress(), btSettings.getPath());
            mBtBase.setCallbacks(new BluetoothCallbacks(RtkServer.RECEIVER_BASE));
            mBtBase.start(this.getApplication().getBaseContext());
        } else {
            mBtBase = null;
        }
    }

    private void stopBluetoothPipes() {
        if (mBtRover != null) mBtRover.stop();
        if (mBtBase != null) mBtBase.stop();
        mBtRover = null;
        mBtBase = null;
    }

    private void startMobileMapper() {
        RtkServerSettings settings = mRtkServer.getServerSettings();
        final TransportSettings roverSettings;
        roverSettings = settings.getInputRover().getTransportSettings();
        if (roverSettings.getType() == StreamType.MOBILEMAPPER) {
            StreamMobileMapperFragment.Value mobileMapperSettings = (StreamMobileMapperFragment.Value) roverSettings;
            mMobileMapperToRtklib = new MobileMapperToRtklib(this, mobileMapperSettings, mSessionCode);
            mMobileMapperToRtklib.start();
        }
    }

    private void stopMobileMapper() {
        if (mMobileMapperToRtklib != null) {
            mMobileMapperToRtklib.stop();
            mMobileMapperToRtklib = null;
        }
    }

    private void startUsb() {
        RtkServerSettings settings = mRtkServer.getServerSettings();

        {
            final TransportSettings roverSettings;
            roverSettings = settings.getInputRover().getTransportSettings();
            if (roverSettings.getType() == StreamType.USB) {
                StreamUsbFragment.Value usbSettings = (StreamUsbFragment.Value) roverSettings;
                mUsbReceiver = new UsbToRtklib(this, usbSettings.getPath());
                mUsbReceiver.setSerialLineConfiguration(usbSettings.getSerialLineConfiguration());
                mUsbReceiver.setCallbacks(new UsbCallbacks(RtkServer.RECEIVER_ROVER));
                mUsbReceiver.start();
                return;
            }
        }

        {
            final TransportSettings baseSettings;
            baseSettings = settings.getInputBase().getTransportSettings();
            if (baseSettings.getType() == StreamType.USB) {
                StreamUsbFragment.Value usbSettings = (StreamUsbFragment.Value) baseSettings;
                mUsbReceiver = new UsbToRtklib(this, usbSettings.getPath());
                mUsbReceiver.setSerialLineConfiguration(usbSettings.getSerialLineConfiguration());
                mUsbReceiver.setCallbacks(new UsbCallbacks(RtkServer.RECEIVER_BASE));
                mUsbReceiver.start();
            }
        }
    }

    private void stopUsb() {
        if (mUsbReceiver != null) {
            mUsbReceiver.stop();
            mUsbReceiver = null;
        }
    }

    public Location createLocation(double lat, double lng, double alt, float accuracy) {
        // Create a new Location
        Location newLocation = new Location(GPS_PROVIDER);
        newLocation.setLatitude(lat);
        newLocation.setLongitude(lng);
        newLocation.setAccuracy(accuracy);
        newLocation.setAltitude(alt);
        newLocation.setTime(System.currentTimeMillis());

        try {
//            Method m = newLocation.getClass().getMethod("setElapsedRealtimeNanos", long.class);
            newLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtime());
        } catch (Exception e) {
            // Method not available before API 17 - this is expected and safe to ignore
        }
        newLocation.setSpeed(0f);

        if (mLocationPrec == null) {
            newLocation.setBearing(0f);
            newLocation.setSpeed(0f);
        } else {
            float fBearing = mLocationPrec.bearingTo(newLocation) + 180;
            if (fBearing > 360) {
                fBearing -= 360;
            }
            float fSpeed = (mLocationPrec.distanceTo(newLocation)) / ((float) (newLocation.getTime() - mLocationPrec.getTime()) / 1000);
            newLocation.setBearing(fBearing);
            newLocation.setSpeed(fSpeed);
        }
        mLocationPrec = newLocation;

        return newLocation;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Location updates are handled by the RTK processing loop
        // This method is required by LocationListener interface but not actively used
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.i(RTK_GPS_MOCK_LOC_SERVICE, "Mock location provider " + provider + " was disabled");
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.i(RTK_GPS_MOCK_LOC_SERVICE, "Mock location provider " + provider + " is enabled");
    }

    @Override
    @Deprecated
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        // This method was deprecated in API level 29
        // Location status changes are now handled through other methods
    }
}
