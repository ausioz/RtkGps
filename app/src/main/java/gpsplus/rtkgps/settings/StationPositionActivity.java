package gpsplus.rtkgps.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtkgps.databinding.ActivityStationPositionBinding;
import gpsplus.rtklib.RtkCommon;
import gpsplus.rtklib.RtkCommon.Position3d;
import gpsplus.rtklib.constants.StationPositionType;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;


public class StationPositionActivity extends AppCompatActivity {

    private static final boolean DBG = BuildConfig.DEBUG;
    static final String TAG = StationPositionActivity.class.getSimpleName();

    public static final String ARG_SHARED_PREFS_NAME = "shared_prefs_name";

    public static final String ARG_HIDE_USE_RTCM = "hide_use_rtcm";

    public static final String SHARED_PREFS_KEY_POSITION_FORMAT = "station_position_format";

    public static final String SHARED_PREFS_KEY_POSITION_TYPE = "station_position_type";

    public static final String SHARED_PREFS_KEY_STATION_X = "station_position_x";

    public static final String SHARED_PREFS_KEY_STATION_Y = "station_position_y";

    public static final String SHARED_PREFS_KEY_STATION_Z = "station_position_z";

    private static final double SHARED_PREFS_XYZ_MULT = 0.0001;

    private static final Pattern sLlhDmsPattern = Pattern.compile(
            "([+-]?\\d{1,2})\\s+(\\d{1,2})\\s+(\\d{1,2}(?:\\.\\d+)?)");

    public static class Value {

        private StationPositionType mType;

        private final Position3d mPosition;

        public Value() {
            mType = StationPositionType.RTCM_POS;
            mPosition = new Position3d();
        }

        public Value(StationPositionType type, Position3d position) {
            this();
            mType = type;
            mPosition.setValues(position);
        }

        public StationPositionType getType() {
            return mType;
        }

        public Position3d getPosition() {
            return new Position3d(mPosition);
        }

        public Value copy() {
            return new Value(mType, mPosition);
        }
    }

    private String mSharedPrefsName;
    private boolean mHideUseRtcm;

    private ArrayAdapter<PositionFormat> mPositionFormatAdapter;

    private PositionFormat mCurrentFormat;

    private StationPositionType mPositionType;
    private ActivityStationPositionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStationPositionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final Intent intent = getIntent();
        mSharedPrefsName = intent.getStringExtra(ARG_SHARED_PREFS_NAME);
        if (mSharedPrefsName == null) {
            throw new IllegalArgumentException("ARG_SHARED_PREFS_NAME not defined");
        }
        mHideUseRtcm = intent.getBooleanExtra(ARG_HIDE_USE_RTCM, false);

        createPositionFormatAdapter();
        binding.positionFormat.setAdapter(mPositionFormatAdapter);

        loadSettings();

        binding.positionFormat.setOnItemSelectedListener(mOnFormatSelectedListener);

        if (!mHideUseRtcm) {
            binding.useRtcmAntennaPosition.setOnCheckedChangeListener(mOnCheckedChangeListener);
        } else {
            binding.useRtcmAntennaPosition.setVisibility(View.GONE);
        }

