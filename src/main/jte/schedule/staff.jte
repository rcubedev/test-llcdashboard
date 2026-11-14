@import java.util.List
@import java.util.stream.Collectors
@import org.limelightcreatives.dashboard.schedule.RunOfShow
@import org.limelightcreatives.dashboard.schedule.Schedule
@import org.limelightcreatives.dashboard.schedule.ScheduleController
@import org.limelightcreatives.dashboard.web.Page

<%-- The staff run of show: "My day" for one person, or "Full view" of everyone (content/run-of-show.json). --%>
@param Page page
@param Schedule schedule
@param List<ScheduleController.DayTab> days
@param Schedule.Day day = null
@param boolean full
@param boolean picking
@param boolean hasBlocks
@param String who = null
@param Schedule.Staff me = null
@param boolean isParticipant
@param String mineHref
@param String fullHref
@param String switchHref
@param RunOfShow.Banner banner = null
@param List<ScheduleController.PickerButton> picker = null
@param String participantHref = null
@param List<ScheduleController.GridRow> grid = null
@param List<Schedule.Staff> staffColumns = null
@param boolean absent = false
@param List<ScheduleController.Card> cards = null

@template.layout(page = page, title = "Schedule", content = @`
<main class="min-h-0 flex-1 animate-page-in overflow-y-auto px-6 py-10">
  <div class="mx-auto ${full && hasBlocks ? "max-w-6xl" : "max-w-3xl"}" data-refresh-on-focus data-live-interval="30">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="font-display text-4xl font-bold md:text-5xl">Schedule</h1>
        <p class="mt-2 font-body text-sm text-[var(--foreground)]/70">${schedule.event().subtitle() != null ? schedule.event().subtitle() : "Run of Show"}: who does what, when and where.</p>
      </div>
      <div class="flex gap-2" role="tablist" aria-label="View">
        <a role="tab" aria-selected="${full ? "false" : "true"}" href="${mineHref}"
           class="tag cursor-pointer ${full ? "bg-transparent text-[var(--foreground)]" : "tag-active"}">My day</a>
        <a role="tab" aria-selected="${full ? "true" : "false"}" href="${fullHref}"
           class="tag cursor-pointer ${full ? "tag-active" : "bg-transparent text-[var(--foreground)]"}">Full view</a>
      </div>
    </div>

    @if(!picking)
    <div class="mt-8 flex flex-wrap items-center gap-3">
      <div class="flex gap-2" role="tablist" aria-label="Day">
        @for(var d : days)
          <a role="tab" aria-selected="${d.active() ? "true" : "false"}" href="${d.href()}"
             class="tag cursor-pointer ${d.active() ? "tag-active" : "bg-transparent text-[var(--foreground)]"}">${d.label()}</a>
        @endfor
      </div>
      @if(who != null)
        <a href="${switchHref}" class="btn-flat btn-outline ml-auto">Switch</a>
      @endif
    </div>
    @endif

    @if(banner != null)
    <div class="card mt-6">
      <p class="label">Now</p>
      <p class="font-body text-sm font-bold">${banner.now()}</p>
      @if(!banner.nextLine().isEmpty())
        <p class="label mt-3">Next</p>
        <p class="font-body text-sm">${banner.nextLine()}</p>
      @endif
      @if(!banner.walk().isEmpty())
        <p class="tag mt-3 bg-[var(--accent)]">→ ${banner.walk()}</p>
      @endif
    </div>
    @endif

    <div class="mt-8">
      @if(day == null)
        <div class="card border-dashed"><p class="font-body text-sm">No days in run-of-show.json yet.</p></div>
      @elseif(picking)
        <%-- Who are you? --%>
        <div>
          <h2 class="font-display text-2xl font-bold">Who are you?</h2>
          <a href="${participantHref}" class="btn-flat btn-solid mt-4 block w-full">Participant</a>
          <div class="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
            @for(var b : picker)
              <a href="${b.href()}" class="btn-flat btn-outline">${b.name()}</a>
            @endfor
          </div>
        </div>
      @elseif(!hasBlocks)
        <div class="card border-dashed">
          <p class="font-display text-xl font-bold">${day.label()} is coming soon</p>
          <p class="mt-1 font-body text-sm">The run of show hasn’t been published yet. Check back here.</p>
        </div>
      @endif

      @if(grid != null)
      <%-- Full view: everyone, by time --%>
      <div class="overflow-x-auto border-[2px] border-[var(--foreground)]">
        <table class="w-full min-w-max border-collapse">
          <caption class="sr-only">${day.label()}: who does what, by time</caption>
          <thead>
          <tr>
            <th scope="col" class="label border-[2px] border-[var(--foreground)] px-3 py-2 text-left align-top text-[var(--foreground)]">Time</th>
            <th scope="col" class="label border-[2px] border-[var(--foreground)] px-3 py-2 text-left align-top text-[var(--foreground)] ${"participant".equals(who) ? "bg-[var(--accent)]/30" : ""}">Participants</th>
            @for(var s : staffColumns)
              <th scope="col" class="label border-[2px] border-[var(--foreground)] px-3 py-2 text-left align-top text-[var(--foreground)] ${s.id().equals(who) ? "bg-[var(--accent)]/30" : ""}">${s.name()}</th>
            @endfor
          </tr>
          </thead>
          <tbody>
          @for(var r : grid)
          <tr class="${"now".equals(r.state()) ? "bg-[var(--accent)]/20" : "past".equals(r.state()) ? "opacity-60" : null}">
            <th scope="row" class="label whitespace-nowrap border-[2px] border-[var(--foreground)] px-3 py-2 text-left align-top text-[var(--foreground)]">
              ${r.time()}
              @if(!r.marker())
                <span class="mt-1 block font-body font-normal normal-case tracking-normal">${r.place()}</span>
              @endif
            </th>
            @if(r.marker())
              <td colspan="${staffColumns.size() + 1}"
                  class="border-[2px] border-[var(--foreground)] px-3 py-2 align-top font-body text-sm font-bold uppercase tracking-[0.08em]">${r.participants()}</td>
            @else
              <td class="min-w-[12rem] border-[2px] border-[var(--foreground)] px-3 py-2 align-top font-body text-sm ${"participant".equals(who) ? "bg-[var(--accent)]/30" : ""}">
                @if(r.participants() != null)
                  ${r.participants()}
                @else
                  <em>Staff only: ${r.staffOnly()}</em>
                @endif
              </td>
              @for(var c : r.cells())
              <td class="border-[2px] border-[var(--foreground)] px-3 py-2 align-top font-body text-sm ${c.off() ? "text-[var(--foreground)]/40" : "min-w-[9rem]"} ${c.mine() ? "bg-[var(--accent)]/30" : ""}">
                @if(c.off())
                  Not on site@if(c.partial() != null)<span class="block text-xs">${c.partial()}</span>@endif
                @else
                  <div class="flex flex-wrap gap-1">
                    @for(var role : c.roles())
                      <span class="tag">${role.name()}${role.flexible() ? " · Flex" : ""}</span>
                    @endfor
                    @if(c.roles().isEmpty())
                      <em>Not assigned</em>
                    @endif
                  </div>
                  @if(c.location() != null)
                    <span class="mt-1 block text-xs">📍 ${c.location()}</span>
                  @endif
                  @if(c.note() != null)
                    <span class="mt-1 block text-xs">${c.note()}</span>
                  @endif
                  @if(c.partial() != null)
                    <span class="mt-1 block text-xs font-bold">${c.partial()}</span>
                  @endif
                @endif
              </td>
              @endfor
            @endif
          </tr>
          @endfor
          </tbody>
        </table>
      </div>
      @endif

      @if(day != null && !picking && hasBlocks && !full && me != null && me.placeholder() != null)
        <div class="card border-dashed">
          <p class="font-display text-xl font-bold">${me.placeholder()}</p>
          <p class="mt-1 font-body text-sm">Your timeline will appear here once assignments are confirmed.</p>
        </div>
      @endif

      @if(cards != null)
      <%-- My day --%>
      <div>
        <h2 class="font-display text-2xl font-bold">${isParticipant ? "Your " + day.label() : me.name() + "’s " + day.label()}</h2>
        @if(me != null && me.note() != null)
          <p class="mt-1 font-body text-sm text-[var(--foreground)]/70">${me.note()}</p>
        @endif
        @if(me != null && schedule.event().redirectNote() != null && !me.id().equals(schedule.event().leadId()))
          <p class="mt-1 font-body text-sm text-[var(--foreground)]/70">${schedule.event().redirectNote()}</p>
        @endif
        @if(absent)
          <div class="card mt-6">
            <p class="font-display text-xl font-bold">Not on site</p>
            <p class="mt-1 font-body text-sm">You’re not scheduled for ${day.label()}.</p>
          </div>
        @endif

        <ul class="mt-6 space-y-4">
          @for(var c : cards)
          <li aria-current="${"now".equals(c.state()) ? "time" : null}"
              class="${c.kind().equals("marker") ? "font-body text-xs uppercase tracking-[0.08em] text-[var(--foreground)]/60" : null}">
            @if(c.kind().equals("marker"))
              ${c.time()} · Participants · ${c.marker()}
            @elseif(c.past() && !c.kind().equals("done"))
              <details class="fold">
                <summary class="flex w-full cursor-pointer items-center gap-3 border-[2px] border-[var(--foreground)]/30 px-3 py-2 text-left font-body text-sm text-[var(--foreground)]/60">
                  <span class="font-bold">${c.time().split(" –")[0]}</span>
                  <span class="min-w-0 flex-1 truncate">${c.title() != null ? c.title() : c.roles().stream().map(RunOfShow.Role::name).collect(Collectors.joining(" + "))}</span>
                  <span aria-hidden="true">✓</span>
                </summary>
                @template.schedule.card(c = c)
              </details>
            @else
              @template.schedule.card(c = c)
            @endif
          </li>
          @endfor
        </ul>
      </div>
      @endif
    </div>
  </div>
</main>
`)
