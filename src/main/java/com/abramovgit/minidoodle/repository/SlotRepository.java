package com.abramovgit.minidoodle.repository;

import com.abramovgit.minidoodle.domain.Slot;
import com.abramovgit.minidoodle.domain.SlotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    @Query("""
            select s from Slot s
            where s.calendar.id = :calendarId
              and s.startTime < :to
              and s.endTime > :from
            order by s.startTime asc
            """)
    Page<Slot> findOverlapping(@Param("calendarId") Long calendarId,
                               @Param("from") Instant from,
                               @Param("to") Instant to,
                               Pageable pageable);

    @Query("""
            select s from Slot s
            where s.calendar.id = :calendarId
              and s.startTime < :to
              and s.endTime > :from
            order by s.startTime asc
            """)
    List<Slot> findAllOverlapping(@Param("calendarId") Long calendarId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);

    @Query("""
            select count(s) from Slot s
            where s.calendar.id = :calendarId
              and s.startTime < :to
              and s.endTime > :from
            """)
    long countOverlapping(@Param("calendarId") Long calendarId,
                          @Param("from") Instant from,
                          @Param("to") Instant to);

    @Query(value = """
            with clipped_slots as (
                select greatest(s.start_time, cast(:from as timestamptz)) as start_time,
                       least(s.end_time, cast(:to as timestamptz)) as end_time,
                       s.status
                from slots s
                where s.calendar_id = :calendarId
                  and s.start_time < cast(:to as timestamptz)
                  and s.end_time > cast(:from as timestamptz)
            ),
            slot_gaps as (
                select start_time,
                       end_time,
                       status,
                       lag(end_time) over (order by start_time) as previous_end_time,
                       row_number() over (order by start_time desc) as reverse_row_number
                from clipped_slots
            ),
            segments as (
                select cast(:from as timestamptz) as start_time,
                       start_time as end_time,
                       'BUSY' as status
                from slot_gaps
                where previous_end_time is null
                  and cast(:from as timestamptz) < start_time
                union all
                select previous_end_time,
                       start_time,
                       'BUSY'
                from slot_gaps
                where previous_end_time < start_time
                union all
                select start_time, end_time, status
                from slot_gaps
                union all
                select end_time,
                       cast(:to as timestamptz),
                       'BUSY'
                from slot_gaps
                where reverse_row_number = 1
                  and end_time < cast(:to as timestamptz)
                union all
                select cast(:from as timestamptz),
                       cast(:to as timestamptz),
                       'BUSY'
                where not exists (select 1 from clipped_slots)
            ),
            marked_segments as (
                select start_time,
                       end_time,
                       status,
                       case when lag(status) over (order by start_time) = status
                                      and lag(end_time) over (order by start_time) = start_time
                            then 0
                            else 1
                       end as starts_group
                from segments
            ),
            grouped_segments as (
                select start_time,
                       end_time,
                       status,
                       sum(starts_group) over (order by start_time) as group_number
                from marked_segments
            )
            select min(start_time) as "startTime",
                   max(end_time) as "endTime",
                   min(status) as status
            from grouped_segments
            group by status, group_number
            order by min(start_time)
            limit :limit offset :offset
            """, nativeQuery = true)
    List<AvailabilityWindowProjection> findAggregatedAvailability(@Param("calendarId") Long calendarId,
                                                                   @Param("from") Instant from,
                                                                   @Param("to") Instant to,
                                                                   @Param("limit") int limit,
                                                                   @Param("offset") long offset);

    @Query(value = """
            with clipped_slots as (
                select greatest(s.start_time, cast(:from as timestamptz)) as start_time,
                       least(s.end_time, cast(:to as timestamptz)) as end_time,
                       s.status
                from slots s
                where s.calendar_id = :calendarId
                  and s.start_time < cast(:to as timestamptz)
                  and s.end_time > cast(:from as timestamptz)
            ),
            slot_gaps as (
                select start_time,
                       end_time,
                       status,
                       lag(end_time) over (order by start_time) as previous_end_time,
                       row_number() over (order by start_time desc) as reverse_row_number
                from clipped_slots
            ),
            segments as (
                select cast(:from as timestamptz) as start_time,
                       start_time as end_time,
                       'BUSY' as status
                from slot_gaps
                where previous_end_time is null
                  and cast(:from as timestamptz) < start_time
                union all
                select previous_end_time, start_time, 'BUSY'
                from slot_gaps
                where previous_end_time < start_time
                union all
                select start_time, end_time, status
                from slot_gaps
                union all
                select end_time, cast(:to as timestamptz), 'BUSY'
                from slot_gaps
                where reverse_row_number = 1
                  and end_time < cast(:to as timestamptz)
                union all
                select cast(:from as timestamptz), cast(:to as timestamptz), 'BUSY'
                where not exists (select 1 from clipped_slots)
            ),
            marked_segments as (
                select start_time,
                       end_time,
                       status,
                       case when lag(status) over (order by start_time) = status
                                      and lag(end_time) over (order by start_time) = start_time
                            then 0
                            else 1
                       end as starts_group
                from segments
            ),
            grouped_segments as (
                select start_time,
                       end_time,
                       status,
                       sum(starts_group) over (order by start_time) as group_number
                from marked_segments
            )
            select count(*)
            from (
                select 1
                from grouped_segments
                group by status, group_number
            ) aggregated_windows
            """, nativeQuery = true)
    long countAggregatedAvailability(@Param("calendarId") Long calendarId,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to);

    @Query("""
            select s from Slot s
            where s.calendar.id = :calendarId
              and s.status = :status
              and s.startTime < :to
              and s.endTime > :from
            order by s.startTime asc
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
