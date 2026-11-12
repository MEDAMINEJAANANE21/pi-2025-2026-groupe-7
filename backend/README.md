# Backend

Projet Maven multi-module :

| Module | Contenu |
|---|---|
| `rest-api` | Contrats de l'API : interfaces d'endpoints annotées (Spring Web, OpenAPI) et DTO `record`. Aucune logique. |
| `server` | Application Spring Boot : contrôleurs, composants métier, optimisation, persistance JPA. |

```bash
./mvnw -f backend/pom.xml verify                          # tests + couverture
./mvnw -f backend/pom.xml -pl server spring-boot:run      # lancer l'API sur le port 8080
```

L'organisation des paquets, les patterns et le modèle de données sont décrits dans
[docs/architecture.md](../docs/architecture.md). Les routes sont listées dans
[docs/api.md](../docs/api.md).
