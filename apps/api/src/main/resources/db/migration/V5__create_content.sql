create table event_comment (
    id                  uuid        primary key default uuidv7(),
    event_id            uuid        not null references event (id),
    author_id           uuid        not null references member (id),
    body                text        not null,
    parent_comment_id   uuid        references event_comment (id),
    deleted_at          timestamptz,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),
    version             bigint      not null default 0
);

create index idx_event_comment_event on event_comment (event_id);

create table event_photo (
    id             uuid         primary key default uuidv7(),
    event_id       uuid         not null references event (id),
    uploaded_by_id uuid         not null references member (id),
    url            varchar(500) not null,
    caption        varchar(500),
    width          integer      not null,
    height         integer      not null,
    created_at     timestamptz  not null default now(),
    updated_at     timestamptz  not null default now(),
    version        bigint       not null default 0
);

create index idx_event_photo_event on event_photo (event_id);
