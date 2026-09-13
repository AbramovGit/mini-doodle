package com.abramovgit.minidoodle.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MeetingCreateRequest(
        @NotBlank String title,
        String description,
        @NotEmpty List<Long> participantIds
) {
}
