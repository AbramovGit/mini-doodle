package com.abramovgit.minidoodle.repository;

import com.abramovgit.minidoodle.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    Optional<Meeting> findBySlotId(Long slotId);
}
