package com.abramovgit.minidoodle.controller;

import com.abramovgit.minidoodle.api.UserCreateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Test
    void createsAndFetchesUser() throws Exception {
        var result = mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(json(new UserCreateRequest("Ada Lovelace", "ADA@Example.com", "Europe/London"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = readJson(result);

        long userId = created.get("id").asLong();
        assertThat(userId).isPositive();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/api/users/" + userId);
        assertThat(created.get("name").asText()).isEqualTo("Ada Lovelace");
        assertThat(created.get("email").asText()).isEqualTo("ada@example.com");
        assertThat(created.get("timezone").asText()).isEqualTo("Europe/London");

        JsonNode fetched = readJson(mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(fetched).isEqualTo(created);
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        JsonNode error = readJson(mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(json(new UserCreateRequest("Ada Lovelace", "not-an-email", "UTC"))))
                .andExpect(status().isBadRequest())
                .andReturn());

        assertThat(error.get("status").asInt()).isEqualTo(400);
        assertThat(error.get("error").asText()).isEqualTo("Bad Request");
        assertThat(error.get("message").asText()).contains("well-formed email");
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        createUser("Ada Lovelace", "ada@example.com", "UTC");

        JsonNode error = readJson(mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(json(new UserCreateRequest("Ada Again", "ADA@example.com", "UTC"))))
                .andExpect(status().isConflict())
                .andReturn());

        assertThat(error.get("status").asInt()).isEqualTo(409);
        assertThat(error.get("error").asText()).isEqualTo("Conflict");
        assertThat(error.get("message").asText()).isEqualTo("A user with this email already exists");
    }

    @Test
    void returnsNotFoundForMissingUser() throws Exception {
        JsonNode error = readJson(mockMvc.perform(get("/api/users/{userId}", 9999L))
                .andExpect(status().isNotFound())
                .andReturn());

        assertThat(error.get("status").asInt()).isEqualTo(404);
        assertThat(error.get("error").asText()).isEqualTo("Not Found");
        assertThat(error.get("message").asText()).isEqualTo("User not found: 9999");
    }
}
