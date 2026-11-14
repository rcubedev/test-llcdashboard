@import org.limelightcreatives.dashboard.checkin.CheckInResult
@import org.limelightcreatives.dashboard.web.Page

<%--
  /staff/checkin: scan a participant's QR code (or type their registration ID) to check them in, and again to
  check them out. The camera and "Recent scans" are static/js/checkin.js; without it, typing an ID still works.
--%>
@param Page page
@param CheckInResult result = null

@template.layout(page = page, title = "Check-in", content = @`
<main class="min-h-0 flex-1 animate-page-in overflow-y-auto px-6 py-10">
    <script type="module" src="${page.asset("/js/checkin.js")}"></script>
    <div class="mx-auto max-w-2xl" data-checkin>
        <h1 class="font-display text-4xl font-bold md:text-5xl">Check-in</h1>

        <div class="card mt-8 overflow-hidden !p-0">
            <div class="relative aspect-square w-full bg-black">
                <video data-camera class="h-full w-full object-cover" muted playsinline></video>
                <div class="pointer-events-none absolute inset-0 flex items-center justify-center">
                    <div data-finder class="h-3/5 w-3/5 border-[3px] border-white transition-colors"></div>
                </div>
                <div data-pending aria-live="polite"
                     class="pointer-events-none absolute inset-0 flex items-center justify-center bg-black/80 opacity-0 transition-opacity duration-150">
                    <span class="spinner"></span>
                </div>
            </div>
        </div>

        <div data-result aria-live="polite">
            @if(result != null)
            <div class="card mt-6 ${result.checkedIn() ? "bg-[var(--accent)]" : ""} ${result.ok() ? "" : "border-dashed"}">
                <span class="tag bg-transparent">${result.label()}</span>
                @if(result.ok())
                    <p class="mt-3 break-words font-display text-3xl font-bold">${result.name()}</p>
                    <p class="mt-2 text-xs uppercase tracking-[0.08em] text-[var(--foreground)]/60">ID: ${result.id()} · ${result.at()}</p>
                @else
                    <p class="mt-3 text-sm">${result.problem()}</p>
                @endif
            </div>
            @endif
        </div>

        @template.tag.form(page = page, action = "/staff/checkin", classes = "mt-8 flex gap-3", manual = true, content = @`
            <input name="code" required maxlength="100" placeholder="Or enter registration ID" aria-label="Registration ID"
                   autocomplete="off" class="search">
            <button type="submit" class="btn-flat btn-solid">Submit</button>
        `)

        <div data-history-wrap class="mt-10" hidden>
            <h2 class="font-display text-xl font-bold">Recent scans</h2>
            <ul data-history class="mt-3 space-y-2"></ul>
        </div>

        <%-- copied by checkin.js for each scan --%>
        <template data-result-template>
            <div class="card mt-6">
                <span class="tag bg-transparent" data-label></span>
                <p class="mt-3 break-words font-display text-3xl font-bold" data-name></p>
                <p class="mt-2 text-xs uppercase tracking-[0.08em] text-[var(--foreground)]/60" data-meta></p>
                <p class="mt-3 text-sm" data-problem></p>
            </div>
        </template>
        <template data-history-template>
            <li class="flex items-center justify-between gap-4 border-2 border-[var(--foreground)] px-3 py-2 text-sm">
                <span class="min-w-0 truncate" data-title></span>
                <span class="tag shrink-0" data-label></span>
            </li>
        </template>
    </div>
</main>
`)
