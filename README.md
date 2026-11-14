# Limelight Creatives Dashboard

The dashboard for Limelight Creatives events, built with Java, Spring Boot and JTE: the launchpads for
participants and staff, each event's dashboard (home, milestones, project, equipment, team, resources), the run
of show, and the staff tools (check-in, participants, milestone review, resources).

It replaces the original Next.js app and keeps its database tables, Google Sheets, Fillout webhook and
environment variable names, so it can take over in place. Signing in now goes through the auth app with OpenID
Connect instead of the shared cookie and `/api/permission`.

## Commands

| Command                                  | What it does                                                                                             |
|------------------------------------------|----------------------------------------------------------------------------------------------------------|
| `./gradlew bootTestRun`                  | Runs it at http://localhost:3000 with its own PostgreSQL and pretend Google Sheets, so there's nothing to set up (see [Running locally](#running-locally)) |
| `./gradlew bootRun`                      | The same, but with a PostgreSQL you run yourself (database `dashboard`, user and password `dashboard`) and the real Google Sheets settings |
| `./gradlew test`                         | Runs the tests, which also start their own PostgreSQL                                                    |
| `./gradlew compileTailwind --continuous` | Rebuilds the CSS whenever a template changes (use alongside `bootRun`)                                   |
| `./gradlew bootJar`                      | Builds `build/libs/dashboard.jar`, which runs with `java -jar` (the Dockerfile does this)                |

