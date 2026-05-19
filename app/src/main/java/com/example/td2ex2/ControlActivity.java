package com.example.td2ex2;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class ControlActivity extends AppCompatActivity implements Notifiable, Menuable, Picturable {

    private Screen1Fragment screen1Fragment;
    private Screen2Fragment screen2Fragment;
    private Screen3Fragment screen3Fragment;
    private Screen4Fragment screen4Fragment;
    private MenuFragment menuFragment;

    private Issue selectedIssue;

    private int currentScreen = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        screen1Fragment = new Screen1Fragment();
        screen2Fragment = new Screen2Fragment();
        screen3Fragment = new Screen3Fragment();
        screen4Fragment = new Screen4Fragment();

        int startScreen = getIntent().getIntExtra("startScreen", 0);

        menuFragment = MenuFragment.newInstance(startScreen);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.menuContainer, menuFragment)
                .commit();

        showScreen(startScreen);
    }

    private void showScreen(int index) {
        currentScreen = index;

        Fragment fragment;

        if (index == 0) {
            fragment = screen1Fragment;
        } else if (index == 1) {
            fragment = screen2Fragment;
        } else if (index == 2) {
            fragment = screen3Fragment;
        } else if (index == 3) {
            fragment = screen4Fragment;
        } else if (index == 4) {
            fragment = new Screen5Fragment();
        } else if (index == 5) {
            fragment = new Screen6Fragment();
        } else {
            fragment = new Screen7Fragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();

        if (menuFragment != null) {
            menuFragment.setCurrentActivatedIndex(index);
        }
    }

    @Override
    public void onMenuChange(int index) {
        showScreen(index);
    }

    @Override
    public void onClick(int numFragment) {
        if (numFragment == Screen1Fragment.FRAGMENT_ID) {
            showScreen(1);
        }
    }

    @Override
    public void onDataChange(int numFragment, Object object, int actionCode, Object argsAction) {
        if (numFragment == Screen2Fragment.FRAGMENT_ID) {
            if (actionCode == Screen2Fragment.ACTION_ITEM_CLICKED && object instanceof Issue) {
                selectedIssue = (Issue) object;
                screen1Fragment.displayIssue(selectedIssue);
                showScreen(0);
            }
        }

        if (numFragment == Screen3Fragment.FRAGMENT_ID) {
            if (object instanceof Issue) {
                Issue newIssue = (Issue) object;

                selectedIssue = newIssue;

                screen2Fragment.addIssue(newIssue);
                IssueManager.getInstance().addIssue(newIssue);

                showScreen(1);
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