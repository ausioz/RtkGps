package gpsplus.rtkgps.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import gpsplus.rtkgps.BuildConfig;
import gpsplus.rtkgps.R;
import gpsplus.rtkgps.settings.widget.EarthTideCorrectionPreference;
import gpsplus.rtkgps.settings.widget.EphemerisOptionPreference;
import gpsplus.rtkgps.settings.widget.IonosphereCorrectionPreference;
import gpsplus.rtkgps.settings.widget.MultiSelectListPreferenceWorkaround;
import gpsplus.rtkgps.settings.widget.PositioningModePreference;
import gpsplus.rtkgps.settings.widget.TroposphereCorrectionPreference;
import gpsplus.rtklib.ProcessingOptions;
import gpsplus.rtklib.constants.EarthTideCorrectionType;
import gpsplus.rtklib.constants.EphemerisOption;
import gpsplus.rtklib.constants.IonosphereOption;
import gpsplus.rtklib.constants.NavigationSystem;
import gpsplus.rtklib.constants.PositioningMode;
import gpsplus.rtklib.constants.TroposphereOption;


public class ProcessingOptions1Fragment extends PreferenceFragmentCompat {

    private static final boolean DBG = BuildConfig.DEBUG;
    static final String TAG = ProcessingOptions1Fragment.class.getSimpleName();

    public static final String SHARED_PREFS_NAME = "ProcessingOptions1";

    static final String KEY_POSITIONING_MODE = "positioning_mode";
    static final String KEY_NUMBER_OF_FREQUENCIES = "number_of_frequencies";
    static final String KEY_NAVIGATION_SYSTEM = "navigation_system";
    static final String KEY_ELEVATION_MASK = "elevation_mask";
    static final String KEY_SNR_MASK = "snr_mask";
    static final String KEY_REC_DYNAMICS = "rec_dynamics";
    static final String KEY_EARTH_TIDES_CORRECTION = "rec_earth_tides_correction";
    static final String KEY_IONOSPHERE_CORRECTION = "ionosphere_correction";
    static final String KEY_TROPOSPHERE_CORRECTION = "troposphere_correction";
    public static final String KEY_SAT_EPHEM_CLOCK = "satellite_ephemeris_clock";
    static final String KEY_SAT_ANTENNA_PCV = "sat_antenna_pcv";
    static final String KEY_RECEIVER_ANTENNA_PCV = "receiver_antenna_pcv";
    static final String KEY_PHASE_WINDUP_CORRECTION = "phase_windup_correction";
    static final String KEY_EXCLUDE_ECLIPSING = "exclude_eclipsing_sat_measurements";
    static final String KEY_RAIM_FDE = "raim_fde";
    public static final String KEY_PROCESSING_CYCLE = "processing_cycle";
    public static final String KEY_AMBIGUITY_RESOLUTION = "ambiguity_resolution";
    public static final String KEY_GLONASS_AMBIGUITY_RESOLUTION = "glonass_ambiguity_resolution";

    public static final String KEY_GPS_AMBIGUITY_RESOLUTION = "gps_ambiguity_resolution";
    public static final String KEY_BDS_AMBIGUITY_RESOLUTION = "bds_ambiguity_resolution";

    public static final String KEY_AR_FILTER = "ar_filter";

    public static final String KEY_AR_MIN_FIX = "ar_min_fix";
    public static final String KEY_MIN_FIX_SATS = "min_fix_sats";
    public static final String KEY_MIN_HOLD_SATS = "min_hold_sats";
    public static final String KEY_MIN_DROP_SATS = "min_drop_sats";
    public static final String KEY_RCV_STDS = "rcv_stds";

    public static final String KEY_AR_MAX_ITER = "ar_max_iter";
    public static final String KEY_N_ITER = "n_iter";
    public static final String KEY_MAX_AVE_AMB = "max_ave_ep";

    public static final String KEY_VAR_HOLD_AMB = "var_hold_amb";
    public static final String KEY_GAIN_HOLD_AMB = "gain_hold_amb";

    public static final String KEY_INIT_RST = "init_rst";
    public static final String KEY_OUT_SINGLE = "out_single";
    public static final String KEY_SYNC_SOL = "sync_sol";
    public static final String KEY_FREQ_OPT = "freq_opt";

