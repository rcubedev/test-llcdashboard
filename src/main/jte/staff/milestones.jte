@import gg.jte.support.ForSupport
@import java.util.List
@import java.util.Map
@import org.limelightcreatives.dashboard.milestone.MilestoneStatus
@import org.limelightcreatives.dashboard.milestone.StaffMilestonesController
@import org.limelightcreatives.dashboard.milestone.Submission
@import org.limelightcreatives.dashboard.web.Links
@import org.limelightcreatives.dashboard.web.Page

<%-- /staff/milestones: review submissions, or see every team's progress --%>
@param Page page
@param String view
@param String filter
@param String q
@param int total
@param long pendingCount
@param long approvedCount
@param List<Submission> visible
@param List<StaffMilestonesController.TeamProgress> teams
@param List<StaffMilestonesController.Column> columns
@param Map<String, String> prizes
@param String resultsUrl = null
@param String error = null

@template.layout(page = page, title = "Milestones", content = @`
<main class="min-h-0 flex-1 animate-page-in overflow-y-auto px-6 py-10" data-refresh-on-focus>
    <div class="mx-auto max-w-3xl">
        <div class="flex flex-wrap items-end justify-between gap-4">
            <div>
                <h1 class="font-display text-4xl font-bold md:text-5xl">Milestones</h1>
                <p class="mt-2 text-sm text-[var(--foreground)]/70">
                    Review milestone submissions. Approving unlocks the team’s next milestone.
                </p>
            </div>
            <div class="flex items-center gap-3">
                @if(resultsUrl != null)
                    <a href="${resultsUrl}" target="_blank" rel="noopener noreferrer" class="text-sm underline">Open in Fillout</a>
                @endif
                <a href="" class="btn-flat btn-outline">Refresh</a>
            </div>
        </div>

        @template.tag.alert(message = error)

        <nav class="mt-8 flex gap-2" aria-label="Choose view">
            <a href="/staff/milestones" class="tag cursor-pointer ${view.equals("review") ? "tag-active" : "bg-transparent"}"
               aria-current="${view.equals("review") ? "page" : null}">Review</a>
            <a href="/staff/milestones?view=progress" class="tag cursor-pointer ${view.equals("progress") ? "tag-active" : "bg-transparent"}"
               aria-current="${view.equals("progress") ? "page" : null}">Progress</a>
        </nav>

        @if(view.equals("progress"))
        <%-- progress: a row per team, a column per milestone --%>
        <p class="mt-6 text-sm text-[var(--foreground)]/70">${teams.size()} ${teams.size() == 1 ? "team" : "teams"} · ${approvedCount} approved · ${pendingCount} awaiting review</p>

        @if(teams.isEmpty())
            <div class="card mt-8 border-dashed">
                <p class="text-sm">No submissions yet, so there is no progress to show.</p>
            </div>
        @else
        <div class="mt-8">
            <div class="overflow-x-auto">
                <table class="w-full border-collapse text-sm">
                    <thead>
                    <tr>
                        <th class="border-2 border-[var(--foreground)] px-3 py-2 text-left font-display text-[0.7rem] font-bold uppercase tracking-[0.08em]">Team</th>
                        @for(var c : ForSupport.of(columns))
                            <th title="${c.get().label()}"
                                class="border-2 border-[var(--foreground)] px-3 py-2 text-center font-display text-[0.7rem] font-bold">${String.format("%02d", c.getIndex() + 1)}</th>
                        @endfor
                        <th class="border-2 border-[var(--foreground)] px-3 py-2 text-center font-display text-[0.7rem] font-bold uppercase tracking-[0.08em]">Approved</th>
                    </tr>
                    </thead>
                    <tbody>
                    @for(var team : teams)
                    <tr>
                        <td class="border-2 border-[var(--foreground)] px-3 py-2">
                            <a class="text-left font-bold underline"
                               href="${Links.to("/staff/milestones", "filter", "all", "q", team.name())}">${team.name()}</a>
                        </td>
                        @for(var c : columns)
                            !{var s = team.statuses().get(c.id());}
                            !{var label = c.label() + ": " + (s == null ? "Not submitted" : s.label());}
                            <td title="${label}" aria-label="${label}"
                                class="border-2 border-[var(--foreground)] px-3 py-2 text-center ${s == MilestoneStatus.APPROVED ? "bg-[var(--accent)]" : ""}">${s == null ? "" : s == MilestoneStatus.APPROVED ? "✓" : "…"}</td>
                        @endfor
                        <td class="border-2 border-[var(--foreground)] px-3 py-2 text-center">${team.approved()}/${columns.size()}</td>
                    </tr>
                    @endfor
                    </tbody>
                </table>
            </div>

            <p class="mt-3 text-xs text-[var(--foreground)]/60">
                ✓ approved · … awaiting review · blank = not submitted. Select a team to see its submissions.
            </p>

            <ol class="mt-6 grid gap-x-8 gap-y-3 text-sm sm:grid-cols-2">
                @for(var c : ForSupport.of(columns))
                <li class="flex gap-3">
                    <span class="font-display font-bold">${String.format("%02d", c.getIndex() + 1)}</span>
                    <span class="min-w-0 break-words">${c.get().label()}@if(prizes.get(c.get().id()) != null)<span
                            class="block text-xs text-[var(--foreground)]/60">Prize: ${prizes.get(c.get().id())}</span>@endif</span>
                </li>
                @endfor
            </ol>
        </div>
        @endif
        @else
        <%-- review: the submissions, with what each team answered --%>
        <form method="get" action="/staff/milestones" class="mt-6 flex flex-wrap items-center gap-3" role="search">
            <nav class="flex gap-2" aria-label="Filter submissions">
                @for(var t : List.of("pending", "approved", "all"))
                    <a href="${Links.to("/staff/milestones", "filter", t, "q", q)}"
                       class="tag cursor-pointer ${filter.equals(t) ? "tag-active" : "bg-transparent"}"
                       aria-current="${filter.equals(t) ? "page" : null}">${t.substring(0, 1).toUpperCase() + t.substring(1)} · ${t.equals("pending") ? pendingCount : t.equals("approved") ? approvedCount : total}</a>
                @endfor
            </nav>
            <input type="hidden" name="filter" value="${filter}">
            <input type="search" name="q" value="${q}" placeholder="Search team or milestone"
                   aria-label="Search team or milestone" class="search" data-filter="#submissions">
        </form>

        @if(visible.isEmpty())
            <div class="card mt-8 border-dashed">
                <p class="text-sm">${total == 0 ? "No submissions yet." : filter.equals("pending") && q.isEmpty() ? "Nothing waiting for review. 🎉" : "No submissions match."}</p>
            </div>
        @else
        <ul id="submissions" class="mt-8 space-y-6">
            @for(var row : visible)
            <li data-filter-text="${row.teamName()} ${row.milestoneLabel()}">
                <div class="card">
                    <div class="flex flex-wrap items-start justify-between gap-4">
                        <div class="min-w-0">
                            <p class="break-words font-display text-xl font-bold">${row.milestoneLabel()}</p>
                            <p class="mt-1 text-sm">
                                <strong>${row.teamName()}</strong>
                                <span class="text-[var(--foreground)]/60"> · ${row.memberCount()} ${row.memberCount() == 1 ? "member" : "members"}</span>
                            </p>
                            <p class="mt-2 text-xs uppercase tracking-[0.08em] text-[var(--foreground)]/60">Submitted ${row.submittedAtText()}</p>
                            @if(prizes.get(row.milestoneId()) != null)
                                <p class="mt-2 text-sm">
                                    <span class="label">Prize </span>${prizes.get(row.milestoneId())}
                                </p>
                            @endif
                        </div>
                        <span class="tag shrink-0 ${row.approved() ? "bg-[var(--accent)] text-[var(--foreground)]" : "bg-transparent"}">${row.approved() ? "Approved" : "Awaiting review"}</span>
                    </div>

                    @if(row.answers().isEmpty())
                        <p class="mt-4 text-sm text-[var(--foreground)]/60">No answers were stored for this submission.</p>
                    @else
                    <div class="mt-4 space-y-3 border-2 border-[var(--foreground)] px-3 py-3">
                        @for(var a : row.answers())
                        <div>
                            <p class="label">${a.name()}</p>
                            <div class="mt-1 flex flex-col gap-0.5 text-sm">
                                @for(var line : a.lines())
                                    @if(Submission.isLink(line))
                                        <a href="${line}" target="_blank" rel="noopener noreferrer" class="break-all underline">${line}</a>
                                    @else
                                        <span class="break-words">${line}</span>
                                    @endif
                                @endfor
                            </div>
                        </div>
                        @endfor
                    </div>
                    @endif

                    @if(row.approved() && row.reviewedAt() != null)
                        <p class="mt-4 text-xs uppercase tracking-[0.08em] text-[var(--foreground)]/60">Approved ${row.reviewedAtText()}@if(row.reviewedBy() != null) by ${row.reviewedBy()}@endif</p>
                    @endif

                    @template.tag.form(page = page, action = "/staff/milestones/review", classes = "mt-5 flex flex-wrap gap-3", content = @`
                        <input type="hidden" name="teamId" value="${row.teamId()}">
                        <input type="hidden" name="milestoneId" value="${row.milestoneId()}">
                        <input type="hidden" name="filter" value="${filter}">
                        <input type="hidden" name="q" value="${q}">
                        @if(row.approved())
                            <button type="submit" name="decision" value="UNDO_APPROVAL" class="btn-flat btn-outline">Undo approval</button>
                        @else
                            <button type="submit" name="decision" value="APPROVE" class="btn-flat btn-solid">Approve</button>
                            <button type="submit" name="decision" value="SEND_BACK" class="btn-flat btn-outline"
                                    data-confirm="Send “${row.milestoneLabel()}” back to ${row.teamName()}? They’ll need to submit it again.">Send back</button>
                        @endif
                    `)
                </div>
            </li>
            @endfor
        </ul>
        @endif
        @endif
    </div>
</main>
`)
