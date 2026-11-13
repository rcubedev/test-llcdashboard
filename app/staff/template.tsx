// Unlike layout.tsx, a template remounts on every navigation, so each staff
// page (schedule, check-in, …) fades/slides in when you open it from the launchpad.
export default function StaffTemplate({ children }: { children: React.ReactNode }) {
  return <div className="flex min-h-0 flex-1 flex-col animate-page-in">{children}</div>;
}
