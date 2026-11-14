package org.limelightcreatives.dashboard.schedule;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.schedule.RunOfShow.Kind;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The run-of-show rules, on the real content/run-of-show.json (Genesis, 24–25 Oct 2026). */
class RunOfShowTest {

    static RunOfShow ros;
    static Schedule.Day sat;
    static Schedule.Day sun;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void load() throws Exception {
        try (var in = new ClassPathResource("content/run-of-show.json").getInputStream()) {
            ros = new RunOfShow(Schedule.fromJson(JsonMapper.builder().build().readValue(in, Map.class)));
        }
        sat = ros.data().days().get(0);
        sun = ros.data().days().get(1);
    }

    @Test
    void locationCodesBecomeNames() {
        assertThat(ros.place("LT → GA")).isEqualTo("La Trobe → Golden Age Cinema");
        assertThat(ros.place("GA-front")).isEqualTo("Front of Golden Age");
        assertThat(ros.place(null)).isEmpty();
    }

    @Test
    void someoneWhoIsAwayForPartOfTheDayGetsABackCard() {
        // Joshua is on site 7:30–9:30 and 11:30–19:00 on Saturday.
        var plan = ros.planFor(sat, "joshua");
        assertThat(plan).extracting(i -> i.kind).contains(Kind.RETURN, Kind.DONE).doesNotContain(Kind.ARRIVE);
        var back = plan.stream().filter(i -> i.kind == Kind.RETURN).findFirst().orElseThrow();
        assertThat(back.at()).isEqualTo(11 * 60 + 30);
        assertThat(plan.stream().filter(i -> i.kind == Kind.BLOCK))
                .allSatisfy(i -> assertThat(i.s < 9 * 60 + 30 || i.s >= 11 * 60 + 30).isTrue());
        assertThat(plan.getLast().kind).isEqualTo(Kind.DONE);
    }

    @Test
    void someoneNotOnSiteThatDay() {
        assertThat(ros.isAbsent(sat, "arda")).isTrue();
        assertThat(ros.isAbsent(sun, "arda")).isFalse();
        assertThat(ros.planFor(sat, "arda")).isEmpty();
    }

    @Test
    void neighbouringBlocksWithTheSameJobMerge() {
        var plan = ros.planFor(sat, "ontik");
        for (int i = 1; i < plan.size(); i++) {
            var a = plan.get(i - 1);
            var b = plan.get(i);
            if (a.kind == Kind.BLOCK && b.kind == Kind.BLOCK && a.e().equals(b.s)) {
                assertThat(ros.title(a) + a.a().location() + a.a().note())
                        .as("blocks %s and %s should have merged", a.s, b.s)
                        .isNotEqualTo(ros.title(b) + b.a().location() + b.a().note());
            }
        }
    }

    @Test
    void participantsSeeTheirOwnPlan() {
        var plan = ros.planFor(sat, RunOfShow.PARTICIPANT);
        assertThat(plan).isNotEmpty();
        assertThat(plan).extracting(i -> i.kind).containsOnly(Kind.MARKER, Kind.PBLOCK);
    }

    @Test
    void theBanner() {
        var before = ros.nowNext(sat, "ontik", 6 * 60).orElseThrow();
        assertThat(before.now()).startsWith("Your day starts 7:30am · in 1h 30m");
        var during = ros.nowNext(sat, "ontik", 10 * 60).orElseThrow();
        assertThat(during.now()).contains(" · until ");
        assertThat(during.nextLine()).isNotEmpty();
        var after = ros.nowNext(sat, "ontik", 23 * 60).orElseThrow();
        assertThat(after.now()).isEqualTo("You’re done for the day. Thanks!");
        assertThat(ros.nowNext(sat, "arda", 10 * 60)).isEmpty();
    }

    @Test
    void timeStates() {
        var now = new RunOfShow.Now("2026-10-24", 600);
        assertThat(RunOfShow.timeState(sat, now, 540, 600)).isEqualTo(RunOfShow.TimeState.PAST);
        assertThat(RunOfShow.timeState(sat, now, 600, 660)).isEqualTo(RunOfShow.TimeState.NOW);
        assertThat(RunOfShow.timeState(sat, now, 601, null)).isEqualTo(RunOfShow.TimeState.FUTURE);
        assertThat(RunOfShow.timeState(sun, now, 0, 10)).isEqualTo(RunOfShow.TimeState.FUTURE);
        assertThat(RunOfShow.timeState(sat, new RunOfShow.Now("2026-10-25", 0), 600, 660)).isEqualTo(RunOfShow.TimeState.PAST);
    }

    @Test
    void formatting() {
        assertThat(RunOfShow.fmt(0)).isEqualTo("12:00am");
        assertThat(RunOfShow.fmt(13 * 60 + 5)).isEqualTo("1:05pm");
        assertThat(RunOfShow.fmtDuration(90)).isEqualTo("1h 30m");
        assertThat(RunOfShow.fmtDuration(120)).isEqualTo("2h");
        assertThat(RunOfShow.fmtDuration(45)).isEqualTo("45m");
        assertThat(RunOfShow.fmtShortDate("2026-10-24")).isEqualTo("24 Oct");
    }
}