    public static final String KEY_AR_OUT_CNT = "ar_out_cnt";
    public static final String KEY_SLIP_THRES = "slip_thres";
    public static final String KEY_MAX_AGE = "max_age";
    public static final String KEY_REJ_GDOP = "rej_gdop";
    public static final String KEY_REJ_IONNO = "rej_ionno";


    public static final String KEY_MIN_FIX_RATIO = "min_fix_ratio";
    public static final String KEY_MAX_POS_VAR = "max_pos_var";
    private static final String KEY_MIN_FIX_ELEVATION = "min_fix_elevation";
    private static final String KEY_MIN_HOLD_ELEVATION = "min_hold_elevation";

    private static final String KEY_MIN_FIX_LOCK = "min_fix_lock";

    // Settings
    private PositioningModePreference mPositioningModePref;
    private ListPreference mNumberOfFrequenciesPref;
    private MultiSelectListPreferenceWorkaround mNavigationSystem;
    private ListPreference mElevationMaskPref;
    private ListPreference mSnrMaskPref;
    private ListPreference mAmbiguityResolutionPref;
    private ListPreference mGlonassAmbiguityResolutionPref;

    private ListPreference mGpsAmbiguityResolutionPref;
    private ListPreference mBDSAmbiguityResolutionPref;

    private ListPreference mArFilterPref;

    private EditTextPreference mArMinFixPref;
    private EditTextPreference mMinFixSatsPref;
    private EditTextPreference mMinHoldSatsPref;
    private EditTextPreference mMinDropSatsPref;
    private ListPreference mRcvStdsPref;

    private EditTextPreference mArMaxIterPref;
    private EditTextPreference mNIterPref;
    private EditTextPreference mMaxAveEpPref;

    private EditTextPreference mVarHoldAmbPref;
    private EditTextPreference mGainHoldAmbPref;

    private ListPreference mInitRstPref;
    private ListPreference mOutSinglePref;
    private ListPreference mSyncSolPref;
    private ListPreference mFreqOptPref;

    private EditTextPreference mArOutCntPref;
    private EditTextPreference mMaxAgePref;
    private EditTextPreference mSlipThreshPref;
    private EditTextPreference mRejGDopPref;
    private EditTextPreference mRejIonnoPref;

    private EarthTideCorrectionPreference mEarthTidesCorrPref;
    private IonosphereCorrectionPreference mIonosphereCorrectionPref;
    private TroposphereCorrectionPreference mTroposphereCorrectionPref;
    private EphemerisOptionPreference mSatEphemClockPref;
    private EditTextPreference mMinRatioFixPref;
    private EditTextPreference mMaxPosVarPref;
    private EditTextPreference mMinElevationFixPref;
    private EditTextPreference mMinElevationHoldPref;
    private EditTextPreference mMinLockFixPref;

    private final PreferenceChangeListener mPreferenceChangeListener;

    public ProcessingOptions1Fragment() {
        mPreferenceChangeListener = new PreferenceChangeListener();
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        Log.d("wkwk", "onCreatePreferences: " + rootKey);
        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
        setPreferencesFromResource(R.xml.processing_options1, rootKey);

        if (rootKey == null) {
            requireActivity().setTitle(R.string.navdraw_item_processing_options);
        } else if (rootKey.equals("button_ambiguity_setting_key")) {
            requireActivity().setTitle(R.string.procopt_ambiguity_resolution_sub_preference_title);
        }

        initSettings1();
    }

    @Override
    public boolean onPreferenceTreeClick(@NonNull Preference preference) {
        if (preference instanceof PreferenceScreen) {
            String key = preference.getKey();

            // Open a new fragment showing this nested screen
            ProcessingOptions1Fragment fragment = new ProcessingOptions1Fragment();
            Bundle args = new Bundle();
            args.putString(ARG_PREFERENCE_ROOT, key);
            fragment.setArguments(args);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, fragment)
                    .addToBackStack(null)
                    .commit();

            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEnable();
        reloadSummaries();
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        super.onPause();
    }

