package com.abramovgit.minidoodle.repository;

import com.abramovgit.minidoodle.domain.Calendar;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    Optional<Calendar> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Calendar c where c.user.id = :userId")
    Optional<Calendar> findByUserIdForUpdate(@Param("userId") Long userId);
}
