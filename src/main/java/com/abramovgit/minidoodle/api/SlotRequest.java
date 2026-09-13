package com.abramovgit.minidoodle.api;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record SlotRequest(
        @NotNull Instant startTime,
        @NotNull Instant endTime
) {
}
