package com.example.td2ex2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class Screen1Fragment extends Fragment {

    public static final int FRAGMENT_ID = 0;
    private static final String KEY_CURRENT_ISSUE = "current_issue";

    private Notifiable notifiable;
    private Issue currentIssue;

    private TextView detailTitle;
    private TextView detailPriority;
    private TextView detailDescription;
    private TextView detailProtocol;
    private TextView detailLocationText;

    private RatingBar detailRatingBar;
    private ImageView detailPriorityDot;
    private MapView detailMap;

    public Screen1Fragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (requireActivity() instanceof Notifiable) {
            notifiable = (Notifiable) requireActivity();
        } else {
            throw new AssertionError("L'activité doit implémenter Notifiable.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        return inflater.inflate(R.layout.fragment_screen1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        detailTitle = view.findViewById(R.id.detailTitle);
        detailPriority = view.findViewById(R.id.detailPriority);
        detailDescription = view.findViewById(R.id.detailDescription);
        detailProtocol = view.findViewById(R.id.detailProtocol);
        detailRatingBar = view.findViewById(R.id.detailRatingBar);
        detailPriorityDot = view.findViewById(R.id.detailPriorityDot);
        detailLocationText = view.findViewById(R.id.detailLocationText);
        detailMap = view.findViewById(R.id.detailMap);

        Button goButton = view.findViewById(R.id.goButton);

        detailRatingBar.setIsIndicator(true);

        setupMap();

        goButton.setOnClickListener(v -> {
            if (notifiable != null) {
                notifiable.onClick(FRAGMENT_ID);
            }
        });

        if (savedInstanceState != null) {
            currentIssue = savedInstanceState.getParcelable(KEY_CURRENT_ISSUE);
        }

        if (currentIssue != null) {
            displayIssue(currentIssue);
        } else {
            showDefaultContent();
        }
    }

    private void setupMap() {
        if (detailMap == null) return;

        detailMap.setTileSource(TileSourceFactory.MAPNIK);
        detailMap.setMultiTouchControls(true);
        detailMap.getController().setZoom(14.5);

        GeoPoint nice = new GeoPoint(43.7009, 7.2684);
        detailMap.getController().setCenter(nice);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (notifiable != null) {
            notifiable.onFragmentDisplayed(FRAGMENT_ID);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (detailMap != null) {
            detailMap.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (detailMap != null) {
            detailMap.onPause();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (currentIssue != null) {
            outState.putParcelable(KEY_CURRENT_ISSUE, currentIssue);
        }
    }

    private void showDefaultContent() {
        detailTitle.setText("Alerte Accident");
        detailPriority.setText("Aucun incident sélectionné");
        detailDescription.setText("Appuyez sur le bouton pour afficher la liste des incidents signalés.");
        detailProtocol.setText("Protocole : aucun");
        detailRatingBar.setRating(0f);
        detailPriorityDot.setImageResource(R.drawable.bg_priority_low);
        detailLocationText.setText("Localisation GPS : aucune alerte sélectionnée");

        showMapMarker(
                43.7009,
                7.2684,
                "Centre de Nice",
                "Carte prête pour afficher un accident."
        );

        sendPictureToCameraFragment(null);
    }

    public void displayIssue(Issue issue) {
        currentIssue = issue;

        if (detailTitle == null) {
            return;
        }

        detailTitle.setText(issue.getTitle());
        detailPriority.setText("Priorité : " + issue.getPriority().name());
        detailDescription.setText(issue.getDescription());
        detailProtocol.setText("Protocole : " + issue.getSafetyProtocol());
        detailRatingBar.setRating(issue.getStatus());

        detailLocationText.setText(
                "Localisation GPS : "
                        + issue.getLatitude()
                        + " / "
                        + issue.getLongitude()
        );

        switch (issue.getPriority()) {
            case CRITICAL:
                detailPriorityDot.setImageResource(R.drawable.bg_priority_critical);
                break;
            case HIGH:
                detailPriorityDot.setImageResource(R.drawable.bg_priority_high);
                break;
            case MEDIUM:
                detailPriorityDot.setImageResource(R.drawable.bg_priority_medium);
                break;
            case LOW:
                detailPriorityDot.setImageResource(R.drawable.bg_priority_low);
                break;
        }

        showMapMarker(
                issue.getLatitude(),
                issue.getLongitude(),
                issue.getTitle(),
                issue.getDescription()
        );

        sendPictureToCameraFragment(issue.getPicture());
    }

    private void sendPictureToCameraFragment(String path) {
        Bundle bundle = new Bundle();
        bundle.putString(CameraFragment.RESULT_PHOTO_PATH, path);

        getChildFragmentManager().setFragmentResult(
                CameraFragment.RESULT_CHANNEL,
                bundle
        );
    }

    private void showMapMarker(
            double latitude,
            double longitude,
            String title,
            String description
    ) {
        if (detailMap == null) return;

        GeoPoint point = new GeoPoint(latitude, longitude);

        detailMap.getOverlays().clear();

        Marker marker = new Marker(detailMap);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        marker.setSnippet(description);

        detailMap.getOverlays().add(marker);

        detailMap.getController().setZoom(16.0);
        detailMap.getController().setCenter(point);
        detailMap.invalidate();
    }
}