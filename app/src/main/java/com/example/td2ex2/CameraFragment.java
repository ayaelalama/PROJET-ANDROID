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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof Picturable) {
            picturable = (Picturable) context;
        } else {
            throw new AssertionError("ControlActivity doit implémenter Picturable.");
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

        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString(KEY_PHOTO_PATH);
        }

        if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
            displayPicture(currentPhotoPath);
        } else {
            pictureView.setImageResource(R.drawable.ic_camera_placeholder);
        }

        takePictureButton.setOnClickListener(v -> checkPermissionAndTakePicture());

        getParentFragmentManager().setFragmentResultListener(
                RESULT_CHANNEL,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    String path = bundle.getString(RESULT_PHOTO_PATH);

                    if (path != null && !path.isEmpty()) {
                        currentPhotoPath = path;
                        displayPicture(path);
                    }
                }
        );
    }

    private void checkPermissionAndTakePicture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            takePicture();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void takePicture() {
        try {
            File photoFile = File.createTempFile(
                    "alerte_accident_",
                    ".jpg",
                    requireContext().getCacheDir()
            );

            currentPhotoPath = photoFile.getAbsolutePath();

            currentPhotoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile
            );

            takePictureLauncher.launch(currentPhotoUri);

        } catch (IOException e) {
            Toast.makeText(requireContext(), "Erreur création fichier photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayPicture(String path) {
        pictureView.setImageURI(Uri.fromFile(new File(path)));
    }

    private void explainPermission() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Permission caméra")
                .setMessage("L'application a besoin de la caméra pour ajouter une photo de l'accident.")
                .setPositiveButton("Réessayer", (dialog, which) ->
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA))
                .setNegativeButton("Refuser", null)
                .show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PHOTO_PATH, currentPhotoPath);
    }
}