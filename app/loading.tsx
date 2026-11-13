export default function Loading() {
  return (
    <div className="flex h-screen items-center justify-center">
      {/* Fades in after a short delay so fast navigations don't flash a spinner.
          Fade and spin sit on separate elements since both set `animation`. */}
      <div role="status" aria-label="Loading" className="animate-fade-in-delayed">
        <span
          aria-hidden
          className="block h-10 w-10 animate-spin rounded-full border-2 border-[var(--foreground)]/20 border-t-[var(--accent)]"
        />
      </div>
    </div>
  );
}
