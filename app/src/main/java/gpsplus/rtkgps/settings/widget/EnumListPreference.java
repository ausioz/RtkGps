package gpsplus.rtkgps.settings.widget;

import android.content.Context;
import android.content.res.Resources;

import androidx.preference.ListPreference;

import android.util.AttributeSet;
import android.util.Log;

import gpsplus.rtklib.constants.IHasRtklibId;

public class EnumListPreference<T extends Enum<T> & IHasRtklibId> extends ListPreference {

    private T mDefault;

    public EnumListPreference(Context context) {
        this(context, null);
    }

    public EnumListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EnumListPreference(Context context, T[] values, T defaultValue) {
        this(context, null, values, defaultValue);
    }

    public EnumListPreference(Context context, AttributeSet attrs, T[] values, T defaultValue) {
        this(context, attrs);
        setValues(values);
        setDefaultValue(defaultValue);
    }

    public void setValue(T value) {
        this.setValue(value.name());
    }

    public void setValues(T[] values) {
        final CharSequence[] entries;
        final CharSequence[] entryValues;
        final Resources r;

        entries = new CharSequence[values.length];
        entryValues = new CharSequence[values.length];

        r = getContext().getResources();

        for (int i = 0; i < values.length; ++i) {
            entries[i] = r.getString(values[i].getNameResId());
            entryValues[i] = values[i].name();
        }

        this.setEntries(entries);
        this.setEntryValues(entryValues);

        if (mDefault == null && values.length > 0) {
            setDefaultValue(values[0]);
        }
    }

    public void setDefaultValue(T value) {
        mDefault = value;
        this.setDefaultValue(value.name());
    }

    public T getValueT() {
        if (mDefault == null) {
            Log.e("EnumListPreference", "Default value is null. Cannot resolve enum.");
            return null;
        }

        String value = getValue();
        if (value == null) {
            Log.e("EnumListPreference", "Stored preference value is null. Using default: " + mDefault.name());
            return mDefault;
        }

        try {
            return T.valueOf(mDefault.getDeclaringClass(), value);
        } catch (IllegalArgumentException e) {
            Log.e("EnumListPreference", "Invalid enum value '" + value + "' for " + mDefault.getDeclaringClass().getSimpleName(), e);
        } catch (Exception e) {
            Log.e("EnumListPreference", "Unexpected error parsing enum value", e);
        }

        return mDefault;
    }


}
