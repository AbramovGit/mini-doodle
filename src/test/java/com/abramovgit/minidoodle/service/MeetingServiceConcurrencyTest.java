package com.abramovgit.minidoodle.service;

import com.abramovgit.minidoodle.api.CreateSlotsRequest;
import com.abramovgit.minidoodle.api.MeetingCreateRequest;
import com.abramovgit.minidoodle.api.SlotRequest;
import com.abramovgit.minidoodle.api.SlotResponse;
import com.abramovgit.minidoodle.api.UserCreateRequest;
import com.abramovgit.minidoodle.api.UserResponse;
import com.abramovgit.minidoodle.exception.ConflictException;
import com.abramovgit.minidoodle.repository.CalendarRepository;
import com.abramovgit.minidoodle.repository.MeetingRepository;
import com.abramovgit.minidoodle.repository.SlotRepository;
import com.abramovgit.minidoodle.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class MeetingServiceConcurrencyTest {

    @Autowired
    private UserService userService;

    @Autowired
    private SlotService slotService;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanDatabase() {
        meetingRepository.deleteAll();
        slotRepository.deleteAll();
        calendarRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void onlyOneConcurrentBookingSucceeds() throws Exception {
        UserResponse organizer = userService.create(
                new UserCreateRequest("Organizer", "organizer@example.com", "UTC"));
        SlotResponse slot = slotService.create(organizer.id(), new CreateSlotsRequest(List.of(
                new SlotRequest(Instant.parse("2026-09-14T10:00:00Z"), Instant.parse("2026-09-14T11:00:00Z"))
        ))).getFirst();
        MeetingCreateRequest request = new MeetingCreateRequest("Planning", null, List.of(organizer.id()));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> book(start, organizer.id(), slot.id(), request));
            Future<Boolean> second = executor.submit(() -> book(start, organizer.id(), slot.id(), request));
            start.countDown();

            assertEquals(1, (first.get() ? 1 : 0) + (second.get() ? 1 : 0));
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean book(CountDownLatch start, Long organizerId, Long slotId, MeetingCreateRequest request)
            throws InterruptedException, ExecutionException {
        start.await();
        try {
            meetingService.book(organizerId, slotId, request);
            return true;
        } catch (ConflictException exception) {
            return false;
        }
    }
}
