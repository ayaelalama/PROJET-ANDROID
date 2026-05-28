package com.example.td2ex2;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ControlActivity extends AppCompatActivity implements Notifiable, Picturable {

    private Screen1Fragment screen1Fragment;
    private Screen2Fragment screen2Fragment;
    private Screen3Fragment screen3Fragment;
    private Screen4Fragment screen4Fragment;
    private Screen5Fragment screen5Fragment;

    private Issue selectedIssue;
    private int currentScreen = 0;

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

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_detail)       { showScreen(0); return true; }
            if (id == R.id.nav_incidents)    { showScreen(1); return true; }
            if (id == R.id.nav_signalement)  { showScreen(2); return true; }
            if (id == R.id.nav_alertes)      { showScreen(3); return true; }
            if (id == R.id.nav_carte)        { showScreen(4); return true; }
            return false;
        });

        int startScreen = getIntent().getIntExtra("startScreen", 0);
        showScreen(startScreen);
        syncBottomNav(startScreen);
    }

    private void showScreen(int index) {
        currentScreen = index;
        Fragment fragment;

        if (index == 0)      fragment = screen1Fragment;
        else if (index == 1) fragment = screen2Fragment;
        else if (index == 2) fragment = screen3Fragment;
        else if (index == 3) fragment = screen4Fragment;
        else                 fragment = screen5Fragment;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void syncBottomNav(int index) {
        if (bottomNav == null) return;
        int[] ids = {R.id.nav_detail, R.id.nav_incidents, R.id.nav_signalement,
                     R.id.nav_alertes, R.id.nav_carte};
        if (index >= 0 && index < ids.length) {
            bottomNav.setSelectedItemId(ids[index]);
        }
    }

    @Override
    public void onClick(int numFragment) {
        if (numFragment == Screen1Fragment.FRAGMENT_ID) {
            showScreen(1);
            syncBottomNav(1);
        }
    }

    @Override
    public void onDataChange(int numFragment, Object object, int actionCode, Object argsAction) {
        if (numFragment == Screen2Fragment.FRAGMENT_ID) {
            if (actionCode == Screen2Fragment.ACTION_ITEM_CLICKED && object instanceof Issue) {
                selectedIssue = (Issue) object;
                screen1Fragment.displayIssue(selectedIssue);
                showScreen(0);
                syncBottomNav(0);
            }
        }

        if (numFragment == Screen3Fragment.FRAGMENT_ID) {
            if (object instanceof Issue) {
                Issue newIssue = (Issue) object;
                selectedIssue = newIssue;
                screen2Fragment.addIssue(newIssue);
                IssueManager.getInstance().addIssue(newIssue);

                if (actionCode == Notifiable.ACTION_SHOW_ISSUE_DETAILS) {
                    // Stay on success screen; navigation handled inside Screen3Fragment
                } else {
                    showScreen(1);
                    syncBottomNav(1);
                }
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
