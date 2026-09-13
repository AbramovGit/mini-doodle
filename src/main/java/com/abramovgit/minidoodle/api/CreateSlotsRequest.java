package com.abramovgit.minidoodle.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateSlotsRequest(@NotEmpty List<@Valid SlotRequest> slots) {
}
