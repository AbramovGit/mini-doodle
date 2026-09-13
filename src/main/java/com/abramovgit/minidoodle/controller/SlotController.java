package com.abramovgit.minidoodle.controller;

import com.abramovgit.minidoodle.api.CreateSlotsRequest;
import com.abramovgit.minidoodle.api.SlotResponse;
import com.abramovgit.minidoodle.api.UpdateSlotRequest;
import com.abramovgit.minidoodle.domain.SlotStatus;
import com.abramovgit.minidoodle.service.SlotService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/slots")
@RequiredArgsConstructor
@Tag(name = "Slots", description = "Free and busy time slot management")
public class SlotController {

    private final SlotService slotService;

    @PostMapping
    @Operation(summary = "Create one or more free slots")
    public ResponseEntity<List<SlotResponse>> create(@PathVariable Long userId,
                                                      @Valid @RequestBody CreateSlotsRequest request) {
        return ResponseEntity.ok(slotService.create(userId, request));
    }

    @PatchMapping("/{slotId}")
    @Operation(summary = "Update a slot time range")
    public SlotResponse update(@PathVariable Long userId,
                               @PathVariable Long slotId,
                               @Valid @RequestBody UpdateSlotRequest request) {
        return slotService.update(userId, slotId, request);
    }

    @DeleteMapping("/{slotId}")
    @Operation(summary = "Delete a free slot")
    public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable Long slotId) {
        slotService.delete(userId, slotId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List slots intersecting a time range")
    public Page<SlotResponse> list(@PathVariable Long userId,
                                   @RequestParam Instant from,
                                   @RequestParam Instant to,
                                   @RequestParam(required = false) SlotStatus status,
                                   @PageableDefault(sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable) {
        return slotService.list(userId, from, to, status, pageable);
    }
}
