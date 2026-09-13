# Mini Doodle

Backend simulation of a meeting scheduling platform inspired by Doodle.

## Planned capabilities

- Create users and manage their available time slots.
- Book a free slot into a meeting with registered participants.
- Query free/busy availability over a time range.
- Cancel meetings and release their slots.

## Planned technology

- Java 21 and Spring Boot 3
- H2 with Flyway migrations
- Spring Data JPA and Bean Validation
- Docker Compose for local development
- OpenAPI documentation and Actuator metrics

## Domain boundaries

`Calendar` is an internal domain concept that owns a user's slots. It is not
exposed as a REST resource; the public API is organized around users, slots,
meetings, and availability.

All persisted timestamps will use UTC `Instant` values. A user's timezone is
kept for display purposes only. Participants are registered users in the first
version, and each slot can produce exactly one meeting.

H2 is used as the application database at this stage. This is a deliberate
deviation from the target PostgreSQL deployment to keep local development
self-contained; the in-memory database is reset whenever the application
restarts.

## Local development

Start the application with:

```bash
docker compose up --build
```

The service is available at `http://localhost:8080`. At this scaffold stage,
health and metrics are exposed at `/actuator/health` and
`/actuator/prometheus`; business endpoints will be added in later milestones.

The user and slot APIs are now available:

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Ada Lovelace","email":"ada@example.com","timezone":"UTC"}'

curl -X POST http://localhost:8080/api/users/USER_ID/slots \
  -H "Content-Type: application/json" \
  -d '{"slots":[{"startTime":"2026-09-14T09:00:00Z","endTime":"2026-09-14T10:00:00Z"}]}'

curl "http://localhost:8080/api/users/USER_ID/slots?from=2026-09-14T00:00:00Z&to=2026-09-15T00:00:00Z"
```

Meetings are booked from a free slot and automatically mark it busy:

```bash
curl -X POST http://localhost:8080/api/users/USER_ID/slots/SLOT_ID/meeting \
  -H "Content-Type: application/json" \
  -d '{"title":"Planning","description":"Weekly planning","participantIds":["USER_ID"]}'
```

Aggregated availability is available for a user and time range:

```bash
curl "http://localhost:8080/api/users/USER_ID/availability?from=2026-09-14T00:00:00Z&to=2026-09-15T00:00:00Z"
```