    private void initSettings1() {
        final Resources r = getResources();
        ProcessingOptions opts = readPrefs(requireActivity());

        // Positioning mode
        mPositioningModePref = findPreference(KEY_POSITIONING_MODE);

        // Number of frequencies
        mNumberOfFrequenciesPref = findPreference(KEY_NUMBER_OF_FREQUENCIES);

        // Navigation system
        mNavigationSystem = findPreference(KEY_NAVIGATION_SYSTEM);
        if (mNavigationSystem != null) {
            mNavigationSystem.setEntries(NavigationSystem.getEntries(r));
            mNavigationSystem.setEntryValues(NavigationSystem.getEntryValues());

            final Set<String> navsysSet = new HashSet<>(7);
            if (opts.getNavigationSystem() != null) {
                for (NavigationSystem ns : opts.getNavigationSystem()) {
                    navsysSet.add(ns.name());
                }
            }
            mNavigationSystem.setValues(navsysSet);
        } else {
            Log.w(TAG, "NavigationSystem preference is null!");
        }

        // Elevation mask
        mElevationMaskPref = findPreference(KEY_ELEVATION_MASK);

        // Earth tides correction
        mEarthTidesCorrPref = findPreference(KEY_EARTH_TIDES_CORRECTION);

        // Ionosphere correction
        mIonosphereCorrectionPref = findPreference(KEY_IONOSPHERE_CORRECTION);

        // Troposphere correction
        mTroposphereCorrectionPref = findPreference(KEY_TROPOSPHERE_CORRECTION);

        // Satellite Ephemeris/Clock
        mSatEphemClockPref = findPreference(KEY_SAT_EPHEM_CLOCK);

        // SNR mask
        mSnrMaskPref = findPreference(KEY_SNR_MASK);

        // Ambiguity resolution preferences
        mAmbiguityResolutionPref = findPreference(KEY_AMBIGUITY_RESOLUTION);
        mGlonassAmbiguityResolutionPref = findPreference(KEY_GLONASS_AMBIGUITY_RESOLUTION);
        mGpsAmbiguityResolutionPref = findPreference(KEY_GPS_AMBIGUITY_RESOLUTION);
        mBDSAmbiguityResolutionPref = findPreference(KEY_BDS_AMBIGUITY_RESOLUTION);

        // AR Filter
        mArFilterPref = findPreference(KEY_AR_FILTER);

        // Numeric preferences
        mArMinFixPref = findPreference(KEY_AR_MIN_FIX);
        mMinFixSatsPref = findPreference(KEY_MIN_FIX_SATS);
        mMinHoldSatsPref = findPreference(KEY_MIN_HOLD_SATS);
        mMinDropSatsPref = findPreference(KEY_MIN_DROP_SATS);
        mRcvStdsPref = findPreference(KEY_RCV_STDS);
        mArMaxIterPref = findPreference(KEY_AR_MAX_ITER);
        mNIterPref = findPreference(KEY_N_ITER);
        mMaxAveEpPref = findPreference(KEY_MAX_AVE_AMB);
        mVarHoldAmbPref = findPreference(KEY_VAR_HOLD_AMB);
        mGainHoldAmbPref = findPreference(KEY_GAIN_HOLD_AMB);
        mInitRstPref = findPreference(KEY_INIT_RST);
        mOutSinglePref = findPreference(KEY_OUT_SINGLE);
        mSyncSolPref = findPreference(KEY_SYNC_SOL);
        mFreqOptPref = findPreference(KEY_FREQ_OPT);
        mArOutCntPref = findPreference(KEY_AR_OUT_CNT);
        mSlipThreshPref = findPreference(KEY_SLIP_THRES);
        mMaxAgePref = findPreference(KEY_MAX_AGE);
        mRejGDopPref = findPreference(KEY_REJ_GDOP);
        mRejIonnoPref = findPreference(KEY_REJ_IONNO);
        mMinRatioFixPref = findPreference(KEY_MIN_FIX_RATIO);
        mMaxPosVarPref = findPreference(KEY_MAX_POS_VAR);
        mMinLockFixPref = findPreference(KEY_MIN_FIX_LOCK);
        mMinElevationFixPref = findPreference(KEY_MIN_FIX_ELEVATION);
        mMinElevationHoldPref = findPreference(KEY_MIN_HOLD_ELEVATION);
    }

