FROM alpine:latest

# Install curl for downloading (update package index first)
RUN apk update && apk add --no-cache curl

# Set build arguments for OSM data
ARG OSM_URL=http://download.geofabrik.de/europe/france/rhone-alpes-latest.osm.pbf
ARG OSM_FILENAME=region.osm.pbf

# Download OSM data at build time
RUN echo "Downloading OSM data from ${OSM_URL}..." && \
    curl -L --progress-bar -o "/tmp/${OSM_FILENAME}" "${OSM_URL}" && \
    echo "Download complete!" && \
    ls -lh "/tmp/${OSM_FILENAME}"

# Copy the downloaded file to the volume on container start
ENTRYPOINT ["/bin/sh", "-c", "if [ -f /data/${OSM_FILENAME} ]; then echo 'OSM file already exists in volume, skipping copy.'; else echo 'Copying OSM data to volume...'; cp /tmp/${OSM_FILENAME} /data/${OSM_FILENAME} && echo 'Copy complete!' && ls -lh /data/${OSM_FILENAME}; fi"]
