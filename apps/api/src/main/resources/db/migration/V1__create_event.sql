create table event (
    id          uuid         primary key,
    title       varchar(200) not null,
    description text,
    location    varchar(200) not null,
    starts_at   timestamptz  not null,
    capacity    integer      not null check (capacity > 0),
    created_at  timestamptz  not null default now()
);

create index idx_event_starts_at on event (starts_at);

insert into event (id, title, description, location, starts_at, capacity) values
    ('11111111-1111-1111-1111-111111111111', 'Spring Boot Meetup',
     'Monthly deep dive into the Spring ecosystem.', 'Cluj-Napoca',
     now() + interval '7 days', 80),
    ('22222222-2222-2222-2222-222222222222', 'Angular Signals Workshop',
     'Hands-on session on reactive state with signals.', 'Bucharest',
     now() + interval '21 days', 40);
