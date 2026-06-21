# Monitoring Setup for Task Manager API

This document summarizes the Prometheus + Grafana monitoring configuration added for Phase 1.

## Files Created

### 1. `monitoring/prometheus.yml`
Prometheus configuration to scrape Spring Boot Actuator metrics.
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-actuator'
    metrics_path: '/api/v1/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8081']  # For Docker on Windows/Mac host
```

### 2. `monitoring/docker-compose.yml`
Docker Compose file to run Prometheus and Grafana.
```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
    ports:
      - "9090:9090"
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - ./grafana/provisioning/datasources:/etc/grafana/provisioning/datasources:ro
      - ./grafana/provisioning/dashboards:/etc/grafana/provisioning/dashboards:ro
    env:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    restart: unless-stopped
```

### 3. `monitoring/grafana/provisioning/datasources/datasource.yml`
Grafana datasource provisioning for Prometheus.
```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus:9090
    access: proxy
    isDefault: true
    editable: false
```

### 4. `monitoring/grafana/provisioning/dashboards/spring-boot-dashboard.json`
A simple dashboard showing JVM memory usage and HTTP request rate.
```json
{
  "dashboard": {
    "id": null,
    "title": "Spring Boot Actuator Metrics",
    "timezone": "browser",
    "schemaVersion": 16,
    "version": 0,
    "refresh": "10s",
    "panels": [
      {
        "type": "graph",
        "title": "JVM Memory Used",
        "gridPos": { "x": 0, "y": 0, "width": 12, "height": 8 },
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area!=\"nonheap\"}",
            "legendFormat": "{{area}}"
          }
        ]
      },
      {
        "type": "graph",
        "title": "HTTP Request Rate (per second)",
        "gridPos": { "x": 12, "y": 0, "width": 12, "height": 8 },
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[1m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ]
      }
    ]
  },
  "overwrite": true
}
```

## How to Test Locally

1. **Ensure the backend is running** on `localhost:8081` (the actuator endpoint is available at `http://localhost:8081/api/v1/actuator/prometheus`).
2. **Start the monitoring stack**:
   ```bash
   cd monitoring
   docker compose up -d
   ```
3. **Verify Prometheus**:
   - Open <http://localhost:9090/targets>
   - Check that the `spring-actuator` job is `UP`.
4. **Access Grafana**:
   - Open <http://localhost:3000>
   - Login with `admin` / `admin`
   - The "Spring Boot Actuator Metrics" dashboard should be available (or you can import it from the provisioned dashboards).

## Notes for Homelab Deployment

- When deploying to your homelab via Docker Compose, you will need to adjust the `targets` in `prometheus.yml` to point to the backend service (e.g., `backend:8081` if both are in the same Docker network, or the appropriate IP/hostname).
- Ensure the monitoring stack is on the same Docker network as the backend (or expose the backend port and adjust the target accordingly).
- You can extend this setup with alerting rules and more detailed dashboards as you progress.

--- 
*Created as part of Phase 1: Prometheus + Grafana monitoring.*