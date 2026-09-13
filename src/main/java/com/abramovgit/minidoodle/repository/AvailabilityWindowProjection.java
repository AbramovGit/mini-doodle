package com.abramovgit.minidoodle.repository;

import java.time.Instant;

public interface AvailabilityWindowProjection {

    Instant getStartTime();

    Instant getEndTime();

    String getStatus();
}
