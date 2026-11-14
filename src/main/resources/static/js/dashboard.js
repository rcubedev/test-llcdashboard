// Small extras on top of pages that already work without JavaScript. Each one is switched on by a data-
// attribute in the templates:
//
//   data-busy="Saving…"        on a submit button: disabled, with that label, while its form submits
//   data-busy                  (no label) a spinner in place of its label instead, keeping its look
//   data-needs="name"          on a submit button: disabled until the form's field of that name has something in it
//   data-confirm="Delete …?"   on a submit button: asks first
//   data-unsaved               on a form: asks before leaving the page with changes that weren't saved
//   data-filter="#list"        on a search box: hides the list's items ([data-filter-text]) that don't match, as you type
//   data-copy="text"           on a button (hidden until copying works): copies the text
//   data-refresh-on-focus      on a page's content: fetched again when you come back to the tab (e.g. after
//                              submitting a form in Fillout), replacing just that element
//     data-live-interval="30"    and every 30 seconds while the tab is showing
//   data-unlocks-at="ms"       equipment booking: counts down in its [data-countdown], then refreshes the page a
//     data-jitter-seconds="5"    random moment (0 to 5 seconds) after opening, so not everyone arrives at once
//   data-fold                  on a button in a <details>: closes it
//   data-menu                  on the account <details>: closes when you click somewhere else
//   data-launch                on a launchpad tile: spins while its app opens, and dims the others
//
// and a .btn link spins while the page it goes to opens.
(() => {
  "use strict";

  // ---------- forms ----------

  document.addEventListener("submit", (event) => {
    const form = event.target;
    const button = event.submitter;
    if (button?.dataset.confirm && !window.confirm(button.dataset.confirm)) {
      event.preventDefault();
      return;
    }
    if (button?.ariaBusy) {
      event.preventDefault();
      return;
    }
    form.dataset.submitting = "true";
    if (button?.dataset.busy) {
      // after the browser has read the form, so the button's own name and value still go with it
      setTimeout(() => {
        button.disabled = true;
        button.textContent = button.dataset.busy;
      });
    } else if (button?.hasAttribute("data-busy")) {
      spin(button);
    }
  });

  // like the old Button's loading: the label stays (invisible) so the button keeps its width
  function spin(button) {
    const label = document.createElement("span");
    label.className = "invisible";
    label.append(...button.childNodes);
    const spinner = document.createElement("span");
    spinner.ariaHidden = "true";
    spinner.className = "absolute inset-0 flex animate-fade-in items-center justify-center";
    spinner.innerHTML = '<span class="h-4 w-4 animate-spin rounded-full border-2 border-current/25 border-t-current"></span>';
    button.append(label, spinner);
    button.ariaBusy = "true";
  }

  // only a plain click: ctrl/cmd/middle-click opens a new tab and leaves this page as it is
  function opensHere(event, link) {
    return !event.defaultPrevented && event.button === 0 && !event.metaKey && !event.ctrlKey && !event.shiftKey &&
      !event.altKey && !link.target && !link.hasAttribute("download");
  }

  document.addEventListener("click", (event) => {
    const link = event.target.closest("a[href]");
    if (!link || !opensHere(event, link)) return;
    if (link.hasAttribute("data-launch")) {
      for (const tile of document.querySelectorAll("[data-launch]")) {
        tile.dataset.launch = tile === link ? "launching" : "dimmed";
      }
      link.ariaBusy = "true";
    } else if (link.matches(".btn") && !link.ariaBusy) {
      spin(link);
    }
  });

  // a page brought back by the back button shouldn't keep its buttons disabled
  window.addEventListener("pageshow", (event) => {
    if (event.persisted) window.location.reload();
  });

  function changed(form, initial) {
    const now = new FormData(form);
    for (const [key, value] of now) if (initial.get(key) !== value) return true;
    for (const [key] of initial) if (!now.has(key)) return true;
    return false;
  }

  // ---------- search boxes ----------

  function filter(input) {
    const list = document.querySelector(input.dataset.filter);
    if (!list) return;
    const q = input.value.trim().toLowerCase();
    for (const item of list.querySelectorAll("[data-filter-text]")) {
      item.hidden = q !== "" && !item.dataset.filterText.toLowerCase().includes(q);
    }
    // keep ?q= in the address, so a refresh shows the same thing
    const url = new URL(window.location.href);
    if (q) url.searchParams.set("q", input.value.trim());
    else url.searchParams.delete("q");
    history.replaceState(null, "", url);
  }

  document.addEventListener("input", (event) => {
    const target = event.target;
    if (target.matches("[data-filter]")) filter(target);
    const form = target.form;
    if (form) for (const button of form.querySelectorAll("[data-needs]")) needs(button);
  });

  function needs(button) {
    const field = button.form?.elements[button.dataset.needs];
    if (field && !field.disabled) button.disabled = field.value.trim() === "";
  }

  // ---------- refreshing in the background ----------

  let refreshing = false;

  async function refresh() {
    const live = document.querySelector("[data-refresh-on-focus]");
    if (!live || refreshing) return;
    // never throw away what someone is typing or has changed
    const active = document.activeElement;
    if (active && live.contains(active) && active.matches("input:not([type=hidden]), textarea, select") && !active.matches("[data-filter]")) return;
    if ([...live.querySelectorAll("form[data-unsaved]")].some((form) => form.unsaved?.())) return;

    refreshing = true;
    try {
      const response = await fetch(window.location.href, { headers: { Accept: "text/html" }, cache: "no-store" });
      // a redirect means signing in again: leave the page alone, the next click goes there
      if (!response.ok || response.redirected) return;
      const page = new DOMParser().parseFromString(await response.text(), "text/html");
      const fresh = page.querySelector("[data-refresh-on-focus]");
      if (!fresh) return;

      const search = live.querySelector("[data-filter]");
      const typed = search ? search.value : null;
      const hadFocus = search && document.activeElement === search;
      const open = [...live.querySelectorAll("details")].map((d) => d.open);
      live.innerHTML = fresh.innerHTML;
      live.dataset.refreshed = "";
      // keep the search text and whatever was unfolded
      const newSearch = live.querySelector("[data-filter]");
      if (newSearch && typed !== null) {
        newSearch.value = typed;
        filter(newSearch);
        if (hadFocus) newSearch.focus();
      }
      live.querySelectorAll("details").forEach((d, i) => {
        if (open[i]) d.open = true;
      });
      setUp(live);
    } catch {
      // offline, or the server is busy: keep what's on screen, the next time tries again
    } finally {
      refreshing = false;
    }
  }

  const live = document.querySelector("[data-refresh-on-focus]");
  if (live) {
    let away = false;
    const back = () => {
      if (away) {
        away = false;
        refresh();
      }
    };
    document.addEventListener("visibilitychange", () => (document.hidden ? (away = true) : back()));
    window.addEventListener("blur", () => (away = true));
    window.addEventListener("focus", back);
    const seconds = Number(live.dataset.liveInterval);
    if (seconds > 0) setInterval(() => document.hidden || refresh(), seconds * 1000);
  }

  // ---------- whatever a page (or a refreshed part of one) has ----------

  function setUp(root) {
    for (const button of root.querySelectorAll("[data-needs]")) needs(button);

    for (const form of root.querySelectorAll("form[data-unsaved]")) {
      const initial = new FormData(form);
      form.unsaved = () => !form.dataset.submitting && changed(form, initial);
    }

    for (const button of root.querySelectorAll("[data-copy]")) {
      if (!navigator.clipboard) continue;
      button.hidden = false;
      button.addEventListener("click", async () => {
        const label = button.textContent;
        try {
          await navigator.clipboard.writeText(button.dataset.copy);
          button.textContent = "Copied";
        } catch {
          button.textContent = "Couldn’t copy";
        }
        setTimeout(() => (button.textContent = label), 1500);
      });
    }

    for (const banner of root.querySelectorAll("[data-unlocks-at]")) {
      const unlocksAt = Number(banner.dataset.unlocksAt) + Math.random() * Number(banner.dataset.jitterSeconds || 0) * 1000;
      const countdown = banner.querySelector("[data-countdown]");
      const tick = () => {
        const left = unlocksAt - Date.now();
        if (left <= 0) {
          clearInterval(timer);
          window.location.reload();
        } else if (countdown) {
          countdown.textContent = formatCountdown(left);
        }
      };
      const timer = setInterval(tick, 1000);
      tick();
    }
  }

  // the same as EquipmentView.countdown
  function formatCountdown(ms) {
    const s = Math.max(0, Math.ceil(ms / 1000));
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    if (h >= 24) return `${Math.floor(h / 24)}d ${h % 24}h`;
    if (h > 0) return `${h}h ${m}m`;
    return `${m}:${String(s % 60).padStart(2, "0")}`;
  }

  window.addEventListener("beforeunload", (event) => {
    if ([...document.querySelectorAll("form[data-unsaved]")].some((form) => form.unsaved?.())) {
      event.preventDefault();
      event.returnValue = "";
    }
  });

  document.addEventListener("click", (event) => {
    const fold = event.target.closest("[data-fold]");
    if (fold) fold.closest("details").open = false;
    for (const menu of document.querySelectorAll("details[data-menu][open]")) {
      if (!menu.contains(event.target)) menu.open = false;
    }
  });

  setUp(document);
  for (const input of document.querySelectorAll("[data-filter]")) if (input.value) filter(input);
})();
