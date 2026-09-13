package com.abramovgit.minidoodle.service;

import com.abramovgit.minidoodle.api.AvailabilityResponse;
import com.abramovgit.minidoodle.api.AvailabilityWindow;
import com.abramovgit.minidoodle.domain.Calendar;
import com.abramovgit.minidoodle.domain.Slot;
import com.abramovgit.minidoodle.repository.CalendarRepository;
import com.abramovgit.minidoodle.repository.SlotRepository;
import com.abramovgit.minidoodle.exception.InvalidSlotException;
import com.abramovgit.minidoodle.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    public AvailabilityResponse get(Long userId, Instant from, Instant to, Pageable pageable) {
        Calendar calendar = calendarRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (!from.isBefore(to)) {
            throw new InvalidSlotException("from must be before to");
        }

        Page<Slot> slots = slotRepository.findOverlapping(calendar.getId(), from, to, pageable);
        List<AvailabilityWindow> windows = aggregate(slots.getContent(), from, to);
        return new AvailabilityResponse(windows, slots.getNumber(), slots.getSize(), slots.getTotalElements());
    }

    private List<AvailabilityWindow> aggregate(List<Slot> slots, Instant from, Instant to) {
        List<AvailabilityWindow> windows = new ArrayList<>();
        for (Slot slot : slots) {
            Instant start = slot.getStartTime().isAfter(from) ? slot.getStartTime() : from;
            Instant end = slot.getEndTime().isBefore(to) ? slot.getEndTime() : to;
            AvailabilityWindow current = new AvailabilityWindow(start, end, slot.getStatus());

            if (!windows.isEmpty()) {
                AvailabilityWindow previous = windows.getLast();
                if (previous.status() == current.status() && previous.endTime().equals(current.startTime())) {
                    windows.set(windows.size() - 1,
                            new AvailabilityWindow(previous.startTime(), current.endTime(), current.status()));
                    continue;
                }
            }
            windows.add(current);
        }
        return windows;
    }
}
