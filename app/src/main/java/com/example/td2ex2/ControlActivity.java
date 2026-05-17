package com.example.td2ex2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class ControlActivity extends AppCompatActivity {

    private MapView accidentMap;
    private TextView locationTextView;
    private Button retryButton;

    private static final double DEFAULT_LATITUDE = 43.7009;
    private static final double DEFAULT_LONGITUDE = 7.2684;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_control);

        accidentMap = findViewById(R.id.accidentMap);
        locationTextView = findViewById(R.id.locationTextView);
        retryButton = findViewById(R.id.retryButton);

        setupMap(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);

        retryButton.setOnClickListener(v -> {
            Toast.makeText(this, "Localisation actualisée", Toast.LENGTH_SHORT).show();
            setupMap(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
        });
    }

    private void setupMap(double latitude, double longitude) {
        GeoPoint accidentPosition = new GeoPoint(latitude, longitude);

        accidentMap.setTileSource(TileSourceFactory.MAPNIK);
        accidentMap.setMultiTouchControls(true);

        accidentMap.getController().setZoom(17.0);
        accidentMap.getController().setCenter(accidentPosition);

        accidentMap.getOverlays().clear();

        Marker marker = new Marker(accidentMap);
        marker.setPosition(accidentPosition);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Lieu de l'accident");
        marker.setSnippet("Position GPS récupérée automatiquement");

        accidentMap.getOverlays().add(marker);
        accidentMap.invalidate();

        locationTextView.setText(
                "Position récupérée automatiquement : "
                        + latitude
                        + " / "
                        + longitude
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (accidentMap != null) {
            accidentMap.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (accidentMap != null) {
            accidentMap.onPause();
        }
    }
}