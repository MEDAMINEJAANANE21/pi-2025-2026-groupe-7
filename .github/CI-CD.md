# CI/CD GitHub Actions

Ce document décrit les workflows CI/CD actuellement utilisés dans le dépôt.

## Workflows

### 1. CI principal
Fichier : `.github/workflows/ci.yml`

Déclenchement :
- push sur `main`
- pull request vers `main`
- déclenchement manuel

Jobs :
- `test-frontend`
  - Node.js 20
  - installation des dépendances dans `client/`
  - build Angular
  - tests unitaires (ChromeHeadless)
  - publication des artefacts frontend
- `test-backend`
  - Java 21 (Temurin)
  - cache Maven
  - compilation/tests/package dans `backend/`
  - publication du JAR
- `build-docker`
  - exécuté uniquement sur push sur `main`
  - build de l'image Docker de production (frontend + backend)
  - lancement rapide d'un conteneur de vérification

## 2. Frontend uniquement
Fichier : `.github/workflows/frontend-test.yml`

Déclenchement :
- changements dans `client/**`
- changements du workflow lui-même

Ce workflow exécute :
- installation npm
- build Angular de production
- tests unitaires avec couverture
- upload des rapports de couverture

## 3. Backend uniquement
Fichier : `.github/workflows/backend-test.yml`

Déclenchement :
- changements dans `backend/**`
- changements du workflow lui-même

Ce workflow exécute :
- tests Maven
- packaging du backend
- upload des rapports de tests et du JAR

## Exécution locale recommandée avant push

### Frontend
```bash
cd client
npm ci
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
```

### Backend
```bash
./mvnw -f backend/pom.xml clean test
./mvnw -f backend/pom.xml package
```

### Build de l'image de production
```bash
docker build -t projet-integrateur:test .
```

## Points d'attention

- Le dossier frontend du dépôt est `client/`.
- Le backend s'exécute avec Java 21.
- En production, l'image Docker contient frontend + backend, et la base PostgreSQL est externe (cloud).
