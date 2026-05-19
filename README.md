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
- Razorpay Payment Gateway

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

RAZORPAY_KEY_ID=change-me-razorpay-key-id
RAZORPAY_KEY_SECRET=change-me-razorpay-key-secret
RAZORPAY_WEBHOOK_SECRET=change-me-razorpay-webhook-secret
RAZORPAY_CURRENCY=INR
```

Important: never commit the real `.env` file to GitHub.

## Run With Docker

```powershell
docker compose up --build
```

API URL:

```text
http://localhost:8080
```

Stop containers:

```powershell
docker compose down
```

Stop containers and remove volumes:

```powershell
docker compose down -v
```

## Run Tests

```powershell
.\mvnw.cmd test
```

If Docker Desktop is not running, the Testcontainers integration test is skipped automatically.

## Health Check

```text
http://localhost:8080/actuator/health
```

Expected:

```json
{"status":"UP"}
```

## Swagger API Docs

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Authentication APIs

Public endpoints:

- `POST /api/register`
- `POST /api/login`
- `POST /api/login/google`

Use the JWT token in protected requests:

```text
Authorization: Bearer <token>
```

## Product APIs

Public endpoints:

- `GET /api/products`
- `GET /api/product/{id}`
- `GET /api/products/search?keyword=phone`
- `GET /api/product-images/{filename}`

Admin-only endpoints:

- `POST /api/admin/product`
- `PUT /api/admin/product/{id}`
- `DELETE /api/admin/product/{id}`

Admin product create/update uses multipart form data:

- `product`: JSON product data
- `imageFile`: product image file

Allowed image types:

- JPG
- PNG
- WEBP

Maximum product image size: 5MB.

## Order APIs

Protected endpoints:

- `POST /api/place`
- `GET /api/orders`
- `PUT /api/cancel/{orderId}`

Order placement reduces product stock inside a transaction.

## Wishlist APIs

Protected endpoints:

- `GET /api/wishlist`
- `POST /api/wishlist/{productId}`
- `DELETE /api/wishlist/{productId}`

## Admin Role

By default, registered users get the `USER` role.

Make a user admin in Docker PostgreSQL:

```powershell
docker exec -it springecom-postgres psql -U springecom -d springecom -c "UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@example.com';"
```

After changing role, log out and log in again to get a new JWT token.

## Database

Flyway migrations are stored in:

```text
src/main/resources/db/migration
```

Current migrations:

- `V1__create_ecommerce_schema.sql`
- `V2__allow_delivered_order_status.sql`

The app uses:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

That means the database schema must match Flyway migrations.

## Product Images

Uploaded product images are stored in:

```text
uploads/products
```

In Docker, product images are stored in the volume:

```text
springecom-product-images
```

## Useful Docker Commands

Start database and Redis only:

```powershell
docker compose up -d postgres redis
```

View running containers:

```powershell
docker ps
```

View API logs:

```powershell
docker logs -f springecom-api
```

Open PostgreSQL shell:

```powershell
docker exec -it springecom-postgres psql -U springecom -d springecom
```

Open Redis shell:

```powershell
docker exec -it springecom-redis redis-cli
```

Clear Redis cache:

```powershell
docker exec -it springecom-redis redis-cli FLUSHALL
```

## Production Notes

Before production delivery:

- Rotate all real secrets.
- Use strong `DB_PASSWORD` and `JWT_SECRET`.
- Do not commit `.env`.
- Keep database and Redis ports private in production.
- Configure real frontend URL in `CORS_ORIGINS`.
- Back up PostgreSQL data regularly.
- Store uploaded product images in persistent storage.

## Client Delivery Checklist

Before sending to the client, confirm:

- `git status` is clean.
- GitHub Actions is green.
- `.\mvnw.cmd test` passes.
- Docker starts with `docker compose up --build`.
- Health check returns `UP`.
- Swagger opens successfully.
- No real secrets are committed.
- Admin login has been tested.
- Product image upload has been tested.
- Order placement and stock reduction have been tested.