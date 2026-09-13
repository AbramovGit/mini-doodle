package com.abramovgit.minidoodle.api;

import com.abramovgit.minidoodle.domain.SlotStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "A contiguous FREE or BUSY window in the requested range.")
public record AvailabilityWindow(
        @Schema(example = "2026-10-01T09:00:00Z") Instant startTime,
        @Schema(example = "2026-10-01T10:00:00Z") Instant endTime,
        @Schema(description = "BUSY also includes time outside declared free slots.", example = "FREE") SlotStatus status
) {
}
