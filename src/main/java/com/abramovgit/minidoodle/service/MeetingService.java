package com.abramovgit.minidoodle.service;

import com.abramovgit.minidoodle.api.MeetingCreateRequest;
import com.abramovgit.minidoodle.api.MeetingResponse;
import com.abramovgit.minidoodle.api.MeetingUpdateRequest;
import com.abramovgit.minidoodle.domain.Meeting;
import com.abramovgit.minidoodle.domain.Participant;
import com.abramovgit.minidoodle.domain.Slot;
import com.abramovgit.minidoodle.domain.SlotStatus;
import com.abramovgit.minidoodle.domain.User;
import com.abramovgit.minidoodle.exception.ConflictException;
import com.abramovgit.minidoodle.exception.InvalidMeetingException;
import com.abramovgit.minidoodle.exception.ResourceNotFoundException;
import com.abramovgit.minidoodle.repository.MeetingRepository;
import com.abramovgit.minidoodle.repository.SlotRepository;
import com.abramovgit.minidoodle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final MeterRegistry meterRegistry;

    @Transactional
    public MeetingResponse book(Long organizerId, Long slotId, MeetingCreateRequest request) {
        User organizer = getUser(organizerId);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));
        if (!slot.getCalendar().getUser().getId().equals(organizerId)) {
            throw new ResourceNotFoundException("Slot not found: " + slotId);
        }
        if (slot.getStatus() != SlotStatus.FREE) {
            throw new ConflictException("Slot is already booked");
        }

        List<User> participants = getParticipants(request.participantIds());
        slot.setStatus(SlotStatus.BUSY);
        try {
            slotRepository.saveAndFlush(slot);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Slot was booked by another request");
        }

        Meeting meeting = new Meeting();
        meeting.setSlot(slot);
        meeting.setTitle(request.title());
        meeting.setDescription(request.description());
        meeting.setOrganizer(organizer);

        for (User participantUser : participants) {
            Participant participant = new Participant();
            participant.setMeeting(meeting);
            participant.setUser(participantUser);
            meeting.getParticipants().add(participant);
        }
        Meeting savedMeeting = meetingRepository.save(meeting);
        meterRegistry.counter("mini_doodle_meetings_booked").increment();
        return toResponse(savedMeeting);
    }

    @Transactional(readOnly = true)
    public MeetingResponse get(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found: " + meetingId));
    }

    @Transactional
    public MeetingResponse update(Long meetingId, MeetingUpdateRequest request) {
        Meeting meeting = getMeeting(meetingId);
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new InvalidMeetingException("title must not be blank");
            }
            meeting.setTitle(request.title());
        }
        if (request.description() != null) {
            meeting.setDescription(request.description());
        }
        return toResponse(meetingRepository.save(meeting));
    }

    @Transactional
    public void cancel(Long meetingId) {
        Meeting meeting = getMeeting(meetingId);
        Slot slot = meeting.getSlot();
        slot.setStatus(SlotStatus.FREE);
        meetingRepository.delete(meeting);
        slotRepository.save(slot);
        meterRegistry.counter("mini_doodle_meetings_cancelled").increment();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private List<User> getParticipants(List<Long> participantIds) {
        if (participantIds.stream().distinct().count() != participantIds.size()) {
            throw new InvalidMeetingException("participantIds must not contain duplicates");
        }
        List<User> participants = userRepository.findAllById(participantIds);
        if (participants.size() != participantIds.size()) {
            throw new ResourceNotFoundException("One or more participants were not found");
        }
        return participants;
    }

    private Meeting getMeeting(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found: " + meetingId));
    }

    private MeetingResponse toResponse(Meeting meeting) {
        Slot slot = meeting.getSlot();
        List<Long> participantIds = meeting.getParticipants().stream()
                .map(participant -> participant.getUser().getId())
                .toList();
        return new MeetingResponse(meeting.getId(), slot.getId(), slot.getStartTime(), slot.getEndTime(),
                meeting.getTitle(), meeting.getDescription(), meeting.getOrganizer().getId(), participantIds);
    }
}
