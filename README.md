# SpringEcom Backend

SpringEcom is a Spring Boot ecommerce backend API with JWT authentication, Google login support, role-based access control, product management, order placement, Razorpay online payment and refund support, return/exchange request handling, wishlist, PostgreSQL database, Redis cache, Redis-backed rate limiting, Flyway migrations, Docker Compose, health checks, scheduled order automation, and Swagger API documentation.

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
RATE_LIMIT_TRUSTED_PROXIES=

RAZORPAY_KEY_ID=change-me-razorpay-key-id
RAZORPAY_KEY_SECRET=change-me-razorpay-key-secret
RAZORPAY_WEBHOOK_SECRET=change-me-razorpay-webhook-secret
RAZORPAY_CURRENCY=INR

JWT_EXPIRATION_MS=3600000
REDIS_PASSWORD=change-me-strong-redis-password

PRODUCT_IMAGES_DIR=/app/uploads/products
```

Important: never commit the real `.env` file to GitHub. For client delivery, send `.env.example` and ask the client/deployment owner to create their own `.env` with live secrets on the server.

## Deployment Scope

This backend is production-ready for a single-server Docker deployment.

The production Docker Compose setup is designed for:

- One API container
- One PostgreSQL container
- One Redis container
- One persistent Docker volume for PostgreSQL data
- One persistent Docker volume for product images

This setup is suitable for small or medium client deployments where the backend runs on one server.

For multi-server or horizontally scaled production, do not run multiple API containers with separate local product image folders. In that setup:

- Move PostgreSQL to a managed database service or a dedicated database server.
- Move product images to shared object storage.
- Use shared/distributed rate limiting through Redis or an API gateway.
- Keep Redis accessible only inside the private deployment network.
- Put the API behind a trusted reverse proxy or load balancer.
- Configure `RATE_LIMIT_TRUSTED_PROXIES` with only the trusted proxy/load-balancer IP address.

Until those multi-server changes are implemented, this project should be deployed as a single-server Docker stack.

## Run With Docker

Start the full backend stack:

```powershell
docker compose up --build
```

Start the production-style stack:

```powershell
docker compose -f docker-compose.prod.yml up --build -d
```

The production-style compose file exposes only the API port. PostgreSQL and Redis stay private inside Docker.

It also requires `REDIS_PASSWORD` in `.env`.

If the API runs behind a trusted reverse proxy, set `RATE_LIMIT_TRUSTED_PROXIES` to the proxy IP address, for example:

```env
RATE_LIMIT_TRUSTED_PROXIES=172.18.0.1
```

Leave it blank when the API is exposed directly:

```env
RATE_LIMIT_TRUSTED_PROXIES=
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

Do not run `docker compose down -v` in production unless you intentionally want to remove database, Redis, and uploaded image volumes.

## Run Tests

Run tests:

```powershell
.\mvnw.cmd test
```

Run full clean test:

```powershell
.\mvnw.cmd clean test
```

Run security tests:

```powershell
.\mvnw.cmd test "-Dtest=SecurityConfigTest"
```

Run rate-limit tests:

```powershell
.\mvnw.cmd test "-Dtest=RateLimitFilterTest"
```

Run return/exchange refund tests:

```powershell
.\mvnw.cmd test "-Dtest=ReturnExchangeServiceTest,ReturnExchangeSchedulerTest"
```

Latest verified result:

```text
126 tests run, 0 failures, 0 errors
```

If Docker Desktop is not running or not accessible, the Testcontainers integration test may be skipped automatically depending on environment.

## Health Check

Health endpoint:

```text
http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

PowerShell check:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health -UseBasicParsing
```

Expected result:

