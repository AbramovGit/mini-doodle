package com.abramovgit.minidoodle.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "A meeting booked from exactly one slot.")
public record MeetingResponse(
        @Schema(example = "1") Long id,
        @Schema(description = "The slot converted into this meeting.", example = "4") Long slotId,
        @Schema(example = "2026-10-01T09:00:00Z") Instant startTime,
        @Schema(example = "2026-10-01T10:00:00Z") Instant endTime,
        @Schema(example = "Sprint planning") String title,
        @Schema(example = "Plan the next sprint.") String description,
        @Schema(description = "User ID of the slot owner.", example = "1") Long organizerId,
        @Schema(description = "Registered participant user IDs.", example = "[1,2]") List<Long> participantIds
) {
}
