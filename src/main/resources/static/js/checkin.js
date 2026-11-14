// The check-in page's camera (staff/checkin.jte). Each QR code is posted to /staff/checkin/scan,
// which checks the person in or out in the sheet; the answer and the last 10 scans show below the camera.
// Typing an ID into the box does the same here (without this script, the box posts the form instead).
// The scanner is qr-scanner, served from its WebJar.
import QrScanner from "/webjars/qr-scanner/qr-scanner.min.js";

const root = document.querySelector("[data-checkin]");
const video = root.querySelector("[data-camera]");
const finder = root.querySelector("[data-finder]");
const pending = root.querySelector("[data-pending]");
const resultBox = root.querySelector("[data-result]");
const historyWrap = root.querySelector("[data-history-wrap]");
const historyList = root.querySelector("[data-history]");
const manual = root.querySelector("[data-manual]");
const csrf = {
  header: document.querySelector("meta[name=csrf-header]")?.content,
  token: document.querySelector("meta[name=csrf-token]")?.content,
};

let busy = false;
let last = { code: "", at: 0 };
const history = [];

const label = (r) =>
  r.status === "ok" ? (r.action === "CHECK IN" ? "Checked in" : "Checked out") : r.status === "unknown" ? "Unknown ID" : "Error";
const title = (r) => (r.status === "ok" ? r.name : r.status === "unknown" ? r.id : r.message);

function setPending(on) {
  pending.classList.toggle("opacity-100", on);
  pending.classList.toggle("opacity-0", !on);
  finder.classList.toggle("border-[var(--accent)]", on);
  finder.classList.toggle("border-white", !on);
}

function show(result) {
  const card = root.querySelector("[data-result-template]").content.firstElementChild.cloneNode(true);
  const ok = result.status === "ok";
  if (ok && result.action === "CHECK IN") card.classList.add("bg-[var(--accent)]");
  if (!ok) card.classList.add("border-dashed");
  card.querySelector("[data-label]").textContent = label(result);
  if (ok) {
    card.querySelector("[data-name]").textContent = result.name;
    card.querySelector("[data-meta]").textContent = `ID: ${result.id} · ${result.at}`;
    card.querySelector("[data-problem]").remove();
  } else {
    card.querySelector("[data-name]").remove();
    card.querySelector("[data-meta]").remove();
    card.querySelector("[data-problem]").textContent =
      result.status === "unknown" ? `No participant with ID "${result.id}".` : result.message;
  }
  resultBox.replaceChildren(card);
}

function remember(result) {
  history.unshift(result);
  history.length = Math.min(history.length, 10);
  const template = root.querySelector("[data-history-template]").content.firstElementChild;
  historyList.replaceChildren(
    ...history.map((h) => {
      const item = template.cloneNode(true);
      item.querySelector("[data-title]").textContent = title(h);
      item.querySelector("[data-label]").textContent = label(h);
      return item;
    }),
  );
  historyWrap.hidden = false;
}

async function submit(raw) {
  const code = raw.trim();
  const now = Date.now();
  if (!code || busy) return;
  if (last.code === code && now - last.at < 3000) return; // the same code is still in front of the camera
  last = { code, at: now };

  busy = true;
  setPending(true);
  // never let a stalled request lock the scanner
  const timeout = new AbortController();
  const timer = setTimeout(() => timeout.abort(), 8000);
  try {
    const headers = { "Content-Type": "application/json", Accept: "application/json" };
    if (csrf.header && csrf.token) headers[csrf.header] = csrf.token;
    const response = await fetch("/staff/checkin/scan", {
      method: "POST",
      headers,
      body: JSON.stringify({ code }),
      signal: timeout.signal,
    });
    if (!response.ok) throw new Error(String(response.status));
    const result = await response.json();
    show(result);
    remember(result);
  } catch {
    show({ status: "error", message: "Network error. Try again." });
    last = { code: "", at: 0 }; // so a failed scan can be retried straight away
  } finally {
    clearTimeout(timer);
    busy = false;
    setPending(false);
  }
}

manual.addEventListener("submit", (event) => {
  event.preventDefault();
  const input = manual.elements.code;
  submit(input.value);
  input.value = "";
});

const scanner = new QrScanner(video, (result) => submit(result.data), {
  returnDetailedScanResult: true,
  preferredCamera: "environment",
  maxScansPerSecond: 1,
});
scanner.start().catch(() => show({ status: "error", message: "Camera unavailable. Check permissions." }));
