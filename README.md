# Mentor-Matching-Microservice

## Local infrastructure

Copy `.env.example` to `.env`, adjust the local development credentials if needed, then start the databases and brokers:

```sh
docker compose up -d
docker compose ps
```

The Compose stack starts MySQL 8.4, SQL Server 2022, MongoDB as replica set `rs0`, RabbitMQ, and Redis. SQL Server and MongoDB initialization run as one-shot Compose services.

To start the API gateway locally, set the service URLs and `FRONTEND_BASE_URL` in the gateway environment. Set `API_DOCS_ENABLED=true` in each service that includes springdoc to publish its OpenAPI document to the gateway Swagger UI.

## Repository conventions

See [CONTRIBUTING.md](CONTRIBUTING.md) for package and pull request conventions.
