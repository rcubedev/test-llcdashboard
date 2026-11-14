@import org.limelightcreatives.dashboard.web.Page

<%-- apps that have a place but aren't built yet (theatre, presenter) --%>
@param Page page

@template.layout(page = page, content = @`
<main class="flex min-h-0 flex-1 flex-col items-center justify-center overflow-y-auto px-6 py-24 text-center">
    <h1 class="relative mt-4 inline-block font-display text-[clamp(2.75rem,6vw,4.5rem)] font-bold leading-[0.9] tracking-[-0.06em]">
        This app isn’t available . . .  yet!
    </h1>
</main>
`)
