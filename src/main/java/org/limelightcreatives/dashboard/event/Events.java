package org.limelightcreatives.dashboard.event;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Every event in content/events.yml, keyed by id. (Keyed rather than a list so one setting can be changed
 * elsewhere, e.g. {@code events.genesis.active=true} as an environment variable EVENTS_GENESIS_ACTIVE=true.)
 */
@Component
public class Events {

    private final List<EventConfig> list;

    public Events(Environment environment) {
        Map<String, EventConfig> byId = Binder.get(environment)
                .bind("events", Bindable.mapOf(String.class, EventConfig.class))
                .orElse(Map.of());
        this.list = byId.entrySet().stream().map(e -> e.getValue().withId(e.getKey())).toList();
    }

    public List<EventConfig> list() {
        return list;
    }

    /** An active event by id: the ones with a participant dashboard. */
    public Optional<EventConfig> active(String id) {
        return list.stream().filter(e -> e.active() && e.id().equals(id)).findFirst();
    }

    /** Prizes by milestone id, across every event (for the staff review page). */
    public Map<String, String> prizes() {
        return list.stream().flatMap(e -> e.milestones().stream())
                .filter(m -> m.prize() != null && !m.prize().isBlank())
                .collect(Collectors.toMap(Milestone::id, Milestone::prize, (a, b) -> a));
    }
}
