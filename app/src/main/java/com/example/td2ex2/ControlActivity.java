package com.example.td2ex2;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ControlActivity extends AppCompatActivity implements Notifiable, Picturable {

    public static final int MODE_SECOURS   = 0;
    public static final int MODE_SIGNALANT = 2;

    private Screen1Fragment screen1Fragment;
    private Screen2Fragment screen2Fragment;
    private Screen3Fragment screen3Fragment;
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
        screen5Fragment = new Screen5Fragment();

        bottomNav = findViewById(R.id.bottomNav);

        int startScreen = getIntent().getIntExtra("startScreen", MODE_SIGNALANT);
        isSecours = (startScreen == MODE_SECOURS);

        if (isSecours) {
            bottomNav.inflateMenu(R.menu.bottom_nav_secours);
            setupSecoursNav();
            showFragment(screen1Fragment);
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_signalant);
            setupSignalantNav();
            showFragment(screen3Fragment);
        }
    }

    private void setupSecoursNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_detail)    { showFragment(screen1Fragment); return true; }
            if (id == R.id.nav_incidents) { showFragment(screen2Fragment); return true; }
            if (id == R.id.nav_carte)     { showFragment(screen5Fragment); return true; }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_detail);
    }

    private void setupSignalantNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_signalement) { showFragment(screen3Fragment); return true; }
            if (id == R.id.nav_incidents)   { showFragment(screen2Fragment); return true; }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_signalement);
    }

    private void showFragment(Fragment f) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, f)
                .commit();
    }

    @Override
    public void onClick(int numFragment) {
        if (isSecours) {
            showFragment(screen2Fragment);
            bottomNav.setSelectedItemId(R.id.nav_incidents);
        }
    }

    @Override
    public void onDataChange(int numFragment, Object object, int actionCode, Object argsAction) {

        if (numFragment == Screen2Fragment.FRAGMENT_ID) {
            if (actionCode == Screen2Fragment.ACTION_ITEM_CLICKED && object instanceof Issue) {
                selectedIssue = (Issue) object;
                if (isSecours) {
                    // Navigate FIRST so fragment gets attached, then pass the issue
                    showFragment(screen1Fragment);
                    bottomNav.setSelectedItemId(R.id.nav_detail);
                    // Post to next frame so fragment is fully attached before displayIssue
                    bottomNav.post(() -> screen1Fragment.displayIssue(selectedIssue));
                } else {
                    showFragment(screen3Fragment);
                    bottomNav.setSelectedItemId(R.id.nav_signalement);
                    bottomNav.post(() -> screen3Fragment.startEditing(selectedIssue));
                }
            }
        }

        if (numFragment == Screen3Fragment.FRAGMENT_ID && object instanceof Issue) {
            Issue newIssue = (Issue) object;
            selectedIssue = newIssue;
            screen2Fragment.addIssue(newIssue);
            // Store issue in screen1 (it will render when navigated to)
            screen1Fragment.displayIssue(newIssue);
        }
    }

    @Override
    public void onFragmentDisplayed(int fragmentId) {}

    @Override
    public void onPictureTaken(String photopath) {
        if (selectedIssue != null) selectedIssue.setPicture(photopath);
    }
}
