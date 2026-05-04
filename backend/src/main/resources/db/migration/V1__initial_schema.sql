create extension if not exists pgcrypto;

create table users (
    id uuid primary key default gen_random_uuid(),
    email varchar(320) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(160) not null,
    created_at timestamp with time zone not null default now()
);

create table organizations (
    id uuid primary key default gen_random_uuid(),
    name varchar(180) not null,
    created_at timestamp with time zone not null default now()
);

create table organization_members (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    role varchar(24) not null,
    created_at timestamp with time zone not null default now(),
    unique (organization_id, user_id)
);

create table projects (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    name varchar(180) not null,
    slug varchar(120) not null unique,
    public_status_enabled boolean not null default true,
    created_at timestamp with time zone not null default now()
);

create table monitors (
    id uuid primary key default gen_random_uuid(),
    project_id uuid not null references projects(id) on delete cascade,
    name varchar(180) not null,
    url text not null,
    method varchar(16) not null,
    expected_status_code integer not null default 200,
    timeout_seconds integer not null default 5,
    interval_seconds integer not null default 300,
    latency_threshold_ms integer not null default 1000,
    failure_threshold integer not null default 3,
    consecutive_failures integer not null default 0,
    enabled boolean not null default true,
    state varchar(24) not null default 'PAUSED',
    headers_json text,
    body text,
    last_checked_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now()
);

create table monitor_checks (
    id uuid primary key default gen_random_uuid(),
    monitor_id uuid not null references monitors(id) on delete cascade,
    checked_at timestamp with time zone not null default now(),
    latency_ms integer,
    status_code integer,
    success boolean not null,
    error_category varchar(80),
    response_snippet text
);

create index idx_monitor_checks_monitor_time on monitor_checks (monitor_id, checked_at desc);

create table incidents (
    id uuid primary key default gen_random_uuid(),
    project_id uuid not null references projects(id) on delete cascade,
    monitor_id uuid not null references monitors(id) on delete cascade,
    title varchar(240) not null,
    status varchar(24) not null,
    started_at timestamp with time zone not null default now(),
    resolved_at timestamp with time zone
);

create index idx_incidents_project_status on incidents (project_id, status);
create index idx_incidents_monitor_status on incidents (monitor_id, status);

create table incident_events (
    id uuid primary key default gen_random_uuid(),
    incident_id uuid not null references incidents(id) on delete cascade,
    type varchar(40) not null,
    message text not null,
    metadata_json text,
    created_by uuid references users(id) on delete set null,
    created_at timestamp with time zone not null default now()
);

create table notification_channels (
    id uuid primary key default gen_random_uuid(),
    project_id uuid not null references projects(id) on delete cascade,
    name varchar(180) not null,
    type varchar(40) not null,
    webhook_url text not null,
    created_at timestamp with time zone not null default now()
);

create table alert_rules (
    id uuid primary key default gen_random_uuid(),
    project_id uuid not null references projects(id) on delete cascade,
    monitor_id uuid references monitors(id) on delete cascade,
    notification_channel_id uuid references notification_channels(id) on delete set null,
    name varchar(180) not null,
    condition varchar(80) not null,
    threshold_ms integer,
    enabled boolean not null default true,
    created_at timestamp with time zone not null default now()
);

create table api_keys (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references organizations(id) on delete cascade,
    name varchar(180) not null,
    key_prefix varchar(16) not null,
    key_hash varchar(128) not null unique,
    created_at timestamp with time zone not null default now(),
    last_used_at timestamp with time zone
);

create table ai_audit_logs (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references users(id) on delete set null,
    organization_id uuid not null references organizations(id) on delete cascade,
    prompt_summary varchar(500) not null,
    tools_invoked text,
    result_status varchar(40) not null,
    created_at timestamp with time zone not null default now()
);
