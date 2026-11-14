package org.limelightcreatives.dashboard.team;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** TEAMS_LOCKED=true: teams stay as they are. */
@TestPropertySource(properties = "dashboard.teams-locked=true")
class LockedTeamsTest extends DatabaseTest {

    @Autowired Teams teams;

    @Test
    void nothingChanges() {
        assertThatThrownBy(() -> teams.create("u1", "Gaffers")).hasMessage("Teams are locked");
        assertThatThrownBy(() -> teams.join("u1", "ABC234")).hasMessage("Teams are locked");
        assertThatThrownBy(() -> teams.leave("u1")).hasMessage("Teams are locked");
    }
}
