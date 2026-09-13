package com.abramovgit.minidoodle.repository;

import com.abramovgit.minidoodle.domain.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    Optional<Calendar> findByUserId(Long userId);
}
