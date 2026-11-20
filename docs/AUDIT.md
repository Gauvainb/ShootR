# Audit de Tir Tracker (ShootR)

Audit du dépôt à la révision `24b3b59` (branche `main`), réalisé le 19/11/2026.
Objectif : en faire une application de référence pour les tireurs sportifs.

## Verdict

Le cœur métier est sain : les calculs de groupement (ES, H, L, H+L, MOA) sont corrects, l'écriture des données est atomique, et la détection des impacts est testée. En revanche, l'application n'est pas encore une référence : une perte de données silencieuse est possible, le barème de score n'est pas validé, une partie du code n'est jamais appelée, et `MainActivity.kt` concentre 2 662 lignes.

## Points forts

- **Calculs** : ES, H, L, H+L, MOA et clics de correction suivent les formules usuelles. Exemple : une séance d'exemple avec un ES de 21,1 mm à 50 m donne 1,45 MOA, ce qui est cohérent.
- **Stockage** : écriture dans un fichier temporaire puis renommage, pour qu'un crash ne corrompe pas l'historique.
- **Données locales** : aucun appel réseau, rien ne quitte l'appareil.
- **Tests** : détection des impacts et repérage du visuel sur images synthétiques, conversion des calibres et dimensions des cibles.
- **CI** : GitHub Actions construit les APK debug et release à chaque push.

## Problèmes, par priorité

### Élevée

1. **Perte de données silencieuse** (`data/LocalJsonStore.kt`, `loadSessions` et `loadWeapons`). Si le fichier JSON est illisible, la fonction renvoie une liste vide ; la sauvegarde suivante remplace le fichier et l'historique d'origine disparaît. **Corrigé** : le fichier illisible est copié à côté (`*.corrupt-<horodatage>.json`) avant d'être remplacé.
2. **Effacement de l'historique en un tap** (`MainActivity.kt`, icône « Vider l'historique »). Aucune confirmation. **Corrigé** : une boîte de dialogue demande confirmation.
3. **Réouverture d'une séance : échelle perdue** (`MainActivity.kt`, `loadSessionForRetroactiveAdjustment`). `resetPhotoAnalysis()` remet `pixelPerMm` à 0 *avant* la lecture de la valeur, donc les impacts sont reconstruits avec l'échelle de repli codée en dur (4 px/mm). Après un recalibrage, `recomputeImpacts` recalcule les coordonnées réelles à partir de ces positions : les mm enregistrés peuvent être faux. **Non corrigé** : il faut un test sur appareil. Piste : stocker l'échelle avec la séance, ou ne pas recalculer les mm depuis le canevas.
4. **Barème de score non validé** (`MainActivity.kt`, `computeScoreForImpact`).
   - La branche C200 passe de 4 (≤ 350 mm) à 1 (≤ 400 mm) : les valeurs 3 et 2 sont absentes. Probablement une erreur, mais le barème officiel doit le confirmer.
   - « Cible Hunter / Précision » et « IPSC » tombent dans le barème C50 sans l'indiquer à l'utilisateur. Pour IPSC, c'est faux.
   - Les rayons des zones carabine 10 m et pistolet 10 m (par exemple mouche de 1 mm de rayon en carabine) doivent être recoupés avec le règlement ISSF / FFTir.
   **Non corrigé** : ce sont des valeurs de référence à valider par un tireur ou la fédération.
5. **Flyers comptés différemment** (`MainActivity.kt`, `totalScore` et `shotsCount`). Le total des points et le nombre de coups incluent les flyers, alors que les statistiques (ES, MOA, MPI) les excluent. Le choix doit être fait : un flyer compte-t-il dans le score ? **Non corrigé** : choix métier.

### Moyenne

