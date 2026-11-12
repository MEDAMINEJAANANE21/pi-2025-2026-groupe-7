# Projet Integrateur

**Groupe 7**

- La tâche : https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026
- Figma : https://www.figma.com/files/team/1580544524843380726/all-projects
- Kanban : https://github.com/users/d0rich/projects/8/views/1

> **Note aux professeurs** : Pour une documentation détaillée de l'application et des fonctionnalités implémentées, veuillez consulter le [README du client](client/README.md).

## Membres du Groupe

- DOROFEEV Nikolai
- BARKOK Omar
- CHEIKH Mohamed Amine
- LEGRENZI Maxime
- ALVES Tennessee
- JAANANE Mohamed Amine 

## Organisation du Projet

### Workflow de Développement

Ce projet suit une approche collaborative basée sur GitHub :

- **Branches & Pull Requests** : Chaque fonctionnalité est développée dans une branche dédiée et intégrée via Pull Request vers `main`
- **Task Tracking** : [GitHub Projects](https://github.com/users/d0rich/projects/8/views/1) est utilisé pour la gestion des tâches et le suivi du projet
- **Code Review** : Validation du code par les pairs avant fusion des PR

### CI/CD avec GitHub Actions

Le projet utilise trois workflows automatisés (voir [CI-CD.md](.github/CI-CD.md)) :

1. **[CI Principal](.github/workflows/ci.yml)** : Exécuté sur push/PR vers `main`
   - Tests frontend (Angular, Node.js 20, ChromeHeadless)
   - Tests backend (Spring Boot, Java 21, Maven)
   - Build Docker multi-stage (production uniquement)

2. **[Frontend Tests](.github/workflows/frontend-test.yml)** : Déclenché sur changements dans `client/`
   - Tests unitaires avec coverage
   - Linter et build production

3. **[Backend Tests](.github/workflows/backend-test.yml)** : Déclenché sur changements dans `backend/`
   - Tests unitaires et rapports de test

### Déploiement Automatique

- **Frontend** : Déployé automatiquement sur [Render](https://l3miage-projet-integrateur-g7.onrender.com/)
  - URL de production : https://l3miage-projet-integrateur-g7.onrender.com/
   - Déploiement continu depuis la branche `main`

## Structure du Projet

Ce projet est organisé en trois blocs principaux :

- **Client Angular** : `./client`
- **Backend Spring Boot (Maven multi-modules)** : `./backend`
- **Base PostgreSQL** : locale en développement, managée en cloud en production

## Exécution en Développement

Le mode développement se lance en **3 services séparés** :

1. **PostgreSQL local** via Docker Compose
2. **Backend** via Maven
3. **Frontend** via npm

### Prérequis

- Node.js 20+
- Java 21+
- Maven Wrapper (fourni dans le dépôt)
- Docker + Docker Compose

### 1. Lancer PostgreSQL local

```bash
docker compose -f docker-compose-local.yml up -d postgres
```

Base créée par défaut :
- Utilisateur : `postgres`
- Mot de passe : `postgres`
- Nom de base : `local`
- Port : `5432`

### 2. Lancer le backend (Maven Wrapper)

```bash
./mvnw -f backend/pom.xml spring-boot:run
```

API disponible sur : http://localhost:8080

### 3. Lancer le frontend (npm)

```bash
cd client
npm install
npm start
```

Application disponible sur : http://localhost:4200

## Exécution en Production

Le mode production repose sur :

- **Une image Docker unique** qui contient le frontend compilé + le backend Spring Boot
- **Une base PostgreSQL cloud** (hors conteneur)

### Build de l'image

```bash
docker build -t projet-integrateur:latest .
```

### Lancement du conteneur avec base cloud

```bash
docker run -p 8080:8080 \
   -e DB_URL="jdbc:postgresql://<host-cloud>:5432/<db-name>" \
   -e DB_USERNAME="<db-user>" \
   -e DB_PASSWORD="<db-password>" \
   -e DB_JPA_DDL_AUTO="validate" \
   projet-integrateur:latest
```

Application disponible sur : http://localhost:8080
