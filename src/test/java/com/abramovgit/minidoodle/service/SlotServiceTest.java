package com.abramovgit.minidoodle.service;

import com.abramovgit.minidoodle.api.CreateSlotsRequest;
import com.abramovgit.minidoodle.api.SlotRequest;
import com.abramovgit.minidoodle.api.UpdateSlotRequest;
import com.abramovgit.minidoodle.domain.Calendar;
import com.abramovgit.minidoodle.domain.Slot;
import com.abramovgit.minidoodle.domain.SlotStatus;
import com.abramovgit.minidoodle.exception.ConflictException;
import com.abramovgit.minidoodle.exception.InvalidSlotException;
import com.abramovgit.minidoodle.repository.CalendarRepository;
import com.abramovgit.minidoodle.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    @Mock
    private CalendarRepository calendarRepository;

    @Mock
    private SlotRepository slotRepository;

    private SlotService slotService;
    private Calendar calendar;

    @BeforeEach
    void setUp() {
        slotService = new SlotService(calendarRepository, slotRepository);
        ReflectionTestUtils.setField(slotService, "minimumDuration", Duration.ofMinutes(15));
        calendar = new Calendar();
        calendar.setId(1L);
        when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(calendar));
        when(calendarRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(calendar));
    }

    @Test
    void rejectsEndBeforeStart() {
        SlotRequest request = new SlotRequest(
                Instant.parse("2026-09-13T10:00:00Z"),
                Instant.parse("2026-09-13T09:00:00Z"));

        assertThrows(InvalidSlotException.class,
                () -> slotService.create(1L, new CreateSlotsRequest(List.of(request))));
        verify(slotRepository, never()).existsOverlapping(anyLong(), any(), any());
    }

    @Test
    void rejectsOverlappingSlot() {
        when(slotRepository.existsOverlapping(anyLong(), any(), any())).thenReturn(true);
        SlotRequest request = new SlotRequest(
                Instant.parse("2026-09-13T10:00:00Z"),
                Instant.parse("2026-09-13T11:00:00Z"));

        assertThrows(ConflictException.class,
                () -> slotService.create(1L, new CreateSlotsRequest(List.of(request))));
        verify(slotRepository, never()).saveAll(any());
    }

    @Test
    void rejectsChangingBusyStatusThroughSlotApi() {
        Slot slot = new Slot();
        slot.setId(10L);
        slot.setCalendar(calendar);
        slot.setStartTime(Instant.parse("2026-09-13T10:00:00Z"));
        slot.setEndTime(Instant.parse("2026-09-13T11:00:00Z"));
        slot.setStatus(SlotStatus.BUSY);
        when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(slotRepository.existsOverlappingExcluding(anyLong(), anyLong(), any(), any())).thenReturn(false);

        assertThrows(ConflictException.class,
                () -> slotService.update(1L, 10L, new UpdateSlotRequest(null, null, SlotStatus.FREE)));
    }
}
