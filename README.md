# SHAR Platform

## Stack

![Java](https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=0B1220)
![JavaScript](https://img.shields.io/badge/JavaScript-ES6-F7DF1E?style=for-the-badge&logo=javascript&logoColor=0B1220)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![MinIO](https://img.shields.io/badge/MinIO-S3%20storage-C72E49?style=for-the-badge&logo=minio&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-Gateway-009639?style=for-the-badge&logo=nginx&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

SHAR Platform is a web-based learning platform with tests.

The system has several roles: administrator, methodologist, teacher, and student. A methodologist creates courses, classes, learning materials, tests, and achievements. A teacher works with assigned classes, opens materials, and reviews student answers. A student completes lessons and tests, views results, and earns achievements.

## Platform Features

- user registration and sign-in;
- user role management;
- course, class, and lesson creation;
- learning material and presentation uploads;
- test and assignment creation;
- student test taking;
- teacher review of open-ended answers;
- statistics for students, classes, and courses;
- achievements and certificates;
- file storage through an S3-compatible storage service.

## Technologies

Backend:

- Java 17;
- Spring Boot 3.2;
- Spring Web;
- Spring Security;
- Spring Data JPA;
- PostgreSQL;
- JWT for authorization;
- MinIO or another S3-compatible storage service;
- RabbitMQ for demo integration;
- PDFBox for working with PDF files;
- Maven.

Frontend:

- React;
- Create React App;
- JavaScript;
- CSS;
- Framer Motion;
- Nginx for serving the built frontend.

Infrastructure:

- Docker;
- Docker Compose;
- Nginx gateway;
- PostgreSQL 15;
- MinIO;
- RabbitMQ;
- GitHub Actions for CI/CD.

## Project Structure

- `backend/` - Spring Boot server application;
- `front/` - React client application;
- `db/init/` - database schema and initial data;
- `db/seed/` - additional test data sets;
- `infra/nginx/` - Nginx configuration;
- `infra/certbot/` - TLS certificate scripts;
- `docker-compose.yml` - local run configuration;
- `compose.prod.yml` - production-like run configuration.

## Running with Docker

Docker and Docker Compose v2 are required.

1. Copy the sample configuration:

```bash
cp .env.example .env
```

2. Start the project:

```bash
docker compose up -d --build
```

3. Open the application:

```text
http://localhost:3000
```

The backend will be available at:

```text
http://localhost:8080
```

MinIO will be available at:

```text
http://localhost:9001
```

Stop the project:

```bash
docker compose down
```

To fully recreate the database together with the initial data:

```bash
docker compose down -v
docker compose up -d --build
```

## Production Docker Run

The server uses `compose.prod.yml`. In this mode, the frontend is built into the gateway container, and only HTTP and HTTPS are exposed externally.

1. Prepare the configuration file:

```bash
cp .env.example .env.prod
```

2. Set the main variables in `.env.prod`:

```env
REACT_APP_API_URL=https://your-domain.ru
APP_S3_PUBLIC_URL=https://your-domain.ru/api/files
TLS_CERTS_DIR=/path/to/certs
POSTGRES_DB=course_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
```

The directory referenced by `TLS_CERTS_DIR` must contain these files:

```text
fullchain.pem
privkey.pem
```

3. Start the stack:

```bash
docker compose --env-file .env.prod -f compose.prod.yml up -d --build
```

4. Check the status:

```bash
curl -k https://localhost/healthz
curl -k https://localhost/actuator/health
```

If there is no certificate yet, you can use the included scripts:

```bash
bash deploy.sh up
bash deploy.sh issue-le DOMAIN=your-domain.ru CERTBOT_EMAIL=mail@example.com
```

## Running the Backend with WildFly

The backend can be built as a WAR file and deployed to WildFly. WildFly with Jakarta EE 10 support, JDK 17, and Maven are required.

PostgreSQL and MinIO must still be running separately. For local development, you can start them with Docker:

```bash
docker compose up -d postgres minio minio_init
```

Before starting WildFly, set the environment variables. If the database and MinIO are running through the local `docker-compose.yml`, these values will work:

```bash
export SERVER_PORT=8080
export DB_HOST=localhost
export DB_PORT=5433
export DB_NAME=course_db
export DB_SSLMODE=disable
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export APP_S3_HOST=localhost
export APP_S3_PORT=9000
export APP_S3_PUBLIC_HOST=localhost
export APP_S3_PUBLIC_PORT=9000
export APP_S3_REGION=us-east-1
export APP_S3_ACCESS_KEY=minioadmin
export APP_S3_SECRET_KEY=minioadmin
export APP_S3_BUCKET=avatars
export APP_MAIL_ENABLED=false
```

Build the backend:

```bash
cd backend
mvn clean package -DskipTests
```

Copy the WAR file to WildFly:

```bash
cp target/course-management-1.0.0.war "$WILDFLY_HOME/standalone/deployments/ROOT.war"
```

Start WildFly:

```bash
"$WILDFLY_HOME/bin/standalone.sh" -b 0.0.0.0
```

Backend health check:

```bash
curl http://localhost:8080/actuator/health
```

If the WAR is not deployed as `ROOT.war`, the backend will have a context path. In that case, `REACT_APP_API_URL` for the frontend must include that path.

When using WildFly, the frontend is built separately:

```bash
cd front
npm ci
REACT_APP_API_URL=http://localhost:8080 npm run build
```

After the build, the `front/build` directory should be served through Nginx or another static web server.

## Initial Login

On the first run, an administrator account is created:

```text
login: admin
password: admin
```

After signing in, it is best to change the password immediately.

## Useful Commands

Run backend tests:

```bash
cd backend
mvn test
```

Run the frontend in development mode:

```bash
cd front
npm install
npm start
```

Build the frontend:

```bash
cd front
npm ci
npm run build
```