6. **Identifiants hérités d'un gabarit** : `applicationId = "com.aistudio.tirtracker.xqvz"` avec `namespace = "com.example"`, `.env.example` mentionnant Gemini et `metadata.json` déclarant une capacité Gemini alors que l'application n'a aucun appel réseau. **Non corrigé** : changer l'`applicationId` empêcherait la mise à jour sur les appareils qui ont déjà l'application.
7. **Code mort d'environ 1 500 lignes** : `data/` (Room : DAO, entités, base, dépôt), `ui/TirTrackerViewModel.kt`, `ui/screens/`, `ui/components/TargetCanvas.kt` et `InteractiveTargetView.kt`, `ui/theme/`. Rien ne les appelle. **Non corrigé** : la suppression est une décision à prendre (voir plus bas).
8. **`MainActivity.kt` monolithique** : 2 662 lignes, une seule fonction `TirTrackerApp` qui contient l'état, la logique de score, la persistance et l'interface. Difficile à tester et à relire. **Non corrigé** : découpage à planifier.
9. **Tests de gabarit** : `ExampleUnitTest` (2 + 2 = 4), `ExampleRobolectricTest`, `ExampleInstrumentedTest`. Ils ne testent rien. **Corrigé** : supprimés.
10. **Clés de signature non ignorées** : le `.gitignore` ne couvrait ni `*.jks` ni `*.keystore`, alors que la clé d'upload est lue à la racine du projet. **Corrigé**.
11. **Balistique et barème peu testés** : `BallisticsCalculator` n'avait aucun test. **Corrigé** : tests de l'ES, de H, L, H+L, du MPI, du MOA et des clics de correction.
12. **Dépendances inutilisées** : Firebase, CameraX, Retrofit, Moshi, OkHttp, Navigation, DataStore sont listés dans `libs.versions.toml` ou commentés dans `build.gradle.kts`. **Non corrigé**.

### Faible

13. **Photo d'une séance perdue au redémarrage** : l'URI `content://` n'est pas persistée. C'est déjà indiqué dans le README. **Non corrigé**.
14. **Données d'exemple incohérentes** : pour la première séance d'exemple, les impacts donnent un point moyen de (1,0 ; 2,4) mm, mais la séance stocke (1,2 ; -0,8). L'ES est aussi approché (20,5 mm calculé pour 21,1 mm stocké). **Non corrigé** : ce sont des valeurs d'exemple, à recalculer si on veut un jeu de données cohérent.
15. **Repli de l'écriture non atomique** (`writeAtomically`) : si le renommage échoue, l'écriture directe perd l'atomicité. Sans effet sur Android, où `renameTo` remplace le fichier.
16. **Détection des impacts** (`domain/ImpactDetector.kt`, 753 lignes) : beaucoup de constantes heuristiques, réglées sur des photos de C50 au pistolet. Les tests utilisent des images synthétiques ; le test sur vraies photos (`RealPhotoHarnessTest`) est manuel.

## Ce qui a été modifié

- `LocalJsonStore` : sauvegarde du fichier illisible avant remplacement (point 1), avec test.
- `MainActivity` : confirmation avant l'effacement de l'historique (point 2).
- `.gitignore` : ignore `*.jks`, `*.keystore` et `local.properties` (point 10).
- Suppression des tests de gabarit (point 9).
- Ajout de `BallisticsCalculatorTest` (point 11).
- Ajout de ce document.

## Décisions à prendre

- Supprimer le code mort (point 7), ou le brancher et abandonner le stockage JSON ? Recommandation : supprimer, car le JSON fonctionne et Room n'apporte rien tant qu'il n'y a pas de requête complexe.
- Valeurs du barème C200, IPSC et rayons carabine / pistolet 10 m (point 4) : à valider avec le règlement en vigueur.
- Un flyer compte-t-il dans le total des points (point 5) ?
- Garder ou changer l'`applicationId` (point 6). Recommandation : le garder.

## Non vérifié

- Comportement sur appareil (réouverture de séance, détection sur photo réelle) : pas d'appareil disponible pour cet audit.
- Exactitude des barèmes officiels : je n'ai pas consulté les règlements ISSF / FFTir.

## Deuxième passe : corrections et repasse design

Chaque point du rapport de design (Design Critic, DesignR) et les points techniques ci-dessus ont été traités, sauf ceux listés plus bas.

### Données et logique