    private void reloadSummaries() {
        final Resources r = getResources();
        CharSequence summary;

        if (mPositioningModePref != null)
            mPositioningModePref.setSummary(mPositioningModePref.getEntry());

        if (mNumberOfFrequenciesPref != null)
            mNumberOfFrequenciesPref.setSummary(mNumberOfFrequenciesPref.getEntry());

        if (mAmbiguityResolutionPref != null)
            mAmbiguityResolutionPref.setSummary(mAmbiguityResolutionPref.getEntry());

        if (mGlonassAmbiguityResolutionPref != null)
            mGlonassAmbiguityResolutionPref.setSummary(mGlonassAmbiguityResolutionPref.getEntry());

        if (mGpsAmbiguityResolutionPref != null)
            mGpsAmbiguityResolutionPref.setSummary(mGpsAmbiguityResolutionPref.getEntry());

        if (mBDSAmbiguityResolutionPref != null)
            mBDSAmbiguityResolutionPref.setSummary(mBDSAmbiguityResolutionPref.getEntry());

        if (mArFilterPref != null)
            mArFilterPref.setSummary(mArFilterPref.getEntry());

        if (mArMinFixPref != null)
            mArMinFixPref.setSummary(mArMinFixPref.getText());

        if (mMinFixSatsPref != null)
            mMinFixSatsPref.setSummary(mMinFixSatsPref.getText());

        if (mMinHoldSatsPref != null)
            mMinHoldSatsPref.setSummary(mMinHoldSatsPref.getText());

        if (mMinDropSatsPref != null)
            mMinDropSatsPref.setSummary(mMinDropSatsPref.getText());

        if (mRcvStdsPref != null)
            mRcvStdsPref.setSummary(mRcvStdsPref.getEntry());

        if (mArMaxIterPref != null)
            mArMaxIterPref.setSummary(mArMaxIterPref.getText());

        if (mNIterPref != null)
            mNIterPref.setSummary(mNIterPref.getText());

        if (mMaxAveEpPref != null)
            mMaxAveEpPref.setSummary(mMaxAveEpPref.getText());

        if (mVarHoldAmbPref != null)
            mVarHoldAmbPref.setSummary(mVarHoldAmbPref.getText());

        if (mGainHoldAmbPref != null)
            mGainHoldAmbPref.setSummary(mGainHoldAmbPref.getText());

        if (mInitRstPref != null)
            mInitRstPref.setSummary(mInitRstPref.getEntry());

        if (mOutSinglePref != null)
            mOutSinglePref.setSummary(mOutSinglePref.getEntry());

        if (mSyncSolPref != null)
            mSyncSolPref.setSummary(mSyncSolPref.getEntry());

        if (mFreqOptPref != null)
            mFreqOptPref.setSummary(mFreqOptPref.getEntry());

        if (mArOutCntPref != null)
            mArOutCntPref.setSummary(mArOutCntPref.getText());

        if (mMaxAgePref != null)
            mMaxAgePref.setSummary(mMaxAgePref.getText());

        if (mSlipThreshPref != null)
            mSlipThreshPref.setSummary(mSlipThreshPref.getText());

        if (mRejGDopPref != null)
            mRejGDopPref.setSummary(mRejGDopPref.getText());

        if (mRejIonnoPref != null)
            mRejIonnoPref.setSummary(mRejIonnoPref.getText());

        if (mMinElevationFixPref != null)
            mMinElevationFixPref.setSummary(mMinElevationFixPref.getText());

        if (mMinElevationHoldPref != null)
            mMinElevationHoldPref.setSummary(mMinElevationHoldPref.getText());

        if (mMinLockFixPref != null)
            mMinLockFixPref.setSummary(mMinLockFixPref.getText());

        if (mMinRatioFixPref != null)
            mMinRatioFixPref.setSummary(mMinRatioFixPref.getText());

        if (mMaxPosVarPref != null)
            mMaxPosVarPref.setSummary(mMaxPosVarPref.getText());

        if (mNavigationSystem != null && mNavigationSystem.getValues() != null) {
            final ArrayList<String> navsys = new ArrayList<>(10);
            final EnumSet<NavigationSystem> navsys0 = EnumSet.noneOf(NavigationSystem.class);

            for (String v : mNavigationSystem.getValues()) {
                try {
                    navsys0.add(NavigationSystem.valueOf(v));
                } catch (IllegalArgumentException ignored) {}
            }

            for (NavigationSystem ns : navsys0) {
                navsys.add(r.getString(ns.getNameResId()));
            }

            summary = TextUtils.join(", ", navsys);
            mNavigationSystem.setSummary(summary);
        }

        if (mElevationMaskPref != null)
            mElevationMaskPref.setSummary(mElevationMaskPref.getEntry());

        if (mSnrMaskPref != null)
            mSnrMaskPref.setSummary(mSnrMaskPref.getEntry());

        if (mIonosphereCorrectionPref != null)
            mIonosphereCorrectionPref.setSummary(mIonosphereCorrectionPref.getEntry());

        if (mTroposphereCorrectionPref != null)
            mTroposphereCorrectionPref.setSummary(mTroposphereCorrectionPref.getEntry());

        if (mSatEphemClockPref != null)
            mSatEphemClockPref.setSummary(mSatEphemClockPref.getEntry());

        if (mEarthTidesCorrPref != null)
            mEarthTidesCorrPref.setSummary(mEarthTidesCorrPref.getEntry());
    }

