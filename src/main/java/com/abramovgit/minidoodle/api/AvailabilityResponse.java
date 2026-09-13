package com.abramovgit.minidoodle.api;

import java.util.List;

public record AvailabilityResponse(
        List<AvailabilityWindow> windows,
        int page,
        int size,
        long totalSlots,
        long totalWindows
) {
}
