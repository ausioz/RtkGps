package gpsplus.rtkgps.settings;

import static android.view.View.LAYER_TYPE_SOFTWARE;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import gpsplus.rtkgps.R;
import gpsplus.rtkgps.databinding.ActivityInputStreamSettingsBinding;

public class StreamSettingsActivity extends AppCompatActivity {

    public static final String ARG_STEAM = "stream";

    private StreamSettingsPagerAdapter mSectionsPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityInputStreamSettingsBinding binding = ActivityInputStreamSettingsBinding.inflate(
                getLayoutInflater()
        );
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.getRoot().setLayerType(LAYER_TYPE_SOFTWARE, null);

        TabLayout mTabLayout = binding.tabLayout;
        ViewPager2 mViewPager = binding.viewPager;

        int stream = getIntent().getIntExtra(ARG_STEAM, StreamSettingsPagerAdapter.STREAM_INPUT_SETTINGS);

        mSectionsPagerAdapter = new StreamSettingsPagerAdapter(this, stream);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        switch (stream) {
            case StreamSettingsPagerAdapter.STREAM_INPUT_SETTINGS:
                setTitle(R.string.title_activity_input_stream_settings);
                break;
            case StreamSettingsPagerAdapter.STREAM_OUTPUT_SETTINGS:
                setTitle(R.string.title_activity_output_stream_settings);
                break;
            case StreamSettingsPagerAdapter.STREAM_LOG_SETTINGS:
                setTitle(R.string.title_activity_log_stream_settings);
                break;
            default:
                throw new IllegalArgumentException("Wrong ARG_STEAM");
        }

        new TabLayoutMediator(mTabLayout, mViewPager, (tab, position) -> tab.setText(mSectionsPagerAdapter.getPageTitle(position))).attach();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}