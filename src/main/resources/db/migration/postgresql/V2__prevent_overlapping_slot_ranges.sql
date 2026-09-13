CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE slots
    ADD CONSTRAINT ex_slots_calendar_time_range
    EXCLUDE USING gist (
        calendar_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    );
