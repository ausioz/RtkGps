package gpsplus.rtkgps;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import gpsplus.rtkgps.databinding.FragmentMapBinding;
import gpsplus.rtkgps.geoportail.GeoportailWMTSTileSource;
import gpsplus.rtklib.RtkCommon;
import gpsplus.rtklib.RtkCommon.Position3d;
import gpsplus.rtklib.RtkControlResult;
import gpsplus.rtklib.RtkServerStreamStatus;
import gpsplus.rtklib.Solution;
import gpsplus.rtklib.constants.SolutionStatus;

public class MapFragment extends Fragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    static final String TAG = MapFragment.class.getSimpleName();

    private static final String SHARED_PREFS_NAME = "map";
    private static final String PREFS_TITLE_SOURCE = "title_source";
    private static final String PREFS_SCROLL_X = "scroll_x";
    private static final String PREFS_SCROLL_Y = "scroll_y";
    private static final String PREFS_ZOOM_LEVEL = "zoom_level";

    private static final String MAP_MODE_BING="BingMap";
    private static final String MAP_MODE_BING_AERIAL="Bing aerial";
    private static final String MAP_MODE_BING_ROAD="Bing road";

    private final RtkServerStreamStatus mStreamStatus;

    private BingMapTileSource mBingRoadTileSource, mBingAerialTileSource;
    private SolutionPointOverlay mPathOverlay;
    private SolutionPathManager mPathManager;
    private MyLocationNewOverlay mMyLocationOverlay;
    private CompassOverlay mCompassOverlay;
    private ScaleBarOverlay mScaleBarOverlay;
    private GeoportailWMTSTileSource mGeoportailCadastralTileSource;
    private GeoportailWMTSTileSource mGeoportailMapTileSource;
    private GeoportailWMTSTileSource mGeoportailOrthoimageTileSource;

    private final RtkControlResult mRtkStatus;

    private FragmentMapBinding binding;

    private MapView mMapView;
    private MapTileProviderBase mGeoportailTileProvider;
    private MyOverlayManager overlayManager;
    private final Handler statusHandler = new Handler(Looper.getMainLooper());
    private final Runnable statusUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            statusHandler.postDelayed(this, 2500);
        }
    };

    public MapFragment() {
        mStreamStatus = new RtkServerStreamStatus();
        mRtkStatus = new RtkControlResult();

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMap();
//        setupMenu();
    }

    @Override
    public void onStart() {
        super.onStart();
        statusHandler.postDelayed(statusUpdateRunnable, 200);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveMapPreferences();
        mMyLocationOverlay.disableMyLocation();
        mCompassOverlay.disableCompass();
    }

    private void setupMap(){
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        final Context context = requireContext();
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();

        mMapView = binding.mapView;
        mMapView.setMultiTouchControls(true);
        mMapView.setTilesScaledToDpi(true);
        mMapView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        //noinspection deprecation
        mMapView.setBuiltInZoomControls(false);

        mPathManager = new SolutionPathManager();

        mMyLocationOverlay = new MyLocationNewOverlay(mMyLocationProvider, mMapView);
        mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), mMapView);
        mCompassOverlay.setCompassCenter(25.0f * dm.density, 5.0f * dm.density);

        mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, (int) (5.0f * dm.density));

        try {
            mMapView.getOverlays().add(mPathManager.getPathLine());
            mMapView.getOverlays().add(mPathManager.getPointOverlay());
            mMapView.getOverlays().add(mScaleBarOverlay);
            mMapView.getOverlays().add(mMyLocationOverlay);
            mMapView.getOverlays().add(mCompassOverlay);
        } catch (Exception e) {
            Log.e(TAG, "Error adding overlays: " + e.getMessage());
        }

        overlayManager = new MyOverlayManager(requireContext(), mMapView, R.drawable.marker_default);

        // Temporarily disable Bing and Geoportail to isolate crash issue
        // BingMapTileSource.retrieveBingKey(context);
        // mBingRoadTileSource = new BingMapTileSource(null);
        // mBingRoadTileSource.setStyle(BingMapTileSource.IMAGERYSET_ROAD);

        // mBingAerialTileSource = new BingMapTileSource(null);
        // mBingAerialTileSource.setStyle(BingMapTileSource.IMAGERYSET_AERIAL);

        // mGeoportailCadastralTileSource = new GeoportailWMTSTileSource(null, GeoportailLayer.CADASTRALPARCELS);
        // mGeoportailMapTileSource = new GeoportailWMTSTileSource(null, GeoportailLayer.MAPS);
        // mGeoportailOrthoimageTileSource = new GeoportailWMTSTileSource(null, GeoportailLayer.ORTHOIMAGE);

        // Set default tile source immediately to avoid null issues
        mMapView.setTileSource(TileSourceFactory.MAPNIK);

        // Load preferences after everything is set up
        mMapView.post(this::loadMapPreferences);

        binding.ivMyLocation.setOnClickListener(v -> panToMyLocation());
    }

    private void panToMyLocation() {
        if (mMyLocationProvider == null) {
            Log.w(TAG, "MyLocationProvider is null");
            return;
        }

        Location lastLocation = mMyLocationProvider.getLastKnownLocation();
        if (lastLocation == null) {
            return;
        }

        double lat = lastLocation.getLatitude();
        double lon = lastLocation.getLongitude();

        GeoPoint gp = new GeoPoint(lat, lon);
        binding.mapView.getController().animateTo(gp);
    }

    private void setupMenu(){
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.fragment_map, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.menu_map_mode_osm) {
                    mMapView.setTileSource(TileSourceFactory.MAPNIK);
                    mMapView.invalidate();
                    return true;
                }

                // Temporarily disabled - only OSM is active
                /*
                else if (id == R.id.menu_map_mode_bing_road) {
                    mMapView.setTileSource(mBingRoadTileSource);
                } else if (id == R.id.menu_map_mode_bing_aerial) {
                    mMapView.setTileSource(mBingAerialTileSource);
                } else if (id == R.id.menu_map_mode_geoportail_map) {
                    mMapView.setTileSource(mGeoportailMapTileSource);
                } else if (id == R.id.menu_map_mode_geoportail_cadastral) {
                    mMapView.setTileSource(mGeoportailCadastralTileSource);
                } else if (id == R.id.menu_map_mode_geoportail_orthoimages) {
                    mMapView.setTileSource(mGeoportailOrthoimageTileSource);
                } else {
                    return false;
                }
                try {
                    mMapView.invalidate();
                } catch (Exception e) {
                    Log.e(TAG, "Error invalidating map view: " + e.getMessage());
                }
                return true;
                */

                return false;
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                if (mMapView == null || mMapView.getTileProvider() == null) return;

                // Temporarily simplified - only OSM is active
                int checkedId = R.id.menu_map_mode_osm;

                MenuItem checkedItem = menu.findItem(checkedId);
                if (checkedItem != null) {
                    checkedItem.setChecked(true);
                }
            }
        }, getViewLifecycleOwner());
    }

    @Override
    public void onResume() {
        super.onResume();
        mMyLocationOverlay.enableMyLocation(mMyLocationProvider);
        mMyLocationOverlay.enableFollowLocation();
        mCompassOverlay.enableCompass(this.mCompassOverlay.getOrientationProvider());
    }

    @Override
    public void onStop() {
        super.onStop();
//        mPathOverlay.clearPath();
        statusHandler.removeCallbacks(statusUpdateRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMapView = null;
        mPathOverlay = null;
        mMyLocationOverlay = null;
        mCompassOverlay = null;
        mScaleBarOverlay = null;
        binding = null;
    }

    void updateStatus() {
        MainActivity ma;
        RtkNaviService rtks;
        int serverStatus;

        // XXX
        ma = (MainActivity)getActivity();

        if (ma == null) return;

        // Add null check for mMapView
        if (mMapView == null) return;

        // Add null check for binding
        if (binding == null) return;

        // Ensure fragment and views are ready
        if (!isAdded() || isDetached()) return;
        if (getView() == null || !getView().isAttachedToWindow()) return;

        rtks = ma.getRtkService();
        if (rtks == null) {
            serverStatus = RtkServerStreamStatus.STATE_CLOSE;
            mStreamStatus.clear();
        }else {
            serverStatus = rtks.getServerStatus();

            if (rtks.isServiceStarted()) {
                try {
                    rtks.getStreamStatus(mStreamStatus);
                    rtks.getRtkStatus(mRtkStatus);
                    if (mMapView != null) {
                        appendSolutions(rtks.readSolutionBuffer());
                        if (MainActivity.getDemoModeLocation() != null && MainActivity.getDemoModeLocation().isInDemoMode()) {
                            mMyLocationProvider.setDemoPosition(MainActivity.getDemoModeLocation().getPosition(), !mMapView.isAnimating());
                        } else {
                            mMyLocationProvider.setStatus(mRtkStatus, !mMapView.isAnimating());
                        }
                    }

                    binding.gtimeView.setTime(mRtkStatus.getSolution().getTime());
                    binding.solutionView.setStats(mRtkStatus);
                } catch (Exception e) {
                    Log.e(TAG, "Error getting RTK status: " + e.getMessage(), e);
                    mStreamStatus.clear();
                }
            } else {
                mStreamStatus.clear();
            }
        }

        try {
            binding.streamIndicatorsView.setStats(mStreamStatus, serverStatus);
        } catch (Exception e) {
            Log.e(TAG, "Error updating stream indicators: " + e.getMessage());
        }
    }

    private void saveMapPreferences() {
        if (mMapView == null) return;

        try {
            requireActivity()
                    .getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREFS_TITLE_SOURCE, getTileSourceName())
                    .putInt(PREFS_SCROLL_X, mMapView.getScrollX())
                    .putInt(PREFS_SCROLL_Y, mMapView.getScrollY())
                    .putInt(PREFS_ZOOM_LEVEL, (int) mMapView.getZoomLevelDouble())
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving map preferences: " + e.getMessage());
        }
    }

    private void loadMapPreferences() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        final String tileSourceName = prefs.getString(PREFS_TITLE_SOURCE, TileSourceFactory.DEFAULT_TILE_SOURCE.name());
        setTileSource(tileSourceName);

        try {
            int zoomLevel = prefs.getInt(PREFS_ZOOM_LEVEL, 1);
            if (zoomLevel < 1) zoomLevel = 1;
            if (zoomLevel > 20) zoomLevel = 20;
            mMapView.getController().setZoom((double) zoomLevel);

            int scrollX = prefs.getInt(PREFS_SCROLL_X, 0);
            int scrollY = prefs.getInt(PREFS_SCROLL_Y, 0);

            mMapView.post(() -> mMapView.scrollTo(scrollX, scrollY));
        } catch (Exception e) {
            Log.e(TAG, "Error loading map preferences: " + e.getMessage());
            mMapView.getController().setZoom(1.0);
        }
    }

    private void setTileSource(String name) {
        ITileSource tileSource;

        /*
        if (MAP_MODE_BING_AERIAL.equals(name)) {
            tileSource = mBingAerialTileSource;
        }else if (MAP_MODE_BING_ROAD.equals(name)) {
            tileSource = mBingRoadTileSource;
        }else if (GeoportailLayer.CADASTRALPARCELS.getLayer().equals(name)) {
            tileSource = mGeoportailCadastralTileSource;
        }else if (GeoportailLayer.MAPS.getLayer().equals(name)) {
            tileSource = mGeoportailMapTileSource;
        }else if (GeoportailLayer.ORTHOIMAGE.getLayer().equals(name)) {
            tileSource = mGeoportailOrthoimageTileSource;
        }else {
        */
            try {
                tileSource = TileSourceFactory.getTileSource(name);
            }catch(IllegalArgumentException iae) {
                tileSource = TileSourceFactory.MAPNIK;
            }
        /* } */

        if (mMapView != null && mMapView.getTileProvider() != null) {
            ITileSource current = mMapView.getTileProvider().getTileSource();
            if (!tileSource.equals(current)) {
                mMapView.setTileSource(tileSource);
            }
        }
    }

    private String getTileSourceName() {
        if (mMapView == null || mMapView.getTileProvider() == null) {
            return TileSourceFactory.DEFAULT_TILE_SOURCE.name();
        }

        ITileSource provider = mMapView.getTileProvider().getTileSource();
        if (provider == null) {
            return TileSourceFactory.DEFAULT_TILE_SOURCE.name();
        }

        /*
        if (MAP_MODE_BING.equals(provider.name())) {
            if (BingMapTileSource.IMAGERYSET_ROAD.equals(((BingMapTileSource)provider).getStyle())) {
                return MAP_MODE_BING_ROAD;
            }else {
                return MAP_MODE_BING_AERIAL;
            }
        }else {
        */
            return provider.name();
        /* } */
    }

    private void appendSolutions(Solution[] solutions) {
        if (mPathManager != null && solutions != null) {
            mPathManager.addSolutions(solutions);
        }
    }

    MyLocationProvider mMyLocationProvider = new MyLocationProvider();

    static class MyLocationProvider implements IMyLocationProvider {

        private final Location mLastLocation = new Location("rtk");
        private boolean mLocationKnown = false;
        private IMyLocationConsumer mConsumer;

        @Override
        public boolean startLocationProvider(IMyLocationConsumer myLocationConsumer) {
            mConsumer = myLocationConsumer;
            return true;
        }

        @Override
        public void stopLocationProvider() {
            mConsumer = null;
        }

        @Override
        public Location getLastKnownLocation() {
            return mLocationKnown ? mLastLocation : null;
        }

        @Override
        public void destroy() {
            mConsumer = null;
            mLocationKnown = false;
        }

        public void setStatus(RtkControlResult status, boolean notifyConsumer) {
            setSolution(status.getSolution(), notifyConsumer);
        }

        public void setDemoPosition(Position3d position, boolean notifyConsumer) {
            if (position == null) return;

            mLastLocation.setTime(System.currentTimeMillis());
            mLastLocation.setLatitude(Math.toDegrees(position.getLat()));
            mLastLocation.setLongitude(Math.toDegrees(position.getLon()));
            mLastLocation.setAltitude(position.getHeight());

            mLocationKnown = true;

            if (mConsumer != null && notifyConsumer) {
                mConsumer.onLocationChanged(mLastLocation, this);
            } else if (DBG) {
                Log.v(TAG, "Demo: onLocationChanged() skipped (animation in progress)");
            }
        }

        private void setSolution(Solution solution, boolean notifyConsumer) {
            if (solution == null || solution.getSolutionStatus() == SolutionStatus.NONE) {
                return;
            }

            Position3d pos = RtkCommon.ecef2pos(solution.getPosition());
            mLastLocation.setTime(solution.getTime().getUtcTimeMillis());
            mLastLocation.setLatitude(Math.toDegrees(pos.getLat()));
            mLastLocation.setLongitude(Math.toDegrees(pos.getLon()));
            mLastLocation.setAltitude(pos.getHeight());

            mLocationKnown = true;

            if (mConsumer != null && notifyConsumer) {
                mConsumer.onLocationChanged(mLastLocation, this);
            } else if (DBG) {
                Log.v(TAG, "onLocationChanged() skipped (animation in progress)");
            }
        }
    }
}
