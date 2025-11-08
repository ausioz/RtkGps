package gpsplus.rtkgps;

import android.graphics.Color;
import android.graphics.Paint;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gpsplus.rtklib.RtkCommon;
import gpsplus.rtklib.Solution;
import gpsplus.rtklib.constants.SolutionStatus;

public class SolutionPathManager {

    private final Object lock = new Object();
    private final List<GeoPoint> geoPoints = new ArrayList<>();
    private final List<SolutionStatus> statuses = new ArrayList<>();

    private final Polyline pathLine;
    private final SolutionPointOverlay pointOverlay;

    public SolutionPathManager() {
        pathLine = new Polyline();
        pathLine.setGeodesic(true);

        Paint pathPaint = pathLine.getOutlinePaint();
        pathPaint.setColor(Color.GRAY);
        pathPaint.setStrokeWidth(2.0f);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setAntiAlias(true);

        pointOverlay = new SolutionPointOverlay();
    }

    public void clear() {
        synchronized (lock) {
            geoPoints.clear();
            statuses.clear();
            pathLine.setPoints(Collections.emptyList());
            pointOverlay.setPoints(Collections.emptyList(), Collections.emptyList());
        }
    }

    public void addSolution(Solution solution) {
        if (solution == null || solution.getSolutionStatus() == SolutionStatus.NONE) return;

        synchronized (lock) {
            GeoPoint point = toGeoPoint(solution);
            if (point == null) return;

            geoPoints.add(point);
            statuses.add(solution.getSolutionStatus());

            pathLine.setPoints(new ArrayList<>(geoPoints));
            pointOverlay.setPoints(new ArrayList<>(geoPoints), new ArrayList<>(statuses));
        }
    }

    public void addSolutions(Solution[] solutions) {
        if (solutions == null || solutions.length == 0) return;

        synchronized (lock) {
            for (Solution solution : solutions) {
                if (solution != null && solution.getSolutionStatus() != SolutionStatus.NONE) {
                    GeoPoint point = toGeoPoint(solution);
                    if (point != null) {
                        geoPoints.add(point);
                        statuses.add(solution.getSolutionStatus());
                    }
                }
            }

            pathLine.setPoints(new ArrayList<>(geoPoints));
            pointOverlay.setPoints(new ArrayList<>(geoPoints), new ArrayList<>(statuses));
        }
    }

    private GeoPoint toGeoPoint(Solution sol) {
        try {
            RtkCommon.Position3d pos = RtkCommon.ecef2pos(sol.getPosition());

            double lat = Math.toDegrees(pos.getLat());
            double lon = Math.toDegrees(pos.getLon());

            // Validate coordinates are within valid range
            if (Double.isNaN(lat) || Double.isNaN(lon) ||
                    Double.isInfinite(lat) || Double.isInfinite(lon) ||
                    lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                return null;
            }

            return new GeoPoint(lat, lon);
        } catch (Exception e) {
            return null;
        }
    }

    public Polyline getPathLine() {
        return pathLine;
    }

    public Overlay getPointOverlay() {
        return pointOverlay;
    }
}