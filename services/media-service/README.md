# media-service

Media metadata lives in the `mm_media` MongoDB replica set; binary content lives in Cloudinary. The service initializes compound, unique and TTL indexes at startup. Cloudinary signatures, Admin API checks, authenticated uploads, private download URLs and deletes use the Cloudinary Java SDK. Credentials are read from environment variables.

## Public API

- `POST /api/v1/media/upload-intents` creates a signed direct-upload intent. `AVATAR`, `ACHIEVEMENT` and `REVIEW_ATTACHMENT` use public delivery; `VERIFICATION` uses authenticated private delivery.
- `POST /api/v1/media/{id}/confirm` verifies `asset_id`, `public_id`, resource/delivery type, exact byte count, format, ETag and secure URL with Cloudinary before marking the record `READY`.
- `GET /api/v1/media/{id}/signed-url` returns a public URL or a short-lived private download URL to the owner.
- `DELETE /api/v1/media/{id}` deletes the Cloudinary asset and marks its metadata deleted.

Upload accepts JPEG, PNG, WebP or GIF images, MP4 or WebM video, and PDF documents. The default file limit is 5 MiB. The API secret is never returned to the browser.

## Service-to-service API

These routes require a JWT with `ROLE_SERVICE` or `ROLE_ADMIN`:

- `POST /internal/v1/media/verify-and-attach` accepts `mediaAssetId`, `uploadedByUserId`, `purpose`, `consumer` (`user-service`, `mentor-service` or `review-service`) and a UUID `referenceId`. It checks ownership, purpose and `READY` status, then records an idempotent usage key.
- `POST /internal/v1/media/private-delivery-urls` accepts `{ "mediaAssetIds": ["..."] }` and returns short-lived links only when every requested asset is `READY` and `PRIVATE`. Requests contain 1–50 unique IDs.

Pending intents older than 24 hours are deleted from Cloudinary and marked `EXPIRED`; metadata is removed by the MongoDB TTL index 30 days later. A failed Cloudinary cleanup remains pending for retry on the next scheduled run.

## Build and run

From this directory, run `docker compose up -d mongo` to start a local single-node replica set, then run `./mvnw clean verify`. Integration tests use a Testcontainers MongoDB replica set and stub Cloudinary, so no Cloudinary account is needed for the test suite. Eureka discovery is enabled by default; set `EUREKA_ENABLED=false` for an isolated local run. Swagger can be enabled with `API_DOCS_ENABLED=true`.
