import type { EventConfig } from "./types";
import { genesisEvent } from "./genesis";

// To add a new event: create `./<event>.ts` exporting an EventConfig, then register it here.
// Nothing in components/dashboard or app/dashboard/[eventId] needs to change.
const registry: Record<string, EventConfig> = {
  [genesisEvent.id]: genesisEvent,
};

export function getEventConfig(eventId: string): EventConfig | undefined {
  console.log(registry[eventId])
  return registry[eventId];
}

export function listEvents(): EventConfig[] {
  return Object.values(registry);
}

export type { EventConfig, EventTheme, Milestone } from "./types";
