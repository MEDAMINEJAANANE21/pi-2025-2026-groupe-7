# OpenRouteService Self-Hosted Setup Guide

This guide describes how to set up, manage, and maintain a self-hosted OpenRouteService instance using Docker.

## Overview

OpenRouteService is a routing service based on OpenStreetMap data. This guide covers running the official Docker image and managing it on a remote server accessible via SSH and VPN.

**Docker Hub**: https://hub.docker.com/r/openrouteservice/openrouteservice

## Prerequisites

- Docker and Docker Compose installed on the server
- SSH access to the server
- VPN configured for secure access
- Sufficient disk space (graphs can be large depending on OSM data size)
- At least 2GB RAM (more for larger datasets)

## Initial Setup

### 1. Create Directory Structure

Connect to your server via SSH and create the necessary directories:

```bash
mkdir -p ~/openrouteservice/config
mkdir -p ~/openrouteservice/elevation_cache
mkdir -p ~/openrouteservice/graphs
mkdir -p ~/openrouteservice/files
mkdir -p ~/openrouteservice/logs
cd ~/openrouteservice
```

### 2. Use Docker Compose Configuration

Three pre-configured docker-compose files are available in this directory:

- **`docker-compose.yml`** - Standard configuration for local/development use
- **`docker-compose.vpn.yml`** - VPN-bound configuration (requires VPN IP update)
- **`docker-compose.production.yml`** - Production-ready with higher resource limits

Choose the appropriate configuration file. For VPN binding, edit `docker-compose.vpn.yml` and replace `10.8.0.1` with your VPN server IP.

You can also copy and customize `.env.example` to `.env` for environment variable management.

### 3. Start the Service

```bash
docker compose up -d
```

This will:
- **Build the OSM downloader image** (downloads map data during build)
- Pull the OpenRouteService Docker image
- Start the container in detached mode (background)
- Begin building graphs from OSM data (this can take time on first run)

### 4. Using Custom OSM Data

The setup includes an automatic OSM data downloader that downloads data **at Docker build time**. To use a different geographic region:

1. **Edit docker-compose.yml** and update the `OSM_URL` in the `osm-downloader` build args:
   ```yaml
   services:
     osm-downloader:
       build:
         args:
           # Change to your desired region from http://download.geofabrik.de/
           - OSM_URL=http://download.geofabrik.de/europe/france/rhone-alpes-latest.osm.pbf
   ```

2. **Available regions** (examples):
   - **Heidelberg (small, default)**: `...germany/baden-wuerttemberg/freiburg-regbez-latest.osm.pbf`
   - **Rhône-Alpes (Grenoble)**: `...europe/france/rhone-alpes-latest.osm.pbf`
   - **All France**: `...europe/france-latest.osm.pbf`
   - **Full list**: http://download.geofabrik.de/

3. **Adjust memory settings** for larger regions in docker-compose.yml:
   ```yaml
   ors:
     environment:
       - XMX=4g  # Rhône-Alpes
       # - XMX=8g  # Full France
   ```

4. **Rebuild the downloader image with new data**:
   ```bash
   docker compose down
   docker compose build osm-downloader
   rm -rf ors/files/region.osm.pbf ors/graphs/*
   docker compose up -d
   ```

**Note:** The OSM data is downloaded during the Docker image build, so changing regions requires rebuilding the image. Building graphs for larger regions can take hours. Monitor progress: `docker logs -f ors-app`

## Exposing via VPN

### Option 1: Direct Port Forwarding (VPN Network)

If your VPN provides a subnet (e.g., 10.8.0.0/24), bind the service to the VPN interface:

1. Use the provided `docker-compose.vpn.yml` file
2. Edit it and replace `10.8.0.1` with your VPN server IP
3. Start with: `docker compose -f docker-compose.vpn.yml up -d`
4. Access via: `http://YOUR_VPN_IP:8080/ors/v2/health`

### Option 2: SSH Tunnel

Forward the port through SSH when you need access:

```bash
ssh -L 8080:localhost:8080 user@your-server.com
```

