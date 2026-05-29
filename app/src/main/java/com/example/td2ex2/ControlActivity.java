package com.example.td2ex2;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Activité principale gérant les deux modes :
 *  - MODE_SIGNALANT (startScreen=2) : menu Signaler + Mes alertes
 *  - MODE_SECOURS  (startScreen=0) : menu Détail + Incidents + Alertes + Carte
 */
public class ControlActivity extends AppCompatActivity implements Notifiable, Picturable {

    public static final String EXTRA_MODE = "startScreen";
    public static final int MODE_SECOURS   = 0;
    public static final int MODE_SIGNALANT = 2;

    private Screen1Fragment screen1Fragment;
    private Screen2Fragment screen2Fragment;
    private Screen3Fragment screen3Fragment;
    private Screen4Fragment screen4Fragment;
    private Screen5Fragment screen5Fragment;

    private Issue selectedIssue;
    private int currentScreen = 0;
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

        int startScreen = getIntent().getIntExtra(EXTRA_MODE, MODE_SIGNALANT);
        isSecours = (startScreen == MODE_SECOURS);

        // Choisir le bon menu selon le profil
        if (isSecours) {
            bottomNav.inflateMenu(R.menu.bottom_nav_secours);
            setupSecoursNav();
            showFragmentSecours(0); // commence sur Détail
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_signalant);
            setupSignalantNav();
            showFragmentSignalant(0); // commence sur Signaler
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
        currentScreen = index;
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
        currentScreen = index;
        Fragment f = (index == 1) ? screen2Fragment : screen3Fragment;
        replace(f);
    }

    // ── Commun ────────────────────────────────────────────────────────────────

    private void replace(Fragment f) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, f)
                .commit();
    }

    @Override
    public void onClick(int numFragment) {
        // Depuis Screen1 → aller à la liste
        if (isSecours) {
            showFragmentSecours(1);
            bottomNav.setSelectedItemId(R.id.nav_incidents);
        }
    }

    @Override
    public void onDataChange(int numFragment, Object object, int actionCode, Object argsAction) {
        if (numFragment == Screen2Fragment.FRAGMENT_ID) {
            if (actionCode == Screen2Fragment.ACTION_ITEM_CLICKED && object instanceof Issue) {
                selectedIssue = (Issue) object;
                screen1Fragment.displayIssue(selectedIssue);
                if (isSecours) {
                    showFragmentSecours(0);
                    bottomNav.setSelectedItemId(R.id.nav_detail);
                }
            }
        }

        if (numFragment == Screen3Fragment.FRAGMENT_ID) {
            if (object instanceof Issue) {
                Issue newIssue = (Issue) object;
                selectedIssue = newIssue;
                screen2Fragment.addIssue(newIssue);
                IssueManager.getInstance().addIssue(newIssue);
                // Screen3 gère lui-même l'affichage de la page succès
            }
        }
    }

    @Override
    public void onFragmentDisplayed(int fragmentId) {
        currentScreen = fragmentId;
    }

    @Override
    public void onPictureTaken(String photopath) {
        if (selectedIssue != null) {
            selectedIssue.setPicture(photopath);
        }
    }
}
