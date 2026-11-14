@import java.util.List
@import org.limelightcreatives.dashboard.schedule.Schedule
@import org.limelightcreatives.dashboard.schedule.ScheduleController
@import org.limelightcreatives.dashboard.web.Page

<%-- The participants' schedule: what's happening, when and where (content/run-of-show.json). --%>
@param Page page
@param Schedule schedule
@param List<ScheduleController.DayTab> days
@param Schedule.Day day = null
@param ScheduleController.NowNext nowNext = null
@param List<ScheduleController.Entry> entries

@template.layout(page = page, title = "Schedule", content = @`
<main class="min-h-0 flex-1 overflow-y-auto px-6 py-10">
  <div class="mx-auto max-w-3xl" data-refresh-on-focus data-live-interval="30">
    <h1 class="font-display text-4xl font-bold md:text-5xl">Schedule</h1>
    <p class="mt-2 max-w-xl font-body text-sm text-[var(--foreground)]/70">${schedule.event().subtitle() != null ? schedule.event().subtitle() : "What’s happening, when and where."}</p>

    @if(days.size() > 1)
    <div class="mt-8 flex flex-wrap gap-2" role="tablist" aria-label="Day">
      @for(var d : days)
        <a role="tab" aria-selected="${d.active() ? "true" : "false"}" href="${d.href()}"
           class="tag cursor-pointer ${d.active() ? "tag-active" : "bg-transparent text-[var(--foreground)]"}">${d.label()}</a>
      @endfor
    </div>
    @endif

    @if(nowNext != null)
    <%-- Now / next --%>
    <div class="card mt-6">
      @if(nowNext.current() != null)
        <p class="label">Now</p>
        <p class="mt-1 font-body text-sm font-bold">${nowNext.current().text()}</p>
        @if(nowNext.current().where() != null && !nowNext.current().where().isEmpty())
          <p class="mt-1 font-body text-sm text-[var(--foreground)]/70">📍 ${nowNext.current().where()}</p>
        @endif
      @endif
      @if(nowNext.next() != null)
        <p class="label ${nowNext.current() != null ? "mt-4" : ""}">Next · ${nowNext.nextTime()}</p>
        <p class="mt-1 font-body text-sm">${nowNext.next().text()}</p>
        @if(nowNext.next().where() != null && !nowNext.next().where().isEmpty())
          <p class="mt-1 font-body text-sm text-[var(--foreground)]/70">📍 ${nowNext.next().where()}</p>
        @endif
      @endif
    </div>
    @endif

    <div class="mt-8">
      @if(day == null)
        <div class="card border-dashed"><p class="font-body text-sm">The schedule isn’t available yet.</p></div>
      @elseif(entries.isEmpty())
        <div class="card border-dashed">
          <p class="font-display text-xl font-bold">${day.label()} is coming soon</p>
          <p class="mt-1 font-body text-sm">The schedule hasn’t been published yet. Check back here.</p>
        </div>
      @else
      <ul class="space-y-4">
        @for(var it : entries)
        <li aria-current="${"now".equals(it.state()) ? "time" : null}"
            class="${it.marker() ? "pt-2 font-body text-xs font-bold uppercase tracking-[0.08em] text-[var(--foreground)]/60" : null}">
          @if(it.marker())
            ${it.time()} · ${it.text()}
          @elseif("past".equals(it.state()))
            <details class="fold">
              <summary class="flex w-full cursor-pointer items-center gap-3 border-[2px] border-[var(--foreground)]/30 px-3 py-2 text-left font-body text-sm text-[var(--foreground)]/60">
                <span class="font-bold">${it.time()}</span>
                <span class="min-w-0 flex-1 truncate">${it.text()}</span>
                <span aria-hidden="true">✓</span>
              </summary>
              @template.schedule.entry(it = it)
            </details>
          @else
            @template.schedule.entry(it = it)
          @endif
        </li>
        @endfor
      </ul>
      @endif
    </div>
  </div>
</main>
`)