```text
StatusCode: 200
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

Use the JWT token in protected requests:

```text
Authorization: Bearer <token>
```

## User Roles

Supported roles:

- `USER`
- `ADMIN`

By default, registered users get the `USER` role.

Role responsibilities:

| Role | Purpose |
|---|---|
| `USER` | Customer shopping, orders, wishlist, payments, return/exchange requests |
| `ADMIN` | Product management and return/exchange administration |

Important role rule:

- `ADMIN` users are blocked from customer shopping APIs.
- `ADMIN` users cannot place orders, pay for orders, use wishlist, cancel customer orders, or create user return/exchange requests.
- `ADMIN` users can manage products and admin return/exchange decisions.
- `USER` users cannot access admin product or admin return/exchange APIs.

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
6. After successful payment, frontend calls `/api/payments/verify`.
7. Backend verifies Razorpay signature.
8. Backend updates payment status to `PAID`.
9. Backend stores Razorpay payment id and paid time.

Payment statuses:

- `PENDING`
- `PAID`
- `FAILED`

For client/live deployment, Razorpay KYC and payment method activation must be completed in the client's Razorpay business account.

## Razorpay Refund Support

The backend supports real Razorpay refunds for online paid return orders.

Online return refund flow:

1. User creates a return request for a delivered order.
2. Admin approves the return request.
3. Refund status becomes `REFUND_PROCESSING`.
4. After 6 days, the return/exchange scheduler processes the approved return.
5. Backend calls Razorpay refund API using the stored Razorpay payment id.
6. If Razorpay refund succeeds:
   - request status becomes `COMPLETED`
   - refund status becomes `REFUNDED`
   - gateway refund id is stored
   - refund amount is stored
   - refund processed time is stored
7. If Razorpay refund fails:
   - request status remains `APPROVED`
   - refund status becomes `REFUND_FAILED`
   - refund failure reason is stored
   - scheduler can retry on the next run

COD refund flow:

- Cash on Delivery refunds cannot be processed through Razorpay.
- COD return approval sets refund status to `MANUAL_REFUND_REQUIRED`.
- Admin/business must process COD refunds manually.

Important: online paid returns use Razorpay refund API when the approved return is completed. COD refunds are marked as `MANUAL_REFUND_REQUIRED` and must be handled manually by admin/business.

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

````markdown
```text
@Scheduled(cron = "0 0 1 * * *")

For local testing only, it can temporarily be changed to:

```java
@Scheduled(fixedRate = 60000)
```

This runs every 60 seconds.

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
- A user cannot create multiple active return/exchange requests for the same order.
- Admin can approve, reject, or complete a request.
- For online paid returns, approval sets refund status to `REFUND_PROCESSING`.
- For Cash on Delivery returns, approval sets refund status to `MANUAL_REFUND_REQUIRED`.
- For online paid returns, completion calls Razorpay refund API.
- For successful online refunds, refund status becomes `REFUNDED`.
- For failed online refunds, refund status becomes `REFUND_FAILED`.
- For approved exchanges, completion marks the request as `COMPLETED`.

Automatic return/exchange completion:

- Approved return/exchange requests are completed automatically after 6 days.
- For approved online paid returns, backend attempts Razorpay refund.
- For approved COD returns, backend marks the request completed and keeps manual refund responsibility with admin/business.
- For approved exchanges, request status becomes `COMPLETED`.
- The scheduler runs daily at 1:30 AM.

```java
@Scheduled(cron = "0 30 1 * * *")
```

Refund and exchange timing:

```text
After admin approval, return refunds and exchange requests are automatically completed after 6 days.
```

## Return / Exchange Response Fields

Return/exchange response includes:

- `requestId`
- `orderId`
- `userEmail`
- `requestType`
- `reason`
- `status`
- `refundStatus`
- `adminNote`
- `gatewayRefundId`
- `refundAmount`
- `approvedAt`
- `completedAt`
- `refundProcessedAt`
- `refundFailureReason`
- `createdAt`
- `updatedAt`

## Wishlist APIs

User-only protected endpoints:

- `GET /api/wishlist`
- `POST /api/wishlist/{productId}`
- `DELETE /api/wishlist/{productId}`

## Rate Limiting

The backend includes rate limiting for sensitive endpoints.

Rate-limited endpoints include:

- `POST /api/login`
- `POST /api/login/google`
- `POST /api/register`
- `POST /api/payments/create`
- `GET /api/products`
- `GET /api/products/search`

Rate-limit state is stored in Redis, so limits are shared across API containers that use the same Redis instance.

