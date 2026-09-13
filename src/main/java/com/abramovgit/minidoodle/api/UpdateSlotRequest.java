package com.abramovgit.minidoodle.api;

import com.abramovgit.minidoodle.domain.SlotStatus;
import jakarta.validation.constraints.AssertTrue;

import java.time.Instant;

public record UpdateSlotRequest(Instant startTime, Instant endTime, SlotStatus status) {

    @AssertTrue(message = "at least one slot field must be provided")
    public boolean hasChanges() {
        return startTime != null || endTime != null || status != null;
    }
}
