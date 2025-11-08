package gpsplus.rtkgps;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

import gpsplus.rtkgps.view.SolutionView;
import gpsplus.rtklib.constants.SolutionStatus;

public class SolutionPointOverlay extends Overlay {

	private final Paint pointPaint = new Paint();
	private List<GeoPoint> geoPoints = new ArrayList<>();
	private List<SolutionStatus> statuses = new ArrayList<>();

	public SolutionPointOverlay() {
		pointPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		pointPaint.setStrokeWidth(5f);
		pointPaint.setAntiAlias(true);
	}

	public void setPoints(List<GeoPoint> points, List<SolutionStatus> statuses) {
		this.geoPoints = new ArrayList<>(points);
		this.statuses = new ArrayList<>(statuses);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow || geoPoints == null || geoPoints.isEmpty()) return;
        if (canvas == null || mapView == null) return;

		Projection projection = mapView.getProjection();
        if (projection == null) return;

        Point screenPoint = new Point();

		for (int i = 0; i < geoPoints.size(); i++) {
            if (i >= statuses.size()) break; // Safety check

            GeoPoint geoPoint = geoPoints.get(i);
            if (geoPoint == null) continue;

            projection.toPixels(geoPoint, screenPoint);
			SolutionStatus status = statuses.get(i);
            if (status == null) continue;

            pointPaint.setColor(SolutionView.SolutionIndicatorView.getIndicatorColor(status));

            // Validate screen point is within reasonable bounds before drawing
            if (Math.abs(screenPoint.x) < 100000 && Math.abs(screenPoint.y) < 100000) {
                canvas.drawPoint(screenPoint.x, screenPoint.y, pointPaint);
            }
		}
	}

}