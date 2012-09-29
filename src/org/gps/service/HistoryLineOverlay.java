package org.gps.service;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.MapView;
import com.baidu.mapapi.Overlay;
import com.baidu.mapapi.Projection;


public class HistoryLineOverlay extends Overlay {
	private List<GeoPoint> geoPoints;

	public HistoryLineOverlay(List<GeoPoint> geoPoints) {
		this.geoPoints = geoPoints;
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		// 画线
		Projection projection = mapView.getProjection();
		Paint paint = new Paint();
		paint.setColor(Color.RED);
		paint.setDither(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeWidth(3);

		Path path = new Path();
		for (int i = 0; i < geoPoints.size(); i++) {
			Point p = new Point();
			projection.toPixels(geoPoints.get(i), p);
			if (i == 0) {
				path.moveTo(p.x, p.y);
			} else {
				path.lineTo(p.x, p.y);
			}
		}
		canvas.drawPath(path, paint);
		super.draw(canvas, mapView, shadow);
	}

	public List<GeoPoint> getGeoPoints() {
		return geoPoints;
	}

	public void setGeoPoints(List<GeoPoint> geoPoints) {
		this.geoPoints = geoPoints;
	}

}
