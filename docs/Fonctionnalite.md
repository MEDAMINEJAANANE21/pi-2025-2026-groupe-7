# Liste de fonctionnalités

Liste établie au début du projet, mise à jour avec l'état actuel.

Légende : ✅ réalisé · 🟡 partiel · ❌ non réalisé

## Construire une journée

| Fonctionnalité | État | Remarque |
|---|---|---|
| Afficher toutes les livraisons prévues | ✅ | Carte et liste, livraisons non livrées par défaut |
| Sélectionner les livraisons à planifier | ✅ | Masquage individuel ; seules les livraisons visibles sont optimisées |
| Ajouter ou supprimer des livraisons | 🟡 | Uniquement par import de jeu de données ou génération aléatoire |
| Filtres d'affichage | ✅ | Livrée / non livrée, plage de dates de commande |
| Choix d'équipe pour le planificateur | ❌ | |
| Solutions selon des priorités (colis lourds, nombre de colis, kilomètres) | 🟡 | Plusieurs solutions générées et triables par distance, carburant, durée ; pas de stratégie dédiée à chaque priorité |
| Statistiques par tournée : kilomètres | ✅ | |
| Statistiques par tournée : heures travaillées | 🟡 | Temps de route et temps de montage |
| Statistiques par tournée : argent gagné | ❌ | |

## Optimiser une journée

| Fonctionnalité | État | Remarque |
|---|---|---|
| Bouton « Optimiser les trajets » | ✅ | Nombre de véhicules et temps maximum par véhicule |
| Optimisation automatique des tournées | ✅ | Calcul en arrière-plan, progression en temps réel |
| Distance totale et temps estimé | ✅ | Par solution et par tournée |
| Ordre recommandé des livraisons | ✅ | |

## Visualiser une journée

| Fonctionnalité | État | Remarque |
|---|---|---|
| Carte avec le dépôt et les points de livraison | ✅ | Sans limite à 10 points |
| Points interactifs (détail, mise en évidence) | ✅ | |
| Construire un itinéraire à la main | 🟡 | Par glisser-déposer dans l'éditeur de solution, pas par clic sur la carte |

## Ajuster une journée

| Fonctionnalité | État | Remarque |
|---|---|---|
| Modifier l'ordre des livraisons | ✅ | Glisser-déposer |
| Ajouter ou enlever une livraison d'une tournée | ✅ | Les livraisons hors solution sont affichées à part |
| Recalcul de l'itinéraire | ✅ | Automatique 15 s après la dernière modification, ou à la demande |
| Mise à jour des estimations (distance, temps) | ✅ | À l'enregistrement de la solution |
| Choisir la solution retenue pour la journée | ✅ | Affichée séparément sur la page Journée |
