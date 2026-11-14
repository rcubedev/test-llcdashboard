@import org.limelightcreatives.dashboard.equipment.EquipmentView
@import org.limelightcreatives.dashboard.event.EventConfig
@import org.limelightcreatives.dashboard.web.Links
@import org.limelightcreatives.dashboard.web.Page

<%-- an event dashboard's Equipment tab (refreshed when you come back to it) --%>
@param Page page
@param EventConfig event
@param EquipmentView view = null
@param int jitterSeconds = 0
@param String loadError = null
@param String error = null

@template.tag.event(page = page, event = event, title = "Equipment", tab = "equipment", content = @`
    @if(loadError != null)
        @template.tag.failed(heading = "Equipment", title = "Couldn’t load equipment", message = loadError)
    @elseif(view == null)
        @template.tag.joinTeam(event = event, heading = "Equipment",
                text = "Equipment is booked per team. Join or create a team, then come back to reserve.")
    @else
    <div data-refresh-on-focus>
        <div class="flex flex-wrap items-end justify-between gap-4">
            <div>
                <h1 class="font-display text-4xl font-bold md:text-5xl">Equipment</h1>
                <p class="mt-2 max-w-xl text-sm text-[var(--foreground)]/70">
                    Reserve gear in timed slots. Each category shows how many slots your team can book per day.
                    <b>If you plan on attending a workshop, do not book any equipment!</b>
                </p>
            </div>
            <a class="btn btn-secondary" href="">Refresh</a>
        </div>

        <%-- the day --%>
        <nav class="mt-10 flex flex-wrap gap-3" aria-label="Day">
            @for(var d : view.days())
                <a href="${Links.to("/participant/" + event.id() + "/equipment", "day", d.date().toString())}"
                   aria-current="${d.active() ? "page" : null}"
                   class="border-2 border-[var(--foreground)] px-4 py-2 text-left font-display transition-colors ${d.active() ? "bg-[var(--accent)] text-[var(--foreground)]" : "bg-transparent hover:bg-[var(--surface-hover)]"}">
                    <span class="block text-sm font-bold">${d.label()}</span>
                    <span class="block font-body text-xs text-[var(--foreground)]/60">${d.shortDate()}</span>
                </a>
            @endfor
        </nav>

        <p class="mt-6 text-sm text-[var(--foreground)]/70">Booking as <strong>${view.team()}</strong></p>

        @if(!view.open())
            <%-- before the day opens: when, and a countdown (dashboard.js keeps it going and reloads when it opens) --%>
            <p role="status" class="mt-4 border-2 border-[var(--foreground)] bg-[var(--accent)] px-3 py-2 text-sm"
               data-unlocks-at="${view.unlocksAt()}" data-jitter-seconds="${jitterSeconds}">
                Bookings for ${view.dayLabel()} open
                <b>${view.opensAt()}</b>
                (in <span data-countdown>${view.countdown()}</span>). You can look at the slots now;
                reserving unlocks automatically.
            </p>
        @endif

        @template.tag.alert(message = error)

        <ol class="mt-6 space-y-8">
            @for(var c : view.categories())
            <li>
                <div class="card">
                    <div class="flex flex-wrap items-start justify-between gap-6">
                        <div class="flex min-w-0 flex-1 basis-56 items-start gap-4">
                            @if(c.image() != null)
                                <div class="relative h-20 w-20 shrink-0 border-2 border-[var(--foreground)] bg-[var(--surface-hover)] md:h-24 md:w-24">
                                    <img src="${page.asset(c.image())}" alt="" class="absolute inset-0 h-full w-full object-cover">
                                </div>
                            @else
                                <span class="flex h-10 w-10 shrink-0 items-center justify-center border-2 border-[var(--foreground)] font-display text-sm font-bold">${c.number()}</span>
                            @endif
                            <div class="min-w-0">
                                <p class="break-words font-display text-xl font-bold">${c.label()}</p>
                                @if(c.description() != null)
                                    <p class="mt-1 text-sm text-[var(--foreground)]/70">${c.description()}</p>
                                @endif
                                <p class="mt-1 text-xs text-[var(--foreground)]/60">${c.units()} available in total</p>
                            </div>
                        </div>

                        <%-- the team's daily allowance --%>
                        <div class="flex shrink-0 flex-col items-end gap-2">
                            <span class="tag ${c.limitReached() ? "bg-[var(--accent)] text-[var(--foreground)]" : "bg-transparent text-[var(--foreground)]"}">@if(c.limitReached())Limit reached@else${c.booked()}/${c.maxSessions()} booked today@endif</span>
                            @if(c.maxSessions() > 0)
                                <div class="flex gap-1" aria-hidden="true">
                                    @for(int i = 0; i < c.maxSessions(); i++)
                                        <span class="h-3 w-6 border-2 border-[var(--foreground)] ${i < c.booked() ? "bg-[var(--foreground)]" : ""}"></span>
                                    @endfor
                                </div>
                            @endif
                        </div>
                    </div>

                    @if(c.slots().isEmpty())
                        <p class="mt-6 text-sm text-[var(--foreground)]/70">No slots are available on this day.</p>
                    @else
                    <div class="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        @for(var slot : c.slots())
                        <div class="flex flex-col gap-3 border-2 border-[var(--foreground)] px-3 py-3 ${slot.mine() ? "bg-[var(--accent)]" : ""} ${!slot.canReserve() && !slot.mine() ? "opacity-60" : ""}">
                            <div class="flex items-center justify-between gap-2">
                                <p class="font-display text-base font-bold">${slot.label()}</p>
                                <span class="tag bg-transparent">${slot.status()}</span>
                            </div>

                            <%-- a square per unit: filled when taken --%>
                            <div class="flex flex-wrap items-center gap-1.5">
                                @for(var u : slot.units())
                                    <span title="${u.title()}" class="h-3.5 w-3.5 border-2 border-[var(--foreground)] ${u.team() != null ? "bg-[var(--foreground)]" : ""}"></span>
                                @endfor
                                <span class="ml-1 text-xs text-[var(--foreground)]/70">${slot.free()} of ${c.units()} free</span>
                            </div>

                            @if(slot.mine())
                                <p class="text-xs text-[var(--foreground)]/80">You have <b>${slot.myUnit()}</b></p>
                            @endif

                            @if(slot.canReserve())
                                @template.tag.form(page = page, action = "/participant/" + event.id() + "/equipment/reserve", content = @`
                                    <input type="hidden" name="category" value="${c.id()}">
                                    <input type="hidden" name="day" value="${view.date().toString()}">
                                    <input type="hidden" name="slot" value="${slot.id()}">
                                    <button type="submit" class="btn btn-primary w-full" data-busy="Reserving…">RESERVE</button>
                                `)
                            @endif
                            @if(slot.canCancel())
                                @template.tag.form(page = page, action = "/participant/" + event.id() + "/equipment/cancel", content = @`
                                    <input type="hidden" name="category" value="${c.id()}">
                                    <input type="hidden" name="day" value="${view.date().toString()}">
                                    <input type="hidden" name="slot" value="${slot.id()}">
                                    <button type="submit" class="btn btn-secondary w-full" data-busy="Cancelling…">Cancel booking</button>
                                `)
                            @endif
                        </div>
                        @endfor
                    </div>
                    @endif
                </div>
            </li>
            @endfor
        </ol>
    </div>
    @endif
`)
