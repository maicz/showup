-- "group" is a reserved SQL word, so the table is named "meetup_group" to avoid a permanent
-- quoting tax in every hand-written query. See docs/domain-model.md#group.
create table meetup_group (
    id              uuid                     primary key default uuidv7(),
    urlname         varchar(80)              not null unique,
    name            varchar(120)             not null,
    description     text,
    category_id     uuid                     not null references category (id),
    city            varchar(100),
    country         varchar(100),
    location        geography(Point, 4326),
    time_zone       varchar(64)              not null,
    visibility      varchar(20)              not null check (visibility in ('PUBLIC', 'PRIVATE')),
    join_policy     varchar(20)              not null check (join_policy in ('OPEN', 'APPROVAL_REQUIRED', 'INVITE_ONLY')),
    member_count    integer                  not null default 0,
    rating_average  numeric(2, 1),
    rating_count    integer                  not null default 0,
    founded_at      timestamptz,
    status          varchar(20)              not null check (status in ('ACTIVE', 'ARCHIVED')),
    created_at      timestamptz              not null default now(),
    updated_at      timestamptz              not null default now(),
    version         bigint                   not null default 0
);

create index idx_meetup_group_category on meetup_group (category_id);
create index idx_meetup_group_location on meetup_group using gist (location);

create table group_membership (
    id            uuid        primary key default uuidv7(),
    group_id      uuid        not null references meetup_group (id),
    member_id     uuid        not null references member (id),
    role          varchar(30) not null check (role in ('ORGANIZER', 'CO_ORGANIZER', 'ASSISTANT_ORGANIZER', 'EVENT_ORGANIZER', 'MEMBER')),
    status        varchar(20) not null check (status in ('ACTIVE', 'PENDING_APPROVAL', 'BANNED', 'LEFT')),
    joined_at     timestamptz not null,
    introduction  text,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    version       bigint      not null default 0,
    constraint uk_group_membership_group_member unique (group_id, member_id)
);

create index idx_group_membership_member_status on group_membership (member_id, status);

create table group_topic (
    id          uuid        primary key default uuidv7(),
    group_id    uuid        not null references meetup_group (id),
    topic_id    uuid        not null references topic (id),
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    version     bigint      not null default 0,
    constraint uk_group_topic_group_topic unique (group_id, topic_id)
);

create table venue (
    id                   uuid                     primary key default uuidv7(),
    name                 varchar(200)             not null,
    address_line1        varchar(200),
    address_line2        varchar(200),
    city                 varchar(100),
    region               varchar(100),
    postal_code          varchar(20),
    country              varchar(100),
    location             geography(Point, 4326),
    notes                text,
    created_by_group_id  uuid                     not null references meetup_group (id),
    created_at           timestamptz              not null default now(),
    updated_at           timestamptz              not null default now(),
    version              bigint                   not null default 0
);

create index idx_venue_location on venue using gist (location);
