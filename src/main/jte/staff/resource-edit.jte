@import java.util.List
@import org.limelightcreatives.dashboard.resource.Resource
@import org.limelightcreatives.dashboard.resource.ResourceInput
@import org.limelightcreatives.dashboard.web.Page

<%-- /staff/resources/new and /staff/resources/<id>: the editor --%>
@param Page page
@param ResourceInput draft
@param List<String> categories
@param List<String> kinds
@param List<String> audiences
@param String error = null

@template.layout(page = page, title = draft.isNew() ? "New resource" : "Edit resource", content = @`
<main class="min-h-0 flex-1 animate-page-in overflow-y-auto px-6 py-10">
    <div class="mx-auto max-w-3xl">
        <h1 class="font-display text-4xl font-bold md:text-5xl">Resources</h1>
        @template.tag.alert(message = error)

        <div class="card mt-8">
            <h2 class="font-display text-2xl font-bold">${draft.isNew() ? "New resource" : "Edit resource"}</h2>

            @template.tag.form(page = page, action = "/staff/resources", id = "resource", unsaved = true, content = @`
                <input type="hidden" name="id" value="${draft.id()}">
                <div class="mt-6 space-y-5">
                    <label class="block">
                        <span class="label">Title</span>
                        <input name="title" value="${draft.title()}" class="field" required>
                    </label>

                    <label class="block">
                        <span class="label">Summary</span>
                        <input name="summary" value="${draft.summary()}" class="field">
                        <span class="mt-1 block text-xs text-[var(--foreground)]/60">One line shown on the card.</span>
                    </label>

                    <div class="grid gap-5 sm:grid-cols-3">
                        <label class="block">
                            <span class="label">Type</span>
                            <select name="kind" class="field">
                                @for(var k : kinds)
                                    <option value="${k}" selected="${k.equals(draft.kind())}">${Resource.kindLabel(k)}</option>
                                @endfor
                            </select>
                        </label>
                        <label class="block">
                            <span class="label">Category</span>
                            <input name="category" value="${draft.category()}" list="resource-categories" class="field">
                            <datalist id="resource-categories">
                                @for(var c : categories)
                                    <option value="${c}"></option>
                                @endfor
                            </datalist>
                        </label>
                        <label class="block">
                            <span class="label">Visible to</span>
                            <select name="audience" class="field">
                                @for(var a : audiences)
                                    <option value="${a}" selected="${a.equals(draft.audience())}">${Resource.audienceLabel(a)}</option>
                                @endfor
                            </select>
                        </label>
                    </div>

                    <label class="block">
                        <span class="label">Link</span>
                        <input name="url" value="${draft.url()}" placeholder="https://" inputmode="url" class="field">
                        <span class="mt-1 block text-xs text-[var(--foreground)]/60">Optional. Adds an Open or Watch button.</span>
                    </label>

                    <label class="block">
                        <span class="label">Thumbnail</span>
                        <input name="thumbnailUrl" value="${draft.thumbnailUrl()}" placeholder="https://" inputmode="url" class="field">
                        <span class="mt-1 block text-xs text-[var(--foreground)]/60">
                            Optional image link. YouTube links get one automatically. Without one, the card shows a plain placeholder.</span>
                    </label>
                    @if(draft.thumbnail() != null)
                        <div class="max-w-xs border-2 border-[var(--foreground)]">
                            <img src="${draft.thumbnail()}" alt="Thumbnail preview" class="aspect-video w-full object-cover">
                        </div>
                    @endif

                    <label class="block">
                        <span class="label">Content</span>
                        <textarea name="body" rows="12" class="field resize-y">${draft.body()}</textarea>
                        <span class="mt-1 block text-xs text-[var(--foreground)]/60">
                            Leave a blank line between paragraphs. Lines that are only a web address become links.</span>
                    </label>

                    <div class="flex flex-wrap items-end gap-6">
                        <label class="block">
                            <span class="label">Order</span>
                            <input name="sortOrder" type="number" step="1" value="${draft.sortOrder()}" class="field w-28">
                            <span class="mt-1 block text-xs text-[var(--foreground)]/60">Lower numbers appear first in a category.</span>
                        </label>
                        <label class="flex cursor-pointer items-center gap-2 pb-2 text-sm font-bold">
                            <input type="checkbox" name="published" value="true" checked="${Boolean.TRUE.equals(draft.published())}"
                                   class="h-5 w-5 accent-[var(--accent)]">
                            Published
                        </label>
                    </div>
                </div>
            `)

            <div class="mt-8 flex flex-wrap gap-3">
                <button type="submit" form="resource" class="btn-flat btn-solid" data-busy="Saving…">Save changes</button>
                <a href="/staff/resources" class="btn-flat btn-outline">Cancel</a>
                @if(!draft.isNew())
                    @template.tag.form(page = page, action = "/staff/resources/" + draft.id() + "/delete", classes = "ml-auto", content = @`
                        <button type="submit" class="btn-flat btn-outline border-red-600 text-red-600"
                                data-confirm="Delete “${draft.title()}”? This can’t be undone.">Delete</button>
                    `)
                @endif
            </div>
        </div>
    </div>
</main>
`)
