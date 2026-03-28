# GodLeiBlog Aliyun ECS Deployment Package

This package is prepared for a typical Alibaba Cloud ECS Linux server.

Recommended environment:

- Nginx 1.20+
- JDK 17 or JDK 21
- MySQL 8.0+
- `unzip`

Suggested target paths:

- Application root: `/opt/godleiblog`
- Nginx config: `/etc/nginx/conf.d/godleiblog.conf`
- systemd service: `/etc/systemd/system/godleiblog-backend.service`

## Package contents

- `backend/app.jar`: Spring Boot backend
- `backend/backend.env.example`: backend environment variable template
- `www/`: frontend static files
- `www/admin/`: admin panel static files
- `nginx/godleiblog.conf`: Nginx reverse proxy config example
- `systemd/godleiblog-backend.service`: backend service example
- `scripts/*.sh`: Linux install and run scripts
- `sql/*.sql`: database initialization scripts

## Deployment steps

1. Upload the zip package to the server and unzip it.
2. Run:

```bash
cd /path/to/GodLeiBlog-aliyun-ecs-*
chmod +x scripts/*.sh
sudo bash scripts/install.sh
```

3. Edit the backend environment file:

```bash
sudo vim /opt/godleiblog/backend/backend.env
```

4. Import the SQL files in order:

```bash
mysql -uroot -p godleiblog < /opt/godleiblog/sql/GodLeiBlog建表.sql
mysql -uroot -p godleiblog < /opt/godleiblog/sql/mysql-site-config-media.sql
```

5. Check and adjust the Nginx config if your domain or Twikoo target is different:

```bash
sudo vim /etc/nginx/conf.d/godleiblog.conf
sudo nginx -t
sudo systemctl reload nginx
```

6. Start the backend:

```bash
sudo systemctl enable --now godleiblog-backend
sudo systemctl status godleiblog-backend
```

## Backend config

The backend reads `/opt/godleiblog/backend/backend.env` through `systemd` or the shell scripts.

Key variables:

- `HM_LOCAL_DATASOURCE_URL`
- `HM_LOCAL_DB_USERNAME`
- `HM_DB_PASSWORD`
- `HM_DB_HOST`
- `HM_LOCAL_UPLOAD_DIR`
- `SPRING_PROFILES_ACTIVE`
- `SERVER_PORT`
- `JAVA_OPTS`

## Manual start without systemd

```bash
cd /opt/godleiblog
chmod +x scripts/*.sh
./scripts/start-backend.sh
./scripts/status-backend.sh
./scripts/stop-backend.sh
```

## Notes

- The default package path assumes `/opt/godleiblog`.
- Frontend and admin panel use same-origin `/api`, `/uploads`, and `/twikoo-proxy`.
- If you are not using Twikoo, you can remove the `/twikoo-proxy` block from the Nginx config.
