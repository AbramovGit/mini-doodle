package com.abramovgit.minidoodle.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "A free time range expressed as UTC ISO-8601 timestamps.")
public record SlotRequest(
        @Schema(description = "Inclusive range start in UTC.", example = "2026-10-01T09:00:00Z")
        @NotNull Instant startTime,
        @Schema(description = "Exclusive range end in UTC. Must be after startTime.", example = "2026-10-01T10:00:00Z")
        @NotNull Instant endTime
) {
}
