package gpsplus.rtkgps.settings;

import android.content.Context;
import android.os.Bundle;

//import com.dropbox.sync.android.DbxAccountManager;

import androidx.preference.PreferenceFragmentCompat;

import gpsplus.rtkgps.R;

public class OutputGPXTraceFragment extends PreferenceFragmentCompat {

    public static final String SHARED_PREFS_NAME = "OutputGPXTrace";

    public static final String KEY_ENABLE = "enable";
    public static final String KEY_FILENAME = "gpxtrace_file_filename";
    // public static final String KEY_SYNCDROPBOX = "syncdropbox"; // Uncomment if needed

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Set the shared preferences name before loading preferences
        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);

        // Load the preferences from XML resource
        setPreferencesFromResource(R.xml.output_gpxtrace_settings, rootKey);

        // Example of setting a preference change listener (commented, for Dropbox)
        /*
        Preference syncDropboxPref = findPreference(KEY_SYNCDROPBOX);
        if (syncDropboxPref != null) {
            syncDropboxPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isChecked = (Boolean) newValue;
                if (isChecked) {
                    DbxAccountManager dbxAcctMgr = DbxAccountManager.getInstance(
                        requireContext().getApplicationContext(),
                        MainActivity.APP_KEY,
                        MainActivity.APP_SECRET
                    );
                    if (!dbxAcctMgr.hasLinkedAccount()) {
                        dbxAcctMgr.startLink(getActivity(), MainActivity.REQUEST_LINK_TO_DBX);
                    }
                }
                return true;
            });
        }
        */
    }

//    @Override
//    public Context getContext() {
//        // return non-null context safely
//        return requireContext();
//    }
}