# Mini Doodle

Spring Boot API for user availability, slots, and single-slot meetings.

## Run locally

```bash
docker compose up --build
```

| Service | Address |
| --- | --- |
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8081 |
| OpenAPI | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |
| Prometheus | http://localhost:8080/actuator/prometheus |

Compose starts the API, PostgreSQL 16, and standalone Swagger UI. PostgreSQL
data is stored in the `postgres-data` volume. Use `docker compose down -v` to
reset it.

## API

Use Swagger UI to explore and test the API. Main endpoints:

- `POST /api/users`, `GET /api/users/{userId}`
- `POST|GET /api/users/{userId}/slots`
- `PATCH|DELETE /api/users/{userId}/slots/{slotId}`
- `GET /api/users/{userId}/availability`
- `POST /api/users/{userId}/slots/{slotId}/meeting`
- `GET|PATCH|DELETE /api/meetings/{meetingId}`

All timestamps are UTC ISO-8601 instants. Pagination is zero-based and capped
at 100 entries. Time outside declared free slots is returned as `BUSY`.

## Design notes

- Each user receives an implicit calendar; it is never exposed as a REST resource.
- Slots cannot overlap and must be at least 15 minutes. PostgreSQL additionally
  enforces non-overlap with an exclusion constraint.
- Booking atomically marks a slot `BUSY` and creates one meeting; cancellation
  removes the meeting and frees the slot. Concurrent booking conflicts return 409.
- PostgreSQL aggregates availability windows; H2 uses the equivalent Java path
  for tests.
- Caffeine caches user and availability reads for five minutes. Availability is
  invalidated after slot and meeting changes.
- Bulk requests allow up to 100 slots and meetings up to 50 participants.

## Tests

H2 is test-only:

```bash
mvn test
```

The project uses Java 21, Spring Boot, PostgreSQL, Flyway, Caffeine, and
standalone Swagger UI. Authentication, external invitees, multi-slot meetings,
and cross-user availability remain out of scope.