Important proxy rule:

- By default, the app ignores `X-Forwarded-For` and `X-Real-IP`.
- These headers are trusted only when the request comes from an IP listed in `RATE_LIMIT_TRUSTED_PROXIES`.
- Leave `RATE_LIMIT_TRUSTED_PROXIES` blank when exposing the API directly.
- Set `RATE_LIMIT_TRUSTED_PROXIES` only when the API is behind a trusted reverse proxy or load balancer.

Example:

```env
RATE_LIMIT_TRUSTED_PROXIES=172.18.0.1
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

Note: application logs may say `rateLimitFilterRegistration was not registered (disabled)`. That is expected because the filter is registered inside the Spring Security chain instead of as a separate servlet filter.

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

The app uses:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

That means the database schema must match Flyway migrations.

In Docker, PostgreSQL data is stored in the volume:

```text
springecom-postgres-data
```

## Production Backup Automation

Manual backup commands are useful, but production delivery should include a repeatable backup process.

This project should include these backup and restore scripts:

```text
scripts/backup-production.ps1
scripts/restore-production.ps1
```

The backup script should create:

- PostgreSQL database backup using `pg_dump`
- Product image volume backup using `tar`
- Timestamped backup folder
- Automatic cleanup of old backups based on retention days

Create a production backup:

```powershell
.\scripts\backup-production.ps1
```

Create a backup with custom folder and retention:

```powershell
.\scripts\backup-production.ps1 -BackupDir "D:\springecom-backups" -RetentionDays 30
```

Restore from a backup folder:

```powershell
.\scripts\restore-production.ps1 -BackupPath "D:\springecom-backups\20260526-153000"
```

Recommended production backup policy:

- Run backup at least once per day.
- Keep at least 14 days of backups.
- Store backups outside the application folder.
- Copy backups to external storage or cloud storage.
- Test restore on a staging machine before client go-live.
- Test restore again after major database migration changes.

Windows Task Scheduler example:

```powershell
powershell.exe -ExecutionPolicy Bypass -File "C:\path\to\SpringEcom\scripts\backup-production.ps1" -BackupDir "D:\springecom-backups" -RetentionDays 30
```

Before client delivery, perform one restore test on a staging machine and confirm:

- API starts successfully.
- Health check returns `UP`.
- Admin login works.
- User login works.
- Product images load correctly.
- Orders and payment records are visible.

Create a database SQL backup manually:

```powershell
docker exec springecom-postgres pg_dump -U springecom -d springecom -Fc -f /tmp/springecom-db.backup
docker cp springecom-postgres:/tmp/springecom-db.backup .\springecom-db.backup
```

Restore a database SQL backup manually:

```powershell
docker cp .\springecom-db.backup springecom-postgres:/tmp/springecom-db.backup
docker exec springecom-postgres pg_restore -U springecom -d springecom --clean --if-exists /tmp/springecom-db.backup
```

For full server migration, also back up the Docker volume:

```powershell
docker run --rm -v springecom_springecom-postgres-data:/data -v ${PWD}:/backup alpine tar czf /backup/postgres-volume-backup.tar.gz -C /data .
```

Daily client backups should use `pg_dump`. Volume backups are best for full machine migration or disaster recovery.

For multi-server production, use managed PostgreSQL or a dedicated database server instead of one local Docker PostgreSQL container.

## Product Images

Uploaded product images are stored in:

```text
uploads/products
```

In Docker, product images are stored in the volume:

```text
springecom-product-images
```

Current product image storage is single-server oriented.

For one-server deployment, Docker volume storage is acceptable and production-ready when regular backups are configured.

Create a product image backup manually:

```powershell
docker run --rm -v springecom_springecom-product-images:/data -v ${PWD}:/backup alpine tar czf /backup/product-images-backup.tar.gz -C /data .
```

Restore a product image backup manually:

```powershell
docker run --rm -v springecom_springecom-product-images:/data -v ${PWD}:/backup alpine sh -c "cd /data && tar xzf /backup/product-images-backup.tar.gz"
```

For multi-server production, Docker-local image storage is not enough. Move product images to shared object storage before running multiple API containers.

Do not run multiple API servers with separate local image folders, because product image URLs can point to files that exist on only one server.

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

Open Redis shell in local compose:

```powershell
docker exec -it springecom-redis redis-cli
```

Open Redis shell in production compose:

```powershell
docker exec -it springecom-redis redis-cli -a <REDIS_PASSWORD>
```

Clear Redis cache in local compose:

```powershell
docker exec -it springecom-redis redis-cli FLUSHALL
```

Clear Redis cache in production compose:

```powershell
docker exec -it springecom-redis redis-cli -a <REDIS_PASSWORD> FLUSHALL
```

## Production Notes

Before production delivery:

- Rotate all real secrets.
- Use strong `DB_PASSWORD` and `JWT_SECRET`.
- Use strong `REDIS_PASSWORD` for the production compose stack.
- Do not commit `.env`.
- Keep database and Redis ports private in production.
- Configure real frontend URL in `CORS_ORIGINS`.
- Confirm with the client whether deployment is single-server or multi-server.
- Use this Docker Compose setup for single-server production only.
- For multi-server production, use managed PostgreSQL and shared object storage for product images.
- Configure scheduled backups using `scripts/backup-production.ps1`.
- Keep at least 14 days of backups, or more if the client requires it.
- Store backups outside the application server when possible.
- Test restore using `scripts/restore-production.ps1` before go-live.
- Complete Razorpay KYC and payment method activation in the client's Razorpay account.
- Use live Razorpay keys only in production environment variables.
- Confirm Razorpay refund permission is enabled before live automatic refunds.
- Ensure Razorpay account has enough balance or settlement support for refunds.
- COD refunds must be handled manually by admin/business.
- Back up PostgreSQL data regularly.
- Store uploaded product images in persistent storage.
- Keep schedulers on cron mode for production.
- Confirm refund and exchange business rules with the client before enabling live operations.

## Final Delivery Verification

Run these commands before sending the backend to the client:

```powershell
git status
.\mvnw.cmd clean test
docker compose config
docker compose -f docker-compose.prod.yml config
docker compose up --build
```

In another PowerShell window, verify the running API:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health -UseBasicParsing
```

