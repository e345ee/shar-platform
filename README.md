# SHAR Platform

## Стек

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

SHAR Platform - это веб-образовательная платформа с тестами.

В системе есть несколько ролей: администратор, методист, преподаватель и ученик. Методист создаёт курсы, классы, учебные материалы, тесты и достижения. Преподаватель работает со своими классами, открывает материалы и проверяет ответы учеников. Ученик проходит занятия, выполняет тесты, смотрит результаты и получает достижения.

## Что умеет платформа

- регистрация и вход пользователей;
- управление ролями пользователей;
- создание курсов, классов и уроков;
- загрузка учебных материалов и презентаций;
- создание тестов и заданий;
- прохождение тестов учениками;
- проверка открытых ответов преподавателем;
- статистика по ученикам, классам и курсам;
- достижения и сертификаты;
- хранение файлов через S3-совместимое хранилище.

## Технологии

Backend:

- Java 17;
- Spring Boot 3.2;
- Spring Web;
- Spring Security;
- Spring Data JPA;
- PostgreSQL;
- JWT для авторизации;
- MinIO или другое S3-совместимое хранилище;
- RabbitMQ для демо-интеграции;
- PDFBox для работы с PDF;
- Maven.

Frontend:

- React;
- Create React App;
- JavaScript;
- CSS;
- Framer Motion;
- Nginx для отдачи собранного frontend.

Инфраструктура:

- Docker;
- Docker Compose;
- Nginx gateway;
- PostgreSQL 15;
- MinIO;
- RabbitMQ;
- GitHub Actions для CI/CD.

## Структура проекта

- `backend/` - серверная часть на Spring Boot;
- `front/` - клиентская часть на React;
- `db/init/` - схема базы и начальные данные;
- `db/seed/` - дополнительные наборы тестовых данных;
- `infra/nginx/` - конфигурация Nginx;
- `infra/certbot/` - скрипты для TLS-сертификатов;
- `docker-compose.yml` - локальный запуск;
- `compose.prod.yml` - запуск ближе к production.

## Запуск через Docker

Нужны Docker и Docker Compose v2.

1. Скопируйте пример настроек:

```bash
cp .env.example .env
```

2. Запустите проект:

```bash
docker compose up -d --build
```

3. Откройте приложение:

```text
http://localhost:3000
```

Backend будет доступен на:

```text
http://localhost:8080
```

MinIO будет доступен на:

```text
http://localhost:9001
```

Остановить проект:

```bash
docker compose down
```

Если нужно полностью пересоздать базу вместе с начальными данными:

```bash
docker compose down -v
docker compose up -d --build
```

## Production-запуск через Docker

Для сервера используется `compose.prod.yml`. В этом варианте frontend собирается в gateway-контейнер, а наружу открываются только HTTP и HTTPS.

1. Подготовьте файл настроек:

```bash
cp .env.example .env.prod
```

2. В `.env.prod` задайте основные переменные:

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

В папке из `TLS_CERTS_DIR` должны лежать файлы:

```text
fullchain.pem
privkey.pem
```

3. Запустите:

```bash
docker compose --env-file .env.prod -f compose.prod.yml up -d --build
```

4. Проверьте состояние:

```bash
curl -k https://localhost/healthz
curl -k https://localhost/actuator/health
```

Если сертификата ещё нет, можно использовать готовые скрипты:

```bash
bash deploy.sh up
bash deploy.sh issue-le DOMAIN=your-domain.ru CERTBOT_EMAIL=mail@example.com
```

## Запуск backend через WildFly

Backend можно собрать как WAR-файл и развернуть в WildFly. Нужен WildFly с поддержкой Jakarta EE 10, JDK 17 и Maven.

PostgreSQL и MinIO всё равно должны быть запущены отдельно. Для локальной разработки их можно поднять через Docker:

```bash
docker compose up -d postgres minio minio_init
```

Перед запуском WildFly задайте переменные окружения. Если база и MinIO подняты через локальный `docker-compose.yml`, подойдут такие значения:

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

Соберите backend:

```bash
cd backend
mvn clean package -DskipTests
```

Скопируйте WAR в WildFly:

```bash
cp target/course-management-1.0.0.war "$WILDFLY_HOME/standalone/deployments/ROOT.war"
```

Запустите WildFly:

```bash
"$WILDFLY_HOME/bin/standalone.sh" -b 0.0.0.0
```

Проверка backend:

```bash
curl http://localhost:8080/actuator/health
```

Если WAR развёрнут не как `ROOT.war`, то у backend появится context path. В этом случае `REACT_APP_API_URL` для frontend должен включать этот путь.

Frontend при запуске через WildFly собирается отдельно:

```bash
cd front
npm ci
REACT_APP_API_URL=http://localhost:8080 npm run build
```

После сборки папку `front/build` нужно отдавать через Nginx или другой статический веб-сервер.

## Начальный вход

При первом запуске создаётся администратор:

```text
логин: admin
пароль: admin
```

После входа пароль лучше сразу поменять.

## Полезные команды

Запустить backend-тесты:

```bash
cd backend
mvn test
```

Запустить frontend в режиме разработки:

```bash
cd front
npm install
npm start
```

Собрать frontend:

```bash
cd front
npm ci
npm run build
```
