# SpringEcom Backend

SpringEcom is a Spring Boot ecommerce backend API with JWT authentication, Google login, role-based access control, product management, order placement, Razorpay payment/refund support, return/exchange handling, wishlist, PostgreSQL, Redis cache, Redis-backed rate limiting, Redis scheduler locks, Flyway migrations, Docker Compose, health checks, Cloudinary product image storage, scheduled automation, and Swagger API documentation.

## Tech Stack

- Java 21
- Spring Boot 3.3.4
- Spring Security with JWT
- PostgreSQL
- Redis
- Flyway
- Docker Compose
- Cloudinary
- Razorpay
- Swagger / OpenAPI
- Maven Wrapper
- Testcontainers

## Requirements

For local development:

- Java 21+
- Maven wrapper included
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
JWT_EXPIRATION_MS=3600000

CORS_ORIGINS=http://localhost:5173
RATE_LIMIT_TRUSTED_PROXIES=

RAZORPAY_KEY_ID=change-me-razorpay-key-id
RAZORPAY_KEY_SECRET=change-me-razorpay-key-secret
RAZORPAY_WEBHOOK_SECRET=change-me-razorpay-webhook-secret
RAZORPAY_CURRENCY=INR

REDIS_PASSWORD=change-me-strong-redis-password

# Image storage: local or cloudinary
STORAGE_TYPE=local
PRODUCT_IMAGES_DIR=/app/uploads/products

CLOUDINARY_CLOUD_NAME=change-me-cloudinary-cloud-name
CLOUDINARY_API_KEY=change-me-cloudinary-api-key
CLOUDINARY_API_SECRET=change-me-cloudinary-api-secret
CLOUDINARY_FOLDER=springecom/products
```

Important:

- Never commit the real `.env` file.
- Never commit real Cloudinary, Razorpay, Google, database, Redis, or JWT secrets.
- For production, set `STORAGE_TYPE=cloudinary`.
- If any real secret was exposed during development, rotate it before production.

## Deployment Scope

This backend is production-ready for single-server Docker deployment and prepared for multi-server API scaling when shared infrastructure is used.

The production Docker Compose setup is designed for:

- One API container
- One PostgreSQL container
- One Redis container
- One persistent Docker volume for PostgreSQL data
- Redis-backed cache, rate limiting, and scheduler locking
- Cloudinary-backed product image storage

For multi-server or horizontally scaled production:

- Use managed PostgreSQL or a dedicated database server.
- Use shared Redis accessible from all API servers inside a private network.
- Use Cloudinary for product image storage.
- Put the API behind a trusted reverse proxy or load balancer.
- Configure `RATE_LIMIT_TRUSTED_PROXIES` with only trusted proxy/load-balancer IP addresses.
- Keep all database and Redis ports private.

Do not run multiple API containers with separate local product image folders.

## Run With Docker

Start the local backend stack:

```powershell
docker compose up --build
```

Local Docker Compose runs the API with the `local` Spring profile so localhost
frontend origins such as `http://localhost:5173` work during development.

Start the production-style stack:

```powershell
docker compose -f docker-compose.prod.yml up --build -d
```

The production compose file exposes only the API port. PostgreSQL and Redis stay private inside Docker.

Production compose requires `REDIS_PASSWORD` and Cloudinary values in `.env`.
Production compose uses the `prod` Spring profile and requires a real frontend
domain in `CORS_ORIGINS`; localhost origins are intentionally rejected in
production.

API URL:

```text
http://localhost:8080
```

Stop containers:

```powershell
docker compose down
```

Do not run this in production unless you intentionally want to remove database and Redis volumes:

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

Recommended Windows clean test command for this project:

```powershell
.\scripts\run-clean-tests.ps1
```

Latest verified result:

```text
Tests run: 127, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

If Docker Desktop is not running or not accessible, Testcontainers integration tests may fail or skip depending on environment.

## Health Check

Health endpoint:

```text
http://localhost:8080/actuator/health
```

PowerShell check:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health -UseBasicParsing
```

Expected result:

```text
StatusCode: 200
{"status":"UP"}
```

## Swagger API Docs

Swagger is for local/admin verification. It is disabled in the production profile.

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

Use JWT token in protected requests:

```text
Authorization: Bearer <token>
```

## User Roles

Supported roles:

- `USER`
- `ADMIN`

By default, registered users get the `USER` role.

Role rules:

- `USER` can shop, place orders, use wishlist, make payments, and request return/exchange.
- `ADMIN` can manage products and return/exchange administration.
- `ADMIN` cannot place orders, pay, use wishlist, cancel customer orders, or create user return/exchange requests.
- `USER` cannot access admin APIs.

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

Maximum image size:

```text
5MB
```

## Product Images

Product image storage supports two modes.

Local mode:

```env
STORAGE_TYPE=local
PRODUCT_IMAGES_DIR=/app/uploads/products
```

Local mode stores files under:

```text
uploads/products
```

