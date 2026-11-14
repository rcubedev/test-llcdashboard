@import java.util.List
@import org.limelightcreatives.dashboard.resource.Resource
@import org.limelightcreatives.dashboard.web.Links
@import org.limelightcreatives.dashboard.web.Page

<%-- /staff/resources: every resource, drafts too --%>
@param Page page
@param String filter
@param String q
@param int total
@param long publishedCount
@param List<Resource> visible
@param String error = null

@template.layout(page = page, title = "Resources", content = @`
<main class="min-h-0 flex-1 animate-page-in overflow-y-auto px-6 py-10">
    <div class="mx-auto max-w-6xl">
        <div class="flex flex-wrap items-end justify-between gap-4">
            <div>
                <h1 class="font-display text-4xl font-bold md:text-5xl">Resources</h1>
                <p class="mt-2 text-sm text-[var(--foreground)]/70">Write and publish the guides and tutorials participants see.</p>
            </div>
            <a href="/staff/resources/new" class="btn-flat btn-solid">New resource</a>
        </div>

        @template.tag.alert(message = error)

        <form method="get" class="mt-8 flex flex-wrap items-center gap-3" role="search">
            <nav class="flex gap-2" aria-label="Filter resources">
                @for(var t : List.of("all", "published", "drafts"))
                    <a href="${Links.to("/staff/resources", "filter", t, "q", q)}"
                       class="tag cursor-pointer ${filter.equals(t) ? "tag-active" : "bg-transparent"}"
                       aria-current="${filter.equals(t) ? "page" : null}">${t.substring(0, 1).toUpperCase() + t.substring(1)} · ${t.equals("all") ? total : t.equals("published") ? publishedCount : total - publishedCount}</a>
                @endfor
            </nav>
            <input type="hidden" name="filter" value="${filter}">
            <input type="search" name="q" value="${q}" placeholder="Search title or category"
                   aria-label="Search resources" class="search" data-filter="#resources">
        </form>

        @if(visible.isEmpty())
            <div class="card mt-8 border-dashed">
                <p class="text-sm">${total == 0 ? "No resources yet. Select New resource to add the first one." : "No resources match."}</p>
            </div>
        @else
        <ul id="resources" class="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for(var r : visible)
            <li class="min-w-0" data-filter-text="${r.title()} ${r.category()}">
                <div class="card flex h-full flex-col">
                    <div class="flex flex-wrap items-start justify-between gap-3">
                        <div class="min-w-0">
                            <p class="break-words font-display text-xl font-bold">${r.title()}</p>
                            <p class="mt-1 text-xs uppercase tracking-[0.08em] text-[var(--foreground)]/60">${r.category()} · ${r.audienceLabel()} · Updated ${r.updatedText()}</p>
                        </div>
                        <span class="tag shrink-0 ${r.published() ? "bg-[var(--accent)] text-[var(--foreground)]" : "bg-transparent"}">${r.published() ? "Published" : "Draft"}</span>
                    </div>
                    <div class="mt-auto flex flex-wrap gap-3 pt-4">
                        <a href="/staff/resources/${r.id()}" class="btn-flat btn-solid">Edit</a>
                        @template.tag.form(page = page, action = "/staff/resources/" + r.id() + "/published", content = @`
                            <input type="hidden" name="published" value="${String.valueOf(!r.published())}">
                            <button type="submit" class="btn-flat btn-outline">${r.published() ? "Unpublish" : "Publish"}</button>
                        `)
                    </div>
                </div>
            </li>
            @endfor
        </ul>
        @endif
    </div>
</main>
`)
