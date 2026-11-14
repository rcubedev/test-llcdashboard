@import org.limelightcreatives.dashboard.event.EventConfig
@import org.limelightcreatives.dashboard.web.Page

<%-- an event that's there but closed (locked: true in events.yml), over its colours --%>
@param Page page
@param EventConfig event

@template.layout(page = page, title = event.name(), content = @`
<div class="flex min-h-0 flex-1 flex-col" style="${event.theme().style()}">
    <div role="dialog" aria-modal="true" aria-labelledby="event-locked-title" aria-describedby="event-locked-description"
         class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-6 py-16 backdrop-blur-sm">
        <div class="card flex w-full max-w-md flex-col items-start gap-5">
            <h1 id="event-locked-title" class="font-display text-3xl font-bold md:text-4xl">This event isn’t open yet</h1>
            <p id="event-locked-description" class="text-sm text-[var(--foreground)]/70">
                <strong class="font-bold text-[var(--foreground)]">${event.name()}</strong>
                hasn’t started, so the dashboard is closed for now. Check back once the event kicks off!
            </p>
            <a class="btn btn-primary" href="/">Go back home</a>
        </div>
    </div>
</div>
`)
