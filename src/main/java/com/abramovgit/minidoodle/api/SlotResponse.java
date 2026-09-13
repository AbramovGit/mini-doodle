package com.abramovgit.minidoodle.api;

import com.abramovgit.minidoodle.domain.SlotStatus;

import java.time.Instant;

public record SlotResponse(Long id, Instant startTime, Instant endTime, SlotStatus status, Long version) {
}
