# Stage 1: Build the React + Vite frontend application
FROM node:20-alpine AS builder

LABEL stage="builder"

WORKDIR /app

# Copy dependency specifications first to leverage build cache
COPY package*.json ./

# Install exact dependencies
RUN npm ci

# Copy project source files
COPY . .

# Build production bundle into dist/ directory
RUN npm run build

# Stage 2: Serve optimized static files with Nginx
FROM nginx:alpine AS runtime

# Metadata labels
LABEL maintainer="DevSecOps Team" \
      version="1.0.0" \
      description="Self-Service Password Reset (SSPR) Frontend UI"

# Install curl for health checking
RUN apk add --no-cache curl

# Remove default nginx static html files
RUN rm -rf /usr/share/nginx/html/*

# Copy built production assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx reverse proxy configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose HTTP port
EXPOSE 80

# Configure health check for the web server
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:80/ || exit 1

# Run Nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
