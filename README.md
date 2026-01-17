# Scrum AI Assistant (Spring Boot)

Backend service for Scrum AI Assistant, built with Java 17 and Spring Boot 3.

## Prerequisites

- Java 17
- Maven (or use `./mvnw` provided in Docker, or local install)
- Docker & Docker Compose (for Database)

## Setup & Run

### 1. Start Database
This project requires PostgreSQL. You can start it using Docker Compose:

```bash
docker-compose up -d postgres
```

### 2. Build & Run Application
Use your Maven installation to run the app:

```bash
# Replace with your Maven path if not in $PATH
/Users/yuvaraj/downloads/apache-maven-3.9.5/bin/mvn clean spring-boot:run
```

The application will start at `http://localhost:8080`.

## API Documentation
Once running, access Swagger UI at:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Project Structure
- `com.scrumaiassistant`
  - `controller`: API Endpoints
  - `service`: Business Logic
  - `repository`: DB Access
  - `model`: JPA Entities
  - `integration`: Mock integration for Jira/Slack/AI

## Features
- **Meeting Management**: Upload audio/transcript, extract items.
- **AI Extraction**: Dummy implementation (ready for LLM integration).
- **Task Management**: Create tasks and sync with Jira/Azure (Mock).
- **Scheduler**: Daily 9 AM job to check task status.
