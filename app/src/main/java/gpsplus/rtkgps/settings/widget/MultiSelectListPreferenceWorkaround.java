package gpsplus.rtkgps.settings.widget;

import java.util.Set;

import android.content.Context;
import androidx.preference.MultiSelectListPreference;
import android.util.AttributeSet;

public class MultiSelectListPreferenceWorkaround extends MultiSelectListPreference {

    public MultiSelectListPreferenceWorkaround(Context context,
            AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiSelectListPreferenceWorkaround(Context context) {
        super(context);
    }

    @Override
    public void setValues(Set<String> values) {

        /**
         * Workaround for amnesia problem in MultiSelectListPreference
         * https://android.googlesource.com/platform/frameworks/base/+/cd9ea08d9cb68004b2d5f69302cddf53dc034e7b%5E!/
         *
         */
        super.setValues(values);
    }
}
