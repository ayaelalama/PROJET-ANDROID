package com.example.td2ex2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.squareup.picasso.Picasso;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.IOException;

/**
 * Screen 3 — Flux guidé de signalement d'accident (8 pages).
 * 01-Vues  02-Parcelable  03-Fragment  08-Factory  11-GPS/Caméra  12-Notifications
 *
 * Pages : 0=infos  1=impliqué  2=type  3=blessés  4=gravité
 *         5=photo  6=GPS  7=récap  8=succès
 */
public class Screen3Fragment extends Fragment {

    public static final int FRAGMENT_ID = 2;
    private static final String CHANNEL_ID = "signalement_channel";

    private Notifiable notifiable;
    private ScrollView scrollView;
    private LinearLayout formContainer;
    private Button backButton, nextButton;
    private TextView stepLabel;
    private LinearLayout progressDots;

    private int page = 0;

    // Données personnelles
    private String nom = "", prenom = "", telephone = "", adresse = "";
    private String jour = "", mois = "", annee = "";

    // Questionnaire
    private String concerne = "", typeAccident = "", blesses = "", gravite = "";

    // GPS
    private double latitude = 43.7009, longitude = 7.2684;
    private boolean locationAcquired = false;
    private FusedLocationProviderClient fusedLocationClient;

    // Photo
    private String currentPhotoPath = null;
    private Uri currentPhotoUri = null;
    private ImageView photoPreview;

    // Edition
    private Issue incidentEnCours = null;
    private boolean modeEdition = false;

    // EditText refs
    private EditText nomEdit, prenomEdit, telEdit, jourEdit, moisEdit, anneeEdit, adresseEdit;

