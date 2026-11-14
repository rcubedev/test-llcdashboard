@import org.limelightcreatives.dashboard.schedule.ScheduleController

<%-- a card on someone's timeline of the staff run of show --%>
@param ScheduleController.Card c

<div class="card ${"now".equals(c.state()) ? "border-l-[10px] border-l-[var(--accent)]" : c.past() ? "opacity-70" : ""}">
  <div class="flex flex-wrap items-start justify-between gap-3">
    <p class="label">${c.time()}</p>
    @if("now".equals(c.state()))
      <span class="tag bg-[var(--accent)] text-[var(--foreground)]">Now</span>
    @endif
    @if(c.past() && !c.kind().equals("done"))
      <button type="button" class="tag cursor-pointer bg-transparent" data-fold>Fold</button>
    @endif
  </div>
  @if(c.kind().equals("arrive") || c.kind().equals("return"))
    <p class="mt-2 font-display text-xl font-bold">${c.title()}</p>
    <p class="mt-1 font-body text-sm text-[var(--foreground)]/70">${c.subtitle()}</p>
  @elseif(c.kind().equals("done"))
    <p class="mt-2 font-display text-xl font-bold">${c.title()}</p>
  @elseif(c.kind().equals("pblock"))
    <p class="mt-2 break-words font-display text-xl font-bold">${c.title()}</p>
    @if(c.note() != null)
      <p class="mt-3 border-[2px] border-[var(--foreground)] px-3 py-2 font-body text-sm">${c.note()}</p>
    @endif
  @elseif(c.kind().equals("block"))
    @if(c.roles().isEmpty())
      <p class="mt-2 font-display text-xl font-bold">Not assigned yet</p>
    @endif
    @for(var r : c.roles())
      <div class="mt-2">
        <p class="font-display text-xl font-bold">${r.name()} @if(r.flexible())<span class="tag">Flex</span>@endif</p>
        @if(!r.steps().isEmpty())
          <ul class="mt-1 list-disc space-y-0.5 pl-5 font-body text-sm">
            @for(var step : r.steps())
              <li>${step}</li>
            @endfor
          </ul>
        @endif
      </div>
    @endfor
    @if(c.note() != null)
      <p class="mt-3 border-[2px] border-[var(--foreground)] px-3 py-2 font-body text-sm">${c.note()}</p>
    @endif
  @endif
  @if(c.location() != null)
    <p class="mt-3 font-body text-sm font-bold">📍 ${c.location()}</p>
  @endif
  @if(c.kind().equals("block") && !c.participants().isEmpty())
    <div class="mt-3 border-t-[2px] border-[var(--foreground)]/20 pt-3 font-body text-sm text-[var(--foreground)]/70">
      @for(var line : c.participants())
        <p>Participants · ${line}</p>
      @endfor
    </div>
  @endif
</div>
