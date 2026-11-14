@import org.limelightcreatives.dashboard.web.Page

<%-- auth sent someone back without signing them in (or the sign-in didn't check out) --%>
@param Page page

@template.layout(page = page, title = "Couldn’t sign in", content = @`
    <main class="flex min-h-0 flex-1 items-center justify-center overflow-y-auto px-6 py-16">
        <div class="card flex w-full max-w-md flex-col items-start gap-5">
            <h1 class="font-display text-3xl font-bold md:text-4xl">We couldn’t sign you in</h1>
            <p class="text-sm text-[var(--foreground)]/70">
                Something went wrong on the way back from signing in. Please try again. If it keeps happening, reach
                out to us at inquiries@limelightcreatives.org.
            </p>
            <a class="btn btn-primary" href="/oauth2/authorization/limelight">Try again</a>
        </div>
    </main>
`)
