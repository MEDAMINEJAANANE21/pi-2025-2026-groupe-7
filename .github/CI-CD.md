# CI/CD

## `ci.yml`

Déclenché à chaque push sur `main`, sur chaque pull request, et à la demande (onglet *Actions*).

| Job | Ce qu'il vérifie |
|---|---|
| `backend` | `mvn verify` : tous les tests (H2, OpenRouteService simulé) puis le seuil JaCoCo (85 % des lignes, 65 % des branches). Le rapport de couverture est publié en artefact. |
| `frontend` | `npm ci`, build de production (avec les budgets de taille), tests Karma en Chrome headless. |
| `docker` | Sur `main` seulement, une fois les deux autres jobs réussis : construit l'image de production (`Dockerfile` à la racine), sans la publier. |

Aucun secret n'est nécessaire : les tests n'appellent aucun service externe.

## `db-update-schema.yml`

Lancement manuel. Met à jour le schéma de la base de production avec la valeur
`spring.jpa.hibernate.ddl-auto` choisie. Il utilise les secrets `DB_URL`, `DB_USERNAME` et
`DB_PASSWORD` du dépôt.
