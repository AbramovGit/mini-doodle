# Mini Doodle

Mini Doodle is a Spring Boot backend for scheduling users' available time
slots, booking meetings, and querying free/busy availability.

## Running locally

The project currently uses an in-memory H2 database, so the only Compose
service is the application:

```bash
docker compose up --build
```

The application is available at `http://localhost:8080`. Stop it with:

```bash
docker compose down
```

Useful endpoints:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`
- Prometheus metrics: `http://localhost:8080/actuator/prometheus`

## API usage

Create a user. Creating a user also creates their implicit domain calendar:

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Ada Lovelace","email":"ada@example.com","timezone":"UTC"}'
```

Create one or more free slots:

```bash
curl -X POST http://localhost:8080/api/users/USER_ID/slots \
  -H "Content-Type: application/json" \
  -d '{"slots":[{"startTime":"2026-09-14T09:00:00Z","endTime":"2026-09-14T10:00:00Z"}]}'
```

List slots intersecting a range:

```bash
curl "http://localhost:8080/api/users/USER_ID/slots?from=2026-09-14T00:00:00Z&to=2026-09-15T00:00:00Z"
```

Book a slot and mark it busy:

```bash
curl -X POST http://localhost:8080/api/users/USER_ID/slots/SLOT_ID/meeting \
  -H "Content-Type: application/json" \
  -d '{"title":"Planning","description":"Weekly planning","participantIds":["USER_ID"]}'
```

Query aggregated availability:

```bash
curl "http://localhost:8080/api/users/USER_ID/availability?from=2026-09-14T00:00:00Z&to=2026-09-15T00:00:00Z"
```

## Architecture

- `domain`: JPA entities and domain enums
- `repository`: Spring Data JPA persistence interfaces
- `service`: transactional business rules and state transitions
- `controller` and `api`: REST endpoints and DTOs
- `db/migration`: Flyway schema migrations

The public API is organized around users, slots, meetings, and availability.
`Calendar` is an internal domain concept and is never exposed as a REST
resource or path segment.

## Design decisions and tradeoffs

- All stored timestamps use UTC `Instant` values. A user's timezone is retained
  for display purposes only.
- A calendar is created implicitly for each user. External email-only invitees
  are not supported; participants must be registered users.
- Each slot creates at most one meeting. A booked slot cannot be deleted and
  can only become free again when its meeting is cancelled.
- Slot overlap and minimum-duration validation are enforced in the service
  layer. `Slot` uses optimistic locking so concurrent booking attempts return
  `409 Conflict` for the losing request.
- H2 is used instead of the target PostgreSQL datastore at the user's request.
  This keeps the Compose setup self-contained, but data is reset when the
  application restarts. The schema remains managed by Flyway.
- The availability endpoint is paginated and clips slot windows to the
  requested range, merging adjacent windows with the same status.

## Deliberately out of scope

- PostgreSQL deployment and PostgreSQL-specific exclusion constraints
- Multi-slot meetings
- External or email-only participants
- Cross-user availability intersection
- Authentication and authorization

The cross-user availability endpoint
`GET /api/availability?userIds=...&from=...&to=...` is the next stretch goal.

## Technology

- Java 21 and Spring Boot 3
- Spring Web, Spring Data JPA, Bean Validation, and Lombok
- H2 and Flyway
- MapStruct dependency reserved for DTO mapping as the API grows
- springdoc-openapi, Spring Boot Actuator, Micrometer, and Prometheus
- Maven and Docker Compose
