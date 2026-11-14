@import org.limelightcreatives.dashboard.web.Page

<%-- every error page (Spring Boot's /error): the big "Not found" for a 404, a card for anything else --%>
@param Page page
@param int status = 500

@template.layout(page = page, title = status == 404 ? "Not found" : "Something went wrong", content = @`
    @if(status == 404)
    <main class="relative min-h-0 flex-1 overflow-y-auto overflow-x-hidden">
        <div class="relative z-10 mx-auto flex min-h-full max-w-7xl flex-col items-center justify-center px-6 py-16 text-center">
            <div class="relative">
                <h1 class="max-w-5xl font-display text-[3.5rem] leading-[0.8] tracking-[-0.06em] md:text-[clamp(3.75rem,6rem,10rem)] md:tracking-[-0.08em]">
                    Not found
                </h1>
                <svg viewBox="0 0 71 112" fill="none" aria-hidden="true"
                     class="pointer-events-none absolute -right-28 -top-7 hidden h-28 w-[4.5rem] md:block">
                    <path d="M34.0842 5.00024C36.1659 32.3071 39.9734 45.0186 60.8435 53.8204C43.2615 68.9484 37.8959 81.9723 34.0842 111C31.2151 79.9997 26.929 66.1442 9.84351 53.8204C26.7067 45.9754 31.8813 35.3111 34.0842 6.0034"
                          stroke="#01995C" stroke-width="10" stroke-linecap="round"/>
                </svg>
                <svg viewBox="0 0 30 66" fill="none" aria-hidden="true"
                     class="pointer-events-none absolute -right-16 -top-16 hidden h-16 w-16 md:block">
                    <path d="M13.7161 18.3907C13.4394 19.5648 13.1153 20.6738 12.734 21.7208C11.1273 26.132 8.57063 29.3408 4.87167 31.7859C8.86388 36.2403 11.6044 40.4724 13.5456 45.2267C14.0249 46.4006 14.4503 47.5998 14.8349 48.8317C15.2256 47.5092 15.6595 46.2165 16.1515 44.9427C18.077 39.9564 20.7891 35.3763 24.6222 30.3476C20.5888 28.3807 17.5663 25.6161 15.2652 21.5672C14.7041 20.5797 14.1914 19.5215 13.7161 18.3907Z"
                          fill="#01995C" stroke="#01995C" stroke-width="6"/>
                </svg>
            </div>

            <p class="mt-14 max-w-xl text-xl">The page you’re looking for was moved or never made it to the final edit.</p>

            <div class="mt-8 flex flex-wrap justify-center gap-4 text-lg">
                <a class="btn btn-primary" href="/">BACK TO HOME &gt;</a>
            </div>
        </div>
    </main>
    @else
    <main class="flex min-h-0 flex-1 items-center justify-center overflow-y-auto px-6 py-16">
        <div class="card flex w-full max-w-md flex-col items-start gap-5">
            <h1 class="font-display text-3xl font-bold md:text-4xl">Something went wrong</h1>
            <p class="text-sm text-[var(--foreground)]/70">
                Please try again. If it keeps happening, reach out to us at inquiries@limelightcreatives.org.
            </p>
            <a class="btn btn-primary" href="/">Go back home</a>
        </div>
    </main>
    @endif
`)
