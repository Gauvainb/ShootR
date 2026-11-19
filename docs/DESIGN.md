# Design de Tir Tracker — repasse tactique

Objectif : une interface moderne, lisible au stand (soleil, gants, lunettes de tir), avec des micro-animations qui confirment chaque geste sans ralentir le pointage.

## 1. Principes

1. **Le score se lit sans la couleur.** Chaque impact porte son score en étiquette (`10X`, `10`, `9`, `F` pour flyer). La légende sous la photo associe forme et libellé.
2. **Un seul accent par zone.** Rouge = action principale et danger. Vert = mouche. Or = 10. Bleu = groupement et MPI. Le reste est gris ardoise.
3. **Tout ce qui se défait est annulable.** Une suppression ouvre une barre « Coup #n supprimé · Annuler ». Une suppression définitive (historique, arme) demande une confirmation explicite.
4. **Texte minimal 12 sp.** Corps 14 sp, libellés 12–13 sp, valeurs de télémétrie 18–22 sp.
5. **Zones tactiles ≥ 48 dp.** Les icônes restent petites, la zone de touche reste grande.
6. **Le mouvement informe, il ne décore pas.** Chaque animation répond à une action de l'utilisateur et dure moins de 500 ms, sauf la pulsation du dernier impact.

## 2. Tokens

| Token | Valeur | Usage |
|---|---|---|
| `ScoreColors.Inner` | `#10B981` | Mouche (10X) |
| `ScoreColors.Ten` | `#FACC15` | Score 10 |
| `ScoreColors.Other` | `#EF4444` | 9 et moins |
| `ScoreColors.Flyer` | `#6B7280` | Coup de doigt, exclu des stats |
| `ScoreColors.Group` | `#38BDF8` | Cercle de groupement, MPI |
| `primary` (sombre) | `#FF4D4D` | Action principale, danger |
| `surface` | `#161E29` | Cartes |
| `onSurfaceVariant` | `#94A3B8` | Texte secondaire (contraste ≈ 6,5:1) |

Typographie (`ui/theme/Type.kt`) : `bodyMedium` 14 sp, `bodySmall` 13 sp, `labelSmall` 12 sp, `labelMedium` 13 sp.

## 3. Micro-animations

| Où | Effet | Déclencheur | Composant |
|---|---|---|---|
| Canevas | Le nouvel impact apparaît à 40 % puis rebondit à 100 % | Pose d'un impact | `impactPop` (Animatable, ressort) |
| Canevas | Halo blanc qui pulse autour du dernier impact | En continu | `latestPulse` (transition infinie) |
| Télémétrie | MOA, ES, H, L, H+L glissent vers la nouvelle valeur | Déplacement d'un impact | `AnimatedNumber` (450 ms) |
| Boutons | Léger rétrécissement (97 %) pendant l'appui | Appui | `Modifier.pressScale` |
| Onglets | Fondu entre « Pas de tir » et « Historique » | Changement d'onglet | `Crossfade` |
| Bandeau d'ajustement | Apparition et disparition en fondu et glissement | Ouverture d'une séance | `AnimatedVisibility` |
| Liste d'historique | Les cartes se replacent au lieu de sauter | Filtre, recherche | `Modifier.animateItem()` |
| Barre d'annulation | Message « Annuler » 4 s après une suppression | Suppression d'un coup | `SnackbarHost` |

Règle : pas d'animation qui bloque la saisie. Le pointage reste réactif pendant qu'une animation tourne.

## 4. Parcours revus

- **Pointage** : le score est sous chaque impact ; la légende est juste sous la photo ; le dernier impact est repérable même sans couleur.
- **Suppression** : un coup se retire en un geste, avec annulation de 4 s. L'historique se vide après confirmation.
- **Historique** : le bouton « vider » est désactivé quand il n'y a rien à vider ; le titre de la confirmation donne le nombre exact de séances.
- **Réouverture** : l'échelle enregistrée est restaurée, donc les impacts gardent leurs millimètres.

## 5. À valider sur appareil (non fait dans cette passe)

- Rendu réel des étiquettes de score sur une photo chargée (taille, chevauchement avec les numéros quand les impacts sont serrés).
- Fluidité de la pulsation et du rebond sur un appareil d'entrée de gamme.
- TalkBack : ordre de lecture du canevas et libellés des icônes non décoratives.
- Lisibilité au soleil : les teintes sombres sont testées seulement sur écran.
- Mode clair : le thème clair n'a pas été repris dans cette passe ; il reste à régler si l'usage en plein air le demande.

## 6. Hors périmètre

- Le découpage de `MainActivity.kt` (2 600 lignes) en écrans séparés : à faire avant d'ajouter des fonctions.
- Les textes en dur (206 chaînes françaises) : à migrer vers `strings.xml`.
- Les barèmes (C200, IPSC, 10 m) : à valider avec la fédération avant de changer le code.
