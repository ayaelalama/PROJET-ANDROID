package com.example.td2ex2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import java.util.List;

/**
 * Screen 5 — Carte + liste MVC.
 * Architecture MVC/MVP/MVVM (10) :
 *   Model   = IssueManager (Singleton + ModelObservable)
 *   View    = ce fragment (ViewObserver)
 *   Controller = IssueController
 */
public class Screen5Fragment extends Fragment implements ViewObserver, ClickableIssue<Issue> {

    public static final int FRAGMENT_ID = 4;

    private Notifiable notifiable;
    private IssueManager model;
    private IssueController controller;
    private MapView mapView;
    private ListView listView;
    private IssueAdapter adapter;

    public Screen5Fragment() {
        model = IssueManager.getInstance();
        controller = new IssueController(model);
    }

    public Screen5Fragment(IssueManager model, IssueController controller) {
        this.model = model;
        this.controller = controller;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Notifiable) notifiable = (Notifiable) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_screen5, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        mapView  = view.findViewById(R.id.mvcMapView);
        listView = view.findViewById(R.id.mvcIssueListView);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        adapter = new IssueAdapter(requireContext(), this, model.getIssues());
        listView.setAdapter(adapter);

        model.addObserver(this);
        refreshMap(model.getIssues());

        if (!model.getIssues().isEmpty()) {
            controller.centerMapOnIssue(mapView, model.getIssues().get(0));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (notifiable != null) notifiable.onFragmentDisplayed(FRAGMENT_ID);
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.removeObserver(this);
    }

    private void refreshMap(List<Issue> issues) {
        mapView.getOverlays().clear();
        for (Issue issue : issues) {
            mapView.getOverlays().add(controller.createMarker(mapView, issue));
        }
        mapView.invalidate();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    @Override
    public void onModelChanged(List<Issue> issues) {
        refreshMap(issues);
        Toast.makeText(requireContext(), "Carte mise à jour", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClickItem(List<Issue> items, int itemIndex) {
        controller.centerMapOnIssue(mapView, items.get(itemIndex));
    }


}
