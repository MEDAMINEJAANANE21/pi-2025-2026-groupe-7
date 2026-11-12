# CyberCommandes — Planification de tournées de livraison

Application web qui aide un planificateur à préparer les tournées de livraison d'une journée.
Elle répartit les colis entre plusieurs véhicules, calcule les meilleurs itinéraires et permet
d'ajuster le résultat à la main.

Projet Intégrateur — L3 MIAGE, Université Grenoble Alpes (2025-2026), groupe 7.
Sujet : [l3m-CyberCommandes-2025-2026](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026).

## Fonctionnalités

- **Journée** : carte des livraisons non livrées, filtres (statut, dates de commande), masquage
  ponctuel de livraisons.
- **Optimisation** : choix du nombre de véhicules et du temps maximum par véhicule, puis
  génération de plusieurs solutions en arrière-plan (stratégies et algorithmes différents).
- **Suivi en temps réel** : l'avancement du calcul est poussé au navigateur par WebSocket.
- **Comparaison** : tableau des solutions triable (distance, carburant, durée, nombre de livraisons).
- **Solution choisie** : une solution peut être retenue pour la journée et s'affiche à part.
- **Édition** : glisser-déposer des livraisons entre tournées, recalcul des itinéraires et des
  statistiques, livraisons hors solution affichées dans une couleur distincte.

Les processus métier sont décrits dans [docs/processes.md](docs/processes.md). La liste des
idées de départ, avec ce qui a été réalisé, est dans [docs/Fonctionnalite.md](docs/Fonctionnalite.md).

## Comment ça marche

L'optimisation se fait en deux phases :

1. **Séparation** : les livraisons sont réparties en un groupe par véhicule, soit par
   **K-medoids** (zones géographiques), soit par **secteurs angulaires** autour du dépôt.
   VROOM peut aussi faire la répartition lui-même.
2. **Itinéraire** : pour chaque groupe, l'ordre de passage est calculé par **VROOM** (via
   OpenRouteService), un **algorithme génétique** ou un **algorithme glouton**.

Chaque combinaison séparation × moteur donne une solution. Le planificateur les compare, puis
choisit la meilleure.

Les matrices de distances et de durées viennent de l'API
[OpenRouteService](https://openrouteservice.org/). Elles restent côté serveur et ne sont jamais
envoyées au navigateur.

Les explications détaillées (découpage des matrices, choix des algorithmes, éditeur) sont dans le
[rapport technique](client/README.md).

## Stack

| Partie | Technologies |
|---|---|
| Frontend | Angular 21 (composants standalone, signals), Taiga UI 4, Leaflet, Zod 4, STOMP |
| Backend | Java 21, Spring Boot, Spring Data JPA, MapStruct, Lombok, springdoc-openapi |
| Données | PostgreSQL (H2 en mémoire pour les tests) |
| Tests | JUnit 5, Mockito, MockMvc, JaCoCo · Karma + Jasmine |
| Livraison | Docker multi-stage, GitHub Actions |

## Structure du dépôt

```text
backend/
  rest-api/     contrats publics : interfaces d'endpoints et DTO (aucune logique)
  server/       implémentation : contrôleurs, services, entités, optimisation
client/         application Angular
docs/           documentation fonctionnelle
OpenRouteService/  installation optionnelle d'un ORS auto-hébergé
deploy/, nginx/    scripts et configuration de déploiement
```

## Démarrage rapide

Prérequis : **Java 21+**, **Node.js 20.19+**, **Docker**.

```bash
# 1. Base de données
docker compose -f docker-compose-local.yml up -d postgres

# 2. Configuration (clé OpenRouteService gratuite sur openrouteservice.org)
cp .env.example .env        # puis renseigner ORS_KEY

# 3. Backend → http://localhost:8080
./mvnw clean install -DskipTests
./mvnw -f backend/pom.xml -pl server spring-boot:run

# 4. Frontend → http://localhost:4200
cd client && npm ci && npm start
```

Pour avoir des données, importer un jeu de test :

```bash
curl -X POST http://localhost:8080/api/data/import \
  -H "Content-Type: application/json" \
  -d '{"dataset": "dataset-50-par-3-exemple-1", "clearBeforeImport": true}'
```

Le guide complet (variables d'environnement, jeux de données, production, dépannage) est dans
[DEVELOPMENT.md](DEVELOPMENT.md).

## API

La documentation interactive est servie par le backend sur
`http://localhost:8080/swagger-ui/index.html`.

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/api/livraisons?livree=&dateDebut=&dateFin=` | Livraisons filtrées |
| GET | `/api/parkings` | Dépôt de départ des véhicules |
| GET | `/api/journees/{date}` | État d'une journée |
| POST | `/api/journees/{date}/optimization` | Lance l'optimisation (asynchrone) |
| GET | `/api/journees/{date}/solutions` | Solutions générées pour la journée |
| GET | `/api/journees/{date}/solutions/choisie` | Solution retenue (404 si aucune) |
| GET | `/api/solutions/{id}` | Détail d'une solution |
| GET | `/api/solutions/{id}/livraisons` | Livraisons dans / hors de la solution |
| PUT | `/api/solutions/{id}/choisie` | Marque la solution comme retenue |
| PUT | `/api/solutions/{id}/tournees` | Enregistre une solution éditée |
| POST | `/api/solutions/{id}/recalculate-stats` | Recalcule les statistiques |
| POST | `/api/routes` | Tracé routier d'une tournée |
| POST | `/api/data/import` | Importe un jeu de données de test |
| POST | `/api/data/generate/livraisons` | Génère des livraisons aléatoires |

WebSocket (STOMP) : point d'entrée `/ws`, sujet `/topic/journee/{date}`.

## Tests

```bash
./mvnw -f backend/pom.xml clean verify                      # backend + seuil de couverture
cd client && npm test -- --watch=false --browsers=ChromeHeadless
```

Les appels à OpenRouteService sont simulés dans les tests : aucune clé n'est nécessaire.

## Équipe

Groupe 7 — voir [AUTHORS](AUTHORS) : Nikolai Dorofeev, Omar Barkok, Mohamed Amine Cheikh,
Maxime Legrenzi, Tennessee Alves, Mohamed Amine Jaanane.

Base de code de départ fournie par Alexandre Demeure et Nicolas Marinho.

## Licence

[MIT](LICENSE).
