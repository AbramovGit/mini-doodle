package com.abramovgit.minidoodle.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingCreateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNullParticipantId() {
        MeetingCreateRequest request = new MeetingCreateRequest("Planning", null,
                Collections.singletonList(null));

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsNonNullParticipantId() {
        MeetingCreateRequest request = new MeetingCreateRequest("Planning", null,
                Collections.singletonList(1L));

        assertTrue(validator.validate(request).isEmpty());
    }
}
