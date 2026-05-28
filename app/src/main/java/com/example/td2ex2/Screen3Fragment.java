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
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

/**
 * Screen 3 — Flux guidé de signalement d'accident.
 *
 * Notions couvertes :
 * 01 – Vues, layouts dynamiques (LinearLayout, EditText, TextView, Button)
 * 02 – Parcelable via Issue transmis à ControlActivity via Notifiable
 * 03 – Fragment (ce fichier)
 * 08 – Factory Pattern (AccidentFactory → HighwayFactory / UrbanFactory)
 * 11 – Permissions GPS (ACCESS_FINE_LOCATION) + FusedLocationProviderClient
 * 12 – Notification locale envoyée à la confirmation du signalement
 */
public class Screen3Fragment extends Fragment {

    public static final int FRAGMENT_ID = 2;
    private static final String CHANNEL_ID = "signalement_channel";

    private Notifiable notifiable;
    private LinearLayout formContainer;
    private Button backButton, nextButton;
    private TextView stepLabel;
    private LinearLayout progressDots;

    private int page = 0;
    private static final int TOTAL_PAGES = 7; // 0..6, page 6 = succès

    // Données personnelles
    private String nom = "", prenom = "", telephone = "", adresse = "";
    private String jour = "", mois = "", annee = "";

    // Réponses questionnaire
    private String concerne = "", typeAccident = "", blesses = "", gravite = "";

    // GPS
    private double latitude = 43.7009, longitude = 7.2684;
    private boolean locationAcquired = false;
    private FusedLocationProviderClient fusedLocationClient;

    // EditText refs
    private EditText nomEdit, prenomEdit, telEdit, jourEdit, moisEdit, anneeEdit, adresseEdit;

