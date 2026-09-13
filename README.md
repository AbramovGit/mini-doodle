# Mini Doodle

Backend simulation of a meeting scheduling platform inspired by Doodle.

## Planned capabilities

- Create users and manage their available time slots.
- Book a free slot into a meeting with registered participants.
- Query free/busy availability over a time range.
- Cancel meetings and release their slots.

## Planned technology

- Java 21 and Spring Boot 3
- PostgreSQL with Flyway migrations
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

## Local development

Run instructions and API examples will be added when the application and
`docker-compose.yml` are introduced.
