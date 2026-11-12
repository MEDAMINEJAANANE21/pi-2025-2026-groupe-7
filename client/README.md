# Frontend

Application Angular 21 de CyberCommandes. La vue d'ensemble du projet est dans le
[README principal](../README.md).

## Commandes

```bash
npm ci              # installe les dépendances
npm start           # http://localhost:4200, /api et /ws redirigés vers localhost:8080
npm run build       # build de production dans dist/browser
npm test            # tests en mode surveillance
npm run test:ci     # tests une fois, Chrome headless, avec couverture
```

## Organisation

```text
src/app/
  core/api        accès à l'API, une classe par ressource
  core/models     schémas Zod des réponses (miroir des DTO Java)
  core/util       dates, durées, formats français, couleurs des tournées
  shared/         carte, icônes, ruban de tournée, notifications, confirmation
  layout/         barre de navigation
  features/       une page par dossier
  testing/        données de test partagées par les specs
```

## Règles

- Composants standalone, `ChangeDetectionStrategy.OnPush`, état en `signal`.
- Toute réponse HTTP est validée par son schéma Zod dans `core/api`.
- Styles communs dans `src/styles.scss` : tokens (`--text`, `--accent`, `--r-card`…), boutons
  `.btn`, champs `.input`, tableaux `.table`, dialogues `dialog.dialog`. Les styles de composant
  restent courts et réutilisent ces tokens.
- Une seule couleur d'accent (orange) pour l'action principale et la sélection. Les autres
  couleurs sont réservées aux tournées (`core/util/route-colors.ts`).
- Icônes : sous-ensemble de [Lucide](https://lucide.dev) dans `shared/icon.ts`, 16 px, trait 1,5.
