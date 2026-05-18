package com.example.td2ex2;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class IssueController {

    private final IssueManager model;

    public IssueController(IssueManager model) {
        this.model = model;
    }

    public Marker createMarker(MapView mapView, Issue issue) {
        GeoPoint point = new GeoPoint(issue.getLatitude(), issue.getLongitude());

        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(issue.getTitle());
        marker.setSnippet(issue.getDescription());

        marker.setDraggable(true);

        marker.setOnMarkerClickListener((clickedMarker, map) -> {
            if (clickedMarker.isInfoWindowShown()) {
                clickedMarker.closeInfoWindow();
            } else {
                clickedMarker.showInfoWindow();
            }
            return true;
        });

        marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                GeoPoint newPosition = marker.getPosition();
                model.setLocation(issue, newPosition.getLatitude(), newPosition.getLongitude());
            }

            @Override
            public void onMarkerDragStart(Marker marker) {
            }
        });

        return marker;
    }

    public void centerMapOnIssue(MapView mapView, Issue issue) {
        GeoPoint point = new GeoPoint(issue.getLatitude(), issue.getLongitude());
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(point);
    }
}