Cloudinary mode:

```env
STORAGE_TYPE=cloudinary
CLOUDINARY_CLOUD_NAME=change-me-cloudinary-cloud-name
CLOUDINARY_API_KEY=change-me-cloudinary-api-key
CLOUDINARY_API_SECRET=change-me-cloudinary-api-secret
CLOUDINARY_FOLDER=springecom/products
```

In Cloudinary mode, product image URLs are stored as Cloudinary HTTPS URLs, so multiple API servers can serve the same product images without sharing a local upload folder.

The production Docker Compose file sets:

```env
STORAGE_TYPE=cloudinary
```

Before production delivery, rotate any exposed Cloudinary secret and put real Cloudinary values only in the server `.env` file.

## Order APIs

User-only protected endpoints:

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

User-only protected endpoints:

- `POST /api/payments/create`
- `POST /api/payments/verify`

Public endpoint:

- `POST /api/payments/webhook`

Online payment flow:

1. User places an order with payment mode `ONLINE`.
2. Backend creates the order with payment status `PENDING`.
3. Frontend calls `/api/payments/create`.
4. Backend creates a Razorpay order.
5. Frontend opens Razorpay Checkout.
6. Frontend calls `/api/payments/verify`.
7. Backend verifies Razorpay signature.
8. Backend updates payment status to `PAID`.

For client/live deployment, Razorpay KYC and payment method activation must be completed in the client's Razorpay business account.

## Razorpay Refund Support

The backend supports Razorpay refunds for online paid return orders.

Online return refund flow:

1. User creates a return request for a delivered order.
2. Admin approves the return request.
3. Refund status becomes `REFUND_PROCESSING`.
4. After 6 days, scheduler processes the approved return.
5. Backend calls Razorpay refund API.
6. If refund succeeds, request becomes `COMPLETED` and refund status becomes `REFUNDED`.
7. If refund fails, request remains retryable and refund status becomes `REFUND_FAILED`.

COD refund flow:

- Cash on Delivery refunds cannot be processed through Razorpay.
- COD return approval sets refund status to `MANUAL_REFUND_REQUIRED`.
- Admin/business must process COD refunds manually.

## Order Status Automation

The backend includes automatic order status updates using Spring Scheduler.

Rules:

- `CASH_ON_DELIVERY` + 7 days old + `PLACED` = `DELIVERED`
- `ONLINE` + 7 days old + `PAID` + `PLACED` = `DELIVERED`
- `ONLINE` + 7 days old + `PENDING` = stays `PLACED`
- `ONLINE` + payment `FAILED` = `FAILED`
- `CANCELLED` orders are never changed by the scheduler

The order status scheduler runs daily at 1:00 AM Asia/Kolkata:

```java
@Scheduled(cron = "0 0 1 * * *", zone = "Asia/Kolkata")
```

Schedulers use Redis locks so only one API server processes the job when multiple API servers are running.

## Return / Exchange APIs

Users can request return or exchange only after an order is delivered.

User-only endpoints:

- `POST /api/orders/{orderId}/return-exchange`
- `GET /api/orders/return-exchange`

Admin-only endpoints:

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
- `REFUND_FAILED`

Return/exchange rules:

- Only `DELIVERED` orders are eligible.
- Request window is 7 days after delivery.
- User must provide a reason between 10 and 1000 characters.
- User cannot create multiple active return/exchange requests for the same order.
- Admin can approve, reject, or complete a request.
- Online paid returns use Razorpay refund API.
- COD returns require manual refund.

Automatic return/exchange completion:

- Approved return/exchange requests are completed automatically after 6 days.
- Scheduler runs daily at 1:30 AM Asia/Kolkata.
- Scheduler uses Redis lock for multi-server safety.

```java
@Scheduled(cron = "0 30 1 * * *", zone = "Asia/Kolkata")
```

## Wishlist APIs

User-only protected endpoints:

- `GET /api/wishlist`
- `POST /api/wishlist/{productId}`
- `DELETE /api/wishlist/{productId}`

## Rate Limiting

Rate-limited endpoints include:

- `POST /api/login`
- `POST /api/login/google`
- `POST /api/register`
- `POST /api/payments/create`
- `GET /api/products`
- `GET /api/products/search`

Rate-limit state is stored in Redis, so limits are shared across API containers using the same Redis.

Proxy rule:

- By default, the app ignores `X-Forwarded-For` and `X-Real-IP`.
- These headers are trusted only when request comes from an IP listed in `RATE_LIMIT_TRUSTED_PROXIES`.

Example:

```env
RATE_LIMIT_TRUSTED_PROXIES=172.18.0.1
```

Leave blank when API is exposed directly:

```env
RATE_LIMIT_TRUSTED_PROXIES=
```

## Security Rules Summary

Public endpoints:

- Register/login APIs
- Product read APIs
- Product image APIs
- Health endpoint
- Razorpay webhook

User-only endpoints:

