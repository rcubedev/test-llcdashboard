"use client";

import Link, { useLinkStatus } from "next/link";

type ButtonProps = {
  href?: string;
  external?: boolean;
  children: React.ReactNode;
  variant?: "primary" | "secondary";
  className?: string;
  type?: "button" | "submit" | "reset";
  disabled?: boolean;
  /** Shows a spinner in place of the label and ignores clicks. */
  loading?: boolean;
  onClick?: () => void;
};

function ButtonSpinner() {
  return (
    <span
      aria-hidden
      className="absolute inset-0 flex items-center justify-center animate-fade-in"
    >
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-current/25 border-t-current" />
    </span>
  );
}

/** Label that swaps to a spinner while its parent button is busy. */
function Content({
  loading,
  children,
}: {
  loading: boolean;
  children: React.ReactNode;
}) {
  return (
    <>
      {/* invisible (not hidden) so the button keeps its width */}
      <span className={loading ? "invisible" : ""}>{children}</span>
      {loading ? <ButtonSpinner /> : null}
    </>
  );
}

/**
 * Must render inside <Link>: useLinkStatus reports whether *that* link's
 * navigation is in flight, so link-buttons get a spinner for free.
 */
function LinkContent({ children }: { children: React.ReactNode }) {
  const { pending } = useLinkStatus();
  return <Content loading={pending}>{children}</Content>;
}

export function Button({
  href,
  external = false,
  children,
  className = "",
  variant = "primary",
  type = "button",
  disabled = false,
  loading = false,
  onClick,
}: ButtonProps) {
  const baseStyles =
    "relative border-[2px] px-4 py-2 font-display font-bold transition-all duration-150 ease-out " +
    "translate-x-0 translate-y-0 shadow-[0_0_0_0_transparent] " +
    "hover:-translate-x-[2px] hover:-translate-y-[2px] " +
    "active:translate-x-0 active:translate-y-0 active:shadow-[2px_2px_0_0_var(--foreground)] " +
    "focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[var(--foreground)] " +
    "disabled:bg-transparent disabled:text-[var(--foreground)] disabled:pointer-events-none disabled:cursor-not-allowed";

  const variants = {
    primary:
      "border-[var(--foreground)] bg-[var(--ontik-accent)] text-[var(--background)] hover:bg-[var(--lime)] hover:text-[var(--foreground)] hover:shadow-[6px_6px_0_0_var(--foreground)]",

    secondary:
      "border-[var(--foreground)] bg-transparent text-[var(--foreground)] hover:bg-[var(--accent)] hover:text-[var(--foreground)] hover:shadow-[6px_6px_0_0_var(--foreground)]",
  };

  const styles = `${baseStyles} ${variants[variant]} ${
    loading ? "pointer-events-none cursor-progress" : ""
  } ${className}`;

  if (href && external) {
    return (
      <a href={href} target="_blank" rel="noopener noreferrer" className={styles}>
        {children}
      </a>
    );
  }

  if (href) {
    return (
      <Link href={href} className={styles}>
        <LinkContent>{children}</LinkContent>
      </Link>
    );
  }

  return (
    <button
      type={type}
      disabled={disabled}
      aria-busy={loading || undefined}
      onClick={(e) => {
        // Not `disabled`, so the button keeps its look while loading;
        // block clicks and keyboard-triggered submits ourselves.
        if (loading) {
          e.preventDefault();
          return;
        }
        onClick?.();
      }}
      className={styles}
    >
      <Content loading={loading}>{children}</Content>
    </button>
  );
}
