package com.abramovgit.minidoodle.controller;

import com.abramovgit.minidoodle.api.MeetingCreateRequest;
import com.abramovgit.minidoodle.api.MeetingResponse;
import com.abramovgit.minidoodle.api.MeetingUpdateRequest;
import com.abramovgit.minidoodle.service.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping("/users/{userId}/slots/{slotId}/meeting")
    public ResponseEntity<MeetingResponse> book(@PathVariable Long userId,
                                                @PathVariable Long slotId,
                                                @Valid @RequestBody MeetingCreateRequest request,
                                                UriComponentsBuilder uriBuilder) {
        MeetingResponse response = meetingService.book(userId, slotId, request);
        URI location = uriBuilder.path("/api/meetings/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/meetings/{meetingId}")
    public MeetingResponse get(@PathVariable Long meetingId) {
        return meetingService.get(meetingId);
    }

    @PatchMapping("/meetings/{meetingId}")
    public MeetingResponse update(@PathVariable Long meetingId,
                                  @Valid @RequestBody MeetingUpdateRequest request) {
        return meetingService.update(meetingId, request);
    }

    @DeleteMapping("/meetings/{meetingId}")
    public ResponseEntity<Void> cancel(@PathVariable Long meetingId) {
        meetingService.cancel(meetingId);
        return ResponseEntity.noContent().build();
    }
}
