@import org.limelightcreatives.dashboard.event.EventConfig

<%-- a tab that needs a team, for someone without one (Home, Milestones, Project, Equipment) --%>
@param EventConfig event
@param String heading
@param String text

<h1 class="font-display text-4xl font-bold md:text-5xl">${heading}</h1>
<div class="mx-auto mt-10 w-full max-w-lg md:mt-16">
    <div class="card">
        <div class="flex flex-col items-center px-4 py-10 text-center md:py-14">
            <p class="font-display text-3xl md:text-4xl">Join a team first</p>
            <p class="mt-4 max-w-sm text-base text-[var(--foreground)]/70">${text}</p>
            <a class="btn btn-primary mt-8" href="/participant/${event.id()}/team">Go to team page</a>
        </div>
    </div>
</div>
