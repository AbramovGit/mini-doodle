package com.abramovgit.minidoodle.api;

import jakarta.validation.constraints.AssertTrue;

public record MeetingUpdateRequest(String title, String description) {

    @AssertTrue(message = "at least one meeting field must be provided")
    public boolean hasChanges() {
        return title != null || description != null;
    }
}
