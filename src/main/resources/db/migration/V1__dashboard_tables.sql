-- The dashboard's tables. The old app's tables were made by hand in Neon, so on the existing database every
-- statement here finds its table already there and does nothing; on a new, empty database it creates them.
-- (Flyway records this in dashboard_schema_history, separate from the auth app's history; see application.yml.)
--
-- User ids are the auth app's "users"."id" (text UUIDs). There's no foreign key to "users": the auth app owns
-- that table, and it may live in another database.

CREATE TABLE IF NOT EXISTS teams (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name       text NOT NULL,
    join_code  text NOT NULL UNIQUE,
    owner_id   text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- One team per person.
CREATE TABLE IF NOT EXISTS team_members (
    user_id   text PRIMARY KEY,
    team_id   uuid NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    joined_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS team_members_team_id_idx ON team_members (team_id);

-- Each team's one project (film).
CREATE TABLE IF NOT EXISTS submissions (
    team_id      uuid PRIMARY KEY REFERENCES teams (id) ON DELETE CASCADE,
    title        text NOT NULL,
    logline      text NOT NULL,
    video_url    text NOT NULL,
    submitted_by text,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now()
);

-- Milestone submissions from Fillout (via /api/fillout/webhook), reviewed by staff.
-- Sending one back deletes the row, which returns the milestone to "Ready to submit".
CREATE TABLE IF NOT EXISTS milestone_submissions (
    team_id         uuid NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    milestone_id    text NOT NULL,
    status          text NOT NULL DEFAULT 'submitted' CHECK (status IN ('submitted', 'approved')),
    submission_id   text,
    milestone_label text,
    answers         jsonb NOT NULL DEFAULT '[]',
    submitted_at    timestamptz NOT NULL DEFAULT now(),
    reviewed_at     timestamptz,
    reviewed_by     text,
    PRIMARY KEY (team_id, milestone_id)
);

-- Guides, tutorials and links on the Resources pages.
CREATE TABLE IF NOT EXISTS resources (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    title         text NOT NULL,
    slug          text NOT NULL UNIQUE,
    summary       text NOT NULL DEFAULT '',
    body          text NOT NULL DEFAULT '',
    url           text NOT NULL DEFAULT '',
    thumbnail_url text NOT NULL DEFAULT '',
    category      text NOT NULL DEFAULT 'General',
    kind          text NOT NULL DEFAULT 'guide' CHECK (kind IN ('guide', 'tutorial', 'video', 'link')),
    audience      text NOT NULL DEFAULT 'all' CHECK (audience IN ('all', 'client', 'organiser')),
    published     boolean NOT NULL DEFAULT false,
    sort_order    integer NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);
