create extension if not exists citext;
create extension if not exists postgis;

create table member (
    id                 uuid                     primary key default uuidv7(),
    email              citext                   not null unique,
    password_hash      varchar(100),
    display_name       varchar(80)              not null,
    bio                text,
    photo_url          varchar(500),
    home_city          varchar(100),
    home_country       varchar(100),
    home_location      geography(Point, 4326),
    status             varchar(20)              not null check (status in ('ACTIVE', 'SUSPENDED', 'DEACTIVATED')),
    email_verified_at  timestamptz,
    created_at         timestamptz              not null default now(),
    updated_at         timestamptz              not null default now(),
    version            bigint                   not null default 0
);

create table member_identity (
    id          uuid        primary key default uuidv7(),
    member_id   uuid        not null references member (id) on delete cascade,
    provider    varchar(20) not null check (provider in ('GOOGLE', 'APPLE', 'FACEBOOK')),
    subject     varchar(255) not null,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    version     bigint      not null default 0,
    constraint uk_member_identity_provider_subject unique (provider, subject),
    constraint uk_member_identity_member_provider unique (member_id, provider)
);

create table category (
    id             uuid         primary key default uuidv7(),
    slug           varchar(80)  not null unique,
    name           varchar(120) not null,
    icon_url       varchar(500),
    display_order  integer      not null,
    created_at     timestamptz  not null default now(),
    updated_at     timestamptz  not null default now(),
    version        bigint       not null default 0
);

create table topic (
    id           uuid         primary key default uuidv7(),
    category_id  uuid         not null references category (id),
    slug         varchar(80)  not null unique,
    name         varchar(120) not null,
    group_count  integer      not null default 0,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now(),
    version      bigint       not null default 0
);

create index idx_topic_category on topic (category_id);

create table member_interest (
    id          uuid        primary key default uuidv7(),
    member_id   uuid        not null references member (id) on delete cascade,
    topic_id    uuid        not null references topic (id) on delete cascade,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    version     bigint      not null default 0,
    constraint uk_member_interest_member_topic unique (member_id, topic_id)
);
