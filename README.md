# SpringEcom Backend

SpringEcom is a Spring Boot ecommerce backend API with JWT authentication, Google login support, product management, order placement, wishlist, PostgreSQL database, Redis cache, Flyway migrations, Docker Compose, health checks, and Swagger API documentation.

## Tech Stack

- Java 21
- Spring Boot 3.3.4
- Spring Security with JWT
- PostgreSQL
- Redis
- Flyway
- Docker Compose
- Swagger / OpenAPI
- Maven Wrapper
- Testcontainers

## Requirements

For local development:

- Java 21+
- Maven wrapper included in this project
- PostgreSQL
- Redis
- Docker Desktop optional for normal local run

For Docker setup:

- Docker Desktop

## Environment Variables

Create a `.env` file in the project root.

Use `.env.example` as reference.

```env
DB_PASSWORD=change-me-strong-db-password

GOOGLE_CLIENT_ID=change-me-google-client-id
GOOGLE_CLIENT_SECRET=change-me-google-client-secret

JWT_SECRET=change-me-base64-256-bit-secret

CORS_ORIGINS=http://localhost:5173