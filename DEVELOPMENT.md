# Guide de développement

Tout ce qu'il faut pour lancer, tester et déployer le projet. La vue d'ensemble est dans le
[README](README.md), l'organisation du code dans [docs/architecture.md](docs/architecture.md).

## Sommaire

1. [Prérequis](#prérequis)
2. [Configuration](#configuration)
3. [Lancer en développement](#lancer-en-développement)
4. [Données d'essai](#données-dessai)
5. [Tests](#tests)
6. [Production](#production)
7. [Conventions](#conventions)
8. [Dépannage](#dépannage)

## Prérequis

| Outil | Version |
|---|---|
| Java (JDK) | 21 ou plus |
| Node.js | 20.19 ou plus (22 conseillé) |
| Docker | récent, pour PostgreSQL |

Maven n'a pas besoin d'être installé : le wrapper `./mvnw` (ou `mvnw.cmd` sous Windows) est
fourni.

## Configuration

Le backend lit ses réglages dans des variables d'environnement ou dans un fichier `.env` à la
racine du dépôt (ignoré par git) :

```bash
cp .env.example .env
```

| Variable | Défaut | Rôle |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/local` | URL JDBC PostgreSQL |
| `DB_USERNAME`, `DB_PASSWORD` | `postgres` | Identifiants de la base |
| `DB_JPA_DDL_AUTO` | `update` | Gestion du schéma : `update` garde les données, `create-drop` repart de zéro |
| `ORS_KEY` | vide | Clé [OpenRouteService](https://openrouteservice.org/dev/#/signup). Vide = mode estimation |
| `ORS_URL` | `https://api.openrouteservice.org` | Pour une instance ORS auto-hébergée |

D'autres réglages vivent dans
[`application.yml`](backend/server/src/main/resources/application.yml) :

- `app.ors.estimation-sans-cle` : active le mode estimation quand `ORS_KEY` est vide (`true`
  par défaut) ;
- `app.couts.*` : consommation par défaut, prix du carburant, taux horaire, tarifs facturés.

## Lancer en développement

Trois terminaux.

```bash
# 1. PostgreSQL
docker compose -f docker-compose-local.yml up -d postgres

# 2. Backend : http://localhost:8080, Swagger sur /swagger-ui/index.html
./mvnw clean install -DskipTests
./mvnw -f backend/pom.xml -pl server spring-boot:run

# 3. Frontend : http://localhost:4200
cd client && npm ci && npm start
```

`npm start` utilise [client/proxy.conf.json](client/proxy.conf.json). Le serveur Angular
redirige `/api` et `/ws` vers `localhost:8080`, sans souci de CORS. Le code n'utilise que des
URL relatives.

Pour tout lancer d'un coup avec Docker (sans rechargement à chaud) :

```bash
docker compose -f docker-compose-local.yml --profile app up --build   # http://localhost:8080
```

## Données d'essai

Le plus simple : **Livraisons → Jeu d'essai** (ou **Équipe → Jeu d'essai**). Il charge un dépôt à
Seyssins, 32 livraisons sur de vraies adresses de l'agglomération grenobloise, 6 chauffeurs et
5 véhicules. Par l'API :

```bash
curl -X POST http://localhost:8080/api/data/demo
```

> Le chargement **remplace toutes les données** : livraisons, solutions, journées et équipe.

Les fichiers sont dans `backend/server/src/main/resources/demo/` (CSV lisibles, faciles à
modifier). Les dates de commande sont relatives au jour du chargement.

Les gros jeux d'adresses bruts (`resources/datasets/`, 50 à 400 adresses avec leurs matrices)
ne sont plus proposés dans l'interface. Ils servent aux tests d'optimisation et restent
importables par `POST /api/data/import`.
## Tests

### Backend

```bash
./mvnw -f backend/pom.xml verify                                          # tout + couverture
./mvnw -f backend/pom.xml -pl server test -Dtest=TourneeEndpointsTest      # une classe
./mvnw -f backend/pom.xml -pl server test -Dtest='!SolutionGenerationEndpointsTest'  # sans le plus lent
```

- Les tests d'intégration démarrent l'application sur une base H2 en mémoire
  (`src/test/resources/application.yml`). OpenRouteService est toujours remplacé par un mock.
- Les algorithmes ont leurs tests unitaires, sans Spring. `fixtures/ProblemFixtures` construit
  un problème sur un plan, à partir de coordonnées en kilomètres.
- `verify` mesure la couverture avec JaCoCo et **échoue** sous 85 % des lignes ou 65 % des
  branches. Le rapport est généré dans `backend/server/target/site/jacoco/index.html`.

### Frontend

```bash
cd client
npm run test:ci
```

Karma a besoin de Chrome ou Chromium. S'il n'est pas trouvé, indiquer son chemin dans
`CHROME_BIN`.

## Production

L'image Docker unique contient le frontend compilé et le backend. La base PostgreSQL est
externe.

```bash
docker build -t cybercommandes .
docker run -p 8080:8080 \
  -e DB_URL="jdbc:postgresql://<hote>:5432/<base>" \
  -e DB_USERNAME="<utilisateur>" -e DB_PASSWORD="<mot-de-passe>" \
  -e DB_JPA_DDL_AUTO=validate \
  -e ORS_KEY="<cle>" \
  cybercommandes
```

Autres fichiers :

- `docker-compose.yml` : PostgreSQL, backend et NGINX sur un même serveur ;
- `deploy/` : redéploiement automatique par systemd, voir [deploy/README.md](deploy/README.md) ;
- `OpenRouteService/` : instance ORS auto-hébergée, voir [OpenRouteService/SETUP.md](OpenRouteService/SETUP.md) ;
- CI : [.github/CI-CD.md](.github/CI-CD.md).

## Conventions

### Backend

- Un nouvel endpoint commence par son interface et ses DTO dans `rest-api`. Viennent ensuite son
  test d'intégration dans `server/src/test/.../endpoints/`, puis l'implémentation.
- Les contrôleurs restent fins : ils délèguent à un composant (`component/`) et convertissent
  avec un mapper. Ils n'accèdent pas aux dépôts.
- Les erreurs passent par `ResourceNotFoundException`, `InvalidRequestException` ou
  `ConflictException`. Jamais de `ResponseStatusException` dans le métier.
- Un nouvel algorithme implémente `SeparationStrategy` ou `RouteOptimizer` et devient un bean :
  le pipeline le prend en compte sans modification.
- Style : conventions Java d'Oracle, [.editorconfig](.editorconfig), messages d'erreur en
  français.

### Frontend

- Chaque DTO Java a son schéma Zod dans `client/src/app/core/models/api.ts`, avec les mêmes noms
  de champs.
- Les appels HTTP passent par une classe de `core/api`. Les composants n'utilisent jamais
  `HttpClient` directement.
- Prettier : `printWidth: 100`, `singleQuote: true`.

### Git

- Une branche par tâche : `task/<id>-<description-courte>`.
- Messages : `feat(#<id>): …`, `fix(#<id>): …`, `refactor: …`, `test: …`, `docs: …`.

## Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| `Connection refused` sur le port 5432 | PostgreSQL non démarré, ou port pris | `docker compose -f docker-compose-local.yml up -d postgres`, ou changer le port dans le compose et `DB_URL` |
| `Port 8080 was already in use` | Un autre programme écoute déjà | Lancer avec `--server.port=8081` et adapter `client/proxy.conf.json` |
| Les tracés sont des lignes droites | Mode estimation (pas de `ORS_KEY`) | Normal sans clé ; renseigner `ORS_KEY` pour les vrais itinéraires |
| L'optimisation échoue en 502 | Clé ORS invalide ou quota dépassé | Vérifier la clé ; tester d'abord avec 50 adresses |
| « Une optimisation est déjà en cours » | Calcul précédent pas terminé | Attendre le statut « Terminée » ; un redémarrage du backend débloque aussi la journée |
| Classes introuvables pendant les tests (`ClassNotFoundException: TourneeRepository`) | L'IDE compile dans `target/` en même temps que Maven | Relancer avec `./mvnw -f backend/pom.xml clean verify` |
| `Cannot start ChromeHeadless` | Chrome non installé | Installer Chrome ou définir `CHROME_BIN` |
