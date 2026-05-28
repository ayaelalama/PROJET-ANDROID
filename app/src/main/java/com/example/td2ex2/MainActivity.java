package com.example.td2ex2;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ImageView animationImageView;
    private AnimationDrawable animationDrawable;
    private Button defaultButton;
    private Button optionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        animationImageView = findViewById(R.id.animationImageView);
        defaultButton = findViewById(R.id.defaultButton);
        optionButton = findViewById(R.id.optionButton);

        animationImageView.setImageResource(R.drawable.animation);
        animationDrawable = (AnimationDrawable) animationImageView.getDrawable();

        // "Signaler un accident" → ouvre le flow de signalement (Screen3)
        defaultButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ControlActivity.class);
            intent.putExtra("startScreen", 2); // Screen3 = signalement
            startActivity(intent);
        });

        // "Interface Secours" → ouvre la vue détail secours (Screen1)
        optionButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ControlActivity.class);
            intent.putExtra("startScreen", 0); // Screen1 = détail secours
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (animationDrawable != null) {
            animationDrawable.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (animationDrawable != null && animationDrawable.isRunning()) {
            animationDrawable.stop();
        }
    }
}
