package gpsplus.rtkgps;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import java.util.concurrent.atomic.AtomicInteger;

import gpsplus.rtkgps.settings.InputBaseFragment;
import gpsplus.rtkgps.settings.InputCorrectionFragment;
import gpsplus.rtkgps.settings.InputRoverFragment;
import gpsplus.rtkgps.settings.SolutionOutputSettingsFragment;
import gpsplus.rtklib.RtkCommon.Position3d;
import gpsplus.rtklib.RtkServerObservationStatus;

public class DemoModeLocation implements LocationListener {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = DemoModeLocation.class.getSimpleName();

    private Context mParentContext = null;
    private static boolean mIsInDemoMode = false;

    private Position3d mPos = null;
    private final AtomicInteger nbSat = new AtomicInteger(0);
    private long lTime = System.currentTimeMillis();
    private float accuracy = Float.MAX_VALUE;

    private LocationManager locationManager;
    private RtkServerObservationStatus mObs;

    private GnssStatus.Callback gnssStatusCallback;

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public DemoModeLocation(Context parentContext) {
        this.mParentContext = parentContext.getApplicationContext();
        reset();
    }

    public DemoModeLocation() {
        if (mParentContext == null) {
            throw new NullPointerException("One instance must be initialized with constructor DemoModeLocation(Context parentContext)");
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void reset() {
        SharedPreferences prefsInputBase = mParentContext.getSharedPreferences(InputBaseFragment.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences prefsInputRover = mParentContext.getSharedPreferences(InputRoverFragment.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences prefsInputCorrection = mParentContext.getSharedPreferences(InputCorrectionFragment.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences prefsSolution = mParentContext.getSharedPreferences(SolutionOutputSettingsFragment.SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        boolean bBaseEnabled = prefsInputBase.getBoolean(InputBaseFragment.KEY_ENABLE, true);
        boolean bRoverEnabled = prefsInputRover.getBoolean(InputRoverFragment.KEY_ENABLE, true);
        boolean bCorrectionEnabled = prefsInputCorrection.getBoolean(InputCorrectionFragment.KEY_ENABLE, true);
        boolean bIsTestModeEnabled = prefsSolution.getBoolean(SolutionOutputSettingsFragment.KEY_ENABLE_TEST_MODE, true);

        mIsInDemoMode = !bBaseEnabled && !bRoverEnabled && !bCorrectionEnabled && bIsTestModeEnabled;

        if (mIsInDemoMode) {
            locationManager = (LocationManager) mParentContext.getSystemService(Context.LOCATION_SERVICE);
            registerGnssStatusCallback();
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void startDemoMode() {
        if (locationManager != null) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 0.5f, this);
        }
    }

    public void stopDemoMode() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            unregisterGnssStatusCallback();
        }
    }

    public boolean isInDemoMode() {
        return mIsInDemoMode;
    }

    public Position3d getPosition() { // in radians
        return mPos;
    }

    public RtkServerObservationStatus getObservationStatus(RtkServerObservationStatus status) {
        if (mObs != null) {
            mObs.copyTo(status);
        }
        return mObs;
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void registerGnssStatusCallback() {
        if (locationManager == null) return;

        gnssStatusCallback = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                if (DBG)
                    Log.d(TAG, "GNSS satellite status changed");
                mObs = new RtkServerObservationStatus();
                nbSat.set(0);

                int satelliteCount = status.getSatelliteCount();
                for (int i = 0; i < satelliteCount; i++) {
                    boolean usedInFix = status.usedInFix(i);
                    if (usedInFix) {
                        nbSat.incrementAndGet();

                        int svid = status.getSvid(i);
                        int constellationType = status.getConstellationType(i);
                        float azimuth = status.getAzimuthDegrees(i);
                        float elevation = status.getElevationDegrees(i);
                        float snr = status.getCn0DbHz(i);

                        // Convert Android SVID and constellation type to RTKLib satellite number
                        int rtkSatNumber = convertToRtkLibSatNumber(constellationType, svid);

                        if (DBG) {
                            String constellation = getConstellationName(constellationType);
                            Log.d(TAG, String.format("%s SVID:%d -> SatNum:%d, SNR:%.1f, AZI:%.1f, ELE:%.1f",
                                    constellation, svid, rtkSatNumber, snr, azimuth, elevation));
                        }

                        mObs.addValues(rtkSatNumber,
                                Math.toRadians(azimuth),
                                Math.toRadians(elevation),
                                Math.round(snr), 0, 0, 1);
                    }
                }

                if (DBG) Log.d(TAG, String.format("Total satellites used in fix: %d", nbSat.get()));
            }
        };

        locationManager.registerGnssStatusCallback(gnssStatusCallback);
    }

    private int convertToRtkLibSatNumber(int constellationType, int svid) {
        switch (constellationType) {
            case GnssStatus.CONSTELLATION_GPS:
                return svid; // GPS: SVID 1-32 maps directly to RTKLib 1-32

            case GnssStatus.CONSTELLATION_SBAS:
                return svid; // SBAS: typically 120-142 range

            case GnssStatus.CONSTELLATION_GLONASS:
                return svid + 38; // GLONASS: SVID 1-24 -> RTKLib 39-62

            case GnssStatus.CONSTELLATION_GALILEO:
                return svid + 70; // Galileo: SVID 1-36 -> RTKLib 71-106

            case GnssStatus.CONSTELLATION_BEIDOU:
                return svid + 142; // BeiDou: SVID 1-37 -> RTKLib 143-179

            case GnssStatus.CONSTELLATION_QZSS:
                return svid + 192; // QZSS: SVID 1-10 -> RTKLib 193-202

            default:
                if (DBG) Log.w(TAG, "Unknown constellation type: " + constellationType);
                return svid; // Fallback to raw SVID
        }
    }

    private String getConstellationName(int constellationType) {
        switch (constellationType) {
            case GnssStatus.CONSTELLATION_GPS:
                return "GPS";
            case GnssStatus.CONSTELLATION_GLONASS:
                return "GLONASS";
            case GnssStatus.CONSTELLATION_GALILEO:
                return "Galileo";
            case GnssStatus.CONSTELLATION_BEIDOU:
                return "BeiDou";
            case GnssStatus.CONSTELLATION_QZSS:
                return "QZSS";
            case GnssStatus.CONSTELLATION_SBAS:
                return "SBAS";
            default:
                return "Unknown";
        }
    }

    private void unregisterGnssStatusCallback() {
        if (locationManager != null && gnssStatusCallback != null) {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
            gnssStatusCallback = null;
        }
    }

    public int getNbSat() {
        return nbSat.get();
    }

    public float getAge() {
        return ((float) (System.currentTimeMillis() - lTime) / 1000);
    }

    public float getNAccuracy() {
        return accuracy;
    }

    public float getEAccuracy() {
        return accuracy;
    }

    public float getVAccuracy() {
        return 2 * accuracy;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        accuracy = location.getAccuracy();
        mPos = new Position3d(
                Math.toRadians(location.getLatitude()),
                Math.toRadians(location.getLongitude()),
                location.getAltitude());
        lTime = System.currentTimeMillis();
    }
}