"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useParams } from "next/navigation";

import { getEventConfig } from "@/lib/events";
import { HardCard } from "@/components/dashboard/HardCard";
import { Button } from "@/components/Button";
import type { MilestoneStatus } from "@/lib/events/types";

const STATUS_LABEL: Record<MilestoneStatus, string> = {
  pending: "Ready to submit",
  submitted: "Awaiting review",
  approved: "Approved",
};

const TAG =
  "inline-block border-[2px] border-[var(--foreground)] px-2 py-0.5 font-display text-[0.7rem] font-bold uppercase tracking-[0.08em]";

const LABEL =
  "font-display text-[0.7rem] font-bold uppercase tracking-[0.08em] text-[var(--foreground)]/60";

type Team = {
  id: string;
  name: string;
  joinCode?: string;
  memberCount?: number;
  maxSize?: number;
};

function Spinner() {
  return (
    <div className="flex justify-center py-20" role="status" aria-label="Loading">
      <span
        aria-hidden
        className="h-10 w-10 animate-spin rounded-full border-2 border-[var(--foreground)]/20 border-t-[var(--accent)]"
      />
    </div>
  );
}

// Same URL shape as the milestones page so the webhook gets identical params.
function buildSubmitUrl(
  base: string,
  milestone: { id: string; label: string },
  team: Team,
) {
  const url = new URL(base);
  url.searchParams.set("milestoneId", milestone.id);
  url.searchParams.set("milestone", milestone.label);
  url.searchParams.set("teamId", team.id);
  url.searchParams.set("team", team.name);
  return url.toString();
}

