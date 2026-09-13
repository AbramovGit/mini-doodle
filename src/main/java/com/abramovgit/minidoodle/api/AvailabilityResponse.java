package com.abramovgit.minidoodle.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated free/busy availability. Time without a declared free slot is BUSY.")
public record AvailabilityResponse(
        @Schema(description = "Windows for this result page.") List<AvailabilityWindow> windows,
        @Schema(description = "Zero-based result page.", example = "0") int page,
        @Schema(description = "Maximum windows per page.", example = "50") int size,
        @Schema(description = "Overlapping source slots before aggregation.", example = "2") long totalSlots,
        @Schema(description = "Total windows after aggregation.", example = "3") long totalWindows
) {
}
