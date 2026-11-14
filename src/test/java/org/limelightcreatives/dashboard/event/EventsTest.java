package org.limelightcreatives.dashboard.event;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class EventsTest extends DatabaseTest {

    @Autowired Events events;

    private EventConfig genesis() {
        return events.list().stream().filter(e -> e.id().equals("genesis")).findFirst().orElseThrow();
    }

    @Test
    void genesisIsOpen() {
        // (the tests add two events of their own, see application-test.yml)
        assertThat(events.list()).extracting(EventConfig::id).contains("genesis", "film-fest", "closed");
        var genesis = genesis();
        assertThat(genesis.active()).isTrue();
        assertThat(genesis.locked()).isFalse();
        assertThat(events.active("genesis")).contains(genesis);
        assertThat(genesis.theme().style()).isEqualTo("--accent:#01995C;--accent-secondary:#81D7D4");
    }

    @Test
    void milestonesUnlockInOrderWithBonusOnesLast() {
        var genesis = genesis();
        assertThat(genesis.required()).extracting(Milestone::id)
                .containsExactly("production-ready", "wrapped-filming", "first-cut", "polish", "final-cut");
        assertThat(genesis.bonus()).extracting(Milestone::id).containsExactly("team-player");
        assertThat(genesis.ordered()).hasSize(6).last().extracting(Milestone::id).isEqualTo("team-player");
        assertThat(events.prizes()).containsEntry("production-ready", "Fidget Spinners");
    }
}