Needs Java 25 (Gradle downloads it if you don't have it). The PostgreSQL that `bootTestRun` and the tests start is
the real thing (PostgreSQL 18, from embedded-postgres), downloaded by Gradle for your machine only: macOS (Apple
Silicon or Intel), Windows (x64) and Linux (x64 or ARM). On anything else, point the tests at an empty database of
your own with `TEST_DATABASE_URL="jdbc:postgresql://localhost/dashboard_test?user=dashboard&password=dashboard"`.

## Running locally

`./gradlew bootTestRun` starts everything the dashboard needs:

- its own PostgreSQL, keeping its data in `build/dev-database` (`./gradlew clean` empties it);
- the Google Sheets in memory: three participants (`LC-001` to `LC-003`) and an empty booking grid, fresh every run;
- equipment booking on today and tomorrow, so there are slots to reserve.

Signing in is the auth app's job. Either:

- **run the auth app too**: its `AUTH_DEV_STAFF_EMAIL=<you> ./gradlew bootTestRun` (on port 8080). Once, sign in
  there, open http://localhost:8080/apps and add the dashboard as a server app with the redirect URL
  `http://localhost:3000/login/oauth2/code/limelight`, the sign-out redirect URL `http://localhost:3000/` and RS256
  ID tokens (auth keeps it in its `build/dev-database`). Then `AUTH_CLIENT_ID=<its client id>
  AUTH_CLIENT_SECRET=<its secret> ./gradlew bootTestRun` here and open http://localhost:3000. You're sent there to
  sign in and come back here, as staff; or
- **skip signing in**: `DEV_ROLE=STAFF ./gradlew bootTestRun` (or `PARTICIPANT`) makes everyone a pretend
  "Development User" with that role. This only works locally (it's in `application-dev.yml`).

To see the run of show as it was on the day, add `?now=2026-10-24T10:30` to the schedule pages.

## Signing in

The dashboard is an OpenID Connect client of the auth app (`spring-boot-starter-security-oauth2-client`,
`account/SecurityConfig`):

- anyone not signed in is sent to auth and comes back to the page they opened;
- auth's `role` claim becomes their role here: `/staff/...` is for `ADMIN` and `STAFF`, anyone else is sent to
  `/participant`;
- sessions are kept in PostgreSQL (Spring Session, table `dashboard_session`), so any instance can serve anyone
  and a redeploy signs nobody out. A sign-in lasts 30 days from the last visit;
- roles stay up to date (`account/FreshRoles`): auth's access tokens last 10 minutes, and when one has run out
  the next page renews it and reads who this is again, so a new role or name arrives within 10 minutes. If auth
  won't renew it (they signed out everywhere), they're signed out here too;
- **Log out** signs out here and at auth, then comes back to the dashboard.

In the auth app the dashboard is an app on its apps page (`<auth>/apps`): a server app with the redirect URL
`<this app>/login/oauth2/code/limelight`, the sign-out redirect URL `<this app>/` and RS256 ID tokens. Adding it
there gives its `AUTH_CLIENT_ID` and `AUTH_CLIENT_SECRET`.

## Settings (environment variables)

The same names as the old app, so the existing Vercel project's variables carry over. `PORT`,
`AUTH_CLIENT_ID` and `AUTH_CLIENT_SECRET` are new.

| Variable | Needed | What it is |
| --- | --- | --- |
| `DATABASE_URL` | yes | The PostgreSQL address. Neon's `postgresql://user:password@host/db?sslmode=require` works as is. Use the direct address, not the `-pooler` one. |
| `AUTH_CLIENT_ID`, `AUTH_CLIENT_SECRET` | yes | The dashboard's client id and secret from auth's apps page (see [Signing in](#signing-in)). It won't start without them. |
| `PORT` | on Vercel | `8080` (Vercel sends traffic to port 80 otherwise, which the app's non-root user can't use) |
| `FILLOUT_WEBHOOK_SECRET` | for milestones | Fillout calls `/api/fillout/webhook?secret=<this>`. Without it, every call is refused. |
| `GOOGLE_SERVICE_ACCOUNT_EMAIL`, `GOOGLE_PRIVATE_KEY_B64` (or `GOOGLE_PRIVATE_KEY`), `GOOGLE_SHEET_ID` | for check-in | The attendee sheet (tabs "Attendees" and "Check-ins") |
| `GOOGLE_SERVICE_EQUIPMENT_EMAIL`, `GOOGLE_EQUIPMENT_PRIVATE_KEY_B64` (or `GOOGLE_EQUIPMENT_PRIVATE_KEY`), `EQUIPMENT_SHEET_ID` | for equipment | The equipment booking sheet |
| `EQUIPMENT_SHEET_TAB` | no | The tab with the booking tables. Default: the first tab that has tables. |
| `TEAMS_LOCKED` | no | `true` stops people creating, joining or leaving teams |
| `SUBMISSIONS_CLOSE_AT` | no | e.g. `2026-11-01T23:59:00+11:00`. After this, teams can't change their project. |
| `FILLOUT_RESULTS_URL` | no | Shows an "Open in Fillout" link on the staff milestones page (`NEXT_PUBLIC_FILLOUT_RESULTS_URL` works too) |
| `AUTH_URL` | no | The auth app. Default `https://auth.limelightcreatives.org` |
| `DASHBOARD_BASE_URL` | no | This app's address, which auth sends people back to. Default `https://dashboard.limelightcreatives.org` |

A private key can be the `private_key` from the service account's JSON file as it is (with real or `\n` line
breaks) or that text in base64; both variable names accept either. If a sheet's settings are missing, its pages
say they can't reach the sheet and the log says which variables to set.

## Content to edit

| File | What's in it |
| --- | --- |
| `src/main/resources/content/events.yml` | Events and their milestones (the old `lib/events/`). |
| `src/main/resources/content/equipment.yml` | Equipment booking: the event's days, when booking opens, and what can be booked (the old `lib/events/equipment.ts`) |
| `src/main/resources/content/run-of-show.json` | The run of show, in exactly the old format (the old `data/run-of-show/schedule.json`) |
| `src/main/java/.../web/App.java` | The tiles on the two launchpads |

One setting of an event can also be changed without editing the file, e.g. `EVENTS_GENESIS_ACTIVE=true` or
`EVENTS_GENESIS_LOCKED=false` as environment variables.

## The database

| Migration | What it is |
| --- | --- |
| `V1__dashboard_tables.sql` | The old app's tables: `teams`, `team_members`, `submissions` (each team's project), `milestone_submissions` and `resources` |
| `V2__sessions.sql` | Spring Session's tables, as `dashboard_session` |

The old app's tables were made by hand in Neon, so V1 only creates the ones that are missing
(`CREATE TABLE IF NOT EXISTS`): on the existing database it changes nothing, on a new one it creates them all.
**Before the switch-over, compare V1 with the real tables in Neon**; it was reconstructed from the old app's
queries. The dashboard keeps its own Flyway history table (`dashboard_schema_history`), so it can share a database
with the auth app, whose history table is `flyway_schema_history`. Never change a migration once it has run; add a
new one.

## Google Sheets

Both sheets are read and written as a Google service account, through the Sheets REST API
(`sheets/GoogleSpreadsheet`, no Google client library). Share each sheet with its service account's email as an
Editor.

- **Attendee sheet**: tab "Attendees" (A Name, B Email, C Phone No., D Emergency No., E Registration ID) and tab
  "Check-ins" (A Participant Name, B Action, C Timestamp, D Registration ID). Each scan of a registration ID adds a
  CHECK IN or CHECK OUT row, whichever is next for that person.
- **Equipment sheet**: per category and day, a native Table named "Gimbal Day 1" and so on, with "Timeslot" in its
  first column and one column per unit; team names in the cells are bookings. The quantities and per-team limits
  are label cells with the number to their right ("Gimbal Quantity" | 3). See `content/equipment.yml`.

## Fillout (milestone submissions)

The SUBMIT buttons open the event's Fillout form with `milestoneId`, `milestone`, `teamId` and `team` added to the
address. Fillout's webhook (set up in Fillout, unchanged) posts each submission to
`https://dashboard.limelightcreatives.org/api/fillout/webhook?secret=...`, which records it as "Awaiting review".
Staff approve it (which unlocks the team's next milestone) or send it back on `/staff/milestones`. A resubmission
never downgrades an approved milestone.

## What's where

```
src/main/java/org/limelightcreatives/dashboard/
├── DashboardApp, DashboardProperties, DatabaseUrl, Times  ← startup, the dashboard: settings, Neon-style DATABASE_URLs, Sydney time
├── account/      ← signing in through auth, roles, keeping them up to date, DEV_ROLE
├── session/      ← sessions in PostgreSQL
├── web/          ← launchpads, what every page needs (PageModel), asset links
├── event/        ← events from events.yml; EventPages (the event in a page's address, not found, locked)
├── team/         ← teams and the Team tab
├── project/      ← each team's project and the Project tab
├── milestone/    ← submissions, the Fillout webhook, the Home and Milestones tabs, staff review
├── resource/     ← resources, the participant tab and the staff editor
├── equipment/    ← booking against the equipment sheet, the Equipment tab
├── checkin/      ← check-in and the Participants page, against the attendee sheet
├── schedule/     ← the run of show (RunOfShow has the rules) and both schedule pages
└── sheets/       ← the Google Sheets client
src/main/resources/
├── content/          ← events, equipment, run of show (see above)
├── db/migration/     ← the tables (Flyway)
└── static/           ← fonts, images, js/dashboard.js (small extras), js/checkin.js (the QR scanner)
src/main/jte/         ← the pages (JTE): layout.jte (head, top bar), event/, staff/, schedule/, tag/ (event.jte is the tabs, form.jte adds the CSRF token)
src/main/css/app.css  ← Tailwind source, with the shared .card, .btn, .tag... classes (built by the plugin in build-logic/)
```

Pages are rendered on the server and work without JavaScript. Forms post and then redirect, so refreshing never
sends one again, and every form carries a CSRF token. `dashboard.js` adds what the old app did in the browser:
refreshing a page in the background when you come back to it (e.g. after submitting in Fillout), instant search,
the equipment countdown, copy buttons and "are you sure?" prompts. The check-in camera uses
[qr-scanner](https://github.com/nimiq/qr-scanner), served from its WebJar (a Gradle dependency, no Node).

## Tests

`./gradlew test` starts its own PostgreSQL, with a pretend auth app, in-memory Google Sheets and a clock set to
11:30 am on Genesis day 1. It covers signing in end to end (and roles changing afterwards), every page, teams
(including two people racing for the last place), projects, the whole milestone loop through the webhook,
resources, check-in and the participant list, equipment booking rules, the run-of-show rules, the Google Sheets
client against a pretend Google, and starting on a database shaped like the old app's.

## Changes from the old app

- Signing in is OpenID Connect through auth, not the shared cookie and `/api/permission`, so the dashboard no
  longer has to be on the same domain as auth.
- Pages are rendered on the server: the dashboard's own JSON routes (`/api/team`, `/api/milestones`, ...) are gone,
  replaced by forms. `/api/fillout/webhook` is unchanged.
- Check-in and the participant list now check that you're staff (the old server actions had a TODO for this and
  could be called by anyone).
- Times are shown in Sydney time, worked out on the server, rather than in each browser's time zone.
- The staff progress table lists milestones in the event's order rather than alphabetically by id.
- The equipment private key falls back to `GOOGLE_EQUIPMENT_PRIVATE_KEY` when there's no `_B64` (the old code read
  the `_B64` variable twice).
- The schedule remembers who you picked in a cookie instead of the browser's local storage.
- An event that doesn't exist answers 404 (still with the same card).

## CI

`.github/workflows/build.yml` runs on every push and pull request: build and test on Linux, Windows and macOS, and
a Docker image build (without publishing it), so a change that would break a deploy fails there first.

## Deploying (Vercel + Neon)

Like the auth app: Vercel runs the `Dockerfile` as a Vercel Function
([Container Runtime](https://vercel.com/docs/functions/runtimes/container), in beta), in Sydney (`vercel.json`).
Every push to `main` deploys to production; other branches get preview deployments. Neon's free plan can sleep
the database once the app has closed its idle connections (after a minute, see `spring.datasource.hikari`).

**Production switch-over** (after auth's own switch-over, since the dashboard signs in through the new auth):

1. In Neon, take a snapshot (or branch) of the database, and compare V1 with the real tables.
2. On auth's apps page add the dashboard for `https://dashboard.limelightcreatives.org` (see
   [Signing in](#signing-in)), and set its `AUTH_CLIENT_ID` and `AUTH_CLIENT_SECRET` in the dashboard's Vercel
   project.
3. In the existing Vercel project for `dashboard.limelightcreatives.org`: set the framework to Container (this
   repo's `vercel.json` does it too) and add `PORT=8080`. The other variables stay (`DATABASE_URL` should be the
   direct Neon address, not `-pooler`). The domain, DNS and the Fillout webhook stay as they are.
4. Merge into `main`. On first start Flyway adds its history table and the session tables; teams, projects,
   submissions and resources stay. Everyone signs in once more.

**A test copy** works on a `*.vercel.app` address now that sign-in doesn't need a shared cookie: a separate Neon
project, and in Vercel `PORT=8080`, `DATABASE_URL`, `DASHBOARD_BASE_URL=https://<dashboard>.vercel.app`,
`AUTH_URL=https://<auth>.vercel.app`, and `AUTH_CLIENT_ID` and `AUTH_CLIENT_SECRET` from adding the dashboard
for `https://<dashboard>.vercel.app` on the test auth's apps page.
