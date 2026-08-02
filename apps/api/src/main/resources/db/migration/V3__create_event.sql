create table event_series (
    id                          uuid         primary key default uuidv7(),
    group_id                    uuid         not null references meetup_group (id),
    recurrence_rule             text         not null,
    until                       timestamptz,
    template_title              varchar(200) not null,
    template_description        text,
    template_venue_id           uuid         references venue (id),
    template_duration_minutes   integer      not null,
    created_at                  timestamptz  not null default now(),
    updated_at                  timestamptz  not null default now(),
    version                     bigint       not null default 0
);

create table event (
    id                       uuid         primary key default uuidv7(),
    group_id                 uuid         not null references meetup_group (id),
    series_id                uuid         references event_series (id),
    title                    varchar(200) not null,
    description              text,
    status                   varchar(20)  not null check (status in ('DRAFT', 'PUBLISHED', 'CANCELLED')),
    format                   varchar(20)  not null check (format in ('IN_PERSON', 'ONLINE', 'HYBRID')),
    venue_id                 uuid         references venue (id),
    online_url               varchar(500),
    starts_at                timestamptz  not null,
    ends_at                  timestamptz,
    time_zone                varchar(64)  not null,
    capacity                 integer      check (capacity > 0),
    waitlist_enabled         boolean      not null default false,
    guests_per_rsvp_limit    integer      not null default 0,
    fee_amount_minor         bigint       not null default 0,
    fee_currency             varchar(3)   not null default 'USD',
    rsvp_opens_at            timestamptz,
    rsvp_closes_at           timestamptz,
    visibility               varchar(20)  not null check (visibility in ('PUBLIC', 'MEMBERS_ONLY')),
    yes_rsvp_count           integer      not null default 0,
    waitlist_count           integer      not null default 0,
    created_at               timestamptz  not null default now(),
    updated_at               timestamptz  not null default now(),
    version                  bigint       not null default 0
);

create index idx_event_group_starts_at on event (group_id, starts_at desc);
create index idx_event_published_starts_at on event (status, starts_at) where status = 'PUBLISHED';

create table event_host (
    id          uuid        primary key default uuidv7(),
    event_id    uuid        not null references event (id),
    member_id   uuid        not null references member (id),
    role        varchar(20) not null check (role in ('HOST', 'CO_HOST')),
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    version     bigint      not null default 0,
    constraint uk_event_host_event_member unique (event_id, member_id)
);

create table rsvp (
    id                  uuid        primary key default uuidv7(),
    event_id            uuid        not null references event (id),
    member_id           uuid        not null references member (id),
    status              varchar(20) not null check (status in ('YES', 'NO', 'WAITLISTED')),
    guest_count         integer     not null default 0,
    waitlist_position   integer,
    responded_at        timestamptz not null,
    promoted_at         timestamptz,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),
    version             bigint      not null default 0,
    constraint uk_rsvp_event_member unique (event_id, member_id)
);

create index idx_rsvp_member_created_at on rsvp (member_id, created_at desc);
create index idx_rsvp_event_status on rsvp (event_id, status);

-- Demo data for the walking skeleton's "list events" slice — see ../../../../../README.md.
with demo_category as (
    insert into category (slug, name, display_order)
    values ('demo', 'Demo', 0)
    returning id
), demo_group as (
    insert into meetup_group (urlname, name, category_id, time_zone, visibility, join_policy, status)
    select 'showup-demo', 'ShowUp Demo Group', id, 'Europe/Bucharest', 'PUBLIC', 'OPEN', 'ACTIVE'
    from demo_category
    returning id
)
insert into event (group_id, title, description, status, format, online_url, starts_at, time_zone, capacity, visibility)
select id, 'Spring Boot Meetup', 'Monthly deep dive into the Spring ecosystem.', 'PUBLISHED', 'ONLINE',
       'https://example.com/spring-boot-meetup', now() + interval '7 days', 'Europe/Bucharest', 80, 'PUBLIC'
from demo_group
union all
select id, 'Angular Signals Workshop', 'Hands-on session on reactive state with signals.', 'PUBLISHED', 'ONLINE',
       'https://example.com/angular-signals-workshop', now() + interval '21 days', 'Europe/Bucharest', 40, 'PUBLIC'
from demo_group;
