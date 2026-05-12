package com.example.td2ex2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MenuFragment extends Fragment {

    private Menuable menuable;
    private int currentActivatedIndex = 0;
    private ImageView[] imageViews;

    public MenuFragment() { }

    public static MenuFragment newInstance(int selectedIndex) {
        MenuFragment fragment = new MenuFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("selectedIndex", selectedIndex);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (requireActivity() instanceof Menuable) {
            menuable = (Menuable) requireActivity();
        } else {
            throw new AssertionError("Classe " + requireActivity().getClass().getName()
                    + " ne met pas en œuvre Menuable.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            currentActivatedIndex = getArguments().getInt("selectedIndex", 0);
        }

        imageViews = new ImageView[] {
                view.findViewById(R.id.menu0),
                view.findViewById(R.id.menu1),
                view.findViewById(R.id.menu2),
                view.findViewById(R.id.menu3),
                view.findViewById(R.id.menu4),
                view.findViewById(R.id.menu5),
                view.findViewById(R.id.menu6)
        };

        for (int i = 0; i < imageViews.length; i++) {
            final int index = i;
            imageViews[i].setOnClickListener(v -> {
                currentActivatedIndex = index;
                updateMenuUI();
                menuable.onMenuChange(index);
            });
        }

        updateMenuUI();
    }

    public void setCurrentActivatedIndex(int index) {
        currentActivatedIndex = index;
        if (imageViews != null) {
            updateMenuUI();
        }
    }

    private void updateMenuUI() {
        for (int i = 0; i < imageViews.length; i++) {
            String name = (i == currentActivatedIndex) ? "menu" + i + "_s" : "menu" + i;
            int resId = getResources().getIdentifier(name, "drawable", requireContext().getPackageName());
            if (resId != 0) {
                imageViews[i].setImageResource(resId);
            }
        }
    }
}