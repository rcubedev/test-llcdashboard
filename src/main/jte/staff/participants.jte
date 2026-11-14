@import java.util.List
@import org.limelightcreatives.dashboard.checkin.Participants
@import org.limelightcreatives.dashboard.checkin.ParticipantsController
@import org.limelightcreatives.dashboard.web.Links
@import org.limelightcreatives.dashboard.web.Page

<%-- /staff/participants: the participant list in Google Sheets, and who's checked in --%>
@param Page page
@param String filter
@param String q
@param int total
@param List<ParticipantsController.Filter> filters
@param List<Participants.Participant> visible
@param String error = null

@template.layout(page = page, title = "Participants", content = @`
<main class="min-h-0 flex-1 animate-page-in overflow-y-auto px-6 py-10">
    <div class="mx-auto max-w-3xl">
        <div class="flex flex-wrap items-end justify-between gap-4">
            <div>
                <h1 class="font-display text-4xl font-bold md:text-5xl">Participants</h1>
                <p class="mt-2 text-sm text-[var(--foreground)]/70">Manage the participant list in Google Sheets and see who is checked in.</p>
            </div>
            <div class="flex gap-3">
                <a href="" class="btn-flat btn-outline">Refresh</a>
                <a href="/staff/participants/new" class="btn-flat btn-solid">New participant</a>
            </div>
        </div>

        @template.tag.alert(message = error)

        <form method="get" class="mt-8 flex flex-wrap items-center gap-3" role="search">
            <nav class="flex flex-wrap gap-2" aria-label="Filter participants">
                @for(var t : filters)
                    <a href="${Links.to("/staff/participants", "filter", t.id(), "q", q)}"
                       class="tag cursor-pointer ${filter.equals(t.id()) ? "tag-active" : "bg-transparent"}"
                       aria-current="${filter.equals(t.id()) ? "page" : null}">${t.label()} · ${t.count()}</a>
                @endfor
            </nav>
            <input type="hidden" name="filter" value="${filter}">
            <input type="search" name="q" value="${q}" placeholder="Search name, email, phone or ID"
                   aria-label="Search participants" class="search" data-filter="#participants">
        </form>

        @if(visible.isEmpty())
            <div class="card mt-8 border-dashed">
                <p class="text-sm">${total == 0 ? "No participants yet. Select New participant to add the first one." : "No participants match."}</p>
            </div>
        @else
        <ul id="participants" class="mt-8 space-y-4">
            @for(var r : visible)
            !{var checkedIn = r.status() == Participants.Status.IN;}
            <li data-filter-text="${r.name()} ${r.email()} ${r.phone()} ${r.registrationId()}">
                <div class="card ${checkedIn ? "border-l-[10px] border-l-[var(--accent)]" : ""}">
                    <div class="flex flex-wrap items-start justify-between gap-3">
                        <div class="min-w-0">
                            <p class="break-words font-display text-xl font-bold">${r.name().isEmpty() ? "(No name)" : r.name()}</p>
                            <p class="mt-1 text-xs uppercase tracking-[0.08em] text-[var(--foreground)]/60">ID: ${r.registrationId().isEmpty() ? "none" : r.registrationId()}@if(!r.lastAt().isEmpty()) · ${r.status().label()} ${r.lastAt()}@endif</p>
                        </div>
                        <span class="tag shrink-0 ${checkedIn ? "bg-[var(--accent)] text-[var(--foreground)]" : "bg-transparent"} ${r.status() == Participants.Status.NONE ? "border-dashed" : ""}">${r.status().label()}</span>
                    </div>

                    <dl class="mt-3 grid gap-x-6 gap-y-1 text-sm sm:grid-cols-3">
                        <div class="min-w-0">
                            <dt class="label">Email</dt>
                            <dd class="break-words">${r.email().isEmpty() ? "-" : r.email()}</dd>
                        </div>
                        <div>
                            <dt class="label">Phone</dt>
                            <dd>${r.phone().isEmpty() ? "-" : r.phone()}</dd>
                        </div>
                        <div>
                            <dt class="label">Emergency</dt>
                            <dd>${r.emergency().isEmpty() ? "-" : r.emergency()}</dd>
                        </div>
                    </dl>

                    <div class="mt-4 flex flex-wrap gap-3">
                        @template.tag.form(page = page, action = "/staff/participants/toggle", content = @`
                            <input type="hidden" name="id" value="${r.registrationId()}">
                            <input type="hidden" name="filter" value="${filter}">
                            <input type="hidden" name="q" value="${q}">
                            <button type="submit" class="btn-flat ${checkedIn ? "btn-outline" : "btn-solid"}"
                                    disabled="${r.registrationId().isEmpty()}">${checkedIn ? "Check out" : "Check in"}</button>
                        `)
                        <a href="${Links.to("/staff/participants/edit", "id", r.registrationId())}" class="btn-flat btn-outline">Edit</a>
                    </div>
                </div>
            </li>
            @endfor
        </ul>
        @endif
    </div>
</main>
`)
