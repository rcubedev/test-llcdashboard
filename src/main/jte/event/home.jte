@import gg.jte.support.ForSupport
@import org.limelightcreatives.dashboard.event.EventConfig
@import org.limelightcreatives.dashboard.milestone.MilestoneProgress
@import org.limelightcreatives.dashboard.milestone.MilestoneStatus
@import org.limelightcreatives.dashboard.team.Team
@import org.limelightcreatives.dashboard.web.Page

<%-- an event dashboard's Home tab (refreshed when you come back to it, after submitting in Fillout) --%>
@param Page page
@param EventConfig event
@param Team team = null
@param MilestoneProgress progress = null
@param String updatedAt = null

@template.tag.event(page = page, event = event, title = event.name(), tab = "home", content = @`
    @if(team == null)
        @template.tag.joinTeam(event = event, heading = "Home", text = "Welcome to your dashboard! Join or create a team to get started.")
    @else
    !{var current = progress.current();}
    <div data-refresh-on-focus>
        <div class="flex flex-wrap items-end justify-between gap-4">
            <div>
                <h1 class="font-display text-4xl font-bold md:text-5xl">Home</h1>
                <p class="mt-2 max-w-xl text-sm text-[var(--foreground)]/70">
                    Showing progress for <strong>${team.name()}</strong>.
                </p>
            </div>
            <a class="btn btn-secondary" href="">Refresh</a>
        </div>

        <div class="mt-10 grid gap-8 lg:grid-cols-[minmax(0,1fr)_20rem]">
            <div class="min-w-0 space-y-12">
                <%-- up next --%>
                <section aria-labelledby="up-next">
                    <h2 id="up-next" class="font-display text-2xl font-bold">${progress.allDone() ? "All done!" : "Up next"}</h2>

                    <div class="card mt-4">
                        @if(progress.allDone())
                        <div>
                            <p class="font-display text-xl font-bold">Every milestone is approved.</p>
                            <p class="mt-2 text-sm text-[var(--foreground)]/70">
                                Nice work, your film is in the can.@if(!progress.bonus().isEmpty() && progress.bonusApproved() < progress.bonus().size()) Bonus milestones are still open if you want to keep going.@endif
                            </p>
                            @if(!progress.bonus().isEmpty())
                                <a class="btn btn-secondary mt-6" href="/participant/${event.id()}/milestones">View bonus milestones</a>
                            @endif
                        </div>
                        @else
                        !{var m = current.milestone();}
                        <div class="flex flex-wrap items-start justify-between gap-6">
                            <div class="min-w-0 flex-1 basis-56">
                                <p class="label">Milestone ${current.position()} of ${progress.required().size()}</p>
                                <div class="mt-2 flex flex-wrap items-center gap-2">
                                    <p class="break-words font-display text-2xl font-bold">${m.label()}</p>
                                    <span class="tag ${current.status() == MilestoneStatus.SUBMITTED ? "bg-[var(--accent)] text-[var(--foreground)]" : "bg-transparent text-[var(--foreground)]"}">${current.status().label()}</span>
                                </div>
                                @if(m.targetTime() != null)
                                    <p class="mt-2 text-sm text-[var(--foreground)]/70">🕒 ${m.targetTime()}</p>
                                @endif
                                @if(m.description() != null)
                                    <p class="mt-3 text-sm text-[var(--foreground)]/70">${m.description()}</p>
                                @endif
                                @if(m.evidence() != null)
                                    <div class="mt-4 border-2 border-[var(--foreground)] px-3 py-2">
                                        <p class="label">Show</p>
                                        <p class="mt-1 text-sm">${m.evidence()}</p>
                                    </div>
                                @endif
                                @if(current.status() == MilestoneStatus.SUBMITTED)
                                    <p class="mt-4 text-sm text-[var(--foreground)]/70">
                                        An organiser is reviewing this. The next milestone unlocks once it’s approved.
                                    </p>
                                @endif
                            </div>

                            <div class="flex shrink-0 flex-col items-end gap-3">
                                @if(current.canSubmit() && current.submitUrl() != null)
                                    <a class="btn btn-primary" href="${current.submitUrl()}" target="_blank" rel="noopener noreferrer">SUBMIT</a>
                                @endif
                                <a class="btn btn-secondary" href="/participant/${event.id()}/milestones">All milestones</a>
                            </div>
                        </div>
                        @endif
                    </div>
                </section>

                <%-- roadmap --%>
                <section aria-labelledby="roadmap">
                    <div class="flex flex-wrap items-end justify-between gap-4">
                        <h2 id="roadmap" class="font-display text-2xl font-bold">Roadmap</h2>
                        <p class="text-xs text-[var(--foreground)]/60">${progress.approvedCount()}/${progress.required().size()} approved@if(!progress.bonus().isEmpty()) · ${progress.bonusApproved()}/${progress.bonus().size()} bonus@endif</p>
                    </div>

                    <ol class="mt-4 space-y-3">
                        @for(var each : ForSupport.of(progress.entries()))
                        !{var entry = each.get();}
                        !{var isCurrent = current != null && current.milestone().id().equals(entry.milestone().id());}
                        <li aria-current="${isCurrent ? "step" : null}" style="animation-delay: ${each.getIndex() * 50}ms"
                            class="flex animate-page-in items-center justify-between gap-4 border-2 border-[var(--foreground)] px-4 py-3 ${entry.locked() ? "opacity-50" : ""} ${isCurrent ? "bg-[var(--accent)]/15" : ""}">
                            <div class="flex min-w-0 items-center gap-4">
                                <span class="flex h-8 w-8 shrink-0 items-center justify-center border-2 border-[var(--foreground)] font-display text-xs font-bold">${entry.badge()}</span>
                                <div class="min-w-0">
                                    <p class="truncate font-display text-base font-bold">${entry.milestone().label()}</p>
                                    @if(entry.milestone().targetTime() != null)
                                        <p class="truncate text-xs text-[var(--foreground)]/60">${entry.milestone().targetTime()}</p>
                                    @endif
                                </div>
                            </div>
                            <span class="tag shrink-0 ${entry.approved() ? "bg-[var(--accent)] text-[var(--foreground)]" : "bg-transparent text-[var(--foreground)]"}">${entry.tag()}</span>
                        </li>
                        @endfor
                    </ol>
                </section>
            </div>

            <%-- sidebar --%>
            <aside class="space-y-6">
                <div class="card flex flex-col gap-3">
                    <p class="label">Progress</p>
                    <p class="font-display text-4xl font-bold">${progress.approvedCount()}<span class="text-[var(--foreground)]/40">/${progress.required().size()}</span></p>
                    <div role="progressbar" aria-valuemin="0" aria-valuemax="${progress.required().size()}" aria-valuenow="${progress.approvedCount()}"
                         aria-label="Milestones approved" class="h-3 w-full overflow-hidden border-2 border-[var(--foreground)]">
                        <div class="h-full origin-left animate-grow-x bg-[var(--accent)] [animation-delay:200ms]" style="width: ${progress.percent()}%"></div>
                    </div>
                    <p class="text-sm text-[var(--foreground)]/70">milestones approved</p>
                </div>

                <div class="card flex flex-col gap-2">
                    <p class="label">In review</p>
                    <p class="font-display text-4xl font-bold">${progress.awaiting().size()}</p>
                    @if(progress.awaiting().isEmpty())
                        <p class="text-sm text-[var(--foreground)]/70">Nothing waiting on an organiser.</p>
                    @else
                        <ul class="text-sm text-[var(--foreground)]/70">
                            @for(var entry : progress.awaiting())
                                <li class="truncate">${entry.milestone().label()}</li>
                            @endfor
                        </ul>
                    @endif
                </div>

                <div class="card flex flex-col gap-2">
                    <p class="label">Team</p>
                    <p class="break-words font-display text-2xl font-bold">${team.name()}</p>
                    <p class="text-sm text-[var(--foreground)]/70">${team.memberCount()} / ${team.maxSize()} members</p>
                    <a class="btn btn-secondary mt-2" href="/participant/${event.id()}/team">Manage team</a>
                </div>

                <p class="animate-fade-in text-xs text-[var(--foreground)]/50">Updated ${updatedAt}</p>
            </aside>
        </div>
    </div>
    @endif
`)
