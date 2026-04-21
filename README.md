# RailWay Pro - Railway Booking System

RailWay Pro is a Spring Boot based railway booking platform inspired by modern Indian railway user flows (ticket booking, PNR status, and live train status).

## Tech Stack

- Java 17 (build target)
- Spring Boot 3.2.x
- Spring Security
- Spring Data MongoDB
- Thymeleaf
- Maven
- Docker + Docker Compose

## Key Features

- Train search by source, destination, and date
- Ticket booking with passenger details
- PNR lookup (authenticated and ownership protected)
- Razorpay order flow with callback signature verification
- Ticket cancellation and refund flow
- Admin panel for train/user/booking operations
- Rate limiting on high-risk endpoints
- Actuator health and Prometheus metrics endpoints

## Project Structure

- src/main/java: application code (controllers, services, dao, config)
- src/main/resources/templates: Thymeleaf views
- src/main/resources/static: CSS/JS/images
- src/test/java: unit tests

## Prerequisites

- JDK 17+ (tested with newer JDK runtime too)
- Maven 3.9+
- MongoDB (local or cloud)

## Environment Variables

Set these before running in non-demo mode:

- MONGODB_URI
- MONGODB_DATABASE
- RAZORPAY_KEY_ID
- RAZORPAY_KEY_SECRET
- DB_URL (prod profile)
- DB_USERNAME (prod profile)
- DB_PASSWORD (prod profile)
- APP_DATASET_IMPORT_ENABLED (optional, true/false)
- APP_DATASET_TRAINS_FILE (optional path)
- APP_DATASET_SCHEDULES_FILE (optional path)

### MongoDB Atlas Setup (PowerShell)

Set these in the same terminal before running:

```powershell
$env:MONGODB_URI="mongodb+srv://<username>:<password>@<cluster-url>/railway_pro?retryWrites=true&w=majority&appName=RailWayPro&connectTimeoutMS=3000&serverSelectionTimeoutMS=5000&socketTimeoutMS=5000"
$env:MONGODB_DATABASE="railway_pro"
```

Then start the app:

```powershell
mvn spring-boot:run
```

## Import Full Railway Dataset

The workspace already includes dataset files at:

- data/kaggle/trains.json
- data/kaggle/stations.json
- data/kaggle/schedules.json

To import/upsert the full train dataset into MongoDB on startup:

```powershell
$env:APP_DATASET_IMPORT_ENABLED="true"
mvn spring-boot:run
```

Notes:

- Importer uses trains and schedules files for train + route stop data.
- Import is idempotent by train number (existing records are updated).
- Built-in demo train seeding is skipped while dataset import is enabled.

## Run Locally

1. Build:

```bash
mvn clean compile
```

2. Run app:

```bash
$env:MONGODB_URI="mongodb+srv://chethan:YOUR_REAL_PASSWORD@node1.kowqldc.mongodb.net/railway_pro?retryWrites=true&w=majority&appName=Node1"
$env:MONGODB_DATABASE="railway_pro"
mvn spring-boot:run
```

3. Open:

- http://localhost:8080
- http://localhost:8080/actuator/health
- http://localhost:8080/actuator/prometheus

## Run Tests

```bash
mvn test
```

## Run with Docker Compose

```bash
docker compose up --build
```

This starts:

- mongodb service
- railway-app service

## CI

GitHub Actions workflow is configured at:

- .github/workflows/ci.yml

It runs Maven clean test on push and pull requests to main/master.

## Production Notes

- Do not commit real secrets.
- Use env vars or a secrets manager.
- Keep Spring profile set to prod in production deployments.
- Use HTTPS and a reverse proxy (Nginx/Cloud LB) in front of the app.

## License

Internal/academic project unless specified otherwise.
