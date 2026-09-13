package com.abramovgit.minidoodle.service;

import com.abramovgit.minidoodle.api.CreateSlotsRequest;
import com.abramovgit.minidoodle.api.SlotRequest;
import com.abramovgit.minidoodle.api.SlotResponse;
import com.abramovgit.minidoodle.api.UpdateSlotRequest;
import com.abramovgit.minidoodle.domain.Calendar;
import com.abramovgit.minidoodle.domain.Slot;
import com.abramovgit.minidoodle.domain.SlotStatus;
import com.abramovgit.minidoodle.exception.ConflictException;
import com.abramovgit.minidoodle.exception.InvalidSlotException;
import com.abramovgit.minidoodle.exception.ResourceNotFoundException;
import com.abramovgit.minidoodle.repository.CalendarRepository;
import com.abramovgit.minidoodle.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final CalendarRepository calendarRepository;
    private final SlotRepository slotRepository;

    @Value("${app.slot.minimum-duration:PT15M}")
    private Duration minimumDuration;

    @Transactional
    @CacheEvict(cacheNames = "availability", allEntries = true)
    public List<SlotResponse> create(Long userId, CreateSlotsRequest request) {
        Calendar calendar = getCalendarForUpdate(userId);
        List<Slot> newSlots = new ArrayList<>();

        for (SlotRequest slotRequest : request.slots()) {
            validateTimeRange(slotRequest.startTime(), slotRequest.endTime());
            ensureNoOverlap(calendar.getId(), slotRequest.startTime(), slotRequest.endTime(), null, newSlots);

            Slot slot = new Slot();
            slot.setCalendar(calendar);
            slot.setStartTime(slotRequest.startTime());
            slot.setEndTime(slotRequest.endTime());
            slot.setStatus(SlotStatus.FREE);
            newSlots.add(slot);
        }

        return slotRepository.saveAll(newSlots).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<SlotResponse> list(Long userId, Instant from, Instant to, SlotStatus status, Pageable pageable) {
        Calendar calendar = getCalendar(userId);
        if (!from.isBefore(to)) {
            throw new InvalidSlotException("from must be before to");
        }

        Page<Slot> slots = status == null
                ? slotRepository.findOverlapping(calendar.getId(), from, to, pageable)
                : slotRepository.findOverlappingByStatus(calendar.getId(), status, from, to, pageable);
        return slots.map(this::toResponse);
    }

    @Transactional
    @CacheEvict(cacheNames = "availability", allEntries = true)
    public SlotResponse update(Long userId, Long slotId, UpdateSlotRequest request) {
        Calendar calendar = getCalendarForUpdate(userId);
        Slot slot = getUserSlot(userId, slotId);
        Instant startTime = request.startTime() == null ? slot.getStartTime() : request.startTime();
        Instant endTime = request.endTime() == null ? slot.getEndTime() : request.endTime();
        validateTimeRange(startTime, endTime);
        ensureNoOverlap(calendar.getId(), startTime, endTime, slotId, List.of());

        if (request.status() != null && request.status() != slot.getStatus()) {
            throw new ConflictException("Slot status changes must be performed by the booking service");
        }

        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        if (request.status() != null) {
            slot.setStatus(request.status());
        }
        return toResponse(slotRepository.save(slot));
    }

    @Transactional
    @CacheEvict(cacheNames = "availability", allEntries = true)
    public void delete(Long userId, Long slotId) {
        Slot slot = getUserSlot(userId, slotId);
        if (slot.getStatus() == SlotStatus.BUSY) {
            throw new ConflictException("Booked slots must be cancelled before deletion");
        }
        slotRepository.delete(slot);
    }

    private Calendar getCalendar(Long userId) {
        return calendarRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Calendar getCalendarForUpdate(Long userId) {
        return calendarRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Slot getUserSlot(Long userId, Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));
        if (!slot.getCalendar().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Slot not found: " + slotId);
        }
        return slot;
    }

    private void validateTimeRange(Instant startTime, Instant endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new InvalidSlotException("endTime must be after startTime");
        }
        if (Duration.between(startTime, endTime).compareTo(minimumDuration) < 0) {
            throw new InvalidSlotException("Slot duration must be at least " + minimumDuration);
        }
    }

    private void ensureNoOverlap(Long calendarId, Instant startTime, Instant endTime, Long excludedSlotId,
                                 List<Slot> pendingSlots) {
        boolean overlapsExisting = excludedSlotId == null
                ? slotRepository.existsOverlapping(calendarId, startTime, endTime)
                : slotRepository.existsOverlappingExcluding(calendarId, excludedSlotId, startTime, endTime);
        if (overlapsExisting || pendingSlots.stream().anyMatch(slot ->
                startTime.isBefore(slot.getEndTime()) && endTime.isAfter(slot.getStartTime()))) {
            throw new ConflictException("Slot overlaps an existing slot");
        }
    }

    private SlotResponse toResponse(Slot slot) {
        return new SlotResponse(slot.getId(), slot.getStartTime(), slot.getEndTime(),
                slot.getStatus(), slot.getVersion());
    }
}
