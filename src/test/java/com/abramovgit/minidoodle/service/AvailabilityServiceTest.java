package com.abramovgit.minidoodle.service;

import com.abramovgit.minidoodle.api.AvailabilityResponse;
import com.abramovgit.minidoodle.domain.Calendar;
import com.abramovgit.minidoodle.domain.Slot;
import com.abramovgit.minidoodle.domain.SlotStatus;
import com.abramovgit.minidoodle.repository.CalendarRepository;
import com.abramovgit.minidoodle.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private CalendarRepository calendarRepository;

    @Mock
    private SlotRepository slotRepository;

    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityService(calendarRepository, slotRepository);
        Calendar calendar = new Calendar();
        calendar.setId(1L);
        when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(calendar));
    }

    @Test
    void clipsMergesAndPaginatesCompleteAvailabilityWindows() {
        Instant from = Instant.parse("2026-09-14T09:30:00Z");
        Instant to = Instant.parse("2026-09-14T13:30:00Z");
        Slot first = slot(Instant.parse("2026-09-14T09:00:00Z"),
                Instant.parse("2026-09-14T10:00:00Z"), SlotStatus.FREE);
        Slot second = slot(Instant.parse("2026-09-14T10:00:00Z"),
                Instant.parse("2026-09-14T11:00:00Z"), SlotStatus.FREE);
        Slot third = slot(Instant.parse("2026-09-14T11:00:00Z"),
                Instant.parse("2026-09-14T12:00:00Z"), SlotStatus.BUSY);
        PageRequest pageRequest = PageRequest.of(0, 2);
        when(slotRepository.findAllOverlapping(1L, from, to))
                .thenReturn(List.of(first, second, third));

        AvailabilityResponse response = availabilityService.get(1L, from, to, pageRequest);

        assertEquals(2, response.windows().size());
        assertEquals(from, response.windows().getFirst().startTime());
        assertEquals(Instant.parse("2026-09-14T11:00:00Z"), response.windows().getFirst().endTime());
        assertEquals(SlotStatus.FREE, response.windows().getFirst().status());
        assertEquals(SlotStatus.BUSY, response.windows().get(1).status());
        assertEquals(Instant.parse("2026-09-14T11:00:00Z"), response.windows().get(1).startTime());
        assertEquals(to, response.windows().get(1).endTime());
        assertEquals(3, response.totalSlots());
        assertEquals(2, response.totalWindows());
    }

    @Test
    void paginatesAggregatedWindowsRatherThanSlots() {
        Instant from = Instant.parse("2026-09-14T09:00:00Z");
        Instant to = Instant.parse("2026-09-14T13:00:00Z");
        Slot free = slot(Instant.parse("2026-09-14T09:00:00Z"),
                Instant.parse("2026-09-14T10:00:00Z"), SlotStatus.FREE);
        Slot secondFree = slot(Instant.parse("2026-09-14T11:00:00Z"),
                Instant.parse("2026-09-14T12:00:00Z"), SlotStatus.FREE);
        when(slotRepository.findAllOverlapping(1L, from, to)).thenReturn(List.of(free, secondFree));

        AvailabilityResponse response = availabilityService.get(1L, from, to, PageRequest.of(1, 1));

        assertEquals(1, response.windows().size());
        assertEquals(Instant.parse("2026-09-14T10:00:00Z"), response.windows().getFirst().startTime());
        assertEquals(Instant.parse("2026-09-14T11:00:00Z"), response.windows().getFirst().endTime());
        assertEquals(SlotStatus.BUSY, response.windows().getFirst().status());
        assertEquals(2, response.totalSlots());
        assertEquals(4, response.totalWindows());
    }

    private Slot slot(Instant start, Instant end, SlotStatus status) {
        Slot slot = new Slot();
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setStatus(status);
        return slot;
    }
}
