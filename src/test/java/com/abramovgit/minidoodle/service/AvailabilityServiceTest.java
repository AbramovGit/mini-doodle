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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void clipsAndMergesAdjacentWindowsWithSameStatus() {
        Instant from = Instant.parse("2026-09-14T09:30:00Z");
        Instant to = Instant.parse("2026-09-14T12:30:00Z");
        Slot first = slot(Instant.parse("2026-09-14T09:00:00Z"),
                Instant.parse("2026-09-14T10:00:00Z"), SlotStatus.FREE);
        Slot second = slot(Instant.parse("2026-09-14T10:00:00Z"),
                Instant.parse("2026-09-14T11:00:00Z"), SlotStatus.FREE);
        Slot third = slot(Instant.parse("2026-09-14T11:00:00Z"),
                Instant.parse("2026-09-14T12:00:00Z"), SlotStatus.BUSY);
        PageRequest pageRequest = PageRequest.of(0, 50);
        when(slotRepository.findOverlapping(eq(1L), eq(from), eq(to), any()))
                .thenReturn(new PageImpl<>(List.of(first, second, third), pageRequest, 3));

        AvailabilityResponse response = availabilityService.get(1L, from, to, pageRequest);

        assertEquals(2, response.windows().size());
        assertEquals(from, response.windows().getFirst().startTime());
        assertEquals(Instant.parse("2026-09-14T11:00:00Z"), response.windows().getFirst().endTime());
        assertEquals(SlotStatus.FREE, response.windows().getFirst().status());
        assertEquals(SlotStatus.BUSY, response.windows().get(1).status());
    }

    private Slot slot(Instant start, Instant end, SlotStatus status) {
        Slot slot = new Slot();
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setStatus(status);
        return slot;
    }
}
