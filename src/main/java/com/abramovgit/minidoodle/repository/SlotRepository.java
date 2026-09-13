package com.abramovgit.minidoodle.repository;

import com.abramovgit.minidoodle.domain.Slot;
import com.abramovgit.minidoodle.domain.SlotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    @Query("""
            select s from Slot s
            where s.calendar.id = :calendarId
              and s.startTime < :to
              and s.endTime > :from
            """)
    Page<Slot> findOverlapping(@Param("calendarId") Long calendarId,
                               @Param("from") Instant from,
                               @Param("to") Instant to,
                               Pageable pageable);

    @Query("""
            select s from Slot s
            where s.calendar.id = :calendarId
              and s.status = :status
              and s.startTime < :to
              and s.endTime > :from
            """)
    Page<Slot> findOverlappingByStatus(@Param("calendarId") Long calendarId,
                                       @Param("status") SlotStatus status,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to,
                                       Pageable pageable);

    @Query("""
            select count(s) > 0 from Slot s
            where s.calendar.id = :calendarId
              and s.startTime < :endTime
              and s.endTime > :startTime
            """)
    boolean existsOverlapping(@Param("calendarId") Long calendarId,
                              @Param("startTime") Instant startTime,
                              @Param("endTime") Instant endTime);

    @Query("""
            select count(s) > 0 from Slot s
            where s.calendar.id = :calendarId
              and s.id <> :slotId
              and s.startTime < :endTime
              and s.endTime > :startTime
            """)
    boolean existsOverlappingExcluding(@Param("calendarId") Long calendarId,
                                       @Param("slotId") Long slotId,
                                       @Param("startTime") Instant startTime,
                                       @Param("endTime") Instant endTime);
}
