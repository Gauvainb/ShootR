# Tir Tracker (ShootR)

Application Android native pour tireurs sportifs : pointage des impacts sur la photo d'une cible, calcul du groupement et suivi des séances dans le temps.

L'interface est en français. Toutes les données restent sur l'appareil ; l'application ne fait aucun appel réseau.

## Fonctionnalités

Une séance s'enregistre en trois étapes, chacune sur son propre écran :

1. **Séance** : titre, type de cible, arme, calibre, distance, munitions.
2. **Cible** : chargement de la photo, repérage automatique du visuel, puis vérification et réglage du centre et de l'échelle. C'est le seul écran où la calibration peut changer. On peut aussi continuer sans photo.
3. **Impacts** : détection automatique à la première arrivée, correction coup par coup, score et groupement, enregistrement.

On revient à une étape précédente avec « Retour » ou en touchant l'étape dans l'indicateur en haut.

- **Pointage sur photo** : chargez la photo d'une cible, puis touchez ou glissez pour placer chaque impact. Zoom et déplacement à deux doigts.
- **Calibration assistée** : placez le centre, puis ajustez un anneau sur un repère de diamètre connu (visuel noir C50 de 200 mm, carabine 10 m de 30,5 mm, pistolet 10 m de 59,5 mm, C200 de 400 mm, ou diamètre personnalisé) pour obtenir l'échelle en px/mm.
- **Repérage automatique du visuel** : le visuel noir est localisé sur la photo par son contour, même avec des impacts, des pastilles ou un objet devant. Son centre et son diamètre donnent le centre de la cible et l'échelle, pour le type de cible sélectionné.
- **Détection automatique des impacts** : en arrivant à l'étape Impacts avec une photo calibrée, les impacts sont recherchés et placés.
  - Dans le noir, un impact est une tache plus claire que l'encre ; sur le blanc, une tache qui s'écarte de la couleur locale du papier ; sur une cible réactive, le halo jaune.
  - La taille d'un impact isolé est mesurée sur la photo elle-même, ce qui permet de séparer les impacts qui se chevauchent.
  - Les cordons, les chiffres imprimés, les pastilles, les mains et les objets sont écartés. Un impact qui touche un chiffre est conservé : un chiffre n'a que des traits fins, un impact a un cœur plein.
  - Les impacts détectés sont cerclés de bleu jusqu'à ce qu'on les déplace ou les modifie, et peuvent être retirés d'un coup. Le bouton « Détecter » de la carte Impacts relance la recherche sans dupliquer ceux déjà placés.
- **Score automatique** selon le type de cible (C50, pistolet 10 m, carabine 10 m, C200), avec mouches (10X). Avec une photo, les points suivent les cordons réellement visibles : sur une C50, les cordons imprimés sont relevés tout autour du centre, ce qui corrige une photo prise de biais ou un carton courbé. Pour les autres cibles, seul le bord du visuel est relevé ainsi.
- **Correction coup par coup, sur la photo** : toucher un coup zoome la cible dessus ; l'impact s'affiche alors en croix ajourée pour laisser voir le trou. On le fait glisser, on touche l'endroit exact du trou, ou on le recale au millimètre. On peut aussi l'exclure du groupement (coup de doigt) ou le supprimer. Les points ne se saisissent pas : ils sont toujours calculés d'après la zone touchée.
- **Télémétrie du groupement** : écart extrême (ES), hauteur, largeur, H+L, MOA et dérive du point moyen d'impact.
- **Historique** : recherche, filtre par distance, réouverture d'une séance pour la corriger.
- **Évolution de la précision** : graphique du MOA par séance (courbe ou histogramme), filtrable par arme.
- **Bibliothèque d'armes** pour pré-remplir une séance.

Le repérage et la détection reposent sur une analyse d'image locale, sans modèle d'IA ni appel réseau. Leurs résultats sont des propositions à vérifier.

## Prérequis

- JDK 21
- Android SDK : plateforme 36.1 et build-tools 36.0.0 (téléchargés automatiquement par Gradle une fois les licences acceptées)
- Android 7.0 (API 24) ou plus récent sur l'appareil

## Compilation