        binding.btCancel.setOnClickListener(v -> finish());
        binding.btOk.setOnClickListener(v -> {
            saveSettings();
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCurrentFormat = (PositionFormat) binding.positionFormat.getSelectedItem();
        mPositionType = binding.useRtcmAntennaPosition.isChecked() ? StationPositionType.RTCM_POS
                : StationPositionType.POS_IN_PRCOPT;
    }

    public void onCancelButtonClicked(View v) {
        finish();
    }

    public void onOkButtonClicked(View v) {
        saveSettings();
        finish();
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName, Value value) {
        final SharedPreferences prefs;
        prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

        prefs.edit()
                .putString(SHARED_PREFS_KEY_POSITION_FORMAT, PositionFormat.LLH.toString())
                .putString(SHARED_PREFS_KEY_POSITION_TYPE, value.mType.toString())
                .putLong(SHARED_PREFS_KEY_STATION_X, Math.round(value.mPosition.getX() / SHARED_PREFS_XYZ_MULT))
                .putLong(SHARED_PREFS_KEY_STATION_Y, Math.round(value.mPosition.getY() / SHARED_PREFS_XYZ_MULT))
                .putLong(SHARED_PREFS_KEY_STATION_Z, Math.round(value.mPosition.getZ() / SHARED_PREFS_XYZ_MULT))
                .apply();

    }

    @Nonnull
    public static Value readSettings(SharedPreferences prefs) {
        Position3d position;
        StationPositionType type;

        try {
            position = new Position3d(
                    prefs.getLong(SHARED_PREFS_KEY_STATION_X, 0) * SHARED_PREFS_XYZ_MULT,
                    prefs.getLong(SHARED_PREFS_KEY_STATION_Y, 0) * SHARED_PREFS_XYZ_MULT,
                    prefs.getLong(SHARED_PREFS_KEY_STATION_Z, 0) * SHARED_PREFS_XYZ_MULT
            );
            type = StationPositionType.valueOf(prefs.getString(SHARED_PREFS_KEY_POSITION_TYPE,
                    StationPositionType.POS_IN_PRCOPT.name()));
        } catch (ClassCastException cce) {
            position = new Position3d(0, 0, 0);
            type = StationPositionType.POS_IN_PRCOPT;
        }

        return new Value(type, position);
    }

    public static String readSummary(Resources r, SharedPreferences prefs) {
        final Value settings;
        final Position3d pos;

        settings = readSettings(prefs);
        if (settings.mType == StationPositionType.RTCM_POS) {
            return r.getString(StationPositionType.RTCM_POS.getNameResId());
        } else {
            pos = RtkCommon.ecef2pos(settings.mPosition);
            return String.format(Locale.US, "%s, %s, %.3f",
                    RtkCommon.Deg2Dms.toString(Math.toDegrees(pos.getLat()), true),
                    RtkCommon.Deg2Dms.toString(Math.toDegrees(pos.getLon()), false),
                    pos.getHeight());
        }
    }


    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(mSharedPrefsName, MODE_PRIVATE);

        PositionFormat format;
        try {
            format = PositionFormat.valueOf(prefs.getString(SHARED_PREFS_KEY_POSITION_FORMAT, PositionFormat.LLH.name()));
        } catch (IllegalArgumentException iae) {
            Log.e(TAG, "loadSettings: " + iae.getMessage());
            format = PositionFormat.LLH;
        }

        Value settings = readSettings(prefs);

        binding.positionFormat.setSelection(mPositionFormatAdapter.getPosition(format));
        setCoordinates(settings.mPosition, format);

        binding.useRtcmAntennaPosition.setChecked(!mHideUseRtcm && (settings.mType == StationPositionType.RTCM_POS));
        onUseRtcmChanged(settings.mType == StationPositionType.RTCM_POS);
    }

    private void saveSettings() {
        final Position3d ecefPos;

        ecefPos = readEcefPosition();

        getSharedPreferences(mSharedPrefsName, MODE_PRIVATE)
                .edit()
                .putString(SHARED_PREFS_KEY_POSITION_FORMAT, mCurrentFormat.name())
                .putString(SHARED_PREFS_KEY_POSITION_TYPE, mPositionType.name())
                .putLong(SHARED_PREFS_KEY_STATION_X, Math.round(ecefPos.getX() / SHARED_PREFS_XYZ_MULT))
                .putLong(SHARED_PREFS_KEY_STATION_Y, Math.round(ecefPos.getY() / SHARED_PREFS_XYZ_MULT))
                .putLong(SHARED_PREFS_KEY_STATION_Z, Math.round(ecefPos.getZ() / SHARED_PREFS_XYZ_MULT))
                .apply();
    }

    private void createPositionFormatAdapter() {
        mPositionFormatAdapter = new ArrayAdapter<PositionFormat>(this, android.R.layout.simple_dropdown_item_1line) {

            @Override
            public View getDropDownView(int position, View convertView,
                                        @NonNull ViewGroup parent) {
                final View v = super.getDropDownView(position, convertView, parent);
                ((TextView) v).setText(Objects.requireNonNull(getItem(position)).mTitleId);
                return v;
            }

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                final View v = super.getView(position, convertView, parent);
                ((TextView) v).setText(Objects.requireNonNull(getItem(position)).mTitleId);
                return v;
            }
        };
        mPositionFormatAdapter.addAll(PositionFormat.values());
        mPositionFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    private void onUseRtcmChanged(boolean checked) {
        if (checked && !mHideUseRtcm) {
            mPositionType = StationPositionType.RTCM_POS;
        } else {
            mPositionType = StationPositionType.POS_IN_PRCOPT;
        }

        binding.positionFormat.setEnabled(!checked);
        binding.coord1Title.setEnabled(!checked);
        binding.coord2Title.setEnabled(!checked);
        binding.coord3Title.setEnabled(!checked);
        binding.coord1.setEnabled(!checked);
        binding.coord2.setEnabled(!checked);
        binding.coord3.setEnabled(!checked);
    }

