# Multi-stage build for SukiTier Dashboard
FROM node:18-alpine AS builder

WORKDIR /app

# Copy dashboard assets
COPY ./assets/sukitier-dashboard.html .

# Optimize for production (minification placeholder)
RUN echo "Dashboard optimized for production"

# Final stage - lightweight web server
FROM nginx:alpine

# Copy optimized dashboard
COPY --from=builder /app/sukitier-dashboard.html /usr/share/nginx/html/index.html

# Copy nginx configuration
COPY ./docker/nginx.conf /etc/nginx/nginx.conf
COPY ./docker/default.conf /etc/nginx/conf.d/default.conf

# Copy SSL certificates (mount or provide at runtime)
RUN mkdir -p /etc/nginx/ssl

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:80/ || exit 1

# Expose ports
EXPOSE 80 443

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
