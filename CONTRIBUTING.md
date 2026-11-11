# Contributing

## Java package conventions

Keep service packages organized by responsibility. Use the existing package names and avoid moving code between services without a separate migration.

- `controller`: HTTP endpoints and request/response mapping.
- `usecase` or `service`: application workflows. Use a use case class for a focused CQRS command or query; keep reads and writes separate when their responsibilities differ.
- `repository`: persistence access only.
- `entity`: persistence models.
- `dto`: API and application data transfer objects.
- `config`: framework and infrastructure configuration.
- `exception`: service errors and HTTP error mapping.

Do not put business decisions in controllers or database operations in controllers. Keep internal APIs under their existing `/internal/**` paths and do not add gateway routes for them.

## Pull requests

Link the issue with `Closes #<number>`, describe the affected service, and run the Maven tests for each changed service before requesting review.