    private void safeSetEnabled(String key, boolean enabled) {
        Preference pref = findPreference(key);
        if (pref != null) pref.setEnabled(enabled);
    }

    private void updateEnable() {
        if (mPositioningModePref == null) {
            Log.w(TAG, "PositioningModePreference is null! Skipping updateEnable.");
            return;
        }

        PositioningMode posMode;
        final boolean rel, ppp, rtk;

        String value = mPositioningModePref.getValue();
        if (value == null) {
            value = String.valueOf(R.string.pmode_single);
        }

        posMode = PositioningMode.valueOf(value);

        rel = posMode.isRelative();
        rtk = posMode.isRtk();
        ppp = posMode.isPpp();

        if (mNumberOfFrequenciesPref != null) mNumberOfFrequenciesPref.setEnabled(rel);
        safeSetEnabled(KEY_REC_DYNAMICS, rel);

        if (mEarthTidesCorrPref != null) mEarthTidesCorrPref.setEnabled(rel || ppp);

        safeSetEnabled(KEY_SAT_ANTENNA_PCV, ppp);
        safeSetEnabled(KEY_RECEIVER_ANTENNA_PCV, ppp);
        safeSetEnabled(KEY_PHASE_WINDUP_CORRECTION, ppp);
        safeSetEnabled(KEY_EXCLUDE_ECLIPSING, ppp);
        safeSetEnabled(KEY_AMBIGUITY_RESOLUTION, rtk || ppp);
        safeSetEnabled(KEY_GLONASS_AMBIGUITY_RESOLUTION, rtk || ppp);
        safeSetEnabled(KEY_GPS_AMBIGUITY_RESOLUTION, rtk || ppp);
        safeSetEnabled(KEY_BDS_AMBIGUITY_RESOLUTION, rtk || ppp);
        safeSetEnabled(KEY_AR_FILTER, rtk || ppp);
        safeSetEnabled(KEY_AR_MIN_FIX, rtk || ppp);
        safeSetEnabled(KEY_MIN_FIX_SATS, rtk || ppp);
        safeSetEnabled(KEY_MIN_HOLD_SATS, rtk || ppp);
        safeSetEnabled(KEY_MIN_DROP_SATS, rtk || ppp);
        safeSetEnabled(KEY_RCV_STDS, rtk || ppp);
        safeSetEnabled(KEY_AR_MAX_ITER, rtk || ppp);
        safeSetEnabled(KEY_N_ITER, rtk || ppp);
        safeSetEnabled(KEY_MAX_AVE_AMB, rtk || ppp);
        safeSetEnabled(KEY_VAR_HOLD_AMB, rtk || ppp);
        safeSetEnabled(KEY_GAIN_HOLD_AMB, rtk || ppp);
        safeSetEnabled(KEY_SYNC_SOL, rtk || ppp);
        safeSetEnabled(KEY_OUT_SINGLE, rtk || ppp);
        safeSetEnabled(KEY_FREQ_OPT, rtk || ppp);
        safeSetEnabled(KEY_INIT_RST, rtk || ppp);
        safeSetEnabled(KEY_MIN_FIX_ELEVATION, rtk || ppp);
        safeSetEnabled(KEY_MIN_HOLD_ELEVATION, rtk || ppp);
        safeSetEnabled(KEY_MIN_FIX_LOCK, rtk || ppp);
        safeSetEnabled(KEY_MIN_FIX_RATIO, rtk || ppp);
        safeSetEnabled(KEY_MAX_POS_VAR, rtk || ppp);
    }

