# media-service

Media asset service using MongoDB metadata and Cloudinary storage. The service connects to the `mm_media` database on a MongoDB replica set. It initializes its compound, unique and TTL indexes at startup. Cloudinary API requests and signed URLs use the Cloudinary Java SDK; live credentials are read from environment variables.

## API

- `POST /api/v1/media/upload-intents` — create signed upload form fields. `purpose` is `AVATAR`, `ACHIEVEMENT`, `REVIEW_ATTACHMENT`, or `VERIFICATION`; the service chooses public delivery for the first three and authenticated private delivery for verification.
- `POST /api/v1/media/{id}/confirm` — body `{ "assetId": "..." }`; server reads Cloudinary asset details and checks owner, public ID, resource type, configured cloud and size before marking `READY`.

Upload intents accept JPEG, PNG, WebP, or GIF images, MP4 or WebM video, and PDF documents. The default per-file limit is 5 MiB. Confirmation compares the actual Cloudinary `asset_id`, `public_id`, resource/delivery type, byte count, format, ETag and secure URL against the pending intent before persisting verified metadata. The browser receives a server-generated signature; the API secret is never returned.

All API routes require a JWT from auth-service. JWT issuer, audience and RSA public key use `AUTH_JWT_*`. The MongoDB URI and Cloudinary credentials are configured through environment variables; see `.env.example`. Do not commit live credentials.

## Build and run

From this directory, run `docker compose up -d mongo` to start a local single-node replica set, then run `./mvnw clean verify`. Integration tests start their own MongoDB replica set with Testcontainers and stub Cloudinary. Eureka discovery is enabled by default; use `EUREKA_ENABLED=false` only for an isolated local run. Swagger can be enabled with `API_DOCS_ENABLED=true`.
