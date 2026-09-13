package com.abramovgit.minidoodle.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registered user.")
public record UserResponse(Long id, String name, String email, String timezone) {
}
