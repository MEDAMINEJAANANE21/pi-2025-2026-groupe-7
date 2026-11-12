#!/bin/bash
cd /home/ecloud/pi-app || exit 1

# Fetch without pulling
git fetch origin main

# Check if origin/main is ahead
if [ "$(git rev-parse HEAD)" != "$(git rev-parse @{u})" ]; then
  echo "$(date): New changes detected on main. Deploying..."
  git pull origin main
  docker compose up -d --build --remove-orphans
  echo "$(date): Deploy complete."
fi
