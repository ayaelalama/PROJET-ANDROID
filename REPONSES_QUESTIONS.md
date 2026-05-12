# Réponses aux questions du Professeur - TD2EX2

## Questions et Réponses

### 1. À quoi sert le booléen `isStarting` ?

Le booléen `isStarting` permet de distinguer les deux cas lors de la création du fragment :
- **Première création** : `isStarting = true` → on crée les données initiales
- **Restauration après rotation** : `isStarting = false` → on restaure l'état précédent

Ce pattern est utile pour garantir que lors d'une rotation d'écran, les données ne sont pas perdues mais restaurées depuis le `Bundle` sauvegardé par `onSaveInstanceState()`.

### 2. Quel est le rôle de ScreenxFragment ?

**Rôle des Fragments (Screen1Fragment, Screen2Fragment, etc.) :**

Les fragments jouent le rôle de **contrôleurs de vue** dans le pattern MVC/MVP :
- **Affichage** : Ils gèrent les vues et la présentation des données à l'utilisateur
- **Capture des interactions** : Ils captent les clics, modifications et autres événements utilisateur
- **Communication** : Ils communiquent avec l'Activité via l'interface `Notifiable`
- **Pas d'interaction directe** : Les fragments ne se communiquent JAMAIS directement entre eux

Le pattern respecte le principe de **séparation des préoccupations** :
- L'Activité = **orchestrateur** et **gestionnaire d'état global**
- Les Fragments = **vues/présentateurs** spécialisés
- La communication passe toujours par l'Activité

### 3. Quand sont appelées `onSaveInstanceState` et `onRestoreInstanceState` ?

#### **onSaveInstanceState(Bundle outState)**
- Appelée **avant la destruction** du fragment (ex: rotation d'écran)
- L'Activité utilise ce Bundle pour sauvegarder l'état temporaire
- C'est notre responsabilité d'y ajouter les données à préserver

#### **onRestoreInstanceState(Bundle savedInstanceState)**
- Appelée **après onCreate()**, lors de la restauration
- Dans notre implémentation, on l'utilise dans `onViewCreated()` pour restaurer les données
- On vérifie `if (savedInstanceState != null)` pour restaurer ou créer de nouvelles données

**Utilité** : Préserver l'état de l'application lors de changements de configuration (rotation, etc.)

### 4. À quoi sert la méthode `onDataChange` ?

La méthode `onDataChange(int numFragment, Object object, int actionCode, Object argsAction)` est une **méthode de notification générique** :

- **numFragment** : Identifie le fragment source de l'action
- **object** : L'objet de données (ex: l'Issue sélectionnée)
- **actionCode** : Le type d'action (ex: `ACTION_ITEM_CLICKED`, `ACTION_RATING_CHANGED`)
- **argsAction** : Arguments supplémentaires (ex: l'index de l'item)

**Rôle** : C'est le **canal de communication** principal Fragment → Activité pour toute modification de données.

**Exemple** :
```java
// Screen2Fragment notifie qu'un item a été cliqué
notifiable.onDataChange(FRAGMENT_ID, issue, ACTION_ITEM_CLICKED, itemIndex);

// ControlActivity reçoit et réagit
@Override
public void onDataChange(int numFragment, Object object, int actionCode, Object argsAction) {
    if (actionCode == ACTION_ITEM_CLICKED) {
        loadScreenFragment(Screen1Fragment.FRAGMENT_ID, true);
        screen1Fragment.displayIssue((Issue) object);
    }
}
```

### 5. À quoi sert la méthode `onFragmentDisplayed` ?

La méthode `onFragmentDisplayed(int fragmentId)` est appelée quand **un fragment devient visible** :

- **Quand** : Depuis `onStart()` du fragment
- **Rôle** : Notifier l'Activité que ce fragment est maintenant affiché
- **Utilité** : L'Activité peut mettre à jour l'UI (surligher le bouton de menu correspondant, etc.)

