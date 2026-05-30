package com.example.td2ex2;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String CODE_SECOURS = "1234";

    private ImageView animationImageView;
    private AnimationDrawable animationDrawable;
    private Button defaultButton;
    private Button optionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        animationImageView = findViewById(R.id.animationImageView);
        defaultButton      = findViewById(R.id.defaultButton);
        optionButton       = findViewById(R.id.optionButton);

        animationImageView.setImageResource(R.drawable.animation);
        animationDrawable = (AnimationDrawable) animationImageView.getDrawable();

        // Signaler un accident — accès libre
        defaultButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ControlActivity.class);
            intent.putExtra("startScreen", 2);
            startActivity(intent);
        });

        // Interface Secours — code requis
        optionButton.setOnClickListener(v -> showSecoursCodeDialog());
    }

    private void showSecoursCodeDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Code d'accès");
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("🚑 Interface Secours")
                .setMessage("Accès réservé aux services de secours.\nEntrez le code d'accès (1234) :")
                .setView(input)
                .setPositiveButton("Accéder", (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    if (CODE_SECOURS.equals(code)) {
                        Intent intent = new Intent(MainActivity.this, ControlActivity.class);
                        intent.putExtra("startScreen", 0);
                        startActivity(intent);
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("❌ Code incorrect")
                                .setMessage("Le code saisi est invalide.")
                                .setPositiveButton("Réessayer", (d, w) -> showSecoursCodeDialog())
                                .setNegativeButton("Annuler", null)
                                .show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (animationDrawable != null) animationDrawable.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (animationDrawable != null && animationDrawable.isRunning()) animationDrawable.stop();
    }
}
