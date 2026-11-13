"use client";

import { useState, useTransition } from "react";
import { Button } from "@/components/Button";
import Image from "next/image";
import Link from "next/link";

type TopbarUser = {
  name: string;
  email?: string;
  avatarUrl?: string;
};

type TopbarProps = {
  label?: string;
  user: TopbarUser;
  onLogout: () => void | Promise<void>; // changed: allow the async server action
};

export function Topbar({ label = "Dashboard", user, onLogout }: TopbarProps) {
  const [open, setOpen] = useState(false);
  const [isLoggingOut, startLogout] = useTransition();

  const handleLogout = () => {
    startLogout(async () => {
      await onLogout();
    });
  };

  const initials = user.name
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  return (
    <header className="flex items-center justify-between border-b border-[var(--border)] bg-[var(--background)] px-6 py-4 md:px-16">
      <Link href="/">
        <Image
          src="/branding/logo.svg"
          alt="Limelight Creatives"
          width={52}
          height={52}
          priority
        />
      </Link>

      <div className="relative">
        <button
          type="button"
          onClick={() => setOpen((prev) => !prev)}
          aria-expanded={open}
          aria-haspopup="menu"
          className="flex items-center gap-2 rounded-none px-1 py-1 font-body text-sm transition-[background-color,transform] duration-150 hover:bg-[var(--surface-hover)] active:scale-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--foreground)]"
        >
          <span className="flex h-8 w-8 items-center justify-center border-[2px] border-[var(--foreground)] bg-[var(--surface)] font-display text-xs">
            {initials || "?"}
          </span>
          <span className="hidden px-1 sm:inline">{user.name}</span>
        </button>

        {open ? (
          <>
            <button
              type="button"
              aria-label="Close menu"
              className="fixed inset-0 z-10 cursor-default"
              onClick={() => setOpen(false)}
            />
            <div
              role="menu"
              className="absolute right-0 z-20 mt-2 origin-top-right animate-pop-in w-max min-w-56 max-w-[min(20rem,calc(100vw-2rem))] border-[2px] border-[var(--foreground)] bg-[var(--background)] p-2 shadow-[4px_4px_0_0_var(--foreground)]"
            >
              <div className="border-b border-[var(--border)] px-3 py-2">
                <p className="break-words font-body text-sm">{user.name}</p>
                {user.email ? (
                  <p
                    title={user.email}
                    className="break-all font-body text-xs text-[var(--foreground)]/60"
                  >
                    {user.email}
                  </p>
                ) : null}
              </div>
              <div className="pt-2">
                <Button
                  variant="secondary"
                  className="w-full"
                  onClick={handleLogout}
                  loading={isLoggingOut}
                >
                  Log out
                </Button>
              </div>
            </div>
          </>
        ) : null}
      </div>

      {isLoggingOut ? (
        <div
          role="status"
          aria-live="polite"
          aria-label="Logging out"
          className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-4 bg-[color-mix(in_srgb,var(--background)_60%,transparent)] backdrop-blur-md animate-fade-in"
        >
          <span
            aria-hidden
            className="h-10 w-10 animate-spin rounded-full border-2 border-[var(--foreground)]/20 border-t-[var(--accent)]"
          />
        </div>
      ) : null}
    </header>
  );
}