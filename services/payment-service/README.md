# payment-service

Payment service creates Stripe Checkout sessions, records webhook outcomes, processes refunds and publishes payment events through a transactional outbox.

- Java 21; Spring Boot 4.1.2; Spring Cloud 2025.1.4; Maven Wrapper riêng.
- Package `com.mentormatching.payment`; port mặc định `8085`.
- DBMS **Microsoft SQL Server**, database riêng `mm_payment`.
- Flyway owns the `payments`, `payment_attempts`, `webhook_receipts`, `refund_transactions`, `outbox_events`, and `booking_event_receipts` tables. JPA validates the schema; it does not create it.

## Build and test

From this directory, run:

```bash
./mvnw clean verify
```

The integration suite uses a real SQL Server Testcontainer; Docker must be running. It covers migrations, checkout idempotency, mentor history authorization, invalid and duplicate Stripe webhooks, and automatic refunds for duplicate booking cancellation events.

## API

- `POST /api/v1/payments` accepts a booking ID and `Idempotency-Key`; the amount is always read from Booking.
- `GET /api/v1/payments/me` lists the authenticated learner's transactions.
- `GET /api/v1/payments/mentor/me` lists a mentor's transactions and requires `ROLE_MENTOR`.
- `GET /api/v1/payments/{id}` allows the payer, mentor for that booking, or an admin.
- `POST /api/v1/payments/{id}/refund` is admin-only and idempotent.
- `POST /api/v1/payments/stripe/webhook` validates the raw payload using `Stripe-Signature`.

Checkout calls `GET /internal/v1/bookings/{bookingId}/payment-context` with the user's bearer token. Booking checks that the caller owns an unexpired `PENDING` booking and returns the authoritative amount and currency.

## Events and local dependencies

Payment status changes and their event payloads are inserted into SQL Server in the same transaction. `PaymentOutboxPublisher` publishes pending rows to the durable `mentor.events` RabbitMQ exchange and marks a row published only after broker confirmation. Consumers deduplicate using `eventId`.

The service publishes `PaymentSucceeded`, `PaymentFailed`, and `PaymentRefunded` messages to `payment.events`. It consumes `BookingCancelled` and `BookingConfirmationFailed` from `payment.booking-lifecycle`; a successful booking event refund is also idempotent. Transient listener failures retry five times and then go to `payment.booking-lifecycle.dead` for reconciliation.

Start local RabbitMQ with:

```bash
docker run -d --name mm-rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```

The management UI is at `http://localhost:15672` (`guest` / `guest`). Set `PAYMENT_MESSAGING_ENABLED=false` only when intentionally running without event delivery.

Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `AUTH_JWT_*`, `BOOKING_SERVICE_URL`, and `RABBITMQ_*` from the environment. `.env.example` contains local reference values; Spring Boot does not load `.env` automatically. Never commit live credentials.
