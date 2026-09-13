package com.abramovgit.minidoodle.api;

import java.time.Instant;
import java.util.List;

public record MeetingResponse(
        Long id,
        Long slotId,
        Instant startTime,
        Instant endTime,
        String title,
        String description,
        Long organizerId,
        List<Long> participantIds
) {
}