**Exemple** :
```java
@Override
public void onStart() {
    super.onStart();
    if (notifiable != null) notifiable.onFragmentDisplayed(FRAGMENT_ID);
}

// L'Activité met à jour le menu
@Override
public void onFragmentDisplayed(int fragmentId) {
    if (fragmentId != currentIndex) {
        currentIndex = fragmentId;
        if (menuFragment != null) menuFragment.setCurrentActivatedIndex(fragmentId);
    }
}
```

### 6. Quand la méthode `onStart()` dans le ScreenxFragment est-elle appelée ?

`onStart()` est appelée **à chaque fois que le fragment devient visible** :

1. **Première création** : Après `onCreateView()` et `onViewCreated()`
2. **Retour d'un BackStack** : Quand on revient au fragment avec le bouton Retour
3. **Rotation d'écran** : Après la restauration
4. **Onglets/Navigation** : À chaque changement d'onglet

**Caractéristique importante** : `onStart()` est appelée **avant `onResume()`** et peut être appelée **plusieurs fois** pour le même fragment.

C'est le bon endroit pour :
- Notifier l'Activité que le fragment est visible
- Initialiser les ressources visibles
- Actualiser l'UI si nécessaire

---

## Architecture de l'Application

### Pattern Implémenté : **MVC + Observer + Adapter**

```
┌─────────────────────────────────────────────────────┐
│                 MainActivity                          │
│              (Point d'entrée)                        │
└────────────────────┬────────────────────────────────┘
                     │
         ┌───────────┴────────────┐
         ▼                        ▼
    ┌──────────────┐      ┌──────────────────┐
    │ MenuFragment │      │ ControlActivity  │
    │              │      │ (Orchestrateur)  │
    │   - Menu     │      │                  │
    └──────────────┘      │  Implémente:     │
                          │  - Notifiable    │
                          │  - Menuable      │
                          └────────┬─────────┘
                                   │
                ┌──────────────────┼──────────────────┐
                ▼                  ▼                  ▼
          ┌───────────┐      ┌──────────────┐  ┌──────────────┐
          │Screen1    │      │Screen2       │  │ScreenN       │
          │Fragment   │      │Fragment      │  │ Fragment     │
          │           │      │              │  │              │
          │ Détails   │      │ Liste (List) │  │ ...          │
          │           │      │              │  │              │
          │Implémente:│      │ Implémente:  │  │              │
          │Notifiable │      │ - ClickableI │  │              │
          │           │      │ - Notifiable │  │              │
          └───────────┘      │              │  │              │
                             │ Utilise:     │  │              │
                             │ - IssueAdapt │  │              │
                             │   (Adapter)  │  │              │
                             └──────────────┘  │              │
                                               │              │
                                               └──────────────┘
```

### Communication

1. **Fragment → Activité** : Via l'interface `Notifiable`
2. **Adapter → Fragment** : Via l'interface `ClickableIssue`
3. **Activité → Fragment** : Via les méthodes publiques du fragment
4. **Fragments ↔ Fragments** : JAMAIS directement, toujours via l'Activité

### Patterns Utilisés

✅ **Fragment Pattern** : Réutilisabilité et navigation
✅ **Adapter Pattern** : Transformation liste → ListView
✅ **Observer/Notifiable** : Communication découplée
✅ **ViewHolder Pattern** : Optimisation ListView
✅ **Parcelable** : Sérialisation des données

---

## Checklist de Conformité

- ✅ Utilisation de plusieurs fragments dans une même activité
- ✅ Utilisation du pattern Adapter (IssueAdapter)
- ✅ Communication via l'interface Notifiable
- ✅ Gestion du BackStack (`addToBackStack(null)`)
- ✅ Sauvegarde/restauration d'état (`onSaveInstanceState/onRestoreInstanceState`)
- ✅ Classe Issue est Parcelable
- ✅ Bouton "option" lance le menu d'Adapter (Screen2Fragment)
- ✅ ListItemView avec titre, description, icône priorité et rating
- ✅ Navigation correcte entre fragments
- ✅ Pas de communication directe entre fragments


