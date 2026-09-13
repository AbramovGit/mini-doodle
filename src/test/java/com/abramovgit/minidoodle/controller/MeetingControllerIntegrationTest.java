package com.abramovgit.minidoodle.controller;

import com.abramovgit.minidoodle.api.MeetingCreateRequest;
import com.abramovgit.minidoodle.api.MeetingUpdateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeetingControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void booksGetsUpdatesAndCancelsMeeting() throws Exception {
        long organizerId = createUser("Organizer", "organizer@example.com", "UTC");
        long attendeeId = createUser("Attendee", "attendee@example.com", "UTC");
        long slotId = createSlots(organizerId,
                slot(Instant.parse("2026-09-17T09:00:00Z"), Instant.parse("2026-09-17T10:00:00Z"))).getFirst();

        var result = mockMvc.perform(post("/api/users/{userId}/slots/{slotId}/meeting", organizerId, slotId)
                        .contentType(APPLICATION_JSON)
                        .content(json(new MeetingCreateRequest("Sprint Planning", "Initial scope", List.of(organizerId, attendeeId)))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = readJson(result);

        long meetingId = created.get("id").asLong();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/api/meetings/" + meetingId);
        assertThat(created.get("slotId").asLong()).isEqualTo(slotId);
        assertThat(created.get("startTime").asText()).isEqualTo("2026-09-17T09:00:00Z");
        assertThat(created.get("endTime").asText()).isEqualTo("2026-09-17T10:00:00Z");
        assertThat(created.get("organizerId").asLong()).isEqualTo(organizerId);
        assertThat(created.get("participantIds").size()).isEqualTo(2);

        JsonNode fetched = readJson(mockMvc.perform(get("/api/meetings/{meetingId}", meetingId))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(fetched).isEqualTo(created);

        JsonNode updated = readJson(mockMvc.perform(patch("/api/meetings/{meetingId}", meetingId)
                        .contentType(APPLICATION_JSON)
                        .content(json(new MeetingUpdateRequest("Sprint Planning Updated", "Final agenda"))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(updated.get("title").asText()).isEqualTo("Sprint Planning Updated");
        assertThat(updated.get("description").asText()).isEqualTo("Final agenda");

        mockMvc.perform(delete("/api/meetings/{meetingId}", meetingId))
                .andExpect(status().isNoContent());

        JsonNode missingMeeting = readJson(mockMvc.perform(get("/api/meetings/{meetingId}", meetingId))
                .andExpect(status().isNotFound())
                .andReturn());
        assertThat(missingMeeting.get("message").asText()).isEqualTo("Meeting not found: " + meetingId);

        JsonNode slots = readJson(mockMvc.perform(get("/api/users/{userId}/slots", organizerId)
                        .param("from", "2026-09-17T08:00:00Z")
                        .param("to", "2026-09-17T11:00:00Z")
                        .param("status", "FREE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(slots.get("content").size()).isEqualTo(1);
        assertThat(slots.get("content").get(0).get("id").asLong()).isEqualTo(slotId);
        assertThat(slots.get("content").get(0).get("status").asText()).isEqualTo("FREE");
    }

    @Test
    void rejectsBookingAlreadyBusySlot() throws Exception {
        long organizerId = createUser("Busy Organizer", "busy-organizer@example.com", "UTC");
        long slotId = createSlots(organizerId,
                slot(Instant.parse("2026-09-17T11:00:00Z"), Instant.parse("2026-09-17T12:00:00Z"))).getFirst();

        bookMeeting(organizerId, slotId, "Daily", null, List.of(organizerId));

        JsonNode error = readJson(mockMvc.perform(post("/api/users/{userId}/slots/{slotId}/meeting", organizerId, slotId)
                        .contentType(APPLICATION_JSON)
                        .content(json(new MeetingCreateRequest("Second Booking", null, List.of(organizerId)))))
                .andExpect(status().isConflict())
                .andReturn());

        assertThat(error.get("status").asInt()).isEqualTo(409);
        assertThat(error.get("message").asText()).isEqualTo("Slot is already booked");
    }

    @Test
    void rejectsDuplicateParticipantIds() throws Exception {
        long organizerId = createUser("Organizer", "meeting-organizer@example.com", "UTC");
        long attendeeId = createUser("Attendee", "meeting-attendee@example.com", "UTC");
        long slotId = createSlots(organizerId,
                slot(Instant.parse("2026-09-17T13:00:00Z"), Instant.parse("2026-09-17T14:00:00Z"))).getFirst();

        JsonNode error = readJson(mockMvc.perform(post("/api/users/{userId}/slots/{slotId}/meeting", organizerId, slotId)
                        .contentType(APPLICATION_JSON)
                        .content(json(new MeetingCreateRequest("Duplicate Participants", null,
                                List.of(attendeeId, attendeeId)))))
                .andExpect(status().isBadRequest())
                .andReturn());

        assertThat(error.get("status").asInt()).isEqualTo(400);
        assertThat(error.get("message").asText()).isEqualTo("participantIds must not contain duplicates");
    }

    @Test
    void rejectsMeetingWithTooManyParticipants() throws Exception {
        long organizerId = createUser("Participant Limit", "participant-limit@example.com", "UTC");
        long slotId = createSlots(organizerId,
                slot(Instant.parse("2026-09-17T15:00:00Z"), Instant.parse("2026-09-17T16:00:00Z"))).getFirst();

        JsonNode error = readJson(mockMvc.perform(post("/api/users/{userId}/slots/{slotId}/meeting", organizerId, slotId)
                        .contentType(APPLICATION_JSON)
                        .content(json(new MeetingCreateRequest("Too Many Participants", null,
                                Collections.nCopies(51, organizerId)))))
                .andExpect(status().isBadRequest())
                .andReturn());

        assertThat(error.get("message").asText()).contains("50");
    }
}
