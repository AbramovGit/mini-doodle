package com.abramovgit.minidoodle.repository;

import com.abramovgit.minidoodle.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
}