export default function EventDashboardPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const event = getEventConfig(eventId)!;
  const basePath = `/participant/${event.id}`;

  // undefined = still checking, null = no team
  const [team, setTeam] = useState<Team | null | undefined>(undefined);
  const [teamError, setTeamError] = useState<string | null>(null);

  // milestone id -> status. A missing key means "pending".
  const [statuses, setStatuses] = useState<Record<string, MilestoneStatus>>({});
  const [statusesLoaded, setStatusesLoaded] = useState(false);
  const [refreshedAt, setRefreshedAt] = useState<Date | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await fetch("/api/team");
        if (!res.ok) throw new Error();
        const data = await res.json();
        if (!cancelled) setTeam(data?.team ?? null);
      } catch {
        if (!cancelled)
          setTeamError("Couldn't check your team. Refresh to try again.");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const loadStatuses = useCallback(async () => {
    try {
      const res = await fetch("/api/milestones");
      const data = res.ok ? await res.json() : { statuses: {} };
      setStatuses(data.statuses ?? {});
      setRefreshedAt(new Date());
    } catch {
      // Keep whatever we already had; the next focus will retry.
    } finally {
      setStatusesLoaded(true);
    }
  }, []);

  useEffect(() => {
    loadStatuses();
    // The form opens in a new tab, so refresh when the user comes back.
    window.addEventListener("focus", loadStatuses);
    return () => window.removeEventListener("focus", loadStatuses);
  }, [loadStatuses]);

  // Manual refresh: show a spinner on the button, and hold it for a beat so a
  // fast response still reads as "something happened".
  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all([loadStatuses(), new Promise((r) => setTimeout(r, 400))]);
    setRefreshing(false);
  }, [loadStatuses]);

  const statusOf = useCallback(
    (m: { id: string }): MilestoneStatus => statuses[m.id] ?? "pending",
    [statuses],
  );

  const derived = useMemo(() => {
    const required = event.milestones.filter((m) => !m.bonus);
    const bonus = event.milestones.filter((m) => m.bonus);

    const approvedCount = required.filter((m) => statusOf(m) === "approved").length;
    const awaiting = event.milestones.filter((m) => statusOf(m) === "submitted");
    const bonusApproved = bonus.filter((m) => statusOf(m) === "approved").length;

    // Milestones unlock in order, so the first unapproved required one is "current"
    const currentIndex = required.findIndex((m) => statusOf(m) !== "approved");
    const current = currentIndex === -1 ? null : required[currentIndex];

    return {
      required,
      bonus,
      approvedCount,
      awaiting,
      bonusApproved,
      current,
      currentIndex,
      allDone: current === null,
    };
  }, [event.milestones, statusOf]);

  const {
    required,
    bonus,
    approvedCount,
    awaiting,
    bonusApproved,
    current,
    currentIndex,
    allDone,
  } = derived;

  // Wait for both lookups so nothing flashes the wrong state.
  if ((team === undefined || !statusesLoaded) && !teamError) return <Spinner />;

  // No team yet: same gate as the milestones page.
  if (team === null) {
    return (
      <div>
        <h1 className="font-display text-4xl font-bold md:text-5xl">Home</h1>
        <div className="mx-auto mt-10 w-full max-w-lg md:mt-16">
          <HardCard>
            <div className="flex flex-col items-center px-4 py-10 text-center md:py-14">
              <p className="font-display text-3xl md:text-4xl">Join a team first</p>
              <p className="mt-4 max-w-sm font-body text-base text-[var(--foreground)]/70">
                Welcome to your dashboard! Join or create a team to get started.
              </p>
              <Button className="mt-8" href={`${basePath}/team`}>
                Go to team page
              </Button>
            </div>
          </HardCard>
        </div>
      </div>
    );
  }

  const submitHref =
    team && current ? buildSubmitUrl(event.formUrl, current, team) : null;

  const progressPct = required.length
    ? Math.round((approvedCount / required.length) * 100)
    : 0;

  const roadmap = [...required, ...bonus];

  return (
    <div>
      {/* Header */}
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-4xl font-bold md:text-5xl">Home</h1>
          <p className="mt-2 max-w-xl font-body text-sm text-[var(--foreground)]/70">
            {team ? (
              <>
                Showing progress for <strong>{team.name}</strong>.
              </>
            ) : (
              "Take a look at your progress, and get started on your milestones here!"
            )}
          </p>
        </div>
        <Button variant="secondary" onClick={handleRefresh} loading={refreshing}>
          Refresh
        </Button>
      </div>

      {teamError ? (
        <p
          role="alert"
          className="mt-6 border-[2px] border-red-600 px-3 py-2 font-body text-sm text-red-600"
        >
          {teamError}
        </p>
      ) : null}

      <div className="mt-10 grid gap-8 lg:grid-cols-[minmax(0,1fr)_20rem]">
        {/* Main column */}
        <div className="min-w-0 space-y-12">
          {/* Up next */}
          <section aria-labelledby="up-next">
            <h2 id="up-next" className="font-display text-2xl font-bold">
              {allDone ? "All done!" : "Up next"}
            </h2>

            <HardCard className="mt-4">
              {allDone || !current ? (
                <div>
                  <p className="font-display text-xl font-bold">
                    Every milestone is approved.
                  </p>
                  <p className="mt-2 font-body text-sm text-[var(--foreground)]/70">
                    Nice work, your film is in the can.
                    {bonus.length > 0 && bonusApproved < bonus.length
                      ? " Bonus milestones are still open if you want to keep going."
                      : ""}
                  </p>
                  {bonus.length > 0 ? (
                    <Button
                      className="mt-6"
                      variant="secondary"
                      href={`${basePath}/milestones`}
                    >
                      View bonus milestones
                    </Button>
                  ) : null}
                </div>
              ) : (
                <div className="flex flex-wrap items-start justify-between gap-6">
                  <div className="min-w-0 flex-1">
                    <p className={LABEL}>
                      Milestone {currentIndex + 1} of {required.length}
                    </p>
                    <div className="mt-2 flex flex-wrap items-center gap-2">
                      <p className="break-words font-display text-2xl font-bold">
                        {current.label}
                      </p>
                      <span
                        className={`${TAG} ${
                          statusOf(current) === "submitted"
                            ? "bg-[var(--accent)] text-[var(--foreground)]"
                            : "bg-transparent text-[var(--foreground)]"
                        }`}
                      >
                        {STATUS_LABEL[statusOf(current)]}
                      </span>
                    </div>

                    {current.targetTime ? (
                      <p className="mt-2 font-body text-sm text-[var(--foreground)]/70">
                        🕒 {current.targetTime}
                      </p>
                    ) : null}

                    {current.description ? (
                      <p className="mt-3 font-body text-sm text-[var(--foreground)]/70">
                        {current.description}
                      </p>
                    ) : null}

                    {current.evidence ? (
                      <div className="mt-4 border-[2px] border-[var(--foreground)] px-3 py-2">
                        <p className={LABEL}>Show</p>
                        <p className="mt-1 font-body text-sm">{current.evidence}</p>
                      </div>
                    ) : null}

                    {statusOf(current) === "submitted" ? (
                      <p className="mt-4 font-body text-sm text-[var(--foreground)]/70">
                        An organiser is reviewing this. The next milestone
                        unlocks once it&apos;s approved.
                      </p>
                    ) : null}
                  </div>

                  <div className="flex shrink-0 flex-col items-end gap-3">
                    {statusOf(current) === "pending" && submitHref ? (
                      <Button href={submitHref} external variant="primary">
                        SUBMIT
                      </Button>
                    ) : null}
                    <Button variant="secondary" href={`${basePath}/milestones`}>
                      All milestones
                    </Button>
                  </div>
                </div>
              )}
            </HardCard>
          </section>

          {/* Roadmap */}
          <section aria-labelledby="roadmap">
            <div className="flex flex-wrap items-end justify-between gap-4">
              <h2 id="roadmap" className="font-display text-2xl font-bold">
                Roadmap
              </h2>
              <p className="font-body text-xs text-[var(--foreground)]/60">
                {approvedCount}/{required.length} approved
                {bonus.length > 0
                  ? ` · ${bonusApproved}/${bonus.length} bonus`
                  : ""}
              </p>
            </div>

            <ol className="mt-4 space-y-3">
              {roadmap.map((m, i) => {
                const status = statusOf(m);
                const isCurrent = current?.id === m.id;
                const reqIndex = required.findIndex((r) => r.id === m.id);
                // Matches the milestones page: required ones lock until the previous is approved.
                const isLocked =
                  !m.bonus && reqIndex > 0 && statusOf(required[reqIndex - 1]) !== "approved";

                return (
                  <li
                    key={m.id}
                    aria-current={isCurrent ? "step" : undefined}
                    style={{ animationDelay: `${i * 50}ms` }}
                    className={`flex animate-page-in items-center justify-between gap-4 border-[2px] border-[var(--foreground)] px-4 py-3 ${
                      isLocked ? "opacity-50" : ""
                    } ${isCurrent ? "bg-[var(--accent)]/15" : ""}`}
                  >
                    <div className="flex min-w-0 items-center gap-4">
                      <span className="flex h-8 w-8 shrink-0 items-center justify-center border-[2px] border-[var(--foreground)] font-display text-xs font-bold">
                        {status === "approved" && !isLocked
                          ? "✓"
                          : m.bonus
                            ? "★"
                            : String(i + 1).padStart(2, "0")}
                      </span>
                      <div className="min-w-0">
                        <p className="truncate font-display text-base font-bold">
                          {m.label}
                        </p>
                        {m.targetTime ? (
                          <p className="truncate font-body text-xs text-[var(--foreground)]/60">
                            {m.targetTime}
                          </p>
                        ) : null}
                      </div>
                    </div>

                    <span
                      className={`${TAG} shrink-0 ${
                        status === "approved" && !isLocked
                          ? "bg-[var(--accent)] text-[var(--foreground)]"
                          : "bg-transparent text-[var(--foreground)]"
                      }`}
                    >
                      {isLocked ? "Locked" : STATUS_LABEL[status]}
                    </span>
                  </li>
                );
              })}
            </ol>
          </section>
        </div>

        {/* Sidebar */}
        <aside className="space-y-6">
          <HardCard className="flex flex-col gap-3">
            <p className={LABEL}>Progress</p>
            <p className="font-display text-4xl font-bold">
              {approvedCount}
              <span className="text-[var(--foreground)]/40">/{required.length}</span>
            </p>
            <div
              role="progressbar"
              aria-valuemin={0}
              aria-valuemax={required.length}
              aria-valuenow={approvedCount}
              aria-label="Milestones approved"
              className="h-3 w-full overflow-hidden border-[2px] border-[var(--foreground)]"
            >
              <div
                className="h-full origin-left bg-[var(--accent)] transition-[width] duration-700 ease-out animate-grow-x"
                style={{ width: `${progressPct}%`, animationDelay: "200ms" }}
              />
            </div>
            <p className="font-body text-sm text-[var(--foreground)]/70">
              milestones approved
            </p>
          </HardCard>

          <HardCard className="flex flex-col gap-2">
            <p className={LABEL}>In review</p>
            <p className="font-display text-4xl font-bold">{awaiting.length}</p>
            {awaiting.length > 0 ? (
              <ul className="font-body text-sm text-[var(--foreground)]/70">
                {awaiting.map((m) => (
                  <li key={m.id} className="truncate">
                    {m.label}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="font-body text-sm text-[var(--foreground)]/70">
                Nothing waiting on an organiser.
              </p>
            )}
          </HardCard>

          <HardCard className="flex flex-col gap-2">
            <p className={LABEL}>Team</p>
            <p className="break-words font-display text-2xl font-bold">
              {team?.name}
            </p>
            <p className="font-body text-sm text-[var(--foreground)]/70">
              {team?.memberCount ?? "–"} / {team?.maxSize ?? event.teamSize} members
            </p>
            <Button className="mt-2" variant="secondary" href={`${basePath}/team`}>
              Manage team
            </Button>
          </HardCard>

          {refreshedAt ? (
            <p
              key={refreshedAt.getTime()}
              className="animate-fade-in font-body text-xs text-[var(--foreground)]/50"
            >
              Updated {refreshedAt.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })}
            </p>
          ) : null}
        </aside>
      </div>
    </div>
  );
}