package org.limelightcreatives.dashboard.team;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamsTest extends DatabaseTest {

    @Autowired Teams teams;

    @Test
    void creatingMakesYouTheOwnerAndOnlyMember() {
        teams.create("u1", "  The Gaffers  ");
        Team team = teams.of("u1").orElseThrow();
        assertThat(team.name()).isEqualTo("The Gaffers");
        assertThat(team.owner()).isTrue();
        assertThat(team.memberCount()).isEqualTo(1);
        assertThat(team.maxSize()).isEqualTo(3);
        assertThat(team.joinCode()).hasSize(6).matches("[ABCDEFGHJKMNPQRSTUVWXYZ2-9]+");
    }

    @Test
    void namesAreNeededAndCutTo60Characters() {
        assertThatThrownBy(() -> teams.create("u1", "   ")).hasMessage("Team name required");
        teams.create("u1", "x".repeat(80));
        assertThat(teams.of("u1").orElseThrow().name()).hasSize(60);
    }

    @Test
    void joiningWithTheCodeInAnyCase() {
        teams.create("u1", "Gaffers");
        String code = teams.of("u1").orElseThrow().joinCode();
        teams.join("u2", " " + code.toLowerCase() + " ");
        Team joined = teams.of("u2").orElseThrow();
        assertThat(joined.name()).isEqualTo("Gaffers");
        assertThat(joined.owner()).isFalse();
        assertThat(joined.memberCount()).isEqualTo(2);
    }

    @Test
    void oneTeamEach() {
        teams.create("u1", "Gaffers");
        teams.create("u2", "Grips");
        assertThatThrownBy(() -> teams.create("u1", "Another")).hasMessage("You're already on a team");
        assertThatThrownBy(() -> teams.join("u1", teams.of("u2").orElseThrow().joinCode()))
                .hasMessage("You're already on a team");
    }

    @Test
    void wrongCodesAndFullTeams() {
        assertThatThrownBy(() -> teams.join("u1", "NOPE22")).hasMessage("No team with that code");
        assertThatThrownBy(() -> teams.join("u1", "")).hasMessage("Join code required");

        teams.create("u1", "Gaffers");
        String code = teams.of("u1").orElseThrow().joinCode();
        teams.join("u2", code);
        teams.join("u3", code);
        assertThatThrownBy(() -> teams.join("u4", code)).hasMessage("That team is full");
    }

    @Test
    void twoPeopleCantBothTakeTheLastPlace() throws Exception {
        teams.create("owner", "Gaffers");
        teams.join("second", teams.of("owner").orElseThrow().joinCode());
        String code = teams.of("owner").orElseThrow().joinCode();

        int racers = 8;
        var start = new CountDownLatch(1);
        var joined = new AtomicInteger();
        var errors = new ArrayList<String>();
        try (var pool = Executors.newFixedThreadPool(racers)) {
            for (int i = 0; i < racers; i++) {
                String user = "racer-" + i;
                pool.submit(() -> {
                    start.await();
                    try {
                        teams.join(user, code);
                        joined.incrementAndGet();
                    } catch (TeamException e) {
                        synchronized (errors) {
                            errors.add(e.getMessage());
                        }
                    }
                    return null;
                });
            }
            start.countDown();
        }
        assertThat(joined).hasValue(1);
        assertThat(errors).hasSize(racers - 1).containsOnly("That team is full");
        assertThat(teams.of("owner").orElseThrow().memberCount()).isEqualTo(3);
    }

    @Test
    void theLongestStandingMemberTakesOverWhenTheOwnerLeaves() throws Exception {
        teams.create("u1", "Gaffers");
        String code = teams.of("u1").orElseThrow().joinCode();
        teams.join("u2", code);
        Thread.sleep(5); // joined_at decides who's been there longest
        teams.join("u3", code);

        teams.leave("u1");
        assertThat(teams.of("u1")).isEmpty();
        assertThat(teams.of("u2").orElseThrow().owner()).isTrue();
        assertThat(teams.of("u3").orElseThrow().owner()).isFalse();
    }

    @Test
    void theLastToLeaveDeletesTheTeamWithItsWork() {
        teams.create("u1", "Gaffers");
        String id = teams.of("u1").orElseThrow().id();
        db.sql("INSERT INTO submissions (team_id, title, logline, video_url) VALUES (?::uuid, 't', 'l', 'https://v')")
                .param(id).update();

        teams.leave("u1");
        assertThat(db.sql("SELECT count(*) FROM teams").query(Integer.class).single()).isZero();
        assertThat(db.sql("SELECT count(*) FROM submissions").query(Integer.class).single()).isZero();
        assertThatThrownBy(() -> teams.leave("u1")).hasMessage("You're not on a team");
    }

    @Test
    void joinCodesAreUnique() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            teams.create("u" + i, "Team " + i);
            codes.add(teams.of("u" + i).orElseThrow().joinCode());
        }
        assertThat(codes).doesNotHaveDuplicates();
    }
}
