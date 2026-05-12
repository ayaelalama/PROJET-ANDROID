package com.example.td2ex2;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Screen3Fragment extends Fragment {

    public static final int FRAGMENT_ID = 2;

    private Notifiable notifiable;
    private LinearLayout formContainer;
    private Button backButton, nextButton;

    private int page = 0;

    private String nom = "";
    private String prenom = "";
    private String telephone = "";
    private String jour = "";
    private String mois = "";
    private String annee = "";
    private String adresse = "";

    private String concerne = "....";
    private String typeAccident = "....";
    private String blesses = "....";
    private String gravite = "....";

    private EditText nomEdit, prenomEdit, telEdit, jourEdit, moisEdit, anneeEdit, adresseEdit;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof Notifiable) {
            notifiable = (Notifiable) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_screen3, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (notifiable != null) {
            notifiable.onFragmentDisplayed(FRAGMENT_ID);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        formContainer = view.findViewById(R.id.formContainer);
        backButton = view.findViewById(R.id.backButton);
        nextButton = view.findViewById(R.id.nextButton);

        backButton.setOnClickListener(v -> {
            saveInputs();

            if (page > 0) {
                page--;
                showPage();
            }
        });

        nextButton.setOnClickListener(v -> {
            saveInputs();

            if (page < 7) {
                page++;
                showPage();
            } else {
                sendIncident();
            }
        });

        showPage();
    }

    private void showPage() {
        formContainer.removeAllViews();

        backButton.setEnabled(page > 0);

        if (page == 6) {
            nextButton.setText("☑ Confirmer");
        } else if (page == 7) {
            nextButton.setText("▶ Terminer");
        } else {
            nextButton.setText("▶ Suivant");
        }

        if (page == 0) {
            showPersonalInfo();
        } else if (page == 1) {
            showQuestion(
                    "Etape 1/4",
                    "Vous êtes concerné(e)\npar l’accident ?",
                    new String[]{"Oui, je suis impliqué(e)", "Non, je suis témoin"},
                    1
            );
        } else if (page == 2) {
            showQuestion(
                    "Etape 2/4",
                    "Quel type d’accident voyez-vous?",
                    new String[]{"Collision entre véhicules", "Véhicule seul", "Moto / Scooter", "Piéton / cycliste", "Plusieurs véhicules", "Je ne sais pas"},
                    2
            );
        } else if (page == 3) {
            showQuestion(
                    "Etape 3/4",
                    "Y a-t-il des blessés ?",
                    new String[]{"Oui", "Non", "Je ne sais pas"},
                    3
            );
        } else if (page == 4) {
            showQuestion(
                    "Etape 4/4",
                    "Quelle est la gravité de la situation ?",
                    new String[]{"Dégâts légers", "Situation préoccupante", "Danger immédiat / blessé"},
                    4
            );
        } else if (page == 5) {
            showPersonalRecap();
        } else if (page == 6) {
            showAnswersRecap();
        } else if (page == 7) {
            showLocation();
        }
    }

    private void showPersonalInfo() {
        addTitle("Vos informations");
        addSubtitle("Ces informations peuvent aider les secours\nà vous identifier.");

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);

        nomEdit = createInput("Nom", nom);
        prenomEdit = createInput("Prénom", prenom);

        row.addView(nomEdit, weightParams());
        row.addView(prenomEdit, weightParams());

        formContainer.addView(row);

        telEdit = createInput("Téléphone", telephone);
        formContainer.addView(telEdit, fullParams());

        addLabel("Date de naissance");

        LinearLayout dateRow = new LinearLayout(requireContext());
        dateRow.setOrientation(LinearLayout.HORIZONTAL);

        jourEdit = createInput("JJ", jour);
        moisEdit = createInput("MM", mois);
        anneeEdit = createInput("AAAA", annee);

        dateRow.addView(jourEdit, weightParams());
        dateRow.addView(moisEdit, weightParams());
        dateRow.addView(anneeEdit, weightParams());

        formContainer.addView(dateRow);

        adresseEdit = createInput("Adresse / résidence", adresse);
        formContainer.addView(adresseEdit, fullParams());
    }

    private void showPersonalRecap() {
        addTitle("Récapitulatif des informations");
        addSubtitleBlueLeft("Vos informations personnelles:");

        addModifyButton(0);

        recapLine("Nom: " + value(nom));
        recapLine("Prénom: " + value(prenom));
        recapLine("Téléphone: " + value(telephone));

        String naissance = value(jour) + "/" + value(mois) + "/" + value(annee);
        recapLine("Date de naissance: " + naissance);

        recapLine("Adresse / résidence: " + value(adresse));
    }

    private void showQuestion(String step, String question, String[] options, int activeStep) {
        addTitle("Questionnaire");
        addSubtitle("Cela nous aide à mieux vous guider");

        TextView stepView = createText(step, 14, Color.BLACK, Typeface.BOLD);
        stepView.setGravity(Gravity.RIGHT);
        formContainer.addView(stepView, fullParams());

        addProgressIcons(activeStep);

        TextView questionView = createText(question, 18, Color.BLACK, Typeface.NORMAL);
        questionView.setGravity(Gravity.CENTER);
        questionView.setBackgroundColor(Color.rgb(177, 203, 235));
        formContainer.addView(questionView, bigQuestionParams());

        for (String option : options) {
            addOption(option, activeStep);
        }
    }

    private void showAnswersRecap() {
        addTitle("Récapitulatif des réponses");

        Space space = new Space(requireContext());
        formContainer.addView(space, new LinearLayout.LayoutParams(1, dp(50)));

        addModifyButton(2);

        recapLine("Êtes-vous concerné par l’accident ? : " + value(concerne));
        recapLine("Type d’accident: " + value(typeAccident));
        recapLine("Y a-t-il des blessés? : " + value(blesses));
        recapLine("Gravité de la situation: " + value(gravite));
    }

    private void showLocation() {
        addTitle("Localisation");
        addSubtitle("Vérifiez l’emplacement affiché avant l’envoi");
        addSubtitle("Lieu de l’accident :");

        ImageView map = new ImageView(requireContext());
        map.setImageResource(R.drawable.fond);
        map.setScaleType(ImageView.ScaleType.CENTER_CROP);
        map.setBackgroundColor(Color.WHITE);
        map.setPadding(dp(2), dp(2), dp(2), dp(2));

        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(290)
        );
        mapParams.setMargins(0, dp(8), 0, dp(8));
        formContainer.addView(map, mapParams);

        TextView position = createText("Position récupérée automatiquement", 16,
                Color.rgb(44, 99, 230), Typeface.NORMAL);
        position.setGravity(Gravity.CENTER);
        formContainer.addView(position, fullParams());

        Button retry = new Button(requireContext());
        retry.setText("↻  Réessayer");
        retry.setAllCaps(false);
        retry.setTextColor(Color.rgb(78, 112, 230));
        retry.setBackground(round(Color.rgb(250, 245, 255), Color.LTGRAY));

        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(dp(160), dp(50));
        retryParams.gravity = Gravity.CENTER_HORIZONTAL;
        retryParams.setMargins(0, dp(8), 0, dp(8));
        formContainer.addView(retry, retryParams);
    }

    private void addOption(String option, int questionNumber) {
        TextView optionView = createText(option, 16, Color.BLACK, Typeface.NORMAL);
        optionView.setGravity(Gravity.CENTER);

        if (isSelected(option, questionNumber)) {
            optionView.setBackgroundColor(Color.rgb(177, 203, 235));
        } else {
            optionView.setBackgroundColor(Color.rgb(214, 214, 214));
        }

        optionView.setOnClickListener(v -> {
            if (questionNumber == 1) {
                concerne = option;
            } else if (questionNumber == 2) {
                typeAccident = option;
            } else if (questionNumber == 3) {
                blesses = option;
            } else if (questionNumber == 4) {
                gravite = option;
            }

            showPage();
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        params.setMargins(0, dp(7), 0, dp(7));

        formContainer.addView(optionView, params);
    }

    private boolean isSelected(String option, int questionNumber) {
        if (questionNumber == 1) return option.equals(concerne);
        if (questionNumber == 2) return option.equals(typeAccident);
        if (questionNumber == 3) return option.equals(blesses);
        if (questionNumber == 4) return option.equals(gravite);

        return false;
    }

    private void addProgressIcons(int activeStep) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        String[] icons = {"👤", "⚠", "🧍", "!"};

        for (int i = 0; i < icons.length; i++) {
            TextView icon = createText(icons[i], 22, Color.BLACK, Typeface.BOLD);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(circle(i + 1 == activeStep));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(56), dp(56));
            params.setMargins(dp(5), 0, dp(5), 0);

            row.addView(icon, params);
        }

        formContainer.addView(row, fullParams());
    }

    private void sendIncident() {
        Issue.Priority priority;

        if ("Danger immédiat / blessé".equals(gravite)) {
            priority = Issue.Priority.CRITICAL;
        } else if ("Situation préoccupante".equals(gravite)) {
            priority = Issue.Priority.HIGH;
        } else {
            priority = Issue.Priority.MEDIUM;
        }

        AccidentFactory factory;

        if ("Plusieurs véhicules".equals(typeAccident)
                || "Collision entre véhicules".equals(typeAccident)) {
            factory = new HighwayFactory();
        } else {
            factory = new UrbanFactory();
        }

        String title = "Accident signalé par " + value(prenom) + " " + value(nom);

        String description =
                "Téléphone : " + value(telephone) + "\n" +
                        "Date de naissance : " + value(jour) + "/" + value(mois) + "/" + value(annee) + "\n" +
                        "Adresse : " + value(adresse) + "\n" +
                        "Personne concernée : " + value(concerne) + "\n" +
                        "Type d'accident : " + value(typeAccident) + "\n" +
                        "Blessés : " + value(blesses) + "\n" +
                        "Gravité : " + value(gravite) + "\n" +
                        "Localisation GPS : zone carte prévue dans le prototype.";

        Issue issue = factory.createIssue(title, description);
        issue.setPriority(priority);
        issue.setStatus(2f);

        Toast.makeText(requireContext(), "Alerte transmise aux secours", Toast.LENGTH_LONG).show();

        if (notifiable != null) {
            notifiable.onDataChange(
                    FRAGMENT_ID,
                    issue,
                    Notifiable.ACTION_SHOW_ISSUE_DETAILS,
                    null
            );
        }

        page = 0;
        showPage();
    }

    private void saveInputs() {
        if (nomEdit != null) nom = nomEdit.getText().toString();
        if (prenomEdit != null) prenom = prenomEdit.getText().toString();
        if (telEdit != null) telephone = telEdit.getText().toString();
        if (jourEdit != null) jour = jourEdit.getText().toString();
        if (moisEdit != null) mois = moisEdit.getText().toString();
        if (anneeEdit != null) annee = anneeEdit.getText().toString();
        if (adresseEdit != null) adresse = adresseEdit.getText().toString();
    }

    private void addTitle(String text) {
        TextView title = createText(text, 20, Color.rgb(87, 120, 230), Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = fullParams();
        params.setMargins(0, dp(10), 0, dp(18));

        formContainer.addView(title, params);
    }

    private void addSubtitle(String text) {
        TextView subtitle = createText(text, 16, Color.rgb(32, 49, 140), Typeface.BOLD);
        subtitle.setGravity(Gravity.CENTER);
        formContainer.addView(subtitle, fullParams());
    }

    private void addSubtitleBlueLeft(String text) {
        TextView subtitle = createText(text, 18, Color.rgb(87, 120, 230), Typeface.BOLD);
        subtitle.setGravity(Gravity.LEFT);
        formContainer.addView(subtitle, fullParams());
    }

    private void addLabel(String text) {
        TextView label = createText(text, 16, Color.rgb(50, 50, 50), Typeface.NORMAL);
        label.setGravity(Gravity.LEFT);
        formContainer.addView(label, fullParams());
    }

    private EditText createInput(String hint, String value) {
        EditText editText = new EditText(requireContext());
        editText.setHint(hint);
        editText.setText(value);
        editText.setTextSize(16);
        editText.setSingleLine(true);
        editText.setBackground(round(Color.WHITE, Color.LTGRAY));
        editText.setPadding(dp(12), 0, dp(12), 0);
        editText.setMinHeight(dp(54));

        return editText;
    }

    private TextView createText(String text, int size, int color, int style) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(size);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        textView.setPadding(dp(10), dp(8), dp(10), dp(8));

        return textView;
    }

    private void recapLine(String text) {
        TextView line = createText(text, 16, Color.rgb(45, 45, 45), Typeface.NORMAL);
        line.setBackground(round(Color.WHITE, Color.LTGRAY));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
        );
        params.setMargins(0, dp(9), 0, dp(9));

        formContainer.addView(line, params);
    }

    private void addModifyButton(int targetPage) {
        Button button = new Button(requireContext());
        button.setText("✎ Modifier");
        button.setAllCaps(false);
        button.setTextColor(Color.rgb(78, 112, 230));
        button.setBackground(round(Color.rgb(250, 245, 255), Color.LTGRAY));

        button.setOnClickListener(v -> {
            page = targetPage;
            showPage();
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(150), dp(50));
        params.gravity = Gravity.RIGHT;
        params.setMargins(0, dp(8), 0, dp(8));

        formContainer.addView(button, params);
    }

    private LinearLayout.LayoutParams fullParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(0, dp(6), 0, dp(6));

        return params;
    }

    private LinearLayout.LayoutParams bigQuestionParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        );

        params.setMargins(dp(10), dp(18), dp(10), dp(18));

        return params;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );

        params.setMargins(dp(5), dp(5), dp(5), dp(5));

        return params;
    }

    private GradientDrawable round(int color, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(dp(1), strokeColor);

        return drawable;
    }

    private GradientDrawable circle(boolean active) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.WHITE);

        if (active) {
            drawable.setStroke(dp(2), Color.rgb(66, 133, 244));
        } else {
            drawable.setStroke(dp(2), Color.BLACK);
        }

        return drawable;
    }

    private String value(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "....";
        }

        return value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}