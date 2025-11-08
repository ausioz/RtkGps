package gpsplus.rtkgps;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.proj4.PJ;

import java.io.File;

//@ReportsCrashes(formKey = "",
//    mailTo = "bug@sudagri-jatropha.com",
//    mode = ReportingInteractionMode.TOAST,
//    resToastText = R.string.crash_toast_text)
public class RtkGps extends Application {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static String VERSION = "";

    @Override
    public void onCreate() {
        super.onCreate();
        if (DBG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
//                    .penaltyDeath()
                    .build());
        }
        //ACRA.init(this);
        System.loadLibrary("proj");
        Log.v("Proj4","Proj4 version: "+ PJ.getVersion());

//        System.loadLibrary("ntripcaster");
//        Log.v("ntripcaster","NTRIP Caster "+NTRIPCaster.getVersion());

        System.loadLibrary("rtkgps");

        //System.loadLibrary("gdalalljni"); //Automaticaly done
//        ogr.RegisterAll();
//        gdal.AllRegister();
//        Log.v("GDAL",gdal.VersionInfo("--version"));
        //set version
        PackageInfo pi;
        try {
            pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            RtkGps.VERSION = pi.versionName;
        } catch (NameNotFoundException e) {
            Log.e("RtkGps", "onCreate: " + e.getMessage());
        }

    }

    public static String getVersion() {
        return RtkGps.VERSION;
    }
}