    public static ProcessingOptions readPrefs(Context ctx) {
        final ProcessingOptions opts;
        final SharedPreferences prefs;
        String v;
        Set<String> vSet;

        opts = new ProcessingOptions();
        prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Activity.MODE_PRIVATE);

        v = prefs.getString(KEY_POSITIONING_MODE, null);
        opts.setPositioningMode(PositioningMode.valueOf(v));

        v = prefs.getString(KEY_NUMBER_OF_FREQUENCIES, null);
        assert v != null;
        opts.setNumberOfFrequencies(Integer.parseInt(v));

        vSet = prefs.getStringSet(KEY_NAVIGATION_SYSTEM, null);
        Set<NavigationSystem> navsys = EnumSet.noneOf(NavigationSystem.class);
        assert vSet != null;
        for (String v0 : vSet) navsys.add(NavigationSystem.valueOf(v0));
        opts.setNavigationSystem(navsys);

        v = prefs.getString(KEY_ELEVATION_MASK, null);
        assert v != null;
        opts.setElevationMask(Math.toRadians(Double.parseDouble(v)));

        v = prefs.getString(KEY_SNR_MASK, null);
        assert v != null;
        opts.setSnrMask(Integer.parseInt(v));

        opts.setRecDynamics(prefs.getBoolean(KEY_REC_DYNAMICS, opts.getRecDynamics()));

        v = prefs.getString(KEY_EARTH_TIDES_CORRECTION, null);
        opts.setEarthTidesCorrection(EarthTideCorrectionType.valueOf(v));

        v = prefs.getString(KEY_IONOSPHERE_CORRECTION, null);
        opts.setIonosphereCorrection(IonosphereOption.valueOf(v));

        v = prefs.getString(KEY_TROPOSPHERE_CORRECTION, null);
        opts.setTroposphereCorrection(TroposphereOption.valueOf(v));

        v = prefs.getString(KEY_SAT_EPHEM_CLOCK, null);
        opts.setSatEphemerisOption(EphemerisOption.valueOf(v));

        opts.setSatAntennaPcvEnabled(prefs.getBoolean(KEY_SAT_ANTENNA_PCV, opts.isSatAntennaPcvEnabled()));
        opts.setReceiverAntennaPcvEnabled(prefs.getBoolean(KEY_RECEIVER_ANTENNA_PCV, opts.isReceiverAntennaPcvEnabled()));
        opts.setPhaseWindupCorrectionEnabled(prefs.getBoolean(KEY_PHASE_WINDUP_CORRECTION, opts.isPhaseWindupCorrectionEnabled()));
        opts.setExcludeEclipsingSatMeasurements(prefs.getBoolean(KEY_EXCLUDE_ECLIPSING, opts.isExcludeEclipsingSatMeasurements()));
        opts.setRaimFdeEnabled(prefs.getBoolean(KEY_RAIM_FDE, opts.isRaimFdeEnabled()));

        opts.setModeAR(Integer.parseInt(prefs.getString(KEY_AMBIGUITY_RESOLUTION, "0")));
        opts.setModeGAR(Integer.parseInt(prefs.getString(KEY_GLONASS_AMBIGUITY_RESOLUTION, "0")));

        opts.setModeGpsAR(Integer.parseInt(prefs.getString(KEY_GPS_AMBIGUITY_RESOLUTION, "0")));
        opts.setModeBDSAR(Integer.parseInt(prefs.getString(KEY_BDS_AMBIGUITY_RESOLUTION, "0")));

        opts.setArFilter(Integer.parseInt(prefs.getString(KEY_AR_FILTER, "0")));

        opts.setMinFixCountToHoldAmbiguity(Integer.parseInt(prefs.getString(KEY_AR_MIN_FIX, "0")));
        opts.setMinFixToFixAmbiguity(Integer.parseInt(prefs.getString(KEY_MIN_FIX_SATS, "0")));
        opts.setMinHoldToFixAmbiguity(Integer.parseInt(prefs.getString(KEY_MIN_HOLD_SATS, "0")));
        opts.setMinDropToFixAmbiguity(Integer.parseInt(prefs.getString(KEY_MIN_DROP_SATS, "0")));
        opts.setRcvStds(Integer.parseInt(prefs.getString(KEY_RCV_STDS, "0")));

