# Déploiement automatique

Ces fichiers redéploient l'application sur un serveur Linux à chaque nouveau commit sur `main`.

## Fonctionnement

1. Toutes les 60 secondes, le timer systemd `pi-app.timer` lance le service `pi-app.service`.
2. Le service exécute `pi-app.sh`, qui fait un `git fetch` de `origin/main`.
3. Si `origin/main` a avancé, le script fait `git pull` puis
   `docker compose up -d --build --remove-orphans` avec le [docker-compose.yml](../docker-compose.yml)
   de la racine.

## Installation

Prérequis sur le serveur : Docker, Docker Compose, git, et le dépôt cloné.

```bash
sudo ./deploy/start-watcher.sh
```

Le script crée des liens symboliques vers `/etc/systemd/system/` et `/opt/`, puis active le timer.

## À adapter avant usage

Les chemins sont écrits en dur pour le serveur d'origine :

- `pi-app.service` : utilisateur `User=ecloud`.
- `pi-app.sh` : dossier `/home/ecloud/pi-app`.

## Commandes utiles

```bash
systemctl status pi-app.timer      # état du timer
journalctl -u pi-app.service -f    # journaux des déploiements
sudo systemctl disable --now pi-app.timer   # arrêter le déploiement automatique
```