- Order placement
- User order listing
- Order cancellation
- Wishlist APIs
- Razorpay payment create/verify APIs
- User return/exchange request APIs

Admin-only endpoints:

- Product create/update/delete APIs
- Admin return/exchange APIs
- Swagger/OpenAPI docs when enabled locally

Security test command:

```powershell
.\mvnw.cmd test "-Dtest=SecurityConfigTest"
```

Rate limit test command:

```powershell
.\mvnw.cmd test "-Dtest=RateLimitFilterTest"
```

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
- `V8__add_razorpay_refund_fields.sql`
- `V9__add_return_exchange_refund_idempotency_key.sql`
- `V10__create_razorpay_webhook_events.sql`
- `V11__add_user_token_version.sql`
- `V12__change_product_id_to_bigint.sql`
- `V13__add_user_id_to_orders_and_wishlist_items.sql`
- `V14__add_product_image_public_id.sql`

The app uses:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Database schema must match Flyway migrations.

## Production Backup Automation

Backup scripts:

```text
scripts/backup-production.ps1
scripts/restore-production.ps1
```

Create backup:

```powershell
.\scripts\backup-production.ps1
```

Create backup with custom folder and retention:

```powershell
.\scripts\backup-production.ps1 -BackupDir "D:\springecom-backups" -RetentionDays 30
```

Restore from backup folder:

```powershell
.\scripts\restore-production.ps1 -BackupPath "D:\springecom-backups\20260526-153000"
```

Recommended production backup policy:

- Run backup at least once per day.
- Keep at least 14 days of backups.
- Store backups outside the application folder.
- Copy backups to external/cloud storage.
- Test restore on staging before go-live.
- Test restore again after major database migration changes.

For production Cloudinary images, database backups preserve image URLs and Cloudinary public IDs. Cloudinary assets should be protected by Cloudinary account access and any client-required media backup/export process.

## Useful Docker Commands

Start database and Redis only:

```powershell
docker compose up -d postgres redis
```

View containers:

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

Open Redis shell in local compose:

```powershell
docker exec -it springecom-redis redis-cli
```

Open Redis shell in production compose:

```powershell
docker exec -it springecom-redis redis-cli -a <REDIS_PASSWORD>
```

Clear Redis in local compose:

```powershell
docker exec -it springecom-redis redis-cli FLUSHALL
```

Clear Redis in production compose:

```powershell
docker exec -it springecom-redis redis-cli -a <REDIS_PASSWORD> FLUSHALL
```

## Production Notes

Before production delivery:

- Rotate all real secrets.
- Use strong `DB_PASSWORD`, `JWT_SECRET`, and `REDIS_PASSWORD`.
- Do not commit `.env`.
- Keep database and Redis ports private.
- Configure real frontend URL in `CORS_ORIGINS`.
- Configure trusted proxy IPs only in `RATE_LIMIT_TRUSTED_PROXIES`.
- Use Cloudinary for production product image storage.
- Keep Cloudinary credentials only in environment variables.
- For multi-server production, use managed PostgreSQL or a dedicated database server.
- For multi-server production, use shared Redis for cache, rate limiting, and scheduler locks.
- Configure scheduled backups.
- Test restore before go-live.
- Complete Razorpay KYC and payment method activation.
- Use live Razorpay keys only in production environment variables.
- Confirm Razorpay refund permission is enabled.
- Ensure Razorpay account has enough balance or settlement support for refunds.
- COD refunds must be handled manually by admin/business.
- Confirm refund and exchange business rules with the client.

## Final Delivery Verification

Run before sending backend to client:

```powershell
git status
.\scripts\run-clean-tests.ps1
docker compose config
docker compose -f docker-compose.prod.yml config
```

Verify running API:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health -UseBasicParsing
```

Expected:

```text
StatusCode: 200
{"status":"UP"}
```

## Client Delivery Checklist

Before sending to the client, confirm:

- `git status` is clean.
- GitHub Actions is green.
- Clean tests pass.
- Docker compose config is valid.
- Production compose config is valid.
- Health check returns `UP`.
- Swagger is disabled in production.
- No real secrets are committed.
- `.env.example` is included.
- Real `.env` is not committed.
- Admin login has been tested.
- User login has been tested.
- Product image upload has been tested in Cloudinary mode.
- Cash on Delivery order placement has been tested.
- Online payment test flow has been tested.
- Admin cannot access user shopping APIs.
- User cannot access admin APIs.
- Payment status updates to `PAID` after Razorpay verification.
- Order placement reduces stock.
- Order scheduler tests pass.
- Return/exchange request flow has been tested.
- Return/exchange scheduler tests pass.
- Online paid return refund flow has been tested in Razorpay test mode.
- Refund failure behavior has been tested.
- COD manual refund behavior has been confirmed.
- Deployment scope has been confirmed with the client.
- Production backup script has been tested.
- Production restore script has been tested on staging.
- Backup retention policy has been confirmed.
- Backup storage location has been confirmed.
