package com.example.td2ex2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

public class CameraFragment extends Fragment {

    public static final String RESULT_CHANNEL = "camera_channel";
    public static final String RESULT_PHOTO_PATH = "photo_path";

    private static final String KEY_PHOTO_PATH = "photo_path";

    private ImageView pictureView;
    private Picturable picturable;
    private String currentPhotoPath;
    private Uri currentPhotoUri;

    // ── Activity Result Launchers ─────────────────────────────────────────────

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    takePicture();
                } else {
                    explainPermission();
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentPhotoPath != null) {
                    displayPicture(currentPhotoPath);
                    if (picturable != null) {
                        picturable.onPictureTaken(currentPhotoPath);
                    }
                } else {
                    Toast.makeText(requireContext(), "Photo annulée", Toast.LENGTH_SHORT).show();
                }
            });

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Picturable) {
            picturable = (Picturable) context;
        } else {
            throw new AssertionError("L'activité hôte doit implémenter Picturable.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pictureView = view.findViewById(R.id.pictureView);
        Button takePictureButton = view.findViewById(R.id.takePictureButton);

        // Restauration après rotation
        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString(KEY_PHOTO_PATH);
        }

        // Affichage initial
        if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
            displayPicture(currentPhotoPath);
        } else {
            pictureView.setImageResource(R.drawable.ic_camera_placeholder);
        }

        takePictureButton.setOnClickListener(v -> checkPermissionAndTakePicture());

        // Canal de communication inter-fragments (depuis Screen1Fragment via getChildFragmentManager)
        // Screen1Fragment appelle getChildFragmentManager().setFragmentResult(...)
        // donc ici on écoute sur getParentFragmentManager() car ce fragment est enfant de Screen1
        getParentFragmentManager().setFragmentResultListener(
                RESULT_CHANNEL,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    String path = bundle.getString(RESULT_PHOTO_PATH);
                    if (path != null && !path.isEmpty()) {
                        currentPhotoPath = path;
                        displayPicture(path);
                    } else {
                        // Pas de photo → placeholder
                        currentPhotoPath = null;
                        pictureView.setImageResource(R.drawable.ic_camera_placeholder);
                    }
                }
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // On sauvegarde uniquement le chemin (String léger), jamais le Bitmap
        outState.putString(KEY_PHOTO_PATH, currentPhotoPath);
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private void checkPermissionAndTakePicture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            takePicture();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void explainPermission() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Permission caméra requise")
                .setMessage("L'application a besoin d'accéder à la caméra pour photographier " +
                        "la scène d'accident. Sans cette permission, la prise de photo est impossible.")
                .setPositiveButton("Réessayer", (dialog, which) ->
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA))
                .setNegativeButton("Refuser", (dialog, which) ->
                        Toast.makeText(requireContext(),
                                "Fonctionnalité photo désactivée.", Toast.LENGTH_SHORT).show())
                .show();
    }

    // ── Capture photo ─────────────────────────────────────────────────────────

    private void takePicture() {
        try {
            // Création d'un fichier temporaire sécurisé dans le cache
            File photoFile = File.createTempFile(
                    "alerte_accident_",
                    ".jpg",
                    requireContext().getCacheDir()
            );

            currentPhotoPath = photoFile.getAbsolutePath();

            // URI sécurisé via FileProvider (évite l'exposition directe du chemin)
            currentPhotoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile
            );

            takePictureLauncher.launch(currentPhotoUri);

        } catch (IOException e) {
            Toast.makeText(requireContext(), "Erreur lors de la création du fichier photo.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ── Affichage ─────────────────────────────────────────────────────────────

    private void displayPicture(String path) {
        // Utilisation de Picasso pour le chargement d'images locales (robuste aux rotations)
        Picasso.get()
                .load(new File(path))
                .placeholder(R.drawable.ic_camera_placeholder)
                .error(R.drawable.ic_camera_placeholder)
                .fit()
                .centerCrop()
                .into(pictureView);
    }
}