# SpringEcom Client Delivery Checklist

Use this checklist before handing the backend to a paid client.

## Delivery Status

Status: conditionally ready for client delivery.

The project is ready for demo and staging once the checks below pass. Treat live
production as blocked until secrets are rotated, Java 21 tests pass, real domains
are configured, and payment/refund flows are verified with the client's accounts.

## Required Before Handover

- Rotate all secrets that were exposed during development: Cloudinary, Google,
  JWT, Razorpay, database, and Redis.
- Confirm `java -version` shows Java 21.
- Run `.\scripts\check-no-secrets.ps1`.
- Run `.\scripts\run-clean-tests.ps1`.
- Run `docker compose config`.
- Run `docker compose -f docker-compose.prod.yml config`.
- Start local Docker compose and confirm `/actuator/health` returns HTTP 200.
- Verify product image create/update in Cloudinary mode.
- Verify frontend displays Cloudinary HTTPS image URLs without prefixing backend
  origin.
- Verify Google OAuth allowed JavaScript origins and redirect URI.
- Verify Razorpay test order creation, payment verification, webhook, and refund
  behavior.
- Test user registration/login, admin login, product CRUD, wishlist, cart/order,
  cancellation, return/exchange, and refund failure behavior.
- Run one backup and one restore test on staging or a local copy.

## Production Values

- `SPRING_PROFILES_ACTIVE=prod`
- `CORS_ORIGINS=https://your-real-frontend-domain.com`
- `STORAGE_TYPE=cloudinary`
- Strong `DB_PASSWORD`
- Strong `REDIS_PASSWORD`
- Strong base64 `JWT_SECRET`
- Real Cloudinary values
- Real Google OAuth client values
- Real Razorpay values

Do not use localhost origins in production. The app intentionally rejects that
configuration in the `prod` profile.

## Local Development Values

- `SPRING_PROFILES_ACTIVE=local`
- `CORS_ORIGINS=http://localhost:5173`
- `STORAGE_TYPE=local` or `cloudinary`

Local Docker compose uses the `local` profile. Production compose uses the
`prod` profile.
