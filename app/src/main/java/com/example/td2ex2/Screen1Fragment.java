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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;

/**
 * Screen 1 — Interface secours : détail d'un incident.
 * Affiche titre, description, priorité, GPS, photo et actions.
 */
public class Screen1Fragment extends Fragment {

    public static final int FRAGMENT_ID = 0;
    private static final String KEY_CURRENT_ISSUE = "current_issue";

    private Notifiable notifiable;
    private Issue currentIssue;
    private ScrollView scrollView;
    private LinearLayout container;
    private MapView detailMap;

    public Screen1Fragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Notifiable) notifiable = (Notifiable) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        ScrollView sv = new ScrollView(requireContext());
        sv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sv.setFillViewport(true);
        sv.setBackgroundColor(0xFFF0F4F8);

        LinearLayout ll = new LinearLayout(requireContext());
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(dp(16), dp(16), dp(16), dp(16));

        sv.addView(ll);
        scrollView = sv;
        this.container = ll;
        return sv;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            currentIssue = savedInstanceState.getParcelable(KEY_CURRENT_ISSUE);
        }
        render();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (notifiable != null) notifiable.onFragmentDisplayed(FRAGMENT_ID);
    }

    @Override public void onResume() { super.onResume(); if (detailMap != null) detailMap.onResume(); }
    @Override public void onPause()  { super.onPause();  if (detailMap != null) detailMap.onPause(); }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentIssue != null) outState.putParcelable(KEY_CURRENT_ISSUE, currentIssue);
    }

    public void displayIssue(Issue issue) {
        currentIssue = issue;
        // Only render if fragment is attached and view is created
        if (isAdded() && container != null) {
            render();
        }
        // Otherwise currentIssue is stored and render() will be called in onViewCreated
    }

    private void render() {
        container.removeAllViews();
        detailMap = null;

        if (currentIssue == null) {
            showEmpty();
            return;
        }

        // ── Header couleur priorité ───────────────────────────────────────────
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(16), dp(16), dp(16));
        header.setBackground(roundedBg(priorityColor(currentIssue.getPriority()), dp(16)));
        LinearLayout.LayoutParams hp = fullP(); hp.setMargins(0, 0, 0, dp(12));
        container.addView(header, hp);

        TextView badge = mkText(priorityLabel(currentIssue.getPriority()), 12, Color.WHITE, Typeface.BOLD);
        badge.setBackground(roundedBg(0x33FFFFFF, dp(12)));
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        header.addView(badge);

        TextView title = mkText(currentIssue.getTitle(), 20, Color.WHITE, Typeface.BOLD);
        title.setPadding(0, dp(8), 0, dp(4));
        header.addView(title);

        String ts = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE)
                .format(new java.util.Date(currentIssue.getTimestamp()));
        header.addView(mkText("🕐 " + ts, 12, 0xCCFFFFFF, Typeface.NORMAL));

        // ── Description ───────────────────────────────────────────────────────
        LinearLayout infoCard = card();
        infoCard.addView(mkText("📋 Description", 13, 0xFF1565C0, Typeface.BOLD));
        infoCard.addView(divider());
        infoCard.addView(mkText(currentIssue.getDescription(), 15, 0xFF1A1A2E, Typeface.NORMAL));
        if (currentIssue.getSafetyProtocol() != null && !currentIssue.getSafetyProtocol().isEmpty()) {
            infoCard.addView(mkText("⚠️ " + currentIssue.getSafetyProtocol(), 13, 0xFFE65100, Typeface.ITALIC));
        }
        addCard(infoCard);

        // ── Statut ────────────────────────────────────────────────────────────
        LinearLayout statusCard = card();
        statusCard.addView(mkText("📊 Statut de l'intervention", 13, 0xFF1565C0, Typeface.BOLD));
        statusCard.addView(divider());
        String statusStr = statusLabel(currentIssue.getStatusEnum());
        statusCard.addView(mkText(statusStr, 16, 0xFF1A1A2E, Typeface.BOLD));
        addCard(statusCard);

        // ── Photo ─────────────────────────────────────────────────────────────
        if (currentIssue.getPicture() != null && !currentIssue.getPicture().isEmpty()) {
            LinearLayout photoCard = card();
            photoCard.addView(mkText("📷 Photo de la scène", 13, 0xFF1565C0, Typeface.BOLD));
            photoCard.addView(divider());

            ImageView img = new ImageView(requireContext());
            img.setAdjustViewBounds(true);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams imgP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(200));
            imgP.setMargins(0, dp(8), 0, 0);
            Picasso.get()
                    .load(new File(currentIssue.getPicture()))
                    .placeholder(R.drawable.ic_camera_placeholder)
                    .error(R.drawable.ic_camera_placeholder)
                    .fit().centerCrop()
                    .into(img);
            photoCard.addView(img, imgP);
            addCard(photoCard);
        }

        // ── Carte ─────────────────────────────────────────────────────────────
        LinearLayout mapCard = card();
        mapCard.addView(mkText("📍 Localisation GPS", 13, 0xFF1565C0, Typeface.BOLD));
        mapCard.addView(mkText(String.format("%.5f, %.5f",
                currentIssue.getLatitude(), currentIssue.getLongitude()),
                13, 0xFF5C6370, Typeface.NORMAL));
        mapCard.addView(divider());

        detailMap = new MapView(requireContext());
        detailMap.setTileSource(TileSourceFactory.MAPNIK);
        detailMap.setMultiTouchControls(true);
        GeoPoint pt = new GeoPoint(currentIssue.getLatitude(), currentIssue.getLongitude());
        detailMap.getController().setZoom(16.0);
        detailMap.getController().setCenter(pt);
        Marker mk = new Marker(detailMap);
        mk.setPosition(pt);
        mk.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mk.setTitle(currentIssue.getTitle());
        detailMap.getOverlays().add(mk);
        detailMap.invalidate();

        LinearLayout.LayoutParams mapLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(220));
        mapLp.setMargins(0, dp(8), 0, 0);
        mapCard.addView(detailMap, mapLp);
        addCard(mapCard);

        // ── Actions ───────────────────────────────────────────────────────────
        LinearLayout actCard = card();
        actCard.addView(mkText("🚨 Actions secours", 13, 0xFF1565C0, Typeface.BOLD));
        actCard.addView(divider());

        addActionBtn(actCard, "🚔  Envoyer une patrouille", 0xFF1565C0, () -> {
            currentIssue.setStatus(2f);
            Toast.makeText(requireContext(), "Patrouille envoyée !", Toast.LENGTH_SHORT).show();
            render();
        });
        addActionBtn(actCard, "🚑  Contacter les secours", 0xFF2E7D32, () -> {
            currentIssue.setStatus(3f);
            Toast.makeText(requireContext(), "Secours contactés !", Toast.LENGTH_SHORT).show();
            render();
        });
        addActionBtn(actCard, "🆘  Demander du renfort", 0xFFC62828, () ->
                Toast.makeText(requireContext(), "Renfort demandé !", Toast.LENGTH_SHORT).show());
        addActionBtn(actCard, "📋  Voir tous les incidents", 0xFF546E7A, () -> {
            if (notifiable != null) notifiable.onClick(FRAGMENT_ID);
        });
        addCard(actCard);
    }

    private void showEmpty() {
        LinearLayout card = card();
        TextView emoji = mkText("🚨", 48, 0xFF1565C0, Typeface.NORMAL);
        emoji.setGravity(Gravity.CENTER);
        card.addView(emoji, fullP());

        TextView t = mkText("Interface Secours", 22, 0xFF1565C0, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        card.addView(t, fullP());

        TextView sub = mkText("Sélectionnez un incident dans la liste pour voir les détails et déclencher une intervention.",
                15, 0xFF5C6370, Typeface.NORMAL);
        sub.setGravity(Gravity.CENTER);
        card.addView(sub, fullP());

        LinearLayout.LayoutParams cp = fullP();
        cp.setMargins(0, dp(60), 0, 0);
        container.addView(card, cp);
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private LinearLayout card() {
        LinearLayout ll = new LinearLayout(requireContext());
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(dp(16), dp(14), dp(16), dp(14));
        ll.setBackground(roundedBg(Color.WHITE, dp(16)));
        return ll;
    }

    private void addCard(LinearLayout card) {
        LinearLayout.LayoutParams p = fullP();
        p.setMargins(0, 0, 0, dp(12));
        container.addView(card, p);
    }

    private void addActionBtn(LinearLayout parent, String label, int color, Runnable action) {
        Button btn = new Button(requireContext());
        btn.setText(label);
        btn.setAllCaps(false);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        p.setMargins(0, dp(6), 0, 0);
        btn.setOnClickListener(v -> action.run());
        parent.addView(btn, p);
    }

    private View divider() {
        View v = new View(requireContext());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        p.setMargins(0, dp(8), 0, dp(8));
        v.setLayoutParams(p);
        v.setBackgroundColor(0xFFE0E4EA);
        return v;
    }

    private TextView mkText(String text, int sp, int color, int style) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.DEFAULT, style);
        tv.setPadding(0, dp(4), 0, dp(4));
        return tv;
    }

    private GradientDrawable roundedBg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private LinearLayout.LayoutParams fullP() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int priorityColor(Issue.Priority p) {
        switch (p) {
            case CRITICAL: return 0xFFC62828;
            case HIGH:     return 0xFFE65100;
            case MEDIUM:   return 0xFFF9A825;
            default:       return 0xFF2E7D32;
        }
    }

    private String priorityLabel(Issue.Priority p) {
        switch (p) {
            case CRITICAL: return "🔴 CRITIQUE";
            case HIGH:     return "🟠 ÉLEVÉE";
            case MEDIUM:   return "🟡 MOYENNE";
            default:       return "🟢 FAIBLE";
        }
    }

    private String statusLabel(Issue.Status s) {
        switch (s) {
            case REPORTED:  return "📥 Signalé";
            case CONFIRMED: return "✅ Confirmé";
            case ON_SITE:   return "🚑 Secours sur place";
            case CLEARING:  return "🔧 Dégagement en cours";
            case RESOLVED:  return "✔️ Résolu";
            default:        return "—";
        }
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
