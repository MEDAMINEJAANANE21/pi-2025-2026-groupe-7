# Fonctionnalités

Liste des besoins établie au début du projet de L3, avec leur état actuel.

Légende : ✅ réalisé · 🟡 partiel · ❌ non réalisé. La colonne « Version » indique si le besoin a
été couvert pendant le projet de L3 ou pendant la reprise en M1.

## Construire une journée

| Besoin | État | Version | Remarque |
|---|---|---|---|
| Afficher toutes les livraisons prévues | ✅ | L3 | Carte et liste, livraisons à faire par défaut |
| Choisir les livraisons à planifier | ✅ | L3 | Inclusion ou exclusion une à une, recherche par rue ou ville |
| Ajouter, modifier, supprimer une livraison | ✅ | M1 | Formulaire avec recherche dans la Base Adresse Nationale ; une suppression retire aussi la livraison des tournées |
| Filtres d'affichage | ✅ | L3 | À livrer, livrées, toutes ; plage de dates de commande |
| Choix de l'équipe | ✅ | M1 | Chauffeurs et véhicules, affectation par tournée, contrôle de la capacité |
| Solutions selon des priorités (kilomètres, durée, coût…) | ✅ | L3, M1 | Tableau triable ; coût et marge ajoutés en M1 |
| Statistiques par tournée : kilomètres | ✅ | L3 | |
| Statistiques par tournée : heures travaillées | ✅ | L3 | Temps de route et temps de montage |
| Statistiques par tournée : argent gagné | ✅ | M1 | Coût carburant et chauffeur, recette, marge ; hypothèses réglables |

## Optimiser une journée

| Besoin | État | Version | Remarque |
|---|---|---|---|
| Bouton « Optimiser les trajets » | ✅ | L3 | Nombre de véhicules et durée maximale par tournée |
| Optimisation automatique des tournées | ✅ | L3 | Calcul en arrière-plan, progression en temps réel ; relance possible une fois terminée (M1) |
| Distance totale et temps estimé | ✅ | L3 | Par solution et par tournée |
| Ordre recommandé des livraisons | ✅ | L3 | |
| Fonctionner sans clé d'API | ✅ | M1 | Mode estimation à vol d'oiseau |

## Visualiser une journée

| Besoin | État | Version | Remarque |
|---|---|---|---|
| Carte avec le dépôt et les points de livraison | ✅ | L3 | Arrêts numérotés dans la couleur de leur tournée (M1) |
| Points interactifs (détail, mise en évidence) | ✅ | L3 | Survol synchronisé entre la carte et la liste |
| Vue d'ensemble des journées | ✅ | M1 | Calendrier mensuel : journées optimisées et solution retenue |
| Construire un itinéraire à la main | 🟡 | L3 | Par glisser-déposer dans l'éditeur, pas par clic sur la carte |

## Ajuster et exécuter une journée

| Besoin | État | Version | Remarque |
|---|---|---|---|
| Modifier l'ordre des livraisons | ✅ | L3 | Glisser-déposer (Angular CDK depuis M1) |
| Ajouter ou enlever une livraison d'une tournée | ✅ | L3 | Liste « hors solution » |
| Recalcul de l'itinéraire et des estimations | ✅ | L3 | À l'enregistrement ; tracés redessinés à la demande |
| Choisir la solution retenue pour la journée | ✅ | L3 | |
| Feuille de route pour le chauffeur | ✅ | M1 | Page imprimable et export CSV compatible Excel |
| Marquer une livraison ou une tournée comme livrée | ✅ | M1 | Date de livraison enregistrée ; une livraison faite sort des optimisations suivantes |