- Historique illisible : copie de sauvegarde avant remplacement (test ajouté).
- Réouverture d'une séance : les impacts gardent leurs coordonnées en mm (`fromHistory`) et sont replacés sur le canevas dès que l'échelle est connue. Plus de 4 px/mm codé en dur après réinitialisation.
- Suppression d'un coup : annulable depuis une barre (« Annuler », 10 s).
- Suppression d'une arme : confirmation.
- Code mort supprimé : Room (DAO, entités, base, dépôt), ViewModel, écrans `ui/screens`, `TargetCanvas`, `InteractiveTargetView`, thème inutilisé. Plugins KSP, Secrets et Google Services retirés, dépendances inutilisées supprimées du catalogue.
- Sauvegarde Android : les deux fichiers JSON sont exclus de la sauvegarde cloud et du transfert d'appareil, comme le promet le README.
- CI : les tests unitaires tournent avant la construction des APK.
- `metadata.json` : capacité Gemini retirée (aucun appel réseau dans l'app).

### Lisibilité et accessibilité

- Plus aucun texte sous 12 sp (et 14 sp pour les boutons).
- Gris de bordure du thème relevé de 2,3:1 à 5,0:1 ; textes d'aide et messages vides passent en gris clair.
- Actions destructives en rouge d'erreur, distinctes du rouge de marque.
- Libellés et descriptions en français (« Réinitialiser le zoom », « Annuler le dernier coup »), accent corrigé (« à posteriori »).
- Icône « Ajuster » agrandie à 16 dp.
- Légende des scores sous la liste des impacts : mouche, 10, autres, flyers, avec le nombre pour chacun. Le score n'est plus codé par la seule couleur.
- Dernier impact : anneau blanc plus épais, sans seconde teinte de rouge (la couleur du score est gardée).

### Micro-animations

- Transition entre onglets (fondu et glissement léger).
- Anneau qui s'élargit autour de chaque impact posé.
- Total des points et MOA qui glissent vers leur nouvelle valeur.
- Boîtes de confirmation pour l'effacement de l'historique et le retrait d'une arme.

### Décisions volontaires

- Orientation portrait conservée : les impacts sont stockés en pixels du canevas ; une rotation déplacerait les impacts posés. Le passage en paysage demande d'abord de stocker les positions en mm pour tous les impacts.
- Nom de l'app : « Tir Tracker » dans l'app, « ShootR » dans le dépôt. Non tranché, à décider.
- Textes en français en dur : l'app est française uniquement ; pas de couche de traduction.

### Restent ouverts (décisions ou validation externe)

- Barème de score : C200 (valeurs 3 et 2 absentes), IPSC et « Cible Hunter » sur barème C50, rayons carabine et pistolet 10 m. À valider avec le règlement ISSF / FFTir.
- Flyers : comptés dans le total des points, exclus des statistiques. Choix métier à trancher.
- `applicationId` `com.aistudio.tirtracker.xqvz` : changer casse les mises à jour existantes.
- Signature release sans keystore : la clé debug est utilisée (installable, non publiable).
- Photos des séances : non conservées après redémarrage (URI `content://`).
- Détection des impacts : heuristiques réglées sur des photos C50 pistolet, non vérifiées sur appareil.

### Vérifications

- `testDebugUnitTest` : 32 tests OK, 1 ignoré (harnais sur photos réelles, demande `TARGET_PHOTOS`).
- `assembleDebug` et `assembleRelease` : OK (release 1,5 Mo).
- Non vérifié : rendu à l'écran, animations, comportement tactile et réouverture de séance sur appareil ou émulateur.

## Décisions appliquées (branche audit/reference-app)

- Nom unique : ShootR (titre de l'app, README, nom du projet Gradle, thème).
- `applicationId` changé : `com.shootr.app`. Les installations existantes ne se mettent plus à jour avec cette version.
- Flyers : ils comptent dans le total des points et sont exclus du groupement et des statistiques.
- Photos : copiées dans `session_photos/` (stockage privé) à l'enregistrement de la séance. Elles restent après un redémarrage et sont supprimées avec l'historique.
- Signature release : clé debug tant qu'il n'y a pas de keystore d'upload (non publiable sur un store).
- Barèmes non sourcés retirés : seule la cible C50 garde des points (`domain/Scoring.kt`, testé). Pour C200, IPSC, Hunter, carabine 10 m et pistolet 10 m, le groupement, le MOA et le point moyen restent calculés, sans points.
- Orthographe : « a posteriori », sans accent.
- Orientation : portrait conservé.
- Dimensions des cibles (`cardSizeMm`, 80 et 170 mm) : gardées en l'état, non sourcées contre les règles ISSF.