        opts.setArMaxIter(Integer.parseInt(prefs.getString(KEY_AR_MAX_ITER, "0")));
        opts.setNIter(Integer.parseInt(prefs.getString(KEY_N_ITER, "0")));
        opts.setMaxAveEp(Integer.parseInt(prefs.getString(KEY_MAX_AVE_AMB, "0")));

        opts.setVarHoldAmb(Double.parseDouble(prefs.getString(KEY_VAR_HOLD_AMB, "0")));
        opts.setGainHoldAmb(Double.parseDouble(prefs.getString(KEY_GAIN_HOLD_AMB, "0")));

        opts.setInitRst(Integer.parseInt(prefs.getString(KEY_INIT_RST, "0")));
        opts.setOutSingle(Integer.parseInt(prefs.getString(KEY_OUT_SINGLE, "0")));
        opts.setSyncSol(Integer.parseInt(prefs.getString(KEY_SYNC_SOL, "0")));
//        String tmp = prefs.getString(KEY_FREQ_OPT, "0");
        opts.setFreqOpt(Integer.parseInt(prefs.getString(KEY_FREQ_OPT, "0")));

        opts.setArOutCnt(Integer.parseInt(prefs.getString(KEY_AR_OUT_CNT, "0")));

        opts.setSlipThres(Double.parseDouble(prefs.getString(KEY_SLIP_THRES, "0")));
        opts.setMaxAge(Double.parseDouble(prefs.getString(KEY_MAX_AGE, "0")));
        opts.setRejGDop(Double.parseDouble(prefs.getString(KEY_REJ_GDOP, "0")));
        opts.setRejIonno(Double.parseDouble(prefs.getString(KEY_REJ_IONNO, "0")));


        opts.setValidThresoldAR(Double.parseDouble(prefs.getString(KEY_MIN_FIX_RATIO, "3.0")));
        opts.setMaxPositionVariance(Double.parseDouble(prefs.getString(KEY_MAX_POS_VAR, "0.004")));

        opts.setMinElevationToFixAmbiguityRad(Double.parseDouble(prefs.getString(KEY_MIN_FIX_ELEVATION, "0")) * (Math.PI / 180));
        opts.setMinElevationToHoldAmbiguityRad(Double.parseDouble(prefs.getString(KEY_MIN_HOLD_ELEVATION, "0")) * (Math.PI / 180));
        opts.setMinLockToFixAmbiguity(Integer.parseInt(prefs.getString(KEY_MIN_FIX_LOCK, "0")));


        SharedPreferences roverPrefs = ctx.getSharedPreferences(InputRoverFragment.SHARED_PREFS_NAME, Activity.MODE_PRIVATE);
        opts.setAntTypeRover(roverPrefs.getString(InputRoverFragment.KEY_ANTENNA, ""));
        SharedPreferences basePrefs = ctx.getSharedPreferences(InputBaseFragment.SHARED_PREFS_NAME, Activity.MODE_PRIVATE);
        opts.setAntTypeBase(basePrefs.getString(InputRoverFragment.KEY_ANTENNA, ""));

