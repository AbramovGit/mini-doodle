package com.abramovgit.minidoodle.api;

import com.abramovgit.minidoodle.domain.SlotStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "A user's free or booked time slot.")
public record SlotResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "2026-10-01T09:00:00Z") Instant startTime,
        @Schema(example = "2026-10-01T10:00:00Z") Instant endTime,
        @Schema(description = "FREE until booked; BUSY while its meeting exists.", example = "FREE") SlotStatus status,
        @Schema(description = "Optimistic-lock version.", example = "0") Long version
) {
}
