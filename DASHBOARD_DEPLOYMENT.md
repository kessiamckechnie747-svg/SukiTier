# SukiTier Dashboard Deployment Guide

## Overview
This guide provides comprehensive instructions for deploying the SukiTier AI Dashboard across different environments.

## Prerequisites
- Docker & Docker Compose (for containerized deployment)
- Node.js 18+ (for local development)
- nginx 1.21+ (for standalone deployment)
- SSL certificates (for production)

---

## Deployment Methods

### 1. Docker Deployment (Recommended)

#### Quick Start
```bash
# Clone/setup repository
cd /home/kessiathecreator/SukiSU\ Tier

# Generate self-signed certificates (development only)
mkdir -p certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout certs/key.pem -out certs/cert.pem

# Build and run with Docker Compose
docker-compose up -d
```

The dashboard will be available at:
- HTTP: `http://localhost`
- HTTPS: `https://localhost` (self-signed)

#### Production Deployment
1. **Generate valid SSL certificates** (Let's Encrypt, commercial CA, etc.)
   ```bash
   # Using Certbot for Let's Encrypt
   certbot certonly --standalone -d your-domain.com
   cp /etc/letsencrypt/live/your-domain.com/fullchain.pem certs/cert.pem
   cp /etc/letsencrypt/live/your-domain.com/privkey.pem certs/key.pem
   ```

2. **Update docker-compose.yml environment**
   ```yaml
   environment:
     - NGINX_HOST=your-domain.com
     - NGINX_PORT=443
   ```

3. **Configure backend API endpoint**
   Update `assets/sukitier-dashboard.html` API configuration:
   ```javascript
   const config = {
       api: {
           reasoning: "https://api.your-domain.com/reasoning/v1",
           embeddings: "https://api.your-domain.com/embeddings/v1"
       }
   };
   ```

4. **Deploy**
   ```bash
   docker-compose -f docker-compose.yml up -d
   ```

---

### 2. Standalone nginx Deployment

#### Setup
```bash
# Copy dashboard files
mkdir -p /var/www/sukitier
cp assets/sukitier-dashboard.html /var/www/sukitier/index.html

# Copy nginx config
sudo cp docker/nginx.conf /etc/nginx/nginx.conf
sudo cp docker/default.conf /etc/nginx/conf.d/default.conf

# Create SSL directory
sudo mkdir -p /etc/nginx/ssl
sudo cp certs/* /etc/nginx/ssl/

# Test and reload
sudo nginx -t
sudo systemctl reload nginx
```

#### Enable Service
```bash
sudo systemctl enable nginx
sudo systemctl start nginx
```

---

### 3. Linux Server Deployment

#### Using Python Simple HTTP Server (Development)
```bash
cd assets
python3 -m http.server 8000
# Dashboard available at http://localhost:8000
```

#### Using Node.js http-server
```bash
npm install -g http-server
cd assets
http-server -p 8000 -c-1
# Dashboard available at http://localhost:8000
```

---

### 4. Android Integration

#### WebView Integration
The dashboard is integrated into the Android app via `DashboardActivity` and `DashboardWebViewService`.

```kotlin
// In MainActivity.kt or navigation
Intent(context, DashboardActivity::class.java).apply {
    startActivity(this)
}
```

#### File Placement
Place `sukitier-dashboard.html` in:
- `app/src/main/assets/` (for WebView loading)
- Or serve from server with `https://your-domain.com/dashboard`

---

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `NGINX_HOST` | `localhost` | Server hostname |
| `NGINX_PORT` | `80` | HTTP port |
| `API_REASONING_URL` | `https://api.sukitier.ai/reasoning/v1` | Reasoning engine API |
| `API_EMBEDDINGS_URL` | `https://api.sukitier.ai/embeddings/v1` | Embeddings API |
| `TZ` | `UTC` | Timezone |

### Performance Tuning

#### nginx Worker Processes
```nginx
worker_processes auto;  # Use available CPU cores
worker_connections 2048;  # Connections per worker
```

#### Gzip Compression
Already enabled for:
- Text files (HTML, CSS, JavaScript)
- JSON responses
- SVG images

#### Rate Limiting
- General endpoints: 10 req/s
- API endpoints: 30 req/s
- Configurable in `docker/default.conf`

---

## Monitoring

### Health Checks
```bash
# Check dashboard
curl https://localhost/health

# Check backend
curl https://localhost/api/health
```

### Logs
```bash
# Docker logs
docker-compose logs -f sukitier-dashboard

# nginx logs (standalone)
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

### Metrics
- **Client inference speed**: < 50ms (target)
- **API response time**: < 200ms (target)
- **Memory usage**: < 128MB (client)
- **GPU utilization**: < 85% (server)

---

## Security Considerations

### SSL/TLS
- Use TLS 1.2 or higher
- Certificate pinning (Android)
- HSTS headers enabled

### CSP (Content Security Policy)
```
default-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com
script-src 'self' 'unsafe-inline' https://cdn.* https://cdn.jsdelivr.net
```

### CORS Headers
Configured in nginx for API requests.

### Rate Limiting
- Prevent brute force attacks
- DoS protection enabled

---

## Troubleshooting

### Dashboard not loading
```bash
# Check Docker container
docker-compose logs sukitier-dashboard

# Check nginx config
sudo nginx -t

# Verify file permissions
ls -la assets/sukitier-dashboard.html
```

### API connection errors
```bash
# Test API endpoint
curl -v https://api.sukitier.ai/reasoning/v1

# Check network connectivity
docker exec sukitier-dashboard curl -v http://sukitier-backend:8080/health
```

### Performance issues
```bash
# Check nginx stats
ngxtop -l /var/log/nginx/access.log

# Monitor resource usage
docker stats sukitier-dashboard
```

---

## Updating

### Docker Deployment
```bash
# Pull latest changes
git pull origin main

# Rebuild image
docker-compose build --no-cache

# Restart services
docker-compose down
docker-compose up -d
```

### File Updates
```bash
# Update dashboard
cp assets/sukitier-dashboard.html /var/www/sukitier/index.html

# Reload nginx
sudo systemctl reload nginx
```

---

## Backup & Recovery

### Backup Dashboard
```bash
# Backup configuration
tar -czf sukitier-backup-$(date +%Y%m%d).tar.gz \
  docker/nginx.conf docker/default.conf certs/

# Backup HTML
cp assets/sukitier-dashboard.html \
  assets/sukitier-dashboard-backup-$(date +%Y%m%d).html
```

### Restore
```bash
# Restore from backup
tar -xzf sukitier-backup-YYYYMMDD.tar.gz

# Verify and reload
sudo nginx -t
docker-compose restart
```

---

## Performance Optimization

### Caching Strategy
- Static assets: 30 days
- HTML: 1 hour
- API responses: Dynamic (based on backend)

### Compression
- gzip enabled (6 compression level)
- ~70% reduction in JavaScript/CSS

### CDN Integration
Update HTML to use CDN endpoints:
```html
<script src="https://cdn.tailwindcss.com"></script>
<script src="https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js"></script>
```

---

## Support & Troubleshooting

For issues:
1. Check logs: `docker-compose logs -f`
2. Verify network: `curl http://localhost/health`
3. Review configuration: `sudo nginx -T`
4. Check dashboard console: Browser DevTools → Console

---

## Additional Resources

- [nginx Documentation](https://nginx.org/en/)
- [Docker Documentation](https://docs.docker.com/)
- [Tailwind CSS](https://tailwindcss.com/)
- [Chart.js](https://www.chartjs.org/)
- [Axios](https://axios-http.com/)

---

**Last Updated**: January 27, 2026
**Dashboard Version**: 1.0.0
