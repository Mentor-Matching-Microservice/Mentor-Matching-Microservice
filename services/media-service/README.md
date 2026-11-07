# media-service

Media asset service using MongoDB metadata and Cloudinary storage. Uploads use server-signed direct upload intents; the browser uploads bytes directly to Cloudinary, then the service verifies the asset against Cloudinary Admin API before marking it ready.

## API

- `POST /api/v1/media/upload-intents` — create signed upload form fields. `purpose` is `AVATAR`, `ACHIEVEMENT`, `REVIEW_ATTACHMENT`, or `VERIFICATION`; the service chooses public delivery for the first three and authenticated private delivery for verification.
- `POST /api/v1/media/{id}/confirm` — body `{ "assetId": "..." }`; server reads Cloudinary asset details and checks owner, public ID, resource type, configured cloud and size before marking `READY`.

All API routes require a JWT from auth-service. JWT issuer, audience and RSA public key use `AUTH_JWT_*`. The MongoDB URI and Cloudinary credentials are configured through environment variables; see `.env.example`. Do not commit live credentials.

## Build and run

From this directory, run `./mvnw -DskipTests package`. MongoDB and Cloudinary are only needed when running the service. Swagger can be enabled with `API_DOCS_ENABLED=true`.
