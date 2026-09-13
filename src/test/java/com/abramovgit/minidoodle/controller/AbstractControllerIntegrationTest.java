package com.abramovgit.minidoodle.controller;

import com.abramovgit.minidoodle.api.CreateSlotsRequest;
import com.abramovgit.minidoodle.api.MeetingCreateRequest;
import com.abramovgit.minidoodle.api.SlotRequest;
import com.abramovgit.minidoodle.api.UserCreateRequest;
import com.abramovgit.minidoodle.repository.CalendarRepository;
import com.abramovgit.minidoodle.repository.MeetingRepository;
import com.abramovgit.minidoodle.repository.SlotRepository;
import com.abramovgit.minidoodle.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractControllerIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    @AfterEach
    void resetState() {
        clearCaches();
        meetingRepository.deleteAll();
        meetingRepository.flush();
        slotRepository.deleteAll();
        slotRepository.flush();
        calendarRepository.deleteAll();
        calendarRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();
        clearCaches();
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    protected long createUser(String name, String email, String timezone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(json(new UserCreateRequest(name, email, timezone))))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
    }

    protected List<Long> createSlots(long userId, SlotRequest... slots) throws Exception {
        JsonNode response = createSlotsResponse(userId, slots);
        return IntStream.range(0, response.size())
                .mapToObj(index -> response.get(index).get("id").asLong())
                .toList();
    }

    protected JsonNode createSlotsResponse(long userId, SlotRequest... slots) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/{userId}/slots", userId)
                        .contentType(APPLICATION_JSON)
                        .content(json(new CreateSlotsRequest(Arrays.asList(slots)))))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result);
    }

    protected long bookMeeting(long userId, long slotId, String title, String description,
                               List<Long> participantIds) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/{userId}/slots/{slotId}/meeting", userId, slotId)
                        .contentType(APPLICATION_JSON)
                        .content(json(new MeetingCreateRequest(title, description, participantIds))))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
    }

    protected SlotRequest slot(Instant startTime, Instant endTime) {
        return new SlotRequest(startTime, endTime);
    }

    private void clearCaches() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
