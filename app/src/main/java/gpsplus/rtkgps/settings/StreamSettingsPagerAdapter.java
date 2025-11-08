package gpsplus.rtkgps.settings;

import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import gpsplus.rtkgps.R;

public class StreamSettingsPagerAdapter extends FragmentStateAdapter {

    public static final int STREAM_INPUT_SETTINGS = 0;
    public static final int STREAM_OUTPUT_SETTINGS = 1;
    public static final int STREAM_LOG_SETTINGS = 2;

    private final int streamType;
    private final Resources resources;

    public StreamSettingsPagerAdapter(@NonNull FragmentActivity fa, int streamType) {
        super(fa);
        this.streamType = streamType;
        this.resources = fa.getResources();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (streamType) {
            case STREAM_INPUT_SETTINGS:
                switch (position) {
                    case 0:
                        return new InputRoverFragment();
                    case 1:
                        return new InputBaseFragment();
                    case 2:
                        return new InputCorrectionFragment();
                    default:
                        throw new IllegalArgumentException("Invalid position for input streams");
                }
            case STREAM_OUTPUT_SETTINGS:
                switch (position) {
                    case 0:
                        return new OutputSolution1Fragment();
                    case 1:
                        return new OutputSolution2Fragment();
                    case 2:
                        return new OutputGPXTraceFragment();
                    default:
                        throw new IllegalArgumentException("Invalid position for output streams");
                }
            case STREAM_LOG_SETTINGS:
                switch (position) {
                    case 0:
                        return new LogRoverFragment();
                    case 1:
                        return new LogBaseFragment();
                    case 2:
                        return new LogCorrectionFragment();
                    default:
                        throw new IllegalArgumentException("Invalid position for log streams");
                }
            default:
                throw new IllegalArgumentException("Invalid stream type");
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public CharSequence getPageTitle(int position) {
        switch (streamType) {
            case STREAM_INPUT_SETTINGS:
                switch (position) {
                    case 0:
                        return resources.getString(R.string.input_streams_settings_rover_tab_title);
                    case 1:
                        return resources.getString(R.string.input_streams_settings_base_tab_title);
                    case 2:
                        return resources.getString(R.string.input_streams_settings_correction_tab_title);
                }
                break;
            case STREAM_OUTPUT_SETTINGS:
                switch (position) {
                    case 0:
                        return resources.getString(R.string.output_streams_settings_solution1_tab_title);
                    case 1:
                        return resources.getString(R.string.output_streams_settings_solution2_tab_title);
                    case 2:
                        return resources.getString(R.string.output_streams_settings_gpxtrace_tab_title);
                }
                break;
            case STREAM_LOG_SETTINGS:
                switch (position) {
                    case 0:
                        return resources.getString(R.string.log_stream_settings_rover_tab_title);
                    case 1:
                        return resources.getString(R.string.log_stream_settings_base_tab_title);
                    case 2:
                        return resources.getString(R.string.log_stream_settings_correction_tab_title);
                }
                break;
        }
        return null;
    }
}

