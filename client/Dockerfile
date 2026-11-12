# Use Node.js LTS
FROM node:22-alpine

# Set working directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy application code
COPY . .

# Expose port
EXPOSE 4200

# Start development server bound to all interfaces (required for Docker)
CMD ["npm", "start", "--", "--host", "0.0.0.0"]
