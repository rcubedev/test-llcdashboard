<%-- a page whose data couldn't be loaded (a Google Sheet that didn't answer) --%>
@param String heading
@param String title
@param String message

<h1 class="font-display text-4xl font-bold md:text-5xl">${heading}</h1>
<div class="mx-auto mt-16 w-full max-w-md">
    <div class="card">
        <div class="flex flex-col items-center text-center">
            <p class="font-display text-2xl">${title}</p>
            <p class="mt-2 text-sm text-red-600">${message}</p>
            <a class="btn btn-secondary mt-6" href="">Try again</a>
        </div>
    </div>
</div>
