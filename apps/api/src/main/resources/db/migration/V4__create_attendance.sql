create table ticket (
    id           uuid        primary key default uuidv7(),
    rsvp_id      uuid        not null unique references rsvp (id),
    code         varchar(64) not null unique,
    issued_at    timestamptz not null,
    revoked_at   timestamptz,
    admit_count  integer     not null check (admit_count > 0),
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    version      bigint      not null default 0
);

create table check_in (
    id                 uuid        primary key default uuidv7(),
    ticket_id          uuid        not null unique references ticket (id),
    checked_in_at      timestamptz not null,
    checked_in_by_id   uuid        not null references member (id),
    method             varchar(20) not null check (method in ('QR_SCAN', 'MANUAL')),
    admitted_count     integer     not null check (admitted_count > 0),
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),
    version            bigint      not null default 0
);

create table staff_assignment (
    id                uuid        primary key default uuidv7(),
    event_id          uuid        not null references event (id),
    member_id         uuid        not null references member (id),
    role              varchar(20) not null check (role in ('GREETER', 'SCANNER', 'SETUP', 'AV', 'SPEAKER_LIAISON', 'CLEANUP')),
    shift_starts_at   timestamptz,
    shift_ends_at     timestamptz,
    notes             text,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now(),
    version           bigint      not null default 0,
    constraint uk_staff_assignment_event_member_role unique (event_id, member_id, role)
);

create table event_feedback (
    id            uuid        primary key default uuidv7(),
    event_id      uuid        not null references event (id),
    member_id     uuid        not null references member (id),
    rating        integer     not null check (rating between 1 and 5),
    comment       text,
    submitted_at  timestamptz not null,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    version       bigint      not null default 0,
    constraint uk_event_feedback_event_member unique (event_id, member_id)
);
