package com.example.td2ex2;

/**
 * Interface de couplage faible entre les fragments caméra et ControlActivity.
 * Permet à l'activité de recevoir le chemin de la photo prise.
 */
public interface Picturable {
    void onPictureTaken(String photopath);
}