@import java.util.List
@import org.limelightcreatives.dashboard.event.EventConfig
@import org.limelightcreatives.dashboard.resource.Resource
@import org.limelightcreatives.dashboard.resource.ResourceControllers
@import org.limelightcreatives.dashboard.web.Links
@import org.limelightcreatives.dashboard.web.Page

<%-- an event dashboard's Resources tab --%>
@param Page page
@param EventConfig event
@param int total
@param List<ResourceControllers.Category> categories
@param String category
@param String q
@param List<Resource> visible

@template.tag.event(page = page, event = event, title = "Resources", tab = "resources", content = @`
<div class="mx-auto max-w-6xl" data-refresh-on-focus>
    <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
            <h1 class="font-display text-4xl font-bold md:text-5xl">Resources</h1>
            <p class="mt-2 text-sm text-[var(--foreground)]/70">Guides and tutorials to help you prepare for the shoot.</p>
        </div>
        <a href="" class="btn-flat btn-outline">Refresh</a>
    </div>

    <form method="get" class="mt-8 flex flex-wrap items-center gap-3" role="search">
        @if(!categories.isEmpty())
            <nav class="flex flex-wrap gap-2" aria-label="Filter by category">
                @for(var c : categories)
                    <a href="${Links.to("/participant/" + event.id() + "/resources", "category", c.name(), "q", q)}"
                       class="tag cursor-pointer ${category.equals(c.name()) ? "tag-active" : "bg-transparent"}"
                       aria-current="${category.equals(c.name()) ? "page" : null}">${c.name()} · ${c.count()}</a>
                @endfor
            </nav>
        @endif
        <input type="hidden" name="category" value="${category}">
        <input type="search" name="q" value="${q}" placeholder="Search resources" aria-label="Search resources"
               class="search" data-filter="#resources">
    </form>

    @if(visible.isEmpty())
        <div class="card mt-8 border-dashed">
            <p class="text-sm">${total == 0 ? "No resources have been published yet. Check back soon." : "No resources match."}</p>
        </div>
    @else
    <ul id="resources" class="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        @for(var r : visible)
        <li class="min-w-0" data-filter-text="${r.title()} ${r.summary()} ${r.body()}">
            <div class="card flex h-full flex-col">
                <%-- the thumbnail, or the type in big letters when there isn't one (or it doesn't load) --%>
                <a href="${r.hasUrl() ? r.url() : null}" target="${r.hasUrl() ? "_blank" : null}" rel="${r.hasUrl() ? "noopener noreferrer" : null}"
                   class="mb-4 block">
                    <span class="relative block aspect-video w-full overflow-hidden border-2 border-[var(--foreground)] bg-[var(--foreground)]/10">
                        <span aria-hidden="true"
                              class="absolute inset-0 flex items-center justify-center font-display text-4xl font-bold uppercase tracking-[0.08em] text-[var(--foreground)]/30">${r.kindLabel()}</span>
                        @if(r.thumbnail() != null)
                            <img src="${r.thumbnail()}" alt="Thumbnail for ${r.title()}" loading="lazy"
                                 onerror="this.remove()" class="relative h-full w-full object-cover">
                        @endif
                        @if(r.kind().equals("video"))
                            <span aria-hidden="true"
                                  class="absolute bottom-3 left-3 border-2 border-[var(--foreground)] bg-[var(--background)] px-2 py-0.5 font-display text-[0.7rem] font-bold uppercase tracking-[0.08em]">▶ Video</span>
                        @endif
                    </span>
                </a>

                <div class="flex flex-wrap items-start justify-between gap-4">
                    <div class="min-w-0">
                        <p class="break-words font-display text-xl font-bold">${r.title()}</p>
                        @if(!r.summary().isEmpty())
                            <p class="mt-1 break-words text-sm">${r.summary()}</p>
                        @endif
                        <p class="mt-2 text-xs uppercase tracking-[0.08em] text-[var(--foreground)]/60">${r.category()} · Updated ${r.updatedText()}</p>
                    </div>
                    <span class="tag shrink-0 bg-transparent text-[var(--foreground)]">${r.kindLabel()}</span>
                </div>

                @if(r.hasBody())
                <%-- Read opens the content below the buttons (no JavaScript needed); Open goes to the link --%>
                <details class="group mt-auto pt-5">
                    <summary class="flex list-none flex-wrap gap-3 [&::-webkit-details-marker]:hidden">
                        <span class="btn-flat ${r.hasUrl() ? "btn-outline" : "btn-solid"}">
                            <span class="group-open:hidden">Read</span><span class="hidden group-open:inline">Hide</span>
                        </span>
                        @if(r.hasUrl())
                            <a href="${r.url()}" target="_blank" rel="noopener noreferrer"
                               class="btn-flat btn-solid">${r.kind().equals("video") ? "Watch" : "Open"}</a>
                        @endif
                    </summary>
                    <div class="mt-4 space-y-3 border-2 border-[var(--foreground)] px-3 py-3 text-sm">
                        @for(var paragraph : r.paragraphs())
                        <p class="flex flex-col gap-0.5">
                            @for(var line : paragraph)
                                @if(Resource.isLink(line))
                                    <a href="${line}" target="_blank" rel="noopener noreferrer" class="break-all underline">${line}</a>
                                @else
                                    <span class="break-words">${line}</span>
                                @endif
                            @endfor
                        </p>
                        @endfor
                    </div>
                </details>
                @elseif(r.hasUrl())
                <div class="mt-auto flex flex-wrap gap-3 pt-5">
                    <a href="${r.url()}" target="_blank" rel="noopener noreferrer"
                       class="btn-flat btn-solid">${r.kind().equals("video") ? "Watch" : "Open"}</a>
                </div>
                @endif
            </div>
        </li>
        @endfor
    </ul>
    @endif
</div>
`)