Then access locally at: `http://localhost:8080`

### Option 3: Reverse Proxy with Nginx

For more advanced setups, use Nginx as a reverse proxy with SSL. A sample configuration is provided in `nginx.conf`.

1. Copy `nginx.conf` to `/etc/nginx/sites-available/ors`
2. Update the domain name and SSL certificate paths
3. Enable the site: `sudo ln -s /etc/nginx/sites-available/ors /etc/nginx/sites-enabled/`
4. Test config: `sudo nginx -t`
5. Reload Nginx: `sudo systemctl reload nginx`

## Managing the Container

### Check Container Status

```bash
# List running containers
docker ps

# Check if ors-app is running
docker ps -f name=ors-app
```

### View Logs

```bash
# View all logs
docker compose logs

# Follow logs in real-time
docker compose logs -f

# View specific number of recent lines
docker compose logs --tail=100

# View logs for specific timestamp
docker compose logs --since 2h

# View logs for specific service
docker compose logs ors
docker compose logs vroom

# Direct docker logs command
docker logs ors-app
docker logs vroom-app

# Follow logs for specific service
docker logs -f vroom-app
```

### Stop the Service

```bash
# Stop the container (preserves data)
docker compose stop

# Or stop and remove container (data in volumes remains safe)
docker compose down
```

### Start the Service Again

```bash
# Start existing container
docker compose start

# Or create and start if removed
docker compose up -d
```

### Restart the Service

```bash
# Restart all services
docker compose restart

# Restart specific service
docker compose restart ors
docker compose restart vroom

# Or with docker command
docker restart ors-app
docker restart vroom-app
```

### Update to Latest Version

```bash
# Pull the latest image
docker compose pull

# Recreate container with new image
docker compose up -d
```

## Verify Remote Access

After starting the services, verify they are accessible from other machines on your network:

### From the Server (localhost)

```bash
# Test OpenRouteService
curl http://localhost:8080/ors/v2/health
curl http://localhost:8080/ors/v2/status

# Test VROOM
curl http://localhost:3000/health
curl http://localhost:3000/
```

### From Remote Machines

Replace `YOUR_SERVER_IP` with your actual server IP address (e.g., `129.88.210.65`):

**OpenRouteService:**
- Health: `http://YOUR_SERVER_IP:8080/ors/v2/health`
- Status: `http://YOUR_SERVER_IP:8080/ors/v2/status`

**VROOM:**
- Health: `http://YOUR_SERVER_IP:3000/health`
- Root: `http://YOUR_SERVER_IP:3000/`

Test from remote machine:
```bash
# Test ORS
curl http://YOUR_SERVER_IP:8080/ors/v2/health

# Test VROOM
curl http://YOUR_SERVER_IP:3000/health
```

Or open these URLs in your browser. If you can't connect:
1. Check containers are running: `docker compose ps`
2. Verify ports are bound to `0.0.0.0` in docker-compose.yml
3. Check firewall: `sudo ufw status` or `sudo firewall-cmd --list-ports`
4. Allow ports if needed: `sudo ufw allow 8080` and `sudo ufw allow 3000`

## Reconnecting via SSH

When you connect to your server after some time, here's a quick reference:

```bash
# 1. SSH into your server
ssh user@your-server.com

# 2. Navigate to OpenRouteService directory
cd ~/openrouteservice

# 3. Check if containers are running
docker compose ps

# 4. View recent logs
docker compose logs --tail=50

# 5. Check OpenRouteService health
curl http://localhost:8080/ors/v2/health
curl http://localhost:8080/ors/v2/status

# 6. Check VROOM health
curl http://localhost:3000/health
```

## Service Endpoints

Once running, the services expose these endpoints:

### OpenRouteService (port 8080)

- **Health**: `http://localhost:8080/ors/v2/health`
- **Status**: `http://localhost:8080/ors/v2/status`
- **Directions**: `http://localhost:8080/ors/v2/directions/driving-car`
- **Isochrones**: `http://localhost:8080/ors/v2/isochrones/driving-car`
- **Matrix**: `http://localhost:8080/ors/v2/matrix/driving-car`
- **Snap**: `http://localhost:8080/ors/v2/snap/driving-car`
- **Export**: `http://localhost:8080/ors/v2/export/driving-car`