        return opts;
    }

    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            updateEnable();
            reloadSummaries();
        }
    }

    public static void setValue(Context ctx, ProcessingOptions opts) {
        SharedPreferences prefs;

        prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        Set<String> navSystemSet = new HashSet<>();
        for (NavigationSystem ns : opts.getNavigationSystem()) navSystemSet.add(ns.name());

        String elmask = String.valueOf(Math.round(Math.toDegrees(opts.getElevationMask())));

        prefs
                .edit()
                .putString(KEY_POSITIONING_MODE, opts.getPositioningMode().name())
                .putString(KEY_NUMBER_OF_FREQUENCIES, String.valueOf(opts.getNumberOfFrequencies()))
                .putStringSet(KEY_NAVIGATION_SYSTEM, navSystemSet)
                .putString(KEY_ELEVATION_MASK, elmask)
                .putString(KEY_SNR_MASK, String.valueOf(opts.getSnrMask()))
                .putBoolean(KEY_REC_DYNAMICS, opts.getRecDynamics())
                .putString(KEY_EARTH_TIDES_CORRECTION, opts.getEarthTidersCorrection().name())
                .putString(KEY_IONOSPHERE_CORRECTION, opts.getIonosphereCorrection().name())
                .putString(KEY_TROPOSPHERE_CORRECTION, opts.getTroposphereCorrection().name())
                .putString(KEY_SAT_EPHEM_CLOCK, opts.getSatEphemerisOption().name())
                .putBoolean(KEY_SAT_ANTENNA_PCV, opts.isSatAntennaPcvEnabled())
                .putBoolean(KEY_RECEIVER_ANTENNA_PCV, opts.isReceiverAntennaPcvEnabled())
                .putBoolean(KEY_PHASE_WINDUP_CORRECTION, opts.isPhaseWindupCorrectionEnabled())
                .putBoolean(KEY_EXCLUDE_ECLIPSING, opts.isExcludeEclipsingSatMeasurements())
                .putBoolean(KEY_RAIM_FDE, opts.isRaimFdeEnabled())
                .putString(KEY_AMBIGUITY_RESOLUTION, String.valueOf(opts.getModeAR()))
                .putString(KEY_GLONASS_AMBIGUITY_RESOLUTION, String.valueOf(opts.getModeGAR()))
                .putString(KEY_GPS_AMBIGUITY_RESOLUTION, String.valueOf(opts.getModeGpsAR()))
                .putString(KEY_BDS_AMBIGUITY_RESOLUTION, String.valueOf(opts.getModeBDSAR()))
                .putString(KEY_AR_FILTER, String.valueOf(opts.getArFilter()))
                .putString(KEY_AR_MIN_FIX, String.valueOf(opts.getMinFixCountToHoldAmbiguity()))
                .putString(KEY_MIN_FIX_SATS, String.valueOf(opts.getMinFixToFixAmbiguity()))
                .putString(KEY_MIN_HOLD_SATS, String.valueOf(opts.getMinHoldToFixAmbiguity()))
                .putString(KEY_MIN_DROP_SATS, String.valueOf(opts.getMinDropToFixAmbiguity()))
                .putString(KEY_RCV_STDS, String.valueOf(opts.getRcvStds()))
                .putString(KEY_AR_MAX_ITER, String.valueOf(opts.getArMaxIter()))
                .putString(KEY_N_ITER, String.valueOf(opts.getNIter()))
                .putString(KEY_MAX_AVE_AMB, String.valueOf(opts.getMaxAveEp()))
                .putString(KEY_VAR_HOLD_AMB, String.valueOf(opts.getVarHoldAmb()))
                .putString(KEY_GAIN_HOLD_AMB, String.valueOf(opts.getGainHoldAmb()))

                .putString(KEY_INIT_RST, String.valueOf(opts.getInitRst()))
                .putString(KEY_OUT_SINGLE, String.valueOf(opts.getOutSingle()))
                .putString(KEY_SYNC_SOL, String.valueOf(opts.getSyncSol()))
                .putString(KEY_FREQ_OPT, String.valueOf(opts.getFreqOpt()))
                .putString(KEY_AR_OUT_CNT, String.valueOf(opts.getArOutCnt()))
                .putString(KEY_SLIP_THRES, String.valueOf(opts.getSlipThres()))
                .putString(KEY_MAX_AGE, String.valueOf(opts.getMaxAge()))
                .putString(KEY_REJ_GDOP, String.valueOf(opts.getRejGDop()))
                .putString(KEY_REJ_IONNO, String.valueOf(opts.getRejIonno()))
                .putString(KEY_MIN_FIX_LOCK, String.valueOf(opts.getMinLockToFixAmbiguity()))
                .putString(KEY_MIN_FIX_RATIO, String.valueOf(opts.getMinElevationToFixAmbiguityRad())) /* fixme*/
                .putString(KEY_MAX_POS_VAR, String.valueOf(opts.getMaxPositionVariance()))
                .putString(KEY_MIN_FIX_ELEVATION, String.valueOf(opts.getMinElevationToFixAmbiguityRad()))
                .putString(KEY_MIN_HOLD_ELEVATION, String.valueOf(opts.getMinElevationToHoldAmbiguityRad()))
                .apply()
        ;
        if (DBG) {
            ProcessingOptions opts2 = readPrefs(ctx);
            if (!opts.equals(opts2)) {
                Log.e(TAG, "saved processing options differ from the original");
            }
        }
    }

    public static void setDefaultValues(Context ctx, boolean force) {
        SharedPreferences prefs;

        prefs = ctx.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        final boolean needUpdate = force || !prefs.contains(KEY_POSITIONING_MODE);

        if (needUpdate) {
            setValue(ctx, new ProcessingOptions());
        }
    }

}
