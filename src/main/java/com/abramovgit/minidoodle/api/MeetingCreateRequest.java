package com.abramovgit.minidoodle.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Books a FREE slot as a meeting. Registered users only may participate.")
public record MeetingCreateRequest(
        @Schema(description = "Meeting title.", example = "Sprint planning")
        @NotBlank String title,
        @Schema(description = "Optional meeting description.", example = "Plan the next sprint.")
        String description,
        @Schema(description = "Non-empty list of registered user IDs without duplicates.", example = "[1,2]")
        @NotEmpty @Size(max = 50) List<@NotNull Long> participantIds
) {
}
