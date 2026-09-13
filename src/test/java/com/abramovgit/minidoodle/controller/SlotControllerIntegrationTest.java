package com.abramovgit.minidoodle.controller;

import com.abramovgit.minidoodle.api.CreateSlotsRequest;
import com.abramovgit.minidoodle.api.MeetingCreateRequest;
import com.abramovgit.minidoodle.api.UpdateSlotRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SlotControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void createsListsUpdatesAndDeletesSlots() throws Exception {
        long userId = createUser("Slot Owner", "slot-owner@example.com", "UTC");
        JsonNode created = createSlotsResponse(userId,
                slot(Instant.parse("2026-09-16T09:00:00Z"), Instant.parse("2026-09-16T10:00:00Z")),
                slot(Instant.parse("2026-09-16T11:00:00Z"), Instant.parse("2026-09-16T12:00:00Z")),
                slot(Instant.parse("2026-09-16T13:00:00Z"), Instant.parse("2026-09-16T14:00:00Z")));

        assertThat(created.size()).isEqualTo(3);
        long firstSlotId = created.get(0).get("id").asLong();
        long secondSlotId = created.get(1).get("id").asLong();
        long bookedSlotId = created.get(2).get("id").asLong();
        assertThat(created.get(0).get("status").asText()).isEqualTo("FREE");
        assertThat(created.get(0).get("version").asLong()).isZero();
        bookMeeting(userId, bookedSlotId, "Booked", null, List.of(userId));

        JsonNode listed = readJson(mockMvc.perform(get("/api/users/{userId}/slots", userId)
                        .param("from", "2026-09-16T08:00:00Z")
                        .param("to", "2026-09-16T15:00:00Z")
                        .param("status", "FREE")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(listed.get("content").size()).isEqualTo(1);
        assertThat(listed.get("totalElements").asLong()).isEqualTo(2L);
        assertThat(listed.get("number").asInt()).isEqualTo(1);
        assertThat(listed.get("size").asInt()).isEqualTo(1);
        assertThat(listed.get("content").get(0).get("id").asLong()).isEqualTo(secondSlotId);

        JsonNode allSlots = readJson(mockMvc.perform(get("/api/users/{userId}/slots", userId)
                        .param("from", "2026-09-16T08:00:00Z")
                        .param("to", "2026-09-16T15:00:00Z")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(allSlots.get("content").size()).isEqualTo(3);
        assertThat(allSlots.get("totalElements").asLong()).isEqualTo(3L);

        JsonNode updated = readJson(mockMvc.perform(patch("/api/users/{userId}/slots/{slotId}", userId, firstSlotId)
                        .contentType(APPLICATION_JSON)
                        .content(json(new UpdateSlotRequest(
                                Instant.parse("2026-09-16T09:30:00Z"),
                                Instant.parse("2026-09-16T10:30:00Z"),
                                null))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(updated.get("id").asLong()).isEqualTo(firstSlotId);
        assertThat(updated.get("startTime").asText()).isEqualTo("2026-09-16T09:30:00Z");
        assertThat(updated.get("endTime").asText()).isEqualTo("2026-09-16T10:30:00Z");
        assertThat(updated.get("status").asText()).isEqualTo("FREE");
        assertThat(updated.get("version").asLong()).isEqualTo(1L);

        mockMvc.perform(delete("/api/users/{userId}/slots/{slotId}", userId, secondSlotId))
                .andExpect(status().isNoContent());

        JsonNode afterDelete = readJson(mockMvc.perform(get("/api/users/{userId}/slots", userId)
                        .param("from", "2026-09-16T08:00:00Z")
                        .param("to", "2026-09-16T13:00:00Z")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(afterDelete.get("content").size()).isEqualTo(1);
        assertThat(afterDelete.get("content").get(0).get("id").asLong()).isEqualTo(firstSlotId);
        assertThat(afterDelete.get("totalElements").asLong()).isEqualTo(1L);
    }

    @Test
    void rejectsSlotShorterThanConfiguredMinimumDuration() throws Exception {
        long userId = createUser("Short Slot", "short-slot@example.com", "UTC");

        JsonNode error = readJson(mockMvc.perform(post("/api/users/{userId}/slots", userId)
                        .contentType(APPLICATION_JSON)
                        .content(json(new CreateSlotsRequest(List.of(
                                slot(Instant.parse("2026-09-16T09:00:00Z"), Instant.parse("2026-09-16T09:10:00Z")))))))
                .andExpect(status().isBadRequest())
                .andReturn());

        assertThat(error.get("status").asInt()).isEqualTo(400);
        assertThat(error.get("message").asText()).isEqualTo("Slot duration must be at least PT15M");
    }

    @Test
    void rejectsOverlappingSlotCreation() throws Exception {
        long userId = createUser("Overlap Owner", "overlap-owner@example.com", "UTC");
        createSlotsResponse(userId,
                slot(Instant.parse("2026-09-16T09:00:00Z"), Instant.parse("2026-09-16T10:00:00Z")));

        JsonNode error = readJson(mockMvc.perform(post("/api/users/{userId}/slots", userId)
                        .contentType(APPLICATION_JSON)
                        .content(json(new CreateSlotsRequest(List.of(
                                slot(Instant.parse("2026-09-16T09:30:00Z"), Instant.parse("2026-09-16T10:30:00Z")))))))
                .andExpect(status().isConflict())
                .andReturn());

        assertThat(error.get("status").asInt()).isEqualTo(409);
        assertThat(error.get("message").asText()).isEqualTo("Slot overlaps an existing slot");
    }

    @Test
    void returnsNotFoundWhenUpdatingAnotherUsersSlot() throws Exception {
        long ownerId = createUser("Owner", "owner@example.com", "UTC");
        long otherUserId = createUser("Other", "other@example.com", "UTC");
        long slotId = createSlots(ownerId,
                slot(Instant.parse("2026-09-16T09:00:00Z"), Instant.parse("2026-09-16T10:00:00Z"))).getFirst();

        JsonNode error = readJson(mockMvc.perform(patch("/api/users/{userId}/slots/{slotId}", otherUserId, slotId)
                        .contentType(APPLICATION_JSON)
                        .content(json(new UpdateSlotRequest(
                                Instant.parse("2026-09-16T10:30:00Z"),
                                Instant.parse("2026-09-16T11:30:00Z"),
                                null))))
                .andExpect(status().isNotFound())
                .andReturn());

        assertThat(error.get("status").asInt()).isEqualTo(404);
        assertThat(error.get("message").asText()).isEqualTo("Slot not found: " + slotId);
    }

    @Test
    void rejectsDeletingBusySlot() throws Exception {
        long userId = createUser("Busy Owner", "busy-owner@example.com", "UTC");
        long slotId = createSlots(userId,
                slot(Instant.parse("2026-09-16T09:00:00Z"), Instant.parse("2026-09-16T10:00:00Z"))).getFirst();

        mockMvc.perform(post("/api/users/{userId}/slots/{slotId}/meeting", userId, slotId)
                        .contentType(APPLICATION_JSON)
                        .content(json(new MeetingCreateRequest("Booked", null, List.of(userId)))))
                .andExpect(status().isCreated());

        JsonNode error = readJson(mockMvc.perform(delete("/api/users/{userId}/slots/{slotId}", userId, slotId))
                .andExpect(status().isConflict())
                .andReturn());

        assertThat(error.get("status").asInt()).isEqualTo(409);
        assertThat(error.get("message").asText()).isEqualTo("Booked slots must be cancelled before deletion");
    }
}
