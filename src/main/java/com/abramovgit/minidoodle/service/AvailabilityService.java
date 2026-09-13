package com.abramovgit.minidoodle.service;

import com.abramovgit.minidoodle.api.AvailabilityResponse;
import com.abramovgit.minidoodle.api.AvailabilityWindow;
import com.abramovgit.minidoodle.domain.Calendar;
import com.abramovgit.minidoodle.domain.Slot;
import com.abramovgit.minidoodle.domain.SlotStatus;
import com.abramovgit.minidoodle.repository.CalendarRepository;
import com.abramovgit.minidoodle.repository.SlotRepository;
import com.abramovgit.minidoodle.exception.InvalidSlotException;
import com.abramovgit.minidoodle.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final CalendarRepository calendarRepository;
    private final SlotRepository slotRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "availability")
    public AvailabilityResponse get(Long userId, Instant from, Instant to, Pageable pageable) {
        Calendar calendar = calendarRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (!from.isBefore(to)) {
            throw new InvalidSlotException("from must be before to");
        }

        List<Slot> slots = slotRepository.findAllOverlapping(calendar.getId(), from, to);
        List<AvailabilityWindow> windows = aggregate(slots, from, to);
        return toResponse(windows, slots.size(), pageable);
    }

    private List<AvailabilityWindow> aggregate(List<Slot> slots, Instant from, Instant to) {
        List<AvailabilityWindow> windows = new ArrayList<>();
        Instant cursor = from;
        for (Slot slot : slots) {
            Instant start = slot.getStartTime().isAfter(from) ? slot.getStartTime() : from;
            Instant end = slot.getEndTime().isBefore(to) ? slot.getEndTime() : to;
            if (cursor.isBefore(start)) {
                appendWindow(windows, new AvailabilityWindow(cursor, start, SlotStatus.BUSY));
            }
            appendWindow(windows, new AvailabilityWindow(start, end, slot.getStatus()));
            cursor = end;
        }
        if (cursor.isBefore(to)) {
            appendWindow(windows, new AvailabilityWindow(cursor, to, SlotStatus.BUSY));
        }
        return windows;
    }

    private void appendWindow(List<AvailabilityWindow> windows, AvailabilityWindow current) {
        if (!windows.isEmpty()) {
            AvailabilityWindow previous = windows.getLast();
            if (previous.status() == current.status() && previous.endTime().equals(current.startTime())) {
                windows.set(windows.size() - 1,
                        new AvailabilityWindow(previous.startTime(), current.endTime(), current.status()));
                return;
            }
        }
        windows.add(current);
    }

    private AvailabilityResponse toResponse(List<AvailabilityWindow> windows, long totalSlots, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new AvailabilityResponse(windows, 0, windows.size(), totalSlots, windows.size());
        }
        long offset = pageable.getOffset();
        int start = offset >= windows.size() ? windows.size() : (int) offset;
        int end = Math.min(start + pageable.getPageSize(), windows.size());
        List<AvailabilityWindow> content = start >= windows.size() ? List.of() : windows.subList(start, end);
        return new AvailabilityResponse(content, pageable.getPageNumber(), pageable.getPageSize(),
                totalSlots, windows.size());
    }
}
