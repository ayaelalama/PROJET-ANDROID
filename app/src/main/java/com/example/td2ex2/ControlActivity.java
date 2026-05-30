package com.example.td2ex2;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Activité hôte — deux modes :
 *  MODE_SIGNALANT (startScreen=2) : Signaler + Mes alertes
 *  MODE_SECOURS  (startScreen=0) : Détail + Incidents + Alertes + Carte
 *
 * Les deux modes partagent IssueManager.getInstance() → les signalements
 * apparaissent immédiatement dans l'interface secours.
 */
public class ControlActivity extends AppCompatActivity implements Notifiable, Picturable {

    public static final int MODE_SECOURS   = 0;
    public static final int MODE_SIGNALANT = 2;

    private Screen1Fragment screen1Fragment;
    private Screen2Fragment screen2Fragment;
    private Screen3Fragment screen3Fragment;
    private Screen4Fragment screen4Fragment;
    private Screen5Fragment screen5Fragment;

    private Issue selectedIssue;
    private boolean isSecours = false;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        screen1Fragment = new Screen1Fragment();
        screen2Fragment = new Screen2Fragment();
        screen3Fragment = new Screen3Fragment();
        screen4Fragment = new Screen4Fragment();
        screen5Fragment = new Screen5Fragment();

        bottomNav = findViewById(R.id.bottomNav);

        int startScreen = getIntent().getIntExtra("startScreen", MODE_SIGNALANT);
        isSecours = (startScreen == MODE_SECOURS);

        if (isSecours) {
            bottomNav.inflateMenu(R.menu.bottom_nav_secours);
            setupSecoursNav();
            showFragmentSecours(0);
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_signalant);
            setupSignalantNav();
            showFragmentSignalant(0);
        }
    }

    // ── Navigation SECOURS ────────────────────────────────────────────────────

    private void setupSecoursNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_detail)    { showFragmentSecours(0); return true; }
            if (id == R.id.nav_incidents) { showFragmentSecours(1); return true; }
            if (id == R.id.nav_alertes)   { showFragmentSecours(2); return true; }
            if (id == R.id.nav_carte)     { showFragmentSecours(3); return true; }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_detail);
    }

    private void showFragmentSecours(int index) {
        Fragment f;
        switch (index) {
            case 1:  f = screen2Fragment; break;
            case 2:  f = screen4Fragment; break;
            case 3:  f = screen5Fragment; break;
            default: f = screen1Fragment; break;
        }
        replace(f);
    }

    // ── Navigation SIGNALANT ──────────────────────────────────────────────────

    private void setupSignalantNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_signalement) { showFragmentSignalant(0); return true; }
            if (id == R.id.nav_incidents)   { showFragmentSignalant(1); return true; }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_signalement);
    }

    private void showFragmentSignalant(int index) {
        Fragment f = (index == 1) ? screen2Fragment : screen3Fragment;
        replace(f);
    }

    private void replace(Fragment f) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, f)
                .commit();
    }

    // ── Notifiable ────────────────────────────────────────────────────────────

    @Override
    public void onClick(int numFragment) {
        if (isSecours) {
            showFragmentSecours(1);
            bottomNav.setSelectedItemId(R.id.nav_incidents);
        }
    }

    @Override
    public void onDataChange(int numFragment, Object object, int actionCode, Object argsAction) {

        // Depuis Screen2 (liste incidents) — clic sur un item
        if (numFragment == Screen2Fragment.FRAGMENT_ID) {
            if (actionCode == Screen2Fragment.ACTION_ITEM_CLICKED && object instanceof Issue) {
                selectedIssue = (Issue) object;

                if (isSecours) {
                    // Mode secours → afficher le détail
                    screen1Fragment.displayIssue(selectedIssue);
                    showFragmentSecours(0);
                    bottomNav.setSelectedItemId(R.id.nav_detail);
                } else {
                    // Mode signalant → proposer modification
                    screen3Fragment.startEditing(selectedIssue);
                    showFragmentSignalant(0);
                    bottomNav.setSelectedItemId(R.id.nav_signalement);
                }
            }
        }

        // Depuis Screen3 (signalement envoyé)
        if (numFragment == Screen3Fragment.FRAGMENT_ID) {
            if (object instanceof Issue) {
                Issue newIssue = (Issue) object;
                selectedIssue = newIssue;

                // Synchroniser Screen2 (liste incidents signalant)
                screen2Fragment.addIssue(newIssue);

                // IssueManager déjà mis à jour dans Screen3Fragment.sendIncident()
                // Screen5Fragment (carte MVC) sera notifié via ModelObservable
                // Screen4Fragment (alertes) sera notifié via EmergencyService.AlertListener

                // Screen1 (détail secours) : préparer pour quand le secours ouvrira le détail
                screen1Fragment.displayIssue(newIssue);
            }
        }
    }

    @Override
    public void onFragmentDisplayed(int fragmentId) {}

    // ── Picturable ────────────────────────────────────────────────────────────

    @Override
    public void onPictureTaken(String photopath) {
        if (selectedIssue != null) selectedIssue.setPicture(photopath);
    }
}
