<%-- the event dashboard's tab icons (lucide: house, milestone, folder-kanban, toolbox, users, tv) --%>
@param String name

<svg xmlns="http://www.w3.org/2000/svg"
     viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"
     stroke-linejoin="round" class="h-4 w-4 shrink-0" aria-hidden="true">
    @if(name.equals("home"))
        <path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/>
        <path d="M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
    @elseif(name.equals("milestones"))
        <path d="M12 13v8"/>
        <path d="M12 3v3"/>
        <path d="M4 6a1 1 0 0 0-1 1v5a1 1 0 0 0 1 1h13a2 2 0 0 0 1.152-.365l3.424-2.317a1 1 0 0 0 0-1.635l-3.424-2.318A2 2 0 0 0 17 6z"/>
    @elseif(name.equals("project"))
        <path d="M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z"/>
        <path d="M8 10v4"/>
        <path d="M12 10v2"/>
        <path d="M16 10v6"/>
    @elseif(name.equals("equipment"))
        <path d="M16 12v4"/>
        <path d="M16 6a2 2 0 0 1 1.414.586l4 4A2 2 0 0 1 22 12v7a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 .586-1.414l4-4A2 2 0 0 1 8 6z"/>
        <path d="M16 6V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/>
        <path d="M2 14h20"/>
        <path d="M8 12v4"/>
    @elseif(name.equals("team"))
        <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M22 21v-2a4 4 0 0 0-3-3.87"/>
        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
    @elseif(name.equals("resources"))
        <path d="m17 2-5 5-5-5"/>
        <rect width="20" height="15" x="2" y="7" rx="2"/>
    @endif
</svg>