    private void setCoordinates(Position3d ecefPos, PositionFormat format) {
        final int coordsArrId;
        Position3d newPos;
        final String[] coordsArr;

        switch (format) {
            case ECEF:
                coordsArrId = R.array.solution_view_coordinates_ecef;
                newPos = ecefPos;
                binding.coord1.setText(String.format(Locale.US, "%.4f", newPos.getX()));
                binding.coord2.setText(String.format(Locale.US, "%.4f", newPos.getY()));
                binding.coord3.setText(String.format(Locale.US, "%.4f", newPos.getZ()));
                break;
            case LLH:
                coordsArrId = R.array.solution_view_coordinates_wgs84;
                newPos = RtkCommon.ecef2pos(ecefPos);
                binding.coord1.setText(String.format(Locale.US, "%.9f", Math.toDegrees(newPos.getLat())));
                binding.coord2.setText(String.format(Locale.US, "%.9f", Math.toDegrees(newPos.getLon())));
                binding.coord3.setText(String.format(Locale.US, "%.4f", newPos.getHeight()));
                break;
            case LLH_DMS:
                coordsArrId = R.array.solution_view_coordinates_wgs84;
                newPos = RtkCommon.ecef2pos(ecefPos);
                binding.coord1.setText(formatLlhDdms(newPos.getLat()));
                binding.coord2.setText(formatLlhDdms(newPos.getLon()));
                binding.coord3.setText(String.format(Locale.US, "%.4f", newPos.getHeight()));
                break;
            default:
                throw new IllegalStateException();
        }

        coordsArr = getResources().getStringArray(coordsArrId);
        binding.coord1Title.setText(coordsArr[0]);
        binding.coord2Title.setText(coordsArr[1]);
        binding.coord3Title.setText(coordsArr[2]);

        mCurrentFormat = format;
    }

    /**
     * @return ECEF prosition from fields
     */
    private Position3d readEcefPosition() {
        double x, y, z;
        double lat, lon, height;
        String cord1 = binding.coord1.getText().toString();
        String cord2 = binding.coord2.getText().toString();
        String cord3 = binding.coord3.getText().toString();

        switch (mCurrentFormat) {
            case ECEF:
                try {
                    x = Double.parseDouble(cord1);
                } catch (NumberFormatException nfe) {
                    x = 0;
                }
                try {
                    y = Double.parseDouble(cord2);
                } catch (NumberFormatException nfe) {
                    y = 0;
                }
                try {
                    z = Double.parseDouble(cord3);
                } catch (NumberFormatException nfe) {
                    z = 0;
                }

                return new Position3d(x, y, z);

            case LLH:
                try {
                    lat = Math.toRadians(Double.parseDouble(cord1));
                } catch (NumberFormatException nfe) {
                    lat = 0;
                }
                try {
                    lon = Math.toRadians(Double.parseDouble(cord2));
                } catch (NumberFormatException nfe) {
                    lon = 0;
                }
                try {
                    height = Double.parseDouble(cord3);
                } catch (NumberFormatException nfe) {
                    height = 0;
                }

                return RtkCommon.pos2ecef(lat, lon, height, null);

            case LLH_DMS:
                lat = scanLlhDdms(cord1);
                lon = scanLlhDdms(cord2);
                try {
                    height = Double.parseDouble(cord3);
                } catch (NumberFormatException nfe) {
                    height = 0;
                }
                return RtkCommon.pos2ecef(lat, lon, height, null);

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * @return radians
     */
    private double scanLlhDdms(String llhDms) {
        double res;
        double deg, min, sec, sign;

        Matcher m = sLlhDmsPattern.matcher(llhDms);
        if (!m.matches()) return 0.0f;

        deg = Double.parseDouble(Objects.requireNonNull(m.group(1)));
        min = Double.parseDouble(Objects.requireNonNull(m.group(2)));
        sec = Double.parseDouble(Objects.requireNonNull(m.group(3)));

        sign = Math.signum(deg);
        res = sign * (Math.abs(deg) + min / 60.0 + sec / 3600.0);

        return Math.toRadians(res);
    }

    /**
     * @param val radians
     */
    private String formatLlhDdms(double val) {
        RtkCommon.Deg2Dms dms;
        val = Math.toDegrees(val);
        val += Math.signum(val) * 1.0E-12;
        dms = new RtkCommon.Deg2Dms(val);
        return String.format(Locale.US, "%.0f %02.0f %09.6f", dms.degree, dms.minute, dms.second);
    }

    private final AdapterView.OnItemSelectedListener mOnFormatSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position,
                                   long id) {
            final PositionFormat newFormat;

            newFormat = (PositionFormat) parent.getItemAtPosition(position);

            if (DBG) Log.v(TAG, "onItemSelected() " + mCurrentFormat + " -> " + newFormat);

            setCoordinates(readEcefPosition(), newFormat);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = (buttonView, isChecked) -> onUseRtcmChanged(isChecked);

    private enum PositionFormat {

        LLH(R.string.station_position_format_llh),

        LLH_DMS(R.string.station_position_format_llh_dms),

        ECEF(R.string.station_position_format_ecef);

        private final int mTitleId;

        PositionFormat(int titleId) {
            mTitleId = titleId;
        }
    }

}