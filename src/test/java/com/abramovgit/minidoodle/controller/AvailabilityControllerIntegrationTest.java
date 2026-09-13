package com.abramovgit.minidoodle.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvailabilityControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void returnsBusyGapsAndEvictsCachedAvailabilityDuringBookingLifecycle() throws Exception {
        long organizerId = createUser("Availability Owner", "availability-owner@example.com", "UTC");
        createUser("Availability Attendee", "availability-attendee@example.com", "UTC");
        List<Long> slotIds = createSlots(organizerId,
                slot(Instant.parse("2026-09-18T09:00:00Z"), Instant.parse("2026-09-18T10:00:00Z")),
                slot(Instant.parse("2026-09-18T11:00:00Z"), Instant.parse("2026-09-18T12:00:00Z")));

        JsonNode initial = availability(organizerId);
        assertWindow(initial, 0, "2026-09-18T08:30:00Z", "2026-09-18T09:00:00Z", "BUSY");
        assertWindow(initial, 1, "2026-09-18T09:00:00Z", "2026-09-18T10:00:00Z", "FREE");
        assertWindow(initial, 2, "2026-09-18T10:00:00Z", "2026-09-18T11:00:00Z", "BUSY");
        assertWindow(initial, 3, "2026-09-18T11:00:00Z", "2026-09-18T12:00:00Z", "FREE");
        assertWindow(initial, 4, "2026-09-18T12:00:00Z", "2026-09-18T12:30:00Z", "BUSY");
        assertThat(initial.get("totalSlots").asLong()).isEqualTo(2L);
        assertThat(initial.get("totalWindows").asLong()).isEqualTo(5L);
        assertThat(initial.get("page").asInt()).isZero();
        assertThat(initial.get("size").asInt()).isEqualTo(10);

        JsonNode paged = availability(organizerId, 1, 2);
        assertThat(paged.get("page").asInt()).isEqualTo(1);
        assertThat(paged.get("size").asInt()).isEqualTo(2);
        assertThat(paged.get("windows").size()).isEqualTo(2);
        assertWindow(paged, 0, "2026-09-18T10:00:00Z", "2026-09-18T11:00:00Z", "BUSY");
        assertWindow(paged, 1, "2026-09-18T11:00:00Z", "2026-09-18T12:00:00Z", "FREE");

        long meetingId = bookMeeting(organizerId, slotIds.get(1), "Booked", null, List.of(organizerId));

        JsonNode afterBooking = availability(organizerId);
        assertThat(afterBooking.get("windows").size()).isEqualTo(3);
        assertWindow(afterBooking, 0, "2026-09-18T08:30:00Z", "2026-09-18T09:00:00Z", "BUSY");
        assertWindow(afterBooking, 1, "2026-09-18T09:00:00Z", "2026-09-18T10:00:00Z", "FREE");
        assertWindow(afterBooking, 2, "2026-09-18T10:00:00Z", "2026-09-18T12:30:00Z", "BUSY");
        assertThat(afterBooking.get("totalSlots").asLong()).isEqualTo(2L);
        assertThat(afterBooking.get("totalWindows").asLong()).isEqualTo(3L);

        mockMvc.perform(delete("/api/meetings/{meetingId}", meetingId))
                .andExpect(status().isNoContent());

        JsonNode afterCancellation = availability(organizerId);
        assertThat(afterCancellation).isEqualTo(initial);
    }

    @Test
    void rejectsInvalidAvailabilityRange() throws Exception {
        long userId = createUser("Range Owner", "range-owner@example.com", "UTC");

        JsonNode error = readJson(mockMvc.perform(get("/api/users/{userId}/availability", userId)
                        .param("from", "2026-09-18T12:00:00Z")
                        .param("to", "2026-09-18T12:00:00Z")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andReturn());

        assertThat(error.get("status").asInt()).isEqualTo(400);
        assertThat(error.get("message").asText()).isEqualTo("from must be before to");
    }

    private JsonNode availability(long userId) throws Exception {
        return availability(userId, 0, 10);
    }

    private JsonNode availability(long userId, int page, int size) throws Exception {
        return readJson(mockMvc.perform(get("/api/users/{userId}/availability", userId)
                        .param("from", "2026-09-18T08:30:00Z")
                        .param("to", "2026-09-18T12:30:00Z")
                        .param("page", Integer.toString(page))
                        .param("size", Integer.toString(size)))
                .andExpect(status().isOk())
                .andReturn());
    }

    private void assertWindow(JsonNode response, int index, String startTime, String endTime, String status) {
        JsonNode window = response.get("windows").get(index);
        assertThat(window.get("startTime").asText()).isEqualTo(startTime);
        assertThat(window.get("endTime").asText()).isEqualTo(endTime);
        assertThat(window.get("status").asText()).isEqualTo(status);
    }
}
