@import org.limelightcreatives.dashboard.checkin.Participants
@import org.limelightcreatives.dashboard.web.Page

<%-- /staff/participants/new and /staff/participants/edit?id=...: a participant's row in the sheet --%>
@param Page page
@param Participants.Input draft
@param String error = null

@template.layout(page = page, title = draft.isNew() ? "New participant" : "Edit participant", content = @`
<main class="min-h-0 flex-1 animate-page-in overflow-y-auto px-6 py-10">
    <div class="mx-auto max-w-3xl">
        <h1 class="font-display text-4xl font-bold md:text-5xl">Participants</h1>
        @template.tag.alert(message = error)

        <div class="card mt-8">
            <h2 class="font-display text-2xl font-bold">${draft.isNew() ? "New participant" : "Edit participant"}</h2>

            @template.tag.form(page = page, action = "/staff/participants", id = "participant", unsaved = true, content = @`
                <input type="hidden" name="originalId" value="${draft.originalId()}">
                <div class="mt-6 space-y-5">
                    <label class="block">
                        <span class="label">Name</span>
                        <input name="name" value="${draft.name()}" class="field" required>
                    </label>
                    <label class="block">
                        <span class="label">Email</span>
                        <input name="email" type="email" inputmode="email" value="${draft.email()}" class="field">
                    </label>
                    <div class="grid gap-5 sm:grid-cols-2">
                        <label class="block">
                            <span class="label">Phone no.</span>
                            <input name="phone" inputmode="tel" value="${draft.phone()}" class="field">
                        </label>
                        <label class="block">
                            <span class="label">Emergency no.</span>
                            <input name="emergency" inputmode="tel" value="${draft.emergency()}" class="field">
                        </label>
                    </div>
                    <label class="block">
                        <span class="label">Registration ID</span>
                        <input name="registrationId" value="${draft.registrationId()}" class="field" required maxlength="100">
                        <span class="mt-1 block text-xs text-[var(--foreground)]/60">
                            Must match the ID in the participant’s QR code. Changing it won’t update an existing code.</span>
                    </label>
                </div>
            `)

            <div class="mt-8 flex flex-wrap gap-3">
                <button type="submit" form="participant" class="btn-flat btn-solid" data-busy="Saving…">Save changes</button>
                <a href="/staff/participants" class="btn-flat btn-outline">Cancel</a>
                @if(!draft.isNew())
                    @template.tag.form(page = page, action = "/staff/participants/delete", classes = "ml-auto", content = @`
                        <input type="hidden" name="id" value="${draft.originalId()}">
                        <button type="submit" class="btn-flat btn-outline border-red-600 text-red-600"
                                data-confirm="Delete “${draft.name()}” from the sheet? This can’t be undone.">Delete</button>
                    `)
                @endif
            </div>
        </div>
    </div>
</main>
`)