```bash
# Indiquer l'emplacement du SDK (ou créer local.properties avec sdk.dir=...)
export ANDROID_HOME=/chemin/vers/android-sdk

# Le plugin de secrets lit un fichier .env ; un fichier vide suffit
touch .env

./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

| Variante | Taille | Usage |
|---|---|---|
| `debug` | environ 18 Mo | Développement. Non minifiée, nettement plus lente à l'usage. |
| `release` | environ 1,5 Mo | Minifiée et optimisée par R8. C'est celle à installer pour un usage réel. |

### Signature

La variante `release` est signée avec le keystore d'upload s'il est présent :

| Variable | Rôle |
|---|---|
| `KEYSTORE_PATH` | Chemin du keystore (par défaut `my-upload-key.jks` à la racine) |
| `STORE_PASSWORD` | Mot de passe du keystore |
| `KEY_PASSWORD` | Mot de passe de la clé (alias `upload`) |

Sans keystore d'upload, elle est signée avec la clé debug : l'APK s'installe à la main mais ne peut pas être publié sur un store. Si un fichier `debug.keystore` existe à la racine, il est utilisé à la place de la clé debug par défaut, ce qui garde la même signature d'une machine à l'autre.

## Tests

```bash
./gradlew testDebugUnitTest
```

Les tests couvrent le repérage du visuel et la détection d'impacts sur des images synthétiques, la conversion des coordonnées photo vers le canevas, et le stockage JSON (via Robolectric).

Pour juger la détection sur de vraies photos, un outil annote chaque photo d'un dossier avec le visuel et les impacts trouvés :

```bash
TARGET_PHOTOS=/chemin/vers/photos ./gradlew testDebugUnitTest --tests '*RealPhotoHarnessTest*'
# résultat dans /chemin/vers/photos/annotated (ou dans TARGET_PHOTOS_OUT)
```

Pour revoir l'interface sans téléphone, un outil fait des captures des écrans (pas de tir, avec impacts, paramètres de séance, historique) :

```bash
SCREENSHOTS_OUT=/chemin/vers/captures ./gradlew testDebugUnitTest --tests '*ScreenshotTest*'
```

## Intégration continue

Le workflow `.github/workflows/build-apk.yml` s'exécute sur chaque push ou pull request vers `main`/`master`, et à la demande. Il construit les deux variantes et les publie comme artefacts `app-debug` et `app-release`.

## Organisation du code

```
app/src/main/java/com/example/
├── MainActivity.kt                 Écran principal : pas de tir, historique, dialogues
├── data/
│   └── LocalJsonStore.kt           Lecture/écriture de l'historique et des armes
├── domain/
│   ├── BallisticsCalculator.kt     ES, H, L, H+L, MOA, point moyen, clics de correction
│   ├── BlackVisualFinder.kt        Repérage du visuel noir (centre, rayon)
│   ├── ImpactDetector.kt           Détection des impacts sur la photo
│   ├── TargetCalibration.kt        Repères de calibration par type de cible
│   ├── TargetRecognitionEngine.kt  Décodage de la photo, point d'entrée de l'analyse
│   └── TargetSpecs.kt              Dimensions des cibles, diamètre des calibres
└── ui/components/
    ├── CalibrationAssistantPanel.kt
    └── MoaProgressionChart.kt
```

Le dépôt contient aussi une couche Room (`data/dao`, `data/model`, `TirTrackerDatabase`, `TirTrackerRepository`), un `TirTrackerViewModel`, les écrans de `ui/screens`, `TargetCanvas`, `InteractiveTargetView` et `ui/theme`. Ce code compile mais n'est appelé nulle part pour l'instant : l'application passe entièrement par `MainActivity` et les fichiers JSON.

Technologies : Kotlin, Jetpack Compose (Material 3), Coil pour l'affichage des photos, coroutines.

## Données

Les données sont stockées dans le répertoire privé de l'application :

- `sessions_history.json` : séances enregistrées, avec leurs impacts et statistiques ;
- `weapons_library.json` : bibliothèque d'armes.

Au premier lancement, quelques séances et armes d'exemple sont créées. Désinstaller l'application supprime ces fichiers.

## Limites connues

- La détection automatique est réglée sur des photos de cibles C50 tirées au pistolet. Elle reste une aide : on peut s'attendre à des oublis ou à des faux impacts quand
  - deux calibres sont mélangés sur la même cible : les gros impacts bien ronds sont comptés une fois, mais ceux dont le bord est déchiré peuvent être comptés deux ou trois fois ;
  - les impacts forment une grande zone arrachée (elle est ignorée) ;
  - les trous apparaissent noirs dans le noir (rien derrière la cible) ;
  - un objet brillant ou un reflet se trouve sur le visuel ;
  - la photo est prise de trop loin pour un petit calibre (comptez au moins 2 pixels par millimètre pour du .22).
- La photo d'une séance n'est pas copiée dans l'application : après un redémarrage, une séance rouverte peut ne plus afficher son image. Les impacts et les statistiques sont conservés.
- La séance en cours de saisie n'est pas sauvegardée tant qu'on n'a pas appuyé sur « Enregistrer ».
- L'affichage est verrouillé en portrait.
