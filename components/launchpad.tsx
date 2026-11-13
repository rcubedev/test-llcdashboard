"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";

export type AppLink = {
  name: string;
  href: string;
  icon: string;
  external?: boolean;
};

function LaunchpadSquiggle(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="100 0 350 34" fill="none" xmlns="http://www.w3.org/2000/svg" {...props}>
      <path
        d="M6.64062 10.1168C7.76451 10.6387 10.8035 13.5183 13.7606 16.0515C16.9258 18.7629 19.7173 19.7922 22.0044 21.7802C24.3245 23.7968 28.217 24.7723 31.9687 26.1604C36.9191 27.9919 50.0001 28.4689 56.7394 27.8246C61.5647 27.3632 67.4668 25.9574 71.9869 24.815C79.8101 22.838 84.4125 20.6713 94.1674 17.068C96.6853 16.1379 98.0482 15.2792 99.8888 14.7667C101.78 14.2401 110.704 11.6089 123.441 8.47735C130.998 6.61947 134.44 7.04346 140.997 9.45744C152.492 13.6898 155.713 17.0559 159.225 19.3845C163.327 22.1043 168.736 24.3346 175.243 25.0015C191.545 26.6724 194.562 23.6503 198.724 22.7937C202.571 22.002 205.444 20.2006 207.649 19.1151C210.152 17.8826 212.594 16.4722 215.067 15.1062C218.65 13.1268 221.406 12.331 223.697 11.0619C227.239 9.10028 234.43 6.78248 238.788 5.63336C244.999 3.99578 247.812 5.85325 250.524 6.33513C254.857 7.105 258.64 7.41326 263.329 9.23895C266.713 10.5564 269.189 12.7062 276.065 16.5087C280.305 18.8535 282.407 20.1582 285.892 22.0163C290.147 24.2848 300.846 23.9448 306.568 23.3278C314.313 22.4928 326.777 18.0487 329.449 17.0426C334.208 15.2508 340.22 13.5932 346.927 10.8164C351.484 8.92985 355.332 8.53496 358.671 8.16619C365.25 7.43957 371.168 10.5178 376.224 11.037C379.83 11.4073 382.451 14.486 385.378 15.9938C387.638 17.1583 391.855 19.0625 395.177 20.8414C397.819 22.2563 400.162 23.122 402.787 23.9854C406.913 25.3426 411.163 24.8781 414.436 25.0688C418.555 25.3088 421.355 23.1201 424.93 21.3535C428.123 19.7756 439.718 17.439 446.709 15.4661C449.722 14.6157 451.757 13.0101 454.516 11.9096C457.343 10.7819 459.469 9.45617 463.501 7.48764C466.307 6.11764 479.346 5.75313 486.724 6.66429C489.493 7.00622 491.275 10.072 494.012 11.5794C497.605 13.5582 501.864 16.8474 504.331 18.4597C506.577 19.9279 508.856 20.9423 511.862 22.2529C519.817 25.7222 527.865 22.6724 534.58 20.8185C536.711 20.2034 538.898 19.4083 540.295 18.8074C541.693 18.2065 542.235 17.8238 542.778 16.8722"
        stroke="currentColor"
        strokeWidth="8"
        strokeLinecap="square"
        strokeLinejoin="round"
        // Normalised length so the draw-in animation works for any path.
        // The gap (2) is longer than the path (1) so no stray cap shows at the start.
        pathLength={1}
        className="animate-draw [stroke-dasharray:1_2]"
      />
    </svg>
  );
}

type TileState = "idle" | "launching" | "dimmed";

function AppTile({
  app,
  index,
  state,
  onLaunch,
}: {
  app: AppLink;
  index: number;
  state: TileState;
  onLaunch: (name: string) => void;
}) {
  const launching = state === "launching";
  const dimmed = state === "dimmed";

  const content = (
    <div
      className={`group flex w-24 flex-col items-center gap-3 text-center transition-[opacity,filter] duration-300 sm:w-28 ${
        dimmed ? "opacity-40 grayscale" : ""
      } ${launching || dimmed ? "pointer-events-none" : ""}`}
    >
      <div className="relative aspect-square w-full">
        <div
          className={`absolute inset-0 bg-black transition-transform duration-150 ${
            launching
              ? "translate-x-0 translate-y-0"
              : "translate-x-0 translate-y-0 group-hover:translate-x-1 group-hover:translate-y-1 group-active:translate-x-0 group-active:translate-y-0"
          }`}
        />
        <div
          className={`absolute inset-0 flex items-center justify-center border-2 border-black bg-black ${
            launching
              ? "animate-launch"
              : "transition-transform duration-150 group-hover:-translate-x-[2px] group-hover:-translate-y-[2px] group-active:translate-x-0 group-active:translate-y-0 group-active:scale-95"
          }`}
        >
          <Image src={app.icon} alt="" fill className="object-cover" />

          {launching ? (
            <span
              aria-hidden
              className="absolute inset-0 z-10 flex items-center justify-center bg-black/50 animate-fade-in"
            >
              <span className="h-8 w-8 animate-spin rounded-full border-[3px] border-white/30 border-t-white" />
            </span>
          ) : null}
        </div>
      </div>
      <span className="max-w-xl text-xl font-body font-bold">{app.name}</span>
      {launching ? <span className="sr-only" role="status">Opening {app.name}…</span> : null}
    </div>
  );

  const entrance = {
    className: "block animate-tile-in rounded-none focus-visible:outline-2 focus-visible:outline-offset-8 focus-visible:outline-[var(--foreground)]",
    style: { animationDelay: `${150 + index * 60}ms` },
  };

  return app.external ? (
    <a
      href={app.href}
      target="_blank"
      rel="noreferrer noopener"
      {...entrance}
    >
      {content}
    </a>
  ) : (
    <Link
      href={app.href}
      aria-busy={launching || undefined}
      onClick={(e) => {
        // Let ctrl/cmd/middle-click open in a new tab without a fake "loading" state.
        if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey || e.button !== 0) return;
        onLaunch(app.name);
      }}
      {...entrance}
    >
      {content}
    </Link>
  );
}

interface LaunchpadProps {
  apps: AppLink[];
  header?: Record<string, string>; // Accept serialized header key-value pairs in client components
}

export default function Launchpad({ apps, header }: LaunchpadProps) {
  // Name of the app the person just clicked, while we wait for the route to load.
  const [launching, setLaunching] = useState<string | null>(null);
  const pathname = usePathname();

  // Clear on any route change so coming back (back button / cached page) never
  // shows a stale spinner.
  useEffect(() => {
    setLaunching(null);
  }, [pathname]);

  return (
    <main className="flex h-screen flex-col overflow-hidden">
      
      <div className="flex flex-1 flex-col items-center justify-center px-6 py-12 text-center overflow-y-auto">
        <h1 className="relative mt-4 inline-block animate-page-in font-display text-[clamp(2.75rem,6vw,4.5rem)] font-bold leading-[0.9] tracking-[-0.06em]">
          Welcome!
          <LaunchpadSquiggle className="pointer-events-none absolute left-0 top-full mt-1 h-auto w-full text-[var(--ontik-accent)]" />
        </h1>

        <div className="mt-16 flex flex-wrap justify-center gap-x-8 gap-y-12">
          {apps.map((app, i) => (
            <AppTile
              key={app.name}
              app={app}
              index={i}
              state={
                launching === null
                  ? "idle"
                  : launching === app.name
                    ? "launching"
                    : "dimmed"
              }
              onLaunch={setLaunching}
            />
          ))}
        </div>

        <p className="mt-12 text-center text-sm"></p>
      </div>
    </main>
  );
}