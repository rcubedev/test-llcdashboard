@import org.limelightcreatives.dashboard.event.EventConfig
@import org.limelightcreatives.dashboard.project.Project
@import org.limelightcreatives.dashboard.web.Page

<%-- an event dashboard's Project tab --%>
@param Page page
@param EventConfig event
@param boolean hasTeam = false
@param Project project = null
@param Project form = null
@param boolean editing = false
@param boolean closed = false
@param String driveUrl = null
@param String error = null

@template.tag.event(page = page, event = event, title = "Your project", tab = "project", content = @`
    @if(!hasTeam)
        @template.tag.joinTeam(event = event, heading = "Your project",
                text = "Your team submits one project together. Join or create a team, then come back here to submit it.")
    @else
        <div class="flex flex-wrap items-end justify-between gap-4">
            <div>
                <h1 class="font-display text-4xl font-bold md:text-5xl">Your project</h1>
                <p class="mt-2 max-w-xl text-sm text-[var(--foreground)]/70">
                    Every team submits a single project. You can update the details until submissions close.
                </p>
            </div>
            @if(project != null && !editing && !closed)
                <a class="btn btn-secondary" href="?edit=true">Edit</a>
            @endif
        </div>

        <div class="mt-10 max-w-2xl">
            <div role="note"
                 class="mb-6 flex flex-wrap items-center justify-between gap-4 border-2 border-[var(--foreground)] bg-[var(--accent)]/10 px-4 py-4">
                <div class="min-w-0 flex-1">
                    <p class="font-display text-base font-bold uppercase tracking-[0.04em]">Upload your files first</p>
                    <p class="mt-1 text-sm text-[var(--foreground)]/70">
                        Upload your film and any supporting files to our shared Google Drive folder, then paste the
                        video link below. Make sure the link is set to "Anyone with the link can view".
                    </p>
                </div>
                <a class="btn btn-primary" href="${driveUrl}" target="_blank" rel="noopener noreferrer">Open Google Drive</a>
            </div>

            @if(closed)
                <p class="mb-6 border-2 border-[var(--foreground)] px-3 py-2 text-sm">
                    Submissions are closed, so your project can’t be changed any more.
                </p>
            @endif

            @if(editing)
            <%-- the form: no project yet, Edit, or saving didn't work --%>
            <div class="card">
                @template.tag.form(page = page, action = "/participant/" + event.id() + "/project", classes = "space-y-6", unsaved = true, content = @`
                    <label class="block">
                        <span class="mb-2 block font-display text-xs font-bold uppercase tracking-[0.08em]">Title</span>
                        <input name="title" required maxlength="120" value="${form.title()}" class="input"
                               placeholder="Give your film a name">
                    </label>
                    <label class="block">
                        <span class="mb-2 block font-display text-xs font-bold uppercase tracking-[0.08em]">Logline</span>
                        <textarea name="logline" required maxlength="500" class="input min-h-28"
                                  placeholder="One or two sentences about your film">${form.logline()}</textarea>
                    </label>
                    <label class="block">
                        <span class="mb-2 block font-display text-xs font-bold uppercase tracking-[0.08em]">Video link</span>
                        <input name="videoUrl" type="url" required value="${form.videoUrl()}" class="input"
                               placeholder="https://...">
                    </label>

                    @if(error != null)
                        <p role="alert" class="alert">${error}</p>
                    @endif

                    <div class="flex flex-wrap items-center gap-3">
                        <button type="submit" class="btn btn-primary" data-busy="Saving…">Save project</button>
                        @if(project != null)
                            <a class="btn btn-secondary" href="/participant/${event.id()}/project">Cancel</a>
                        @endif
                    </div>
                `)
            </div>
            @elseif(project != null)
            <%-- saved --%>
            <div class="card">
                <div class="flex flex-wrap items-start justify-between gap-4">
                    <div class="min-w-0 flex-1">
                        <p class="break-words font-display text-xl font-bold">${project.title()}</p>
                        <p class="mt-2 whitespace-pre-wrap break-words text-sm text-[var(--foreground)]/70">${project.logline()}</p>
                    </div>
                    <span class="tag bg-[var(--accent)] text-[var(--background)]">Saved</span>
                </div>
                <div class="mt-6">
                    <a class="btn btn-secondary" href="${project.videoUrl()}" target="_blank" rel="noopener noreferrer">Watch submission</a>
                </div>
            </div>
            @endif
        </div>
    @endif
`)
