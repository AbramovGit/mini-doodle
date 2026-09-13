package com.abramovgit.minidoodle.controller;

import com.abramovgit.minidoodle.api.AvailabilityResponse;
import com.abramovgit.minidoodle.service.AvailabilityService;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/users/{userId}/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Aggregated free and busy windows")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    @Operation(summary = "Get aggregated availability for a user and time range")
    public AvailabilityResponse get(@PathVariable Long userId,
                                    @RequestParam Instant from,
                                    @RequestParam Instant to,
                                    @ParameterObject
                                    @PageableDefault(size = 50, sort = "startTime", direction = Sort.Direction.ASC)
                                    Pageable pageable) {
        return availabilityService.get(userId, from, to, pageable);
    }
}
