@import org.limelightcreatives.dashboard.event.EventConfig
@import org.limelightcreatives.dashboard.milestone.MilestoneProgress
@import org.limelightcreatives.dashboard.team.Team
@import org.limelightcreatives.dashboard.web.Page

<%-- an event dashboard's Milestones tab (refreshed when you come back to it, after submitting in Fillout) --%>
@param Page page
@param EventConfig event
@param Team team = null
@param MilestoneProgress progress = null

@template.tag.event(page = page, event = event, title = "Milestones", tab = "milestones", content = @`
    @if(team == null)
        @template.tag.joinTeam(event = event, heading = "Milestones",
                text = "Milestones are submitted per team. Join or create a team, then come back to submit.")
    @else
    <div data-refresh-on-focus>
        <h1 class="font-display text-4xl font-bold md:text-5xl">Milestones</h1>
        <p class="mt-2 max-w-xl text-sm text-[var(--foreground)]/70">
            Submit each milestone for review. The next one unlocks once an organiser approves your current one.
        </p>
        <p class="mt-6 text-sm text-[var(--foreground)]/70">Submitting as <strong>${team.name()}</strong></p>

        <ol class="mt-10 space-y-8">
            @for(var entry : progress.entries())
            !{var m = entry.milestone();}
            <li>
                <div class="flex flex-wrap items-start justify-between gap-6 ${entry.locked() ? "card-flat opacity-50" : "card"}">
                    <div class="flex min-w-0 flex-1 basis-56 items-start gap-4">
                        <span class="flex h-10 w-10 shrink-0 items-center justify-center border-2 border-[var(--foreground)] font-display text-sm font-bold">${entry.number()}</span>

                        <div class="min-w-0">
                            <div class="flex flex-wrap items-center gap-2">
                                <p class="break-words font-display text-xl font-bold">${m.label()}</p>
                                @if(m.bonus())
                                    <span class="tag bg-[var(--accent)] text-[var(--foreground)]">Bonus</span>
                                @endif
                            </div>

                            @if(m.description() != null)
                                <p class="mt-3 text-sm text-[var(--foreground)]/70">${m.description()}</p>
                            @endif

                            @if(m.evidence() != null)
                                <div class="mt-4 border-2 border-[var(--foreground)] px-3 py-2">
                                    <p class="label">Show</p>
                                    <p class="mt-1 text-sm">${m.evidence()}</p>
                                </div>
                            @endif

                            @if(m.prize() != null)
                                <div class="mt-3 border-2 border-[var(--foreground)] bg-[var(--accent)] px-3 py-2 text-[var(--background)]">
                                    <p class="font-display text-[0.7rem] font-bold uppercase tracking-[0.08em]">Prize</p>
                                    <p class="mt-1 text-sm">🏆 ${m.prize()}</p>
                                </div>
                            @endif

                            @if(entry.locked())
                                <div class="mt-4 border-2 border-dashed border-[var(--foreground)] px-3 py-2 text-sm">🔒 Unlocks after “${entry.previous().label()}” is approved.</div>
                            @endif
                        </div>
                    </div>

                    <div class="flex shrink-0 flex-col items-end gap-3">
                        <span class="tag ${entry.approved() ? "bg-[var(--accent)] text-[var(--foreground)]" : "bg-transparent text-[var(--foreground)]"}">${entry.tag()}</span>
                        @if(entry.canSubmit() && entry.submitUrl() != null)
                            <a class="btn btn-primary" href="${entry.submitUrl()}" target="_blank" rel="noopener noreferrer">SUBMIT</a>
                        @endif
                    </div>
                </div>
            </li>
            @endfor
        </ol>
    </div>
    @endif
`)
