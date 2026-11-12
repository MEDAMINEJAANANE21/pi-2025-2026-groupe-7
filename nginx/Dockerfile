# Multi-stage Dockerfile for frontend + NGINX
# Builds Angular app and serves static files with custom NGINX config

# Stage 1: Build Angular application
FROM node:22-alpine AS frontend-build

WORKDIR /frontend

# Copy package files
COPY client/package*.json ./

# Install dependencies
RUN npm ci

# Copy frontend source
COPY client/ ./

# Build Angular app for production
RUN npm run build

# Stage 2: NGINX runtime
FROM nginx:1.27-alpine

# Copy custom NGINX config
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# Copy Angular build artifacts
COPY --from=frontend-build /frontend/dist/browser/browser /usr/share/nginx/html

EXPOSE 80
