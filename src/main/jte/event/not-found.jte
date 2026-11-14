@import org.limelightcreatives.dashboard.web.Page

<%-- /participant/<id> for an event that isn't there (or isn't active) --%>
@param Page page
@param String eventId

@template.layout(page = page, title = "Event not found", content = @`
<main class="flex min-h-0 flex-1 items-center justify-center overflow-y-auto px-6 py-16">
    <div class="card flex w-full max-w-md flex-col items-start gap-5">
        <h1 class="font-display text-3xl font-bold md:text-4xl">This event doesn’t exist . . . or does it?</h1>
        <p class="text-sm text-[var(--foreground)]/70">
            We couldn’t find an event called
            <strong class="break-all font-bold text-[var(--foreground)]">"${eventId}"</strong>.
            It may have concluded, or the link might be wrong.
        </p>
        <a class="btn btn-primary" href="/">Go back home</a>
    </div>
</main>
`)
