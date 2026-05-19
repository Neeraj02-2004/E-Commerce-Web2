# SpringEcom Backend

SpringEcom is a Spring Boot ecommerce backend API with JWT authentication, Google login support, product management, order placement, Razorpay online payment support, return/exchange request handling, wishlist, PostgreSQL database, Redis cache, Flyway migrations, Docker Compose, health checks, scheduled order automation, and Swagger API documentation.

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

Start the full backend stack:

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

Run tests:

```powershell
.\mvnw.cmd test
```

Run full clean test:

```powershell
.\mvnw.cmd clean test
```

Latest verified result:

```text
57+ tests passing, 0 failures, 0 errors
```

If Docker Desktop is not running, the Testcontainers integration test may be skipped automatically depending on environment.

## Health Check

Health endpoint:

```text
http://localhost:8080/actuator/health
```

Expected response:

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

## User Roles

Supported roles:

- `USER`
- `ADMIN`

By default, registered users get the `USER` role.

Make a user admin in Docker PostgreSQL:

```powershell
docker exec -it springecom-postgres psql -U springecom -d springecom -c "UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@example.com';"
```

After changing role, log out and log in again to get a new JWT token.

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

Maximum product image size:

```text
5MB
```

## Order APIs

Protected endpoints:

- `POST /api/place`
- `GET /api/orders`
- `PUT /api/cancel/{orderId}`

Order placement reduces product stock inside a transaction.

Supported payment modes:

- `CASH_ON_DELIVERY`
- `ONLINE`

Supported order statuses:

- `PLACED`
- `SHIPPED`
- `DELIVERED`
- `CANCELLED`
- `FAILED`

## Razorpay Payment APIs

Protected endpoints:

- `POST /api/payments/create`
- `POST /api/payments/verify`

Online payment flow:

1. User places an order with payment mode `ONLINE`.
2. Backend creates the order with payment status `PENDING`.
3. Frontend calls `/api/payments/create`.
4. Backend creates a Razorpay order.
5. Frontend opens Razorpay Checkout.
6. After successful payment, frontend calls `/api/payments/verify`.
7. Backend verifies Razorpay signature.
8. Backend updates payment status to `PAID`.

Payment statuses:

- `PENDING`
- `PAID`
- `FAILED`

For client/live deployment, Razorpay KYC and payment method activation must be completed in the client's Razorpay business account.

## Order Status Automation

The backend includes automatic order status updates using Spring Scheduler.

Rules:

- `CASH_ON_DELIVERY` + 7 days old + `PLACED` = `DELIVERED`
- `ONLINE` + 7 days old + `PAID` + `PLACED` = `DELIVERED`
- `ONLINE` + 7 days old + `PENDING` = stays `PLACED`
- `ONLINE` + payment `FAILED` = `FAILED`
- `CANCELLED` orders are never changed by the scheduler

When an order becomes `DELIVERED`, the backend stores `deliveredAt`.

The order status scheduler runs daily at 1:00 AM.

```java
@Scheduled(cron = "0 0 1 * * *")
```

For local testing only, it can temporarily be changed to:

```java
@Scheduled(fixedRate = 60000)
```

This runs every 60 seconds.

## Return / Exchange APIs

Users can request return or exchange only after an order is delivered.

User endpoints:

- `POST /api/orders/{orderId}/return-exchange`
- `GET /api/orders/return-exchange`

Admin endpoints:

- `GET /api/admin/return-exchange`
- `PUT /api/admin/return-exchange/{requestId}/approve`
- `PUT /api/admin/return-exchange/{requestId}/reject`
- `PUT /api/admin/return-exchange/{requestId}/complete`

Request types:

- `RETURN`
- `EXCHANGE`

Request statuses:

- `REQUESTED`
- `APPROVED`
- `REJECTED`
- `COMPLETED`

Refund statuses:

- `NOT_REQUIRED`
- `REFUND_PROCESSING`
- `MANUAL_REFUND_REQUIRED`
- `REFUNDED`

Return/exchange rules:

- Only `DELIVERED` orders are eligible.
- Request window is 7 days after delivery.
- User must provide a reason between 10 and 1000 characters.
- A user cannot create multiple active return/exchange requests for the same order.
- Admin can approve, reject, or complete a request.
- For online paid returns, approval sets refund status to `REFUND_PROCESSING`.
- For Cash on Delivery returns, approval sets refund status to `MANUAL_REFUND_REQUIRED`.
- Completing an approved return sets refund status to `REFUNDED`.

Automatic return/exchange completion:

- Approved return/exchange requests are completed automatically after 6 days.
- For approved returns, refund status becomes `REFUNDED`.
- For approved exchanges, request status becomes `COMPLETED`.
- The scheduler runs daily at 1:30 AM.

```java
@Scheduled(cron = "0 30 1 * * *")
```

Refund and exchange timing:

```text
After admin approval, return refunds and exchange requests are automatically completed after 6 days.
```

Important: this backend currently tracks refund status. Real automatic Razorpay refund API integration can be added later if required.

## Wishlist APIs

Protected endpoints:

- `GET /api/wishlist`
- `POST /api/wishlist/{productId}`
- `DELETE /api/wishlist/{productId}`

## Database

Flyway migrations are stored in:

```text
src/main/resources/db/migration
```

Current migrations:

- `V1__create_ecommerce_schema.sql`
- `V2__allow_delivered_order_status.sql`
- `V3__add_order_address_and_payment_mode.sql`
- `V4__add_razorpay_payment_fields.sql`
- `V5__allow_online_payment_mode.sql`
- `V6__allow_failed_order_status.sql`
- `V7__create_return_exchange_requests.sql`

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
- Complete Razorpay KYC and payment method activation in the client's Razorpay account.
- Use live Razorpay keys only in production environment variables.
- Back up PostgreSQL data regularly.
- Store uploaded product images in persistent storage.
- Keep schedulers on cron mode for production.
- Confirm refund business rules with the client before enabling live automatic Razorpay refunds.

## Client Delivery Checklist

Before sending to the client, confirm:

- `git status` is clean.
- GitHub Actions is green.
- `.\mvnw.cmd clean test` passes.
- Docker starts with `docker compose up --build`.
- Health check returns `UP`.
- Swagger opens successfully.
- No real secrets are committed.
- Admin login has been tested.
- Product image upload has been tested.
- Cash on Delivery order placement has been tested.
- Online payment test flow has been tested.
- Payment status updates to `PAID` after successful Razorpay verification.
- Order placement and stock reduction have been tested.
- Order status scheduler tests pass.
- Return/exchange request flow has been tested.
- Return/exchange scheduler tests pass.