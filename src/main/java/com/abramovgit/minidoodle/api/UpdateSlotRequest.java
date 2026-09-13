package com.abramovgit.minidoodle.api;

import com.abramovgit.minidoodle.domain.SlotStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

import java.time.Instant;

@Schema(description = "Updates one or more slot fields. A BUSY slot cannot be made FREE through this endpoint; cancel its meeting instead.")
public record UpdateSlotRequest(Instant startTime, Instant endTime, SlotStatus status) {

    @AssertTrue(message = "at least one slot field must be provided")
    public boolean hasChanges() {
        return startTime != null || endTime != null || status != null;
    }
}
