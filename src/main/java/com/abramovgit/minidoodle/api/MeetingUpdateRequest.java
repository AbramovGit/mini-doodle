package com.abramovgit.minidoodle.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

@Schema(description = "Updates a meeting title and/or description. At least one field is required.")
public record MeetingUpdateRequest(String title, String description) {

    @AssertTrue(message = "at least one meeting field must be provided")
    public boolean hasChanges() {
        return title != null || description != null;
    }
}
