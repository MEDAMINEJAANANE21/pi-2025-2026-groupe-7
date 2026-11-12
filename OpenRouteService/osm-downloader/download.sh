#!/bin/sh
set -e

# Default values
OSM_URL="${OSM_URL:-http://download.geofabrik.de/europe/germany/baden-wuerttemberg/freiburg-regbez-latest.osm.pbf}"
OSM_FILENAME="${OSM_FILENAME:-region.osm.pbf}"

if [ -f "/data/$OSM_FILENAME" ]; then
    echo "OSM file already exists at /data/$OSM_FILENAME, skipping download."
    exit 0
fi

echo "Downloading OSM data from $OSM_URL..."
echo "Target file: /data/$OSM_FILENAME"

curl -L --progress-bar -o "/data/$OSM_FILENAME" "$OSM_URL"

if [ $? -eq 0 ]; then
    echo "Download complete!"
    ls -lh "/data/$OSM_FILENAME"
else
    echo "Download failed!"
    exit 1
fi
