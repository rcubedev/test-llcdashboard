@import gg.jte.Content
@import org.limelightcreatives.dashboard.web.Page

<%-- every page: the top bar (logo and account menu), then the page --%>
@param Page page
@param String title = null
@param Content content

<!DOCTYPE html>
<html lang="en" class="h-full antialiased">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <title>${title != null ? title + " - Limelight Creatives" : "Dashboard - Limelight Creatives"}</title>
        <meta name="robots" content="noindex">

        <meta name="theme-color" content="#fffdf5">
        <link rel="icon" href="${page.asset("/favicon.ico")}">

        <link rel="preload" href="${page.asset("/fonts/classica/Classica-Bold.woff2")}" as="font" type="font/woff2" crossorigin>
        <link rel="preload" href="${page.asset("/fonts/hanken-grotesk/HankenGrotesk-Regular.woff2")}" as="font" type="font/woff2" crossorigin>

        <link rel="stylesheet" href="${page.asset("/css/app.css")}">

        @if(page.csrf() != null)
            <%-- for scripts that post (the check-in camera): forms carry their own token --%>
            <meta name="csrf-token" content="${page.csrf().getToken()}">
            <meta name="csrf-header" content="${page.csrf().getHeaderName()}">
        @endif

        <%-- extras only: every page works without it --%>
        <script defer src="${page.asset("/js/dashboard.js")}"></script>
    </head>
    <body class="flex h-dvh flex-col overflow-hidden font-body">
        <header class="flex items-center justify-between border-b border-[var(--border)] bg-[var(--background)] px-6 py-4 md:px-16">
            <a href="/"><img src="${page.asset("/branding/logo.svg")}" alt="Limelight Creatives" width="52" height="52"></a>

            @if(page.person() != null)
            <%-- the account menu: who's signed in, and log out --%>
            <details class="relative" data-menu>
                <summary class="flex cursor-pointer list-none items-center gap-2 px-1 py-1 text-sm transition-[background-color,transform] duration-150 hover:bg-[var(--surface-hover)] active:scale-95
                                focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--foreground)] [&::-webkit-details-marker]:hidden"
                         aria-haspopup="menu">
                    <span class="flex h-8 w-8 items-center justify-center border-2 border-[var(--foreground)] bg-[var(--surface)] font-display text-xs"
                         >${page.person().initials()}</span>
                    <span class="hidden px-1 sm:inline">${page.person().displayName()}</span>
                </summary>
                <div role="menu"
                     class="absolute right-0 z-20 mt-2 w-max origin-top-right animate-pop-in min-w-56 max-w-[min(20rem,calc(100vw-2rem))] border-2 border-[var(--foreground)] bg-[var(--background)] p-2 shadow-[4px_4px_0_0_var(--foreground)]">
                    <div class="border-b border-[var(--border)] px-3 py-2">
                        <p class="break-words text-sm">${page.person().displayName()}</p>
                        <p class="break-all text-xs text-[var(--foreground)]/60" title="${page.person().email()}">${page.person().email() != null ? page.person().email() : "unavailable"}</p>
                    </div>
                    @template.tag.form(page = page, action = "/logout", classes = "pt-2", content = @`
                        <button type="submit" class="btn btn-secondary w-full" data-busy>Log out</button>
                    `)
                </div>
            </details>
            @endif
        </header>

        <div class="flex min-h-0 flex-1 flex-col">
            ${content}
        </div>
    </body>
</html>
