# Guide de Développement

Ce document décrit le mode d'exécution actuel du projet après migration de l'architecture.

## Résumé rapide

- **Dev** : PostgreSQL local + backend lancé avec Maven + frontend lancé avec npm
- **Prod** : une image Docker unique (frontend compilé + backend Spring Boot), base PostgreSQL hébergée en cloud

## Prérequis

- Node.js 20+
- npm 10+
- Java 21+
- Maven Wrapper (fourni dans le dépôt)
- Docker + Docker Compose

## Mode Développement

Le développement se fait avec trois processus séparés.

### 1. Lancer PostgreSQL en local

Depuis la racine du dépôt :

```bash
docker compose -f docker-compose-local.yml up -d postgres
```

Configuration par défaut :
- Hôte : `localhost`
- Port : `5432`
- Base : `local`
- Utilisateur : `postgres`
- Mot de passe : `postgres`

### 2. Lancer le backend avec le Maven Wrapper

```bash
./mvnw -f backend/pom.xml spring-boot:run
```

Le backend écoute sur : `http://localhost:8080`

Le backend lit la configuration de base via les variables d'environnement suivantes (avec valeurs par défaut) :
- `DB_URL` (défaut: `jdbc:postgresql://localhost:5432/local`)
- `DB_USERNAME` (défaut: `postgres`)
- `DB_PASSWORD` (défaut: `postgres`)
- `DB_JPA_DDL_AUTO` (défaut: `create-drop`)

### 3. Lancer le frontend avec npm

```bash
cd client
npm install
npm start
```

Le frontend est disponible sur : `http://localhost:4200`

## Mode Production

La production utilise une seule image Docker qui embarque :
- le frontend Angular compilé
- le backend Spring Boot

La base PostgreSQL n'est **pas** lancée en conteneur local en production : elle est attendue sur un service cloud managé.

### Build de l'image

Depuis la racine du dépôt :

```bash
docker build -t projet-integrateur:latest .
```

### Démarrage du conteneur

```bash
docker run -p 8080:8080 \
  -e DB_URL="jdbc:postgresql://<host-cloud>:5432/<db-name>" \
  -e DB_USERNAME="<db-user>" \
  -e DB_PASSWORD="<db-password>" \
  -e DB_JPA_DDL_AUTO="validate" \
  projet-integrateur:latest
```

Application accessible sur : `http://localhost:8080`

## Vérifications utiles

### Arrêter PostgreSQL local

```bash
docker compose -f docker-compose-local.yml down
```

### Vérifier que le backend répond

```bash
curl -I http://localhost:8080
```

### Vérifier que le frontend répond

```bash
curl -I http://localhost:4200
```
