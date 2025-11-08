package gpsplus.rtkgps;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class MyOverlayManager {

    private static final String TAG = "MyOverlayManager";

    private final Context context;
    private final MapView mapView;
    private final Drawable markerIcon;
    private final List<Marker> markers = new ArrayList<>();

    public MyOverlayManager(Context context, MapView mapView, int markerResId) {
        this.context = context;
        this.mapView = mapView;
        this.markerIcon = ContextCompat.getDrawable(context, markerResId);

        if (this.markerIcon == null) {
            Log.e(TAG, "Failed to load marker drawable resource: " + markerResId);
        }
    }

    public void addMarker(String title, String description, GeoPoint point) {
        // Don't add marker if icon failed to load
        if (markerIcon == null) {
            Log.w(TAG, "Cannot add marker: icon is null");
            return;
        }

        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(markerIcon);
        marker.setTitle(title);
        marker.setSubDescription(description);

        // Optional tap listener
        marker.setOnMarkerClickListener((marker1, mapView) -> {
            marker1.showInfoWindow(); // Show popup
            return true;
        });

        markers.add(marker);
        mapView.getOverlays().add(marker);
    }

    public void clear() {
        for (Marker marker : markers) {
            mapView.getOverlays().remove(marker);
        }
        markers.clear();
    }
}