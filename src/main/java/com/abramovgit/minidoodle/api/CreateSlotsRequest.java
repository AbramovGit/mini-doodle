package com.abramovgit.minidoodle.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Creates one or more free time slots for a user.")
public record CreateSlotsRequest(
        @Schema(description = "Slots to create. Each range must be at least 15 minutes and must not overlap an existing slot.",
                example = "[{\"startTime\":\"2026-10-01T09:00:00Z\",\"endTime\":\"2026-10-01T10:00:00Z\"}]")
        @NotEmpty @Size(max = 100) List<@Valid SlotRequest> slots
) {
}
