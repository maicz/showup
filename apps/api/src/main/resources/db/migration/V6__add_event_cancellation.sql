-- CancelEventRequest carries a reason with nowhere to land: the event table records the status
-- transition but not why or when it happened. Attendees are owed the reason, and organizer
-- reporting needs to tell "cancelled" apart from "never happened".
alter table event
    add column cancelled_at timestamptz,
    add column cancellation_reason text;

comment on column event.cancellation_reason is 'Organizer-supplied explanation shown to attendees; null unless status = CANCELLED.';