    // Permission launcher (GPS)
    private final ActivityResultLauncher<String> gpsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    fetchLocation(null);
                } else {
                    Toast.makeText(requireContext(),
                            "Permission GPS refusée. Position par défaut utilisée.", Toast.LENGTH_SHORT).show();
                }
            });

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
        formContainer = view.findViewById(R.id.formContainer);
        backButton    = view.findViewById(R.id.backButton);
        nextButton    = view.findViewById(R.id.nextButton);
        stepLabel     = view.findViewById(R.id.stepLabel);
        progressDots  = view.findViewById(R.id.progressDots);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        backButton.setOnClickListener(v -> {
            saveInputs();
            if (page > 0 && page < 6) { page--; showPage(); }
        });

        nextButton.setOnClickListener(v -> {
            saveInputs();
            if (page == 5) {
                sendIncident();
            } else if (page < 5) {
                page++;
                showPage();
            }
        });

        createNotificationChannel();
        showPage();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void showPage() {
        formContainer.removeAllViews();

        backButton.setVisibility(page > 0 && page < 6 ? View.VISIBLE : View.INVISIBLE);
        nextButton.setVisibility(page < 6 ? View.VISIBLE : View.GONE);

        updateHeader();

        switch (page) {
            case 0: showPersonalInfo(); break;
            case 1: showQuestion("Étape 1/4", "Vous êtes concerné(e) par l'accident ?",
                        new String[]{"Oui, je suis impliqué(e)", "Non, je suis témoin"}, 1);
                    break;
            case 2: showQuestion("Étape 2/4", "Quel type d'accident voyez-vous ?",
                        new String[]{"Collision entre véhicules", "Véhicule seul",
                                     "Moto / Scooter", "Piéton / cycliste",
                                     "Plusieurs véhicules", "Je ne sais pas"}, 2);
                    break;
            case 3: showQuestion("Étape 3/4", "Y a-t-il des blessés ?",
                        new String[]{"Oui", "Non", "Je ne sais pas"}, 3);
                    break;
            case 4: showQuestion("Étape 4/4", "Quelle est la gravité de la situation ?",
                        new String[]{"Dégâts légers", "Situation préoccupante",
                                     "Danger immédiat / blessé grave"}, 4);
                    break;
            case 5: showLocationAndRecap(); break;
            case 6: showSuccess(); break;
        }
    }

    private void updateHeader() {
        if (stepLabel == null || progressDots == null) return;

        String[] labels = {"Vos informations", "Êtes-vous impliqué ?",
                           "Type d'accident", "Blessés ?", "Gravité",
                           "Localisation & Recap", "✅ Envoyé"};
        stepLabel.setText(page < labels.length ? labels[page] : "");

        progressDots.removeAllViews();
        if (page == 6) return;

        for (int i = 0; i <= 5; i++) {
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

    // ── Pages ─────────────────────────────────────────────────────────────────

    private void showPersonalInfo() {
        nextButton.setText("Suivant →");
        addSectionTitle("Vos informations");
        addSectionSubtitle("Ces informations aident les secours à vous identifier.");

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        nomEdit   = createInput("Nom", nom);
        prenomEdit = createInput("Prénom", prenom);
        row.addView(nomEdit, weightParams());
        row.addView(prenomEdit, weightParams());
        formContainer.addView(row, fullParams());

        telEdit = createInput("Téléphone", telephone);
        formContainer.addView(wrap(telEdit), fullParams());

        addLabel("Date de naissance");
        LinearLayout dateRow = new LinearLayout(requireContext());
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        jourEdit  = createInput("JJ", jour);
        moisEdit  = createInput("MM", mois);
        anneeEdit = createInput("AAAA", annee);
        dateRow.addView(jourEdit, weightParams());
        dateRow.addView(moisEdit, weightParams());
        dateRow.addView(anneeEdit, weightParams());
        formContainer.addView(dateRow, fullParams());

        adresseEdit = createInput("Adresse / résidence", adresse);
        formContainer.addView(wrap(adresseEdit), fullParams());
    }

    private void showQuestion(String step, String question, String[] options, int qNum) {
        nextButton.setText("Suivant →");

        addSectionTitle("Questionnaire");

        // Numéro d'étape
        TextView stepTv = createText(step, 13, 0xFF5C6370, Typeface.NORMAL);
        stepTv.setGravity(Gravity.END);
        formContainer.addView(stepTv, fullParams());

        // Question
        TextView qTv = createText(question, 18, 0xFF1A1A2E, Typeface.BOLD);
        qTv.setGravity(Gravity.CENTER);
        qTv.setPadding(dp(16), dp(20), dp(16), dp(20));
        qTv.setBackground(cardDrawable(0xFFE3F2FD));
        formContainer.addView(qTv, cardParams());

        // Options
        for (String opt : options) {
            addOptionButton(opt, qNum);
        }
    }

    private void showLocationAndRecap() {
        nextButton.setText("✅ Confirmer et envoyer");

        addSectionTitle("Récapitulatif & Localisation");
        addSectionSubtitle("Vérifiez vos informations avant envoi.");

        // Recap bloc
        recapCard("Vous", nom + " " + prenom + "  |  " + telephone +
                "\n" + jour + "/" + mois + "/" + annee +
                (adresse.isEmpty() ? "" : "\n" + adresse));

        recapCard("Accident", "Impliqué : " + value(concerne) +
                "\nType : " + value(typeAccident) +
                "\nBlessés : " + value(blesses) +
                "\nGravité : " + value(gravite));

        // GPS section
        addSectionSubtitle("📍 Localisation GPS");

        if (!locationAcquired) {
            requestLocation();
        }

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

        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(220));
        mapParams.setMargins(0, dp(8), 0, dp(8));
        formContainer.addView(map, mapParams);

        TextView coordTv = createText(
                locationAcquired ? "✅ Position GPS : " + String.format("%.5f, %.5f", latitude, longitude)
                                 : "⏳ Récupération GPS en cours…",
                14, 0xFF1565C0, Typeface.NORMAL);
        coordTv.setGravity(Gravity.CENTER);
        formContainer.addView(coordTv, fullParams());

        // Retry GPS button
        Button retryBtn = new Button(requireContext());
        retryBtn.setText("↻  Actualiser la position");
        retryBtn.setAllCaps(false);
        retryBtn.setTextColor(0xFF1565C0);
        retryBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
        LinearLayout.LayoutParams retryP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(46));
        retryP.gravity = Gravity.CENTER_HORIZONTAL;
        retryP.setMargins(0, dp(4), 0, dp(4));
        retryBtn.setOnClickListener(v -> {
            locationAcquired = false;
            showPage();
        });
        formContainer.addView(retryBtn, retryP);
    }

    private void showSuccess() {
        backButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(40), dp(24), dp(40));
        card.setBackground(cardDrawable(0xFFFFFFFF));

        TextView emoji = createText("✅", 64, 0xFF2E7D32, Typeface.NORMAL);
        emoji.setGravity(Gravity.CENTER);
        card.addView(emoji, fullParams());

        TextView title = createText("Alerte transmise !", 24, 0xFF2E7D32, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = fullParams();
        tp.setMargins(0, dp(12), 0, dp(8));
        card.addView(title, tp);

        TextView sub = createText("Votre signalement a bien été envoyé aux services compétents.\n\n" +
                "Les secours ont été alertés et interviendront dans les plus brefs délais.",
                15, 0xFF5C6370, Typeface.NORMAL);
        sub.setGravity(Gravity.CENTER);
        card.addView(sub, fullParams());

        LinearLayout.LayoutParams cp = fullParams();
        cp.setMargins(0, dp(40), 0, dp(24));
        formContainer.addView(card, cp);

        Button newReport = new Button(requireContext());
        newReport.setText("Nouveau signalement");
        newReport.setAllCaps(false);
        newReport.setTextColor(Color.WHITE);
        newReport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1565C0));
        newReport.setOnClickListener(v -> {
            resetForm();
            page = 0;
            showPage();
        });
        formContainer.addView(newReport, fullParams());
    }

    // ── Sending ───────────────────────────────────────────────────────────────

    private void sendIncident() {
        Issue.Priority priority;
        if ("Danger immédiat / blessé grave".equals(gravite)) {
            priority = Issue.Priority.CRITICAL;
        } else if ("Situation préoccupante".equals(gravite)) {
            priority = Issue.Priority.HIGH;
        } else {
            priority = Issue.Priority.MEDIUM;
        }

        AccidentFactory factory;
        if ("Plusieurs véhicules".equals(typeAccident) || "Collision entre véhicules".equals(typeAccident)) {
            factory = new HighwayFactory();
        } else {
            factory = new UrbanFactory();
        }

        String title = "Accident signalé — " + value(prenom) + " " + value(nom);
        String description =
                "Tél : " + value(telephone) + "\n" +
                "Né(e) le : " + value(jour) + "/" + value(mois) + "/" + value(annee) + "\n" +
                "Adresse : " + value(adresse) + "\n" +
                "Impliqué : " + value(concerne) + "\n" +
                "Type : " + value(typeAccident) + "\n" +
                "Blessés : " + value(blesses) + "\n" +
                "Gravité : " + value(gravite) + "\n" +
                "GPS : " + String.format("%.5f, %.5f", latitude, longitude);

        Issue issue = factory.createIssue(title, description);
        issue.setPriority(priority);
        issue.setStatus(2f);
        issue.setLocation(latitude, longitude);

        // Notification locale (notion 12)
        sendLocalNotification(title, "Gravité : " + value(gravite) + " | GPS enregistré");

        if (notifiable != null) {
            notifiable.onDataChange(FRAGMENT_ID, issue, Notifiable.ACTION_SHOW_ISSUE_DETAILS, null);
        }

        // Afficher page succès
        page = 6;
        showPage();
    }

    // ── GPS ───────────────────────────────────────────────────────────────────

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocation(null);
        } else {
            gpsPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation(Runnable onDone) {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                locationAcquired = true;
                if (page == 5) showPage(); // refresh map
            }
            if (onDone != null) onDone.run();
        });
    }

    // ── Notifications (notion 12) ─────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Signalement accident", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Confirmation d'envoi des signalements d'accident");
            NotificationManager nm = requireContext().getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void sendLocalNotification(String title, String body) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(requireContext(), 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🚨 " + title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        NotificationManagerCompat.from(requireContext())
                .notify((int) System.currentTimeMillis(), builder.build());
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private void addOptionButton(String option, int qNum) {
        boolean selected = isSelected(option, qNum);
        TextView tv = createText(option, 15, selected ? 0xFF1565C0 : 0xFF1A1A2E, Typeface.NORMAL);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(selected ? drawable(0xFFE3F2FD, 0xFF1565C0, 2) : drawable(0xFFF0F4F8, 0xFFD0D8E4, 1));

        tv.setOnClickListener(v -> {
            if (qNum == 1) concerne     = option;
            else if (qNum == 2) typeAccident = option;
            else if (qNum == 3) blesses      = option;
            else if (qNum == 4) gravite      = option;
            showPage();
        });

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        p.setMargins(0, dp(8), 0, dp(8));
        formContainer.addView(tv, p);
    }

    private boolean isSelected(String opt, int qNum) {
        if (qNum == 1) return opt.equals(concerne);
        if (qNum == 2) return opt.equals(typeAccident);
        if (qNum == 3) return opt.equals(blesses);
        if (qNum == 4) return opt.equals(gravite);
        return false;
    }

    private void recapCard(String title, String content) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(cardDrawable(0xFFFFFFFF));

        TextView t = createText(title, 13, 0xFF1565C0, Typeface.BOLD);
        card.addView(t, fullParams());

        TextView c = createText(content, 14, 0xFF1A1A2E, Typeface.NORMAL);
        LinearLayout.LayoutParams cp = fullParams();
        cp.setMargins(0, dp(4), 0, 0);
        card.addView(c, cp);

        LinearLayout.LayoutParams lp = fullParams();
        lp.setMargins(0, dp(8), 0, dp(8));
        formContainer.addView(card, lp);
    }

    private void addSectionTitle(String text) {
        TextView tv = createText(text, 20, 0xFF1565C0, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = fullParams();
        p.setMargins(0, dp(8), 0, dp(16));
        formContainer.addView(tv, p);
    }

    private void addSectionSubtitle(String text) {
        TextView tv = createText(text, 14, 0xFF5C6370, Typeface.NORMAL);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = fullParams();
        p.setMargins(0, dp(4), 0, dp(12));
        formContainer.addView(tv, p);
    }

    private void addLabel(String text) {
        TextView tv = createText(text, 13, 0xFF5C6370, Typeface.NORMAL);
        tv.setGravity(Gravity.START);
        LinearLayout.LayoutParams p = fullParams();
        p.setMargins(dp(4), dp(8), 0, dp(2));
        formContainer.addView(tv, p);
    }

    private LinearLayout wrap(View child) {
        LinearLayout ll = new LinearLayout(requireContext());
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(child, fullParams());
        return ll;
    }

    private EditText createInput(String hint, String value) {
        EditText e = new EditText(requireContext());
        e.setHint(hint);
        e.setText(value);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setBackground(inputDrawable());
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        e.setMinHeight(dp(52));
        return e;
    }

    private TextView createText(String text, int sizeSp, int color, int style) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(sizeSp);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.DEFAULT, style);
        tv.setPadding(dp(10), dp(8), dp(10), dp(8));
        return tv;
    }

    private void saveInputs() {
        if (nomEdit   != null) nom       = nomEdit.getText().toString();
        if (prenomEdit != null) prenom   = prenomEdit.getText().toString();
        if (telEdit   != null) telephone  = telEdit.getText().toString();
        if (jourEdit  != null) jour       = jourEdit.getText().toString();
        if (moisEdit  != null) mois       = moisEdit.getText().toString();
        if (anneeEdit != null) annee      = anneeEdit.getText().toString();
        if (adresseEdit != null) adresse  = adresseEdit.getText().toString();
    }

    private void resetForm() {
        nom = ""; prenom = ""; telephone = ""; adresse = "";
        jour = ""; mois = ""; annee = "";
        concerne = ""; typeAccident = ""; blesses = ""; gravite = "";
        locationAcquired = false;
    }

    // ── Drawables helpers ─────────────────────────────────────────────────────

    private GradientDrawable cardDrawable(int bgColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(bgColor);
        d.setCornerRadius(dp(14));
        d.setStroke(dp(1), 0xFFE0E4EA);
        return d;
    }

    private GradientDrawable drawable(int bg, int stroke, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(bg);
        d.setCornerRadius(dp(12));
        d.setStroke(dp(strokeWidth), stroke);
        return d;
    }

    private GradientDrawable inputDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.WHITE);
        d.setCornerRadius(dp(10));
        d.setStroke(dp(1), 0xFFC0C8D8);
        return d;
    }

    // ── Layout params ─────────────────────────────────────────────────────────

    private LinearLayout.LayoutParams fullParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(5), 0, dp(5));
        return p;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(72));
        p.setMargins(0, dp(10), 0, dp(14));
        return p;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        p.setMargins(dp(4), dp(4), dp(4), dp(4));
        return p;
    }

    private String value(String v) {
        return (v == null || v.trim().isEmpty()) ? "—" : v;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
