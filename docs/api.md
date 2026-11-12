# API REST

Toutes les routes sont préfixées par `/api`. Dates au format `yyyy-MM-dd`, durées en ISO-8601
(`PT7H30M`), distances en mètres, carburant en millilitres, montants en euros.

La documentation interactive, générée depuis les contrats du module `rest-api`, est servie sur
`/swagger-ui/index.html`.

## Erreurs

Toutes les erreurs renvoient le même corps :

```json
{ "uri": "/api/tournees/12/affectation", "message": "Léa Martin conduit déjà une autre tournée de cette solution" }
```

| Code | Quand |
|---|---|
| 400 | Données invalides (validation, date ou durée illisible) |
| 404 | Ressource introuvable |
| 409 | Conflit métier : optimisation déjà en cours, chauffeur déjà pris, capacité dépassée, immatriculation en double |
| 502 | OpenRouteService indisponible |

## Journées

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/journees?debut=&fin=` | Journées optimisées sur une période (un an au plus) : statut, nombre de solutions, solution retenue |
| GET | `/journees/{date}` | État d'une journée, sans la créer |
| POST | `/journees/{date}/optimization` | Lance l'optimisation : `202`, puis avancement par WebSocket |
| GET | `/journees/{date}/solutions` | Solutions de la journée avec leurs statistiques |
| GET | `/journees/{date}/solutions/choisie` | Solution retenue (`404` si aucune) |

Corps de l'optimisation :

```json
{ "nbVehicules": 3, "maxTempsParVehicule": "PT7H", "livraisonIds": [1, 2, 3] }
```

## Solutions

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/solutions/{id}` | Détail : tournées dans l'ordre de passage, statistiques, bilan financier |
| GET | `/solutions/{id}/livraisons` | Livraisons dans la solution et livraisons à faire hors solution |
| PUT | `/solutions/{id}/choisie` | Retient la solution pour sa journée |
| PUT | `/solutions/{id}/tournees` | Enregistre une solution éditée, puis recalcule distances et coûts |
| POST | `/solutions/{id}/recalculate-stats` | Recalcule les statistiques à partir des trajets |
| POST | `/solutions` | Crée une solution vide (usage technique) |

## Tournées

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/tournees/{id}` | Détail d'une tournée |
| PUT | `/tournees/{id}/affectation` | Affecte un chauffeur et un véhicule (`null` = retirer) |
| POST | `/tournees/{id}/terminer` | Marque toutes les livraisons de la tournée comme livrées |
| GET | `/tournees/{id}/feuille-de-route` | Feuille de route en CSV (séparateur `;`, UTF-8 avec BOM pour Excel) |

## Livraisons

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/livraisons?livree=&dateDebut=&dateFin=` | Livraisons filtrées. Sans filtre : uniquement celles à livrer |
| GET | `/livraisons/{id}` | Détail |
| POST | `/livraisons` | Création |
| PUT | `/livraisons/{id}` | Modification (une adresse modifiée crée une nouvelle adresse, pour ne pas déplacer d'autres clients) |
| DELETE | `/livraisons/{id}` | Suppression, et retrait des tournées qui la contiennent |
| PATCH | `/livraisons/{id}/statut` | `{ "livree": true }` ou `false` |

Corps d'une livraison :

```json
{
  "adresse": { "numero": "12", "rue": "Rue Félix Viallet", "codePostal": "38000", "ville": "Grenoble", "lat": 45.1913, "lng": 5.7146 },
  "poids": 3.5,
  "tempsMontage": "PT15M",
  "dateCommande": "2026-11-10"
}
```

## Équipe

| Méthode | Route | Rôle |
|---|---|---|
| GET, POST | `/chauffeurs` | Liste (triée par nom) et création |
| PUT, DELETE | `/chauffeurs/{id}` | Modification, suppression (les tournées perdent leur affectation) |
| GET, POST | `/vehicules` | Liste et création (immatriculation unique, mise en majuscules) |
| PUT, DELETE | `/vehicules/{id}` | Modification, suppression |
| PUT | `/chauffeurs/{id}/photo`, `/vehicules/{id}/photo` | Envoie une photo (multipart, champ `photo` ; JPEG, PNG ou WebP, 3 Mo au plus) |
| GET | `/chauffeurs/{id}/photo`, `/vehicules/{id}/photo` | Image ; l'adresse fournie dans `photoUrl` change à chaque nouvelle photo |
| DELETE | `/chauffeurs/{id}/photo`, `/vehicules/{id}/photo` | Retire la photo |

## Cartographie et données

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/parkings` | Adresse du dépôt |
| POST | `/routes` | Tracé routier passant par une liste de points (lignes droites en mode estimation) |
| POST | `/data/demo` | Charge le jeu d'essai : dépôt, 32 livraisons, 6 chauffeurs, 5 véhicules. **Remplace toutes les données** |
| GET | `/data` | Jeux d'adresses bruts (utilisés par les tests) |
| POST | `/data/import` | Importe un jeu d'adresses. **Vide les livraisons, solutions et journées existantes** |
| POST | `/data/generate/livraisons` | Génère `count` livraisons aléatoires sur les adresses importées |

## WebSocket

Point d'entrée STOMP : `/ws`. Sujet : `/topic/journee/{date}`.

```json
{ "date": "2026-11-12", "statutGeneration": "GENERATION_ROUTE", "timestamp": "2026-11-12T08:14:03Z" }
```

Statuts dans l'ordre : `GENERATION_MATRICE`, `GENERATION_ROUTE` (envoyé à chaque solution
prête), puis `TERMINE`.