Expected result:

```text
StatusCode: 200
{"status":"UP"}
```

After verification, stop the local stack:

```powershell
docker compose down
```

## Client Delivery Checklist

Before sending to the client, confirm:

- `git status` is clean.
- GitHub Actions is green.
- `.\mvnw.cmd clean test` passes.
- `.\mvnw.cmd test "-Dtest=SecurityConfigTest"` passes.
- `.\mvnw.cmd test "-Dtest=RateLimitFilterTest"` passes.
- Docker starts with `docker compose up --build`.
- Health check returns `UP`.
- Swagger opens locally/admin-only if enabled, and is disabled in production.
- No real secrets are committed.
- `.env.example` is included.
- Real `.env` is not committed.
- Admin login has been tested.
- User login has been tested.
- Product image upload has been tested.
- Cash on Delivery order placement has been tested with `USER` role.
- Online payment test flow has been tested with `USER` role.
- Admin cannot place orders through API.
- Admin cannot use wishlist APIs.
- Admin cannot use payment create/verify APIs.
- User cannot access admin product APIs.
- User cannot access admin return/exchange APIs.
- Payment status updates to `PAID` after successful Razorpay verification.
- Order placement and stock reduction have been tested.
- Order status scheduler tests pass.
- Return/exchange request flow has been tested.
- Return/exchange scheduler tests pass.
- Online paid return refund flow has been tested with Razorpay test mode.
- Refund failure behavior has been tested.
- COD manual refund behavior has been confirmed.
- Deployment scope has been confirmed with the client.
- If single-server deployment is selected, Docker volumes and backups are configured.
- If multi-server deployment is required, managed PostgreSQL and shared object storage are planned before go-live.
- Production backup script has been tested.
- Production restore script has been tested on staging.
- Backup retention policy has been confirmed with the client.
- Backup storage location has been confirmed with the client.