    // ── Launchers ─────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<String> gpsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    fetchLocation();
                } else {
                    Toast.makeText(requireContext(),
                            "Permission GPS refusée — position simulée utilisée.", Toast.LENGTH_LONG).show();
                    latitude = 43.7009;
                    longitude = 7.2684;
                    locationAcquired = true;
                    if (page == 6) showPage();
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else Toast.makeText(requireContext(), "Permission caméra refusée.", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentPhotoPath != null) {
                    Toast.makeText(requireContext(), " Photo enregistrée !", Toast.LENGTH_SHORT).show();
                    // Refresh page so preview and button text update
                    showPage();
                } else {
                    currentPhotoPath = null;
                    Toast.makeText(requireContext(), "Photo annulée.", Toast.LENGTH_SHORT).show();
                }
            });

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Notifiable) notifiable = (Notifiable) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_screen3, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (notifiable != null) notifiable.onFragmentDisplayed(FRAGMENT_ID);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        scrollView    = view.findViewById(R.id.formScrollView);
        formContainer = view.findViewById(R.id.formContainer);
        backButton    = view.findViewById(R.id.backButton);
        nextButton    = view.findViewById(R.id.nextButton);
        stepLabel     = view.findViewById(R.id.stepLabel);
        progressDots  = view.findViewById(R.id.progressDots);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        backButton.setOnClickListener(v -> {
            saveInputs();
            if (page > 0 && page < 8) { page--; showPage(); scrollToTop(); }
        });

        nextButton.setOnClickListener(v -> {
            saveInputs();
            if (!validateCurrentPage()) return;
            if (page == 7) {
                sendIncident();
            } else if (page < 7) {
                page++;
                showPage();
                scrollToTop();
            }
        });

        createNotificationChannel();
        showPage();
    }

    // ── Mode édition ──────────────────────────────────────────────────────────

    public void startEditing(Issue issue) {
        this.incidentEnCours = issue;
        this.modeEdition = true;
        page = 0;
        showPage();
        scrollToTop();
    }

    private void scrollToTop() {
        if (scrollView != null) scrollView.post(() -> scrollView.smoothScrollTo(0, 0));
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateCurrentPage() {
        switch (page) {
            case 0: return validatePersonalInfo();
            case 1: return validateQ(concerne,     "Veuillez indiquer si vous êtes impliqué(e) ou témoin.");
            case 2: return validateQ(typeAccident, "Veuillez sélectionner le type d'accident.");
            case 3: return validateQ(blesses,      "Veuillez indiquer s'il y a des blessés.");
            case 4: return validateQ(gravite,      "Veuillez indiquer la gravité de la situation.");
            default: return true;
        }
    }

    private boolean validateQ(String value, String msg) {
        if (value == null || value.trim().isEmpty()) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean validatePersonalInfo() {
        if (nom.trim().isEmpty())            { showErr(nomEdit,    "Nom obligatoire.");             return false; }
        if (!nom.matches("[a-zA-ZÀ-ÿ\\s'\\-]+")) { showErr(nomEdit, "Lettres uniquement.");         return false; }
        if (prenom.trim().isEmpty())         { showErr(prenomEdit, "Prénom obligatoire.");           return false; }
        if (!telephone.matches("0[0-9]{9}")) { showErr(telEdit,    "Format 0XXXXXXXXX.");           return false; }
        if (!isValidDate(jour, mois, annee)) {
            Toast.makeText(requireContext(), "Date invalide (JJ/MM/AAAA).", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void showErr(EditText f, String msg) {
        if (f != null) f.setError(msg);
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private boolean isValidDate(String j, String m, String a) {
        try {
            int d = Integer.parseInt(j), mo = Integer.parseInt(m), y = Integer.parseInt(a);
            if (mo < 1 || mo > 12 || d < 1 || d > 31 || y < 1900 || y > 2100) return false;
            if ((mo==4||mo==6||mo==9||mo==11) && d > 30) return false;
            if (mo == 2) { boolean leap = (y%4==0&&y%100!=0)||(y%400==0); if (d > (leap?29:28)) return false; }
            return true;
        } catch (NumberFormatException e) { return false; }
    }

    // ── Affichage pages ───────────────────────────────────────────────────────

    private void showPage() {
        formContainer.removeAllViews();
        photoPreview = null;

        backButton.setVisibility(page > 0 && page < 8 ? View.VISIBLE : View.INVISIBLE);
        nextButton.setVisibility(page < 8 ? View.VISIBLE : View.GONE);

        updateHeader();

        switch (page) {
            case 0: showPersonalInfo();  break;
            case 1: showQuestion("Étape 1/4", "Vous êtes concerné(e) par l'accident ?",
                        new String[]{"Oui, je suis impliqué(e)", "Non, je suis témoin"}, 1); break;
            case 2: showQuestion("Étape 2/4", "Quel type d'accident ?",
                        new String[]{"Collision entre véhicules","Véhicule seul","Moto / Scooter",
                                     "Piéton / cycliste","Plusieurs véhicules","Je ne sais pas"}, 2); break;
            case 3: showQuestion("Étape 3/4", "Y a-t-il des blessés ?",
                        new String[]{"Oui","Non","Je ne sais pas"}, 3); break;
            case 4: showQuestion("Étape 4/4", "Quelle est la gravité ?",
                        new String[]{"Dégâts légers","Situation préoccupante","Danger immédiat / blessé grave"}, 4); break;
            case 5: showPhotoPage();     break;
            case 6: showGpsPage();       break;
            case 7: showRecapPage();     break;
            case 8: showSuccess();       break;
        }
    }

    private void updateHeader() {
        if (stepLabel == null || progressDots == null) return;
        String[] labels = {"Vos informations","Êtes-vous impliqué ?","Type d'accident",
                           "Blessés ?","Gravité","Photo de la scène","Localisation GPS",
                           "Récapitulatif"," Envoyé"};
        stepLabel.setText(page < labels.length ? labels[page] : "");
        progressDots.removeAllViews();
        if (page >= 8) return;
        for (int i = 0; i <= 7; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(i == page ? 24 : 8), dp(8));
            lp.setMargins(dp(3), 0, dp(3), 0);
            dot.setLayoutParams(lp);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(4));
            bg.setColor(i == page ? Color.WHITE : 0x80FFFFFF);
            dot.setBackground(bg);
            progressDots.addView(dot);
        }
    }

    // ── Page 0 : Infos personnelles ───────────────────────────────────────────

    private void showPersonalInfo() {
        nextButton.setText("Suivant ");
        addTitle("Vos informations");
        addSubtitle(modeEdition ? " Mode modification — corrigez vos informations"
                : "Ces informations aident les secours à vous identifier.");

        TextView req = mkText("* Champs obligatoires", 12, 0xFFD32F2F, Typeface.ITALIC);
        formContainer.addView(req, fullP());

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        nomEdit    = mkInput("Nom *", nom,    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        prenomEdit = mkInput("Prénom *", prenom, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        row.addView(nomEdit,    wP());
        row.addView(prenomEdit, wP());
        formContainer.addView(row, fullP());

        telEdit = mkInput("Téléphone * (0XXXXXXXXX)", telephone, InputType.TYPE_CLASS_PHONE);
        telEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        formContainer.addView(telEdit, fullP());

        addLabel("Date de naissance *");
        LinearLayout dateRow = new LinearLayout(requireContext());
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        jourEdit  = mkInput("JJ",   jour,  InputType.TYPE_CLASS_NUMBER);
        moisEdit  = mkInput("MM",   mois,  InputType.TYPE_CLASS_NUMBER);
        anneeEdit = mkInput("AAAA", annee, InputType.TYPE_CLASS_NUMBER);
        jourEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        moisEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        anneeEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        dateRow.addView(jourEdit,  wP());
        dateRow.addView(moisEdit,  wP());
        dateRow.addView(anneeEdit, wP());
        formContainer.addView(dateRow, fullP());

        adresseEdit = mkInput("Adresse (optionnel)", adresse, InputType.TYPE_CLASS_TEXT);
        formContainer.addView(adresseEdit, fullP());
    }

    // ── Pages 1-4 : Questionnaire ─────────────────────────────────────────────

    private void showQuestion(String step, String question, String[] options, int qNum) {
        nextButton.setText("Suivant ");
        addTitle("Questionnaire");

        TextView stepTv = mkText(step, 13, 0xFF5C6370, Typeface.NORMAL);
        stepTv.setGravity(Gravity.END);
        formContainer.addView(stepTv, fullP());

        TextView qTv = mkText(question, 18, 0xFF1A1A2E, Typeface.BOLD);
        qTv.setGravity(Gravity.CENTER);
        qTv.setPadding(dp(16), dp(20), dp(16), dp(20));
        qTv.setBackground(card(0xFFE3F2FD));
        LinearLayout.LayoutParams cp = fullP();
        cp.setMargins(0, dp(10), 0, dp(14));
        formContainer.addView(qTv, cp);

        for (String opt : options) addOption(opt, qNum);
    }

    // ── Page 5 : Photo ────────────────────────────────────────────────────────

    private void showPhotoPage() {
        nextButton.setVisibility(View.GONE);
        backButton.setVisibility(View.VISIBLE);

        addTitle(" Photo de la scène");
        addSubtitle("Prenez une photo pour aider les secours. (Optionnel)");

        photoPreview = new ImageView(requireContext());
        photoPreview.setAdjustViewBounds(true);
        photoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoPreview.setBackground(card(0xFFF0F4F8));
        LinearLayout.LayoutParams imgP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(220));
        imgP.setMargins(0, dp(8), 0, dp(8));

        if (currentPhotoPath != null) {
            Picasso.get().load(new File(currentPhotoPath))
                    .placeholder(R.drawable.ic_camera_placeholder)
                    .fit().centerCrop().into(photoPreview);
        } else {
            photoPreview.setImageResource(R.drawable.ic_camera_placeholder);
        }
        formContainer.addView(photoPreview, imgP);

        Button btnPhoto = new Button(requireContext());
        btnPhoto.setText(currentPhotoPath != null ? " Reprendre la photo" : " Prendre une photo");
        btnPhoto.setAllCaps(false);
        btnPhoto.setTextColor(Color.WHITE);
        btnPhoto.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1565C0));
        btnPhoto.setOnClickListener(v -> checkCameraAndTake());
        formContainer.addView(btnPhoto, fullP());

        if (currentPhotoPath != null) {
            TextView ok = mkText(" Photo enregistrée", 13, 0xFF2E7D32, Typeface.BOLD);
            ok.setGravity(Gravity.CENTER);
            formContainer.addView(ok, fullP());
        }

        Button btnNext = new Button(requireContext());
        btnNext.setText("Suivant ");
        btnNext.setAllCaps(false);
        btnNext.setTextColor(Color.WHITE);
        btnNext.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1565C0));
        btnNext.setOnClickListener(v -> { page++; showPage(); scrollToTop(); });
        formContainer.addView(btnNext, fullP());

        Button btnSkip = new Button(requireContext());
        btnSkip.setText("Passer cette étape");
        btnSkip.setAllCaps(false);
        btnSkip.setTextColor(0xFF5C6370);
        btnSkip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF0F4F8));
        btnSkip.setOnClickListener(v -> { currentPhotoPath = null; page++; showPage(); scrollToTop(); });
        formContainer.addView(btnSkip, fullP());
    }

    // ── Page 6 : GPS ──────────────────────────────────────────────────────────

    private void showGpsPage() {
        nextButton.setText("Suivant ");

        addTitle(" Localisation GPS");
        addSubtitle("Détectez votre position réelle pour guider les secours.");

        if (!locationAcquired) requestGPS();

        TextView statusTv = mkText(
                locationAcquired
                    ? " Position détectée :\n" + String.format("%.5f, %.5f", latitude, longitude)
                    : " Récupération de la position en cours…",
                15,
                locationAcquired ? 0xFF2E7D32 : 0xFF1565C0,
                Typeface.BOLD);
        statusTv.setGravity(Gravity.CENTER);
        statusTv.setPadding(dp(12), dp(16), dp(12), dp(16));
        statusTv.setBackground(card(locationAcquired ? 0xFFE8F5E9 : 0xFFE3F2FD));
        LinearLayout.LayoutParams sp = fullP();
        sp.setMargins(0, dp(8), 0, dp(12));
        formContainer.addView(statusTv, sp);

        // Carte
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        MapView map = new MapView(requireContext());
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        GeoPoint pt = new GeoPoint(latitude, longitude);
        map.getController().setZoom(16.0);
        map.getController().setCenter(pt);
        Marker marker = new Marker(map);
        marker.setPosition(pt);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Lieu de l'accident");
        map.getOverlays().add(marker);
        map.invalidate();

        LinearLayout.LayoutParams mapP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(240));
        mapP.setMargins(0, dp(4), 0, dp(12));
        formContainer.addView(map, mapP);

        Button retryBtn = new Button(requireContext());
        retryBtn.setText(" Actualiser la position GPS");
        retryBtn.setAllCaps(false);
        retryBtn.setTextColor(0xFF1565C0);
        retryBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
        retryBtn.setOnClickListener(v -> { locationAcquired = false; showPage(); });
        formContainer.addView(retryBtn, fullP());
    }

    // ── Page 7 : Récap ────────────────────────────────────────────────────────

    private void showRecapPage() {
        nextButton.setText(" Confirmer et envoyer");

        addTitle("Récapitulatif");
        addSubtitle("Vérifiez vos informations. Appuyez sur ✏ pour modifier.");

        addRecapEditable("👤 Identité",
                nom + " " + prenom + "\nTél : " + telephone +
                "\nNé(e) le : " + jour + "/" + mois + "/" + annee +
                (adresse.isEmpty() ? "" : "\n" + adresse), 0);

        addRecapEditable(" Accident",
                "Impliqué : " + v(concerne) +
                "\nType : " + v(typeAccident) +
                "\nBlessés : " + v(blesses) +
                "\nGravité : " + v(gravite), 2);

        addRecapEditable(" Localisation",
                String.format("%.5f, %.5f", latitude, longitude), 6);

        if (currentPhotoPath != null) {
            addSubtitle(" Photo jointe :");
            ImageView mini = new ImageView(requireContext());
            mini.setAdjustViewBounds(true);
            mini.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams imgP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(120));
            imgP.setMargins(0, dp(4), 0, dp(8));
            Picasso.get().load(new File(currentPhotoPath))
                    .placeholder(R.drawable.ic_camera_placeholder)
                    .fit().centerCrop().into(mini);
            formContainer.addView(mini, imgP);
        } else {
            TextView noPhoto = mkText(" Aucune photo (optionnel)", 13, 0xFF9E9E9E, Typeface.ITALIC);
            noPhoto.setGravity(Gravity.CENTER);
            formContainer.addView(noPhoto, fullP());
        }
    }

    // ── Page 8 : Succès ───────────────────────────────────────────────────────

    private void showSuccess() {
        backButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(40), dp(24), dp(40));
        card.setBackground(card(Color.WHITE));

        TextView emoji = mkText("", 64, 0xFF2E7D32, Typeface.NORMAL);
        emoji.setGravity(Gravity.CENTER);
        card.addView(emoji, fullP());

        TextView title = mkText("Alerte transmise !", 24, 0xFF2E7D32, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title, fullP());

        TextView sub = mkText(
                "Votre signalement a bien été envoyé aux services compétents.\n\n" +
                "Les secours ont été alertés et interviendront dans les plus brefs délais.\n\n" +
                "Restez en sécurité.",
                15, 0xFF5C6370, Typeface.NORMAL);
        sub.setGravity(Gravity.CENTER);
        card.addView(sub, fullP());

        LinearLayout.LayoutParams cp = fullP();
        cp.setMargins(0, dp(40), 0, dp(24));
        formContainer.addView(card, cp);

        Button newReport = new Button(requireContext());
        newReport.setText("Nouveau signalement");
        newReport.setAllCaps(false);
        newReport.setTextColor(Color.WHITE);
        newReport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1565C0));
        newReport.setOnClickListener(v -> { resetForm(); page = 0; showPage(); scrollToTop(); });
        formContainer.addView(newReport, fullP());
    }

    // ── Envoi ─────────────────────────────────────────────────────────────────

    private void sendIncident() {
        Issue.Priority priority;
        if ("Danger immédiat / blessé grave".equals(gravite))  priority = Issue.Priority.CRITICAL;
        else if ("Situation préoccupante".equals(gravite))     priority = Issue.Priority.HIGH;
        else                                                    priority = Issue.Priority.MEDIUM;

        // La factory est toujours UrbanFactory : le formulaire ne collecte pas
        // d'information permettant de distinguer autoroute / urbain de façon fiable.
        // La priorité est déterminée par la réponse "gravité" de l'utilisateur.
        AccidentFactory factory = new UrbanFactory();

        String titleStr = "Accident — " + prenom + " " + nom;
        String desc = "Tél : " + telephone
                + "\nNé(e) le : " + jour + "/" + mois + "/" + annee
                + (adresse.isEmpty() ? "" : "\nAdresse : " + adresse)
                + "\nImpliqué : " + concerne
                + "\nType : " + typeAccident
                + "\nBlessés : " + blesses
                + "\nGravité : " + gravite
                + "\nGPS : " + String.format("%.5f, %.5f", latitude, longitude)
                + (currentPhotoPath != null ? "\n Photo jointe" : "");

        if (modeEdition && incidentEnCours != null) {
            incidentEnCours.setPriority(priority);
            incidentEnCours.setLocation(latitude, longitude);
            if (currentPhotoPath != null) incidentEnCours.setPicture(currentPhotoPath);
            EmergencyService.getInstance().onStatusChanged(incidentEnCours);
            sendLocalNotification("Signalement modifié", titleStr);
            if (notifiable != null)
                notifiable.onDataChange(FRAGMENT_ID, incidentEnCours, Notifiable.ACTION_SHOW_ISSUE_DETAILS, null);
            modeEdition = false;
        } else {
            Issue issue = factory.createIssue(titleStr, desc, priority);
            issue.setStatus(2f);
            issue.setLocation(latitude, longitude);
            if (currentPhotoPath != null) issue.setPicture(currentPhotoPath);

            IssueManager.getInstance().addIssue(issue);
            issue.addObserver(EmergencyService.getInstance());
            EmergencyService.getInstance().onStatusChanged(issue);

            // Notification pour le signalant (confirmation d'envoi)
            sendLocalNotification(titleStr, "Gravité : " + gravite);

            // Notification pour le secouriste (nouvel incident disponible)
            SecoursNotificationHelper.notifyNouvelIncident(requireContext(), issue);
            if (notifiable != null)
                notifiable.onDataChange(FRAGMENT_ID, issue, Notifiable.ACTION_SHOW_ISSUE_DETAILS, null);
        }

        page = 8;
        showPage();
        scrollToTop();
    }

    // ── GPS ───────────────────────────────────────────────────────────────────

    private void requestGPS() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            gpsPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        // Try getCurrentLocation first (more reliable than getLastLocation on emulator)
        com.google.android.gms.location.CurrentLocationRequest request =
                new com.google.android.gms.location.CurrentLocationRequest.Builder()
                        .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                        .setMaxUpdateAgeMillis(0)
                        .setDurationMillis(10000)
                        .build();

        fusedLocationClient.getCurrentLocation(request, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        locationAcquired = true;
                        if (page == 6) showPage();
                    } else {
                        // Fallback: getLastLocation
                        fusedLocationClient.getLastLocation().addOnSuccessListener(last -> {
                            if (last != null) {
                                latitude = last.getLatitude();
                                longitude = last.getLongitude();
                            } else {
                                Toast.makeText(requireContext(),
                                        "GPS indisponible — position simulée utilisée.", Toast.LENGTH_SHORT).show();
                            }
                            locationAcquired = true;
                            if (page == 6) showPage();
                        }).addOnFailureListener(e -> {
                            locationAcquired = true;
                            if (page == 6) showPage();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to last known
                    fusedLocationClient.getLastLocation().addOnSuccessListener(last -> {
                        if (last != null) {
                            latitude = last.getLatitude();
                            longitude = last.getLongitude();
                        }
                        locationAcquired = true;
                        if (page == 6) showPage();
                    }).addOnFailureListener(ex -> {
                        locationAcquired = true;
                        if (page == 6) showPage();
                    });
                });
    }

    // ── Caméra ────────────────────────────────────────────────────────────────

    private void checkCameraAndTake() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File dir = requireContext().getExternalFilesDir(null);
            if (dir == null) dir = requireContext().getCacheDir();
            if (!dir.exists()) dir.mkdirs();
            File photoFile = File.createTempFile("alerte_", ".jpg", dir);
            currentPhotoPath = photoFile.getAbsolutePath();
            currentPhotoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePictureLauncher.launch(currentPhotoUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(),
                    "Erreur création fichier photo : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Signalement accident", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Confirmation d'envoi des signalements");
            NotificationManager nm = requireContext().getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void sendLocalNotification(String title, String body) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) return;
        }
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(requireContext(), 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(" " + title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        NotificationManagerCompat.from(requireContext()).notify((int) System.currentTimeMillis(), b.build());
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void addOption(String option, int qNum) {
        boolean sel = isSelected(option, qNum);
        TextView tv = mkText(option, 15, sel ? 0xFF1565C0 : 0xFF1A1A2E, Typeface.NORMAL);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(sel ? shape(0xFFE3F2FD, 0xFF1565C0, 2) : shape(0xFFF0F4F8, 0xFFD0D8E4, 1));
        tv.setOnClickListener(v -> {
            if (qNum==1) concerne=option;
            else if (qNum==2) typeAccident=option;
            else if (qNum==3) blesses=option;
            else if (qNum==4) gravite=option;
            showPage();
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        p.setMargins(0, dp(8), 0, dp(8));
        formContainer.addView(tv, p);
    }

    private void addRecapEditable(String title, String content, int goToPage) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(card(Color.WHITE));

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = mkText(title, 13, 0xFF1565C0, Typeface.BOLD);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(t);
        TextView edit = mkText("✏ Modifier", 12, 0xFF1565C0, Typeface.NORMAL);
        header.addView(edit);
        card.addView(header, fullP());

        TextView c = mkText(content, 14, 0xFF1A1A2E, Typeface.NORMAL);
        LinearLayout.LayoutParams cp = fullP(); cp.setMargins(0, dp(4), 0, 0);
        card.addView(c, cp);

        LinearLayout.LayoutParams lp = fullP(); lp.setMargins(0, dp(8), 0, dp(8));
        formContainer.addView(card, lp);

        View.OnClickListener goEdit = v -> {
            page = goToPage;
            showPage();
            scrollToTop();
            Toast.makeText(requireContext(), "Modifiez puis cliquez Suivant.", Toast.LENGTH_SHORT).show();
        };
        card.setOnClickListener(goEdit);
        edit.setOnClickListener(goEdit);
    }

    private boolean isSelected(String opt, int qNum) {
        switch (qNum) {
            case 1: return opt.equals(concerne);
            case 2: return opt.equals(typeAccident);
            case 3: return opt.equals(blesses);
            case 4: return opt.equals(gravite);
            default: return false;
        }
    }

    private void addTitle(String text) {
        TextView tv = mkText(text, 20, 0xFF1565C0, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = fullP(); p.setMargins(0, dp(8), 0, dp(16));
        formContainer.addView(tv, p);
    }

    private void addSubtitle(String text) {
        TextView tv = mkText(text, 14, 0xFF5C6370, Typeface.NORMAL);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = fullP(); p.setMargins(0, dp(4), 0, dp(12));
        formContainer.addView(tv, p);
    }

    private void addLabel(String text) {
        TextView tv = mkText(text, 13, 0xFF5C6370, Typeface.NORMAL);
        LinearLayout.LayoutParams p = fullP(); p.setMargins(dp(4), dp(8), 0, dp(2));
        formContainer.addView(tv, p);
    }

    private EditText mkInput(String hint, String value, int inputType) {
        EditText e = new EditText(requireContext());
        e.setHint(hint); e.setText(value); e.setTextSize(15);
        e.setSingleLine(true); e.setInputType(inputType);
        e.setBackground(inputBg());
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        e.setMinHeight(dp(52));
        return e;
    }

    private TextView mkText(String text, int sp, int color, int style) {
        TextView tv = new TextView(requireContext());
        tv.setText(text); tv.setTextSize(sp); tv.setTextColor(color);
        tv.setTypeface(Typeface.DEFAULT, style);
        tv.setPadding(dp(10), dp(8), dp(10), dp(8));
        return tv;
    }

    private void saveInputs() {
        if (nomEdit    != null) nom       = nomEdit.getText().toString().trim();
        if (prenomEdit != null) prenom    = prenomEdit.getText().toString().trim();
        if (telEdit    != null) telephone = telEdit.getText().toString().trim();
        if (jourEdit   != null) jour      = jourEdit.getText().toString().trim();
        if (moisEdit   != null) mois      = moisEdit.getText().toString().trim();
        if (anneeEdit  != null) annee     = anneeEdit.getText().toString().trim();
        if (adresseEdit!= null) adresse   = adresseEdit.getText().toString().trim();
    }

    private void resetForm() {
        nom=""; prenom=""; telephone=""; adresse="";
        jour=""; mois=""; annee="";
        concerne=""; typeAccident=""; blesses=""; gravite="";
        currentPhotoPath=null; currentPhotoUri=null;
        locationAcquired=false; modeEdition=false; incidentEnCours=null;
    }

    // ── Drawables ─────────────────────────────────────────────────────────────

    private GradientDrawable card(int bg) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(bg); d.setCornerRadius(dp(14)); d.setStroke(dp(1), 0xFFE0E4EA);
        return d;
    }

    private GradientDrawable shape(int bg, int stroke, int sw) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(bg); d.setCornerRadius(dp(12)); d.setStroke(dp(sw), stroke);
        return d;
    }

    private GradientDrawable inputBg() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.WHITE); d.setCornerRadius(dp(10)); d.setStroke(dp(1), 0xFFC0C8D8);
        return d;
    }

    // ── Layout params ─────────────────────────────────────────────────────────

    private LinearLayout.LayoutParams fullP() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(5), 0, dp(5)); return p;
    }

    private LinearLayout.LayoutParams wP() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        p.setMargins(dp(4), dp(4), dp(4), dp(4)); return p;
    }

    private String v(String s) { return (s==null||s.trim().isEmpty()) ? "—" : s; }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
