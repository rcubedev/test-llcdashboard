@import org.limelightcreatives.dashboard.schedule.ScheduleController

<%-- a block of the participants' schedule --%>
@param ScheduleController.Entry it

<div class="card ${"now".equals(it.state()) ? "border-l-[10px] border-l-[var(--accent)]" : "past".equals(it.state()) ? "opacity-70" : ""}">
    <div class="flex flex-wrap items-start justify-between gap-3">
        <p class="label">${it.timeRange()}</p>
        @if("now".equals(it.state()))
            <span class="tag bg-[var(--accent)] text-[var(--foreground)]">Now</span>
        @elseif("past".equals(it.state()))
            <button type="button" class="tag cursor-pointer bg-transparent" data-fold>Fold</button>
        @endif
    </div>
    <p class="mt-2 break-words font-display text-xl font-bold">${it.text()}</p>
    @if(it.where() != null && !it.where().isEmpty())
        <p class="mt-3 font-body text-sm font-bold">📍 ${it.where()}</p>
    @endif
</div>
