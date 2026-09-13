package com.abramovgit.minidoodle.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Registers a user and creates their implicit calendar.")
public record UserCreateRequest(
        @Schema(description = "Display name.", example = "Ada Lovelace")
        @NotBlank String name,
        @Schema(description = "Unique email address.", example = "ada@example.com")
        @NotBlank @Email String email,
        @Schema(description = "IANA timezone for display only; all timestamps are stored in UTC.", example = "Europe/London")
        @NotBlank String timezone
) {
}
