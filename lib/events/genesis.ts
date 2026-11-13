import type { EventConfig } from "./types";

export const genesisEvent: EventConfig = {
  id: "genesis",
  name: "Limelight GENESIS",
  tagline: "Pilot event",
  locked: false,
  formUrl: "https://limelightcreatives.fillout.com/t/qyMeFhFzZuus",
  theme: {
    accent: "#01995C",
    accentSecondary: "#81D7D4", // --lc-mint
  },
  teamSize: 3,
  milestones: [
    {
      id: "production-ready",
      label: "Production Ready",
      targetTime: "Day 1, after workshop 1",
      description:
        "The team has completed pre-production and is ready to start filming. They have a complete storyboard/script, assigned roles, and have organised their locations, props and actors.",
      evidence: "Completed storyboard",
      status: "pending",
      prize: "Fidget Spinners"
    },
    {
      id: "wrapped-filming",
      label: "Wrapped up with filming",
      targetTime: "Day 1, when they return from filming",
      description:
      "The team brings their phone/camera back and shows the footage they've captured. You don't need to watch everything, just verify they've shot the material needed to construct their film.",
      evidence: "Footage on their device / camera roll",
      status: "pending",
      prize: "Story Factory 'Idea Mints' + Pencil & TMBL Piggy Banks"
    },
    {
      id: "first-cut",
      label: "First Cut",
      targetTime: "Day 2, editing period",
      description:
      "A complete rough cut of the film. The whole film needs to exist from beginning to end, even if it's ugly. It should have the basic footage in the right order and tell the complete story.",
      evidence: "Playable rough cut",
      status: "pending",
      prize: "TMBL Tote Bag + NIDA Hats"
    },
    {
      id: "polish",
      label: "Polish",
      description:
      "Quality check. The team has applied a consistent visual look across the entire project.",
      evidence: "A playable export of the film",
      status: "pending",
      prize: "TMBL Notebook (has a calc, old money)"
    },
    {
      id: "final-cut",
      label: "Final Cut (feedback)",
      targetTime: "Before submissions close",
      description:
      "The film is completely finished, and the team has done one deliberate final review: it plays correctly from beginning to end, the audio works, there are no accidental clips or black frames, titles/credits are correct, it meets the required length and format, and everyone on the team is happy with it.",
      evidence: "Final exported film",
      status: "pending",
      prize: "Limelight Creatives Custom T-Shirt"
    },
    {
      id: "team-player",
      label: "Team Player",
      bonus: true,
      targetTime: "Any time throughout the weekend",
      description:
      "Awarded for helping another team progress with their film. This could be helping with filming, acting, sound, editing, troubleshooting, or sharing a useful filmmaking skill.",
      evidence:
      "The other team confirms they received meaningful help. Video recording will suffice.",
      status: "pending",
      prize: "Your choice of: 5 SAE eco bags, 5 SAE water bottles or 5 SAE USB flash drives. !!! Only the first 15 people gets a prize !!!"
    },
  ],
};