### VROOM (port 3000)

- **Health**: `http://localhost:3000/health`
- **Optimization**: `http://localhost:3000/` (POST request)

**Note**: VROOM is used for vehicle routing optimization problems. It uses OpenRouteService as its routing engine. Send POST requests to the root endpoint with job and vehicle data.

## Configuration

### Custom OSM Data

To use your own OSM data file:

1. Download OSM PBF file (e.g., from https://download.geofabrik.de/)
2. Place it in the `files` directory
3. Update `docker-compose.yml` or config to reference the file

### Environment Variables

Common environment variables are documented in `.env.example`. Copy it to `.env` and adjust values:

```bash
cp .env.example .env
# Edit .env with your preferred settings
nano .env
```

Key variables:
- `REBUILD_GRAPHS` - Set to True to force graph rebuild
- `CONTAINER_LOG_LEVEL` - DEBUG, INFO, WARN, ERROR
- `XMS` - Initial Java heap size
- `XMX` - Maximum Java heap size

## Troubleshooting

### Container Keeps Restarting

```bash
# Check logs for errors
docker compose logs --tail=200

# Check resource usage
docker stats ors-app
```

### Graph Building Takes Too Long

- First build can take hours for large regions
- Monitor progress in logs: `docker compose logs -f`
- Consider using smaller OSM extracts for testing

### Out of Memory Errors

Increase Java heap size in `docker-compose.yml`:

```yaml
environment:
  - XMS=2g
  - XMX=4g
```

### Port Already in Use

```bash
# Find what's using port 8080
sudo lsof -i :8080

# Change port in docker-compose.yml
ports:
  - "8081:8080"
```

## Backup and Maintenance

### Backup Graphs

Graphs are stored in `./graphs` directory and take time to build. Back them up:

```bash
# Create backup
tar -czf ors-graphs-backup-$(date +%Y%m%d).tar.gz graphs/

# Restore backup
tar -xzf ors-graphs-backup-YYYYMMDD.tar.gz
```

### Data Persistence

All important data is stored in volumes mapped to local directories:
- `./graphs` - Pre-built routing graphs (most important to backup)
- `./elevation_cache` - Elevation data cache
- `./config` - Configuration files
- `./logs` - Application logs

### Cleanup

```bash
# Remove logs older than 7 days
find ./logs -name "*.log" -mtime +7 -delete

# Clean up unused Docker images
docker image prune -a
```

## Security Considerations

1. **Firewall**: Only expose port 8080 to VPN network, not public internet
2. **Authentication**: Consider adding authentication via reverse proxy
3. **Rate Limiting**: Implement rate limiting to prevent abuse
4. **Updates**: Regularly update the Docker image for security patches
5. **Monitoring**: Set up monitoring and alerts for service health

## Performance Tuning

For production use, the `docker-compose.production.yml` file is pre-configured with optimal settings:
- 4-8GB Java heap size
- CPU and memory limits
- Warning-level logging

Start with: `docker compose -f docker-compose.production.yml up -d`

## Useful Commands Reference

```bash
# Quick status check
docker compose ps

# Resource usage
docker stats ors-app

# Execute command in container
docker compose exec ors bash

# View container config
docker inspect ors-app

# Export logs to file
docker compose logs > ors-logs-$(date +%Y%m%d).log

# Remove everything (CAUTION: destroys data)
docker compose down -v
```

## Additional Resources

- [Official Documentation](https://giscience.github.io/openrouteservice/)
- [API Reference](https://giscience.github.io/openrouteservice/api-reference/)
- [GitHub Repository](https://github.com/GIScience/openrouteservice)
- [Community Forum](https://ask.openrouteservice.org/)
- [Docker Hub](https://hub.docker.com/r/openrouteservice/openrouteservice)
