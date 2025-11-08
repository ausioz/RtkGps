package gpsplus.rtkgps.settings;

import static android.view.View.LAYER_TYPE_SOFTWARE;

import gpsplus.rtkgps.R;
import gpsplus.rtkgps.databinding.ActivitySettingsBinding;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    public static final String TAG = "SettingsActivity";
    public static final String EXTRA_FRAGMENT_NAME = "fragment_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySettingsBinding binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.getRoot().setLayerType(LAYER_TYPE_SOFTWARE, null);
        
        if (savedInstanceState == null) {
            String fragmentName = getIntent().getStringExtra(EXTRA_FRAGMENT_NAME);
            Fragment fragment = createFragmentByName(fragmentName);

            if (fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(binding.settingsContainer.getId(), fragment)
                        .commit();
            } else {
                finish();
            }
        }
    }

    private Fragment createFragmentByName(String name) {
        try {
            Class<?> clazz = Class.forName(name);
            if (PreferenceFragmentCompat.class.isAssignableFrom(clazz)) {
                return (Fragment) clazz.newInstance();
            }
        } catch (Exception e) {
            Log.e(TAG, "createFragmentByName: " + e.getMessage() );
        }
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

