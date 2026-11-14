@import org.limelightcreatives.dashboard.event.EventConfig
@import org.limelightcreatives.dashboard.team.Team
@import org.limelightcreatives.dashboard.web.Page

<%-- an event dashboard's Team tab --%>
@param Page page
@param EventConfig event
@param Team team = null
@param boolean locked = false
@param String error = null
@param String joinCode = null
@param String teamName = null

@template.tag.event(page = page, event = event, title = "Team", tab = "team", content = @`
    <h1 class="font-display text-4xl font-bold md:text-5xl">Team</h1>

    @if(team != null)
    <%-- on a team: who, how many, the code to share, and leaving --%>
    <div class="mx-auto mt-10 w-full max-w-xl">
        <div class="card">
            <p class="text-xs uppercase tracking-[0.15em] text-[var(--foreground)]/50">You’re on</p>
            <p class="mt-2 font-display text-3xl">${team.name()}</p>
            <p class="mt-1 text-sm text-[var(--foreground)]/70">${team.memberCount()} / ${team.maxSize()} members</p>

            <p class="mt-6 text-xs uppercase tracking-[0.15em] text-[var(--foreground)]/50">Join code</p>
            <div class="mt-2 flex items-center gap-3">
                <code class="font-display text-4xl font-bold tracking-[0.2em]">${team.joinCode()}</code>
                <%-- dashboard.js shows it --%>
                <button type="button" class="btn btn-secondary" hidden data-copy="${team.joinCode()}">Copy</button>
            </div>

            @if(error != null)
                <p class="mt-4 text-sm text-red-600" role="alert">${error}</p>
            @endif

            @if(locked)
                <p class="mt-6 text-sm text-[var(--foreground)]/70">Teams are locked.</p>
            @else
                @template.tag.form(page = page, action = "/participant/" + event.id() + "/team/leave", classes = "mt-6", content = @`
                    <button type="submit" class="btn btn-secondary" data-busy="Leaving…">Leave team</button>
                `)
            @endif
        </div>
    </div>
    @else
    <%-- not on a team: join one, or start one --%>
    <p class="mt-2 max-w-xl text-sm text-[var(--foreground)]/70">
        Enter a join code from a teammate, or start your own team.
    </p>
    @if(error != null)
        <p class="mt-4 text-sm text-red-600" role="alert">${error}</p>
    @endif
    @if(locked)
        <p class="mt-4 text-sm text-[var(--foreground)]/70">Teams are locked, so you can’t join or create one right now.</p>
    @endif

    <div class="mt-10 grid gap-6 md:grid-cols-2">
        <div class="card">
            <p class="font-display text-2xl">Join a team</p>
            @template.tag.form(page = page, action = "/participant/" + event.id() + "/team/join", classes = "mt-6 space-y-4", content = @`
                <input name="code" value="${joinCode}" placeholder="Join code" maxlength="8" required
                       autocomplete="off" aria-label="Join code" class="input uppercase tracking-[0.2em]"
                       disabled="${locked}">
                <button type="submit" class="btn btn-primary" data-busy="Joining…" data-needs="code"
                        disabled="${locked}">Join team</button>
            `)
        </div>

        <div class="card">
            <p class="font-display text-2xl">Create a team</p>
            @template.tag.form(page = page, action = "/participant/" + event.id() + "/team/create", classes = "mt-6 space-y-4", content = @`
                <input name="name" value="${teamName}" placeholder="Team name" maxlength="60" required
                       aria-label="Team name" class="input" disabled="${locked}">
                <button type="submit" class="btn btn-secondary" data-busy="Creating…" data-needs="name"
                        disabled="${locked}">Create team</button>
            `)
        </div>
    </div>
    @endif
`)
