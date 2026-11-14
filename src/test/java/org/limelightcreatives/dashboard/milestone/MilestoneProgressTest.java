package org.limelightcreatives.dashboard.milestone;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.event.EventConfig;
import org.limelightcreatives.dashboard.event.Milestone;
import org.limelightcreatives.dashboard.team.Team;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.limelightcreatives.dashboard.milestone.MilestoneStatus.APPROVED;
import static org.limelightcreatives.dashboard.milestone.MilestoneStatus.PENDING;
import static org.limelightcreatives.dashboard.milestone.MilestoneStatus.SUBMITTED;

class MilestoneProgressTest {

    private static final EventConfig EVENT = new EventConfig("fest", "Fest", null, true, false,
            "https://forms.example/t/abc?ref=dash", null, 3, List.of(
            milestone("a", false), milestone("bonus", true), milestone("b", false), milestone("c", false)));
    private static final Team TEAM = new Team("11111111-2222-3333-4444-555555555555", "Sam & Co #1", "ABC234", true, 2);

    private static Milestone milestone(String id, boolean bonus) {
        return new Milestone(id, "Label " + id, null, null, null, bonus, null);
    }

    @Test
    void requiredOnesUnlockInOrderAndBonusOnesAreAlwaysOpen() {
        var progress = new MilestoneProgress(EVENT, TEAM, Map.of("a", SUBMITTED));
        assertThat(progress.entries()).extracting(e -> e.milestone().id()).containsExactly("a", "b", "c", "bonus");
        assertThat(progress.entries()).extracting(MilestoneProgress.Entry::locked).containsExactly(false, true, true, false);
        assertThat(progress.entries().get(1).previous().id()).isEqualTo("a");
        assertThat(progress.entries()).extracting(MilestoneProgress.Entry::number).containsExactly("01", "02", "03", "★");
        assertThat(progress.current().milestone().id()).isEqualTo("a");
        assertThat(progress.awaiting()).extracting(e -> e.milestone().id()).containsExactly("a");
        assertThat(progress.entries().get(0).canSubmit()).isFalse(); // waiting on review
        assertThat(progress.entries().get(3).canSubmit()).isTrue();
    }

    @Test
    void approvingOneUnlocksTheNext() {
        var progress = new MilestoneProgress(EVENT, TEAM, Map.of("a", APPROVED, "bonus", APPROVED));
        assertThat(progress.entries()).extracting(MilestoneProgress.Entry::locked).containsExactly(false, false, true, false);
        assertThat(progress.current().milestone().id()).isEqualTo("b");
        assertThat(progress.current().position()).isEqualTo(2);
        assertThat(progress.approvedCount()).isEqualTo(1);
        assertThat(progress.bonusApproved()).isEqualTo(1);
        assertThat(progress.percent()).isEqualTo(33);
        assertThat(progress.entries()).extracting(MilestoneProgress.Entry::badge).containsExactly("✓", "02", "03", "✓");
    }

    @Test
    void allDone() {
        var progress = new MilestoneProgress(EVENT, TEAM, Map.of("a", APPROVED, "b", APPROVED, "c", APPROVED));
        assertThat(progress.allDone()).isTrue();
        assertThat(progress.percent()).isEqualTo(100);
    }

    @Test
    void anApprovalAfterALockStillLooksLocked() {
        // e.g. staff undid an earlier approval: the later one keeps its row but shows as locked
        var progress = new MilestoneProgress(EVENT, TEAM, Map.of("b", APPROVED));
        var b = progress.entries().get(1);
        assertThat(b.locked()).isTrue();
        assertThat(b.approved()).isFalse();
        assertThat(b.tag()).isEqualTo("Locked");
        assertThat(b.status()).isEqualTo(APPROVED);
    }

    @Test
    void theSubmitLinkCarriesTheMilestoneAndTeamForTheWebhook() {
        var entry = new MilestoneProgress(EVENT, TEAM, Map.of()).entries().getFirst();
        assertThat(entry.status()).isEqualTo(PENDING);
        assertThat(entry.submitUrl()).isEqualTo("https://forms.example/t/abc?ref=dash&milestoneId=a&milestone=Label%20a"
                + "&teamId=11111111-2222-3333-4444-555555555555&team=Sam%20%26%20Co%20%231");
    }
}
