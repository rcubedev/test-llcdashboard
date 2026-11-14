@import gg.jte.Content
@import org.limelightcreatives.dashboard.event.EventConfig
@import org.limelightcreatives.dashboard.event.Tab
@import org.limelightcreatives.dashboard.web.Page

<%-- every page of an event's dashboard: the tabs, then the page, in the event's colours (not the top bar's) --%>
@param Page page
@param EventConfig event
@param String title
@param String tab
@param Content content

@template.layout(page = page, title = title, content = @`
<div class="flex min-h-0 flex-1 flex-col overflow-hidden md:flex-row" style="${event.theme().style()}">
    <aside class="shrink-0 border-b border-[var(--border)] bg-[var(--background)] md:w-60 md:overflow-y-auto md:border-b-0 md:border-r">
        <%-- the padding leaves room for the hard shadows, which overflow-x-auto would otherwise clip --%>
        <nav class="flex gap-4 overflow-x-auto px-6 pb-5 pt-4 md:flex-col md:gap-4 md:overflow-visible md:p-6 md:pt-8"
             aria-label="Event">
            @for(Tab t : Tab.ALL)
                <a href="${t.href(event)}" aria-current="${t.id().equals(tab) ? "page" : null}"
                   class="flex items-center gap-3 whitespace-nowrap border-2 px-4 py-3 font-display text-sm font-bold uppercase tracking-[0.1em] transition-all duration-150 ease-out
                          focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--foreground)]
                          ${t.id().equals(tab)
                              ? "border-[var(--foreground)] bg-[var(--accent)] text-white shadow-[4px_4px_0_0_var(--foreground)]"
                              : "border-transparent text-[var(--foreground)]/60 hover:-translate-x-[2px] hover:-translate-y-[2px] hover:border-[var(--foreground)] hover:text-[var(--foreground)] hover:shadow-[4px_4px_0_0_var(--foreground)] active:translate-x-0 active:translate-y-0 active:shadow-[2px_2px_0_0_var(--foreground)]"}">
                    @template.tag.icon(name = t.id())
                    <span>${t.label()}</span>
                </a>
            @endfor
        </nav>
    </aside>

    <main class="min-h-0 min-w-0 flex-1 overflow-y-auto px-6 py-10 md:px-16 md:py-14">
        <div class="animate-page-in">
            ${content}
        </div>
    </main>
</div>
`)
