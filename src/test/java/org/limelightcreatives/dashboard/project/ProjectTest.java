package org.limelightcreatives.dashboard.project;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.limelightcreatives.dashboard.team.Teams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@AutoConfigureMockMvc
class ProjectTest extends DatabaseTest {

    private static final String PAGE = "/participant/film-fest/project";

    @Autowired MockMvc mvc;
    @Autowired Teams teams;
    @Autowired Projects projects;

    @Test
    void oneProjectPerTeamThatAnyMemberCanUpdate() throws Exception {
        teams.create("sam", "Gaffers");
        teams.join("alex", teams.of("sam").orElseThrow().joinCode());
        String teamId = teams.of("sam").orElseThrow().id();

        projects.save("sam", "  Night Shift ", "A film about a night.", "https://drive.google.com/file/d/1");
        projects.save("alex", "Night Shift 2", "Longer.", "http://example.com/film");

        assertThat(projects.of(teamId)).contains(new Project("Night Shift 2", "Longer.", "http://example.com/film"));
        assertThat(db.sql("SELECT submitted_by FROM submissions").query(String.class).single()).isEqualTo("alex");
    }

    @Test
    void needsATitleALoglineAndAWebAddress() {
        teams.create("sam", "Gaffers");
        assertThatThrownBy(() -> projects.save("sam", "", "l", "https://x.com")).hasMessageContaining("required");
        assertThatThrownBy(() -> projects.save("sam", "t", " ", "https://x.com")).hasMessageContaining("required");
        assertThatThrownBy(() -> projects.save("sam", "t", "l", "javascript:alert(1)")).hasMessageContaining("required");
        assertThatThrownBy(() -> projects.save("sam", "t", "l", "not a link")).hasMessageContaining("required");
    }

    @Test
    void longTitlesAndLoglinesAreCut() {
        teams.create("sam", "Gaffers");
        projects.save("sam", "t".repeat(200), "l".repeat(900), "https://x.com");
        var saved = projects.of(teams.of("sam").orElseThrow().id()).orElseThrow();
        assertThat(saved.title()).hasSize(120);
        assertThat(saved.logline()).hasSize(500);
    }

    @Test
    void withoutATeamThereIsNothingToSave() throws Exception {
        assertThatThrownBy(() -> projects.save("loner", "t", "l", "https://x.com")).hasMessage("Join a team first");
        assertThat(new Visitor(mvc).as(SignedIn.participant("loner")).get(PAGE).getContentAsString())
                .contains("Join a team first", "Your team submits one project together", "href=\"/participant/film-fest/team\"");
    }

    @Test
    void theFormThenTheSavedProjectThenEditing() throws Exception {
        teams.create("sam", "Gaffers");
        var sam = new Visitor(mvc).as(SignedIn.participant("sam"));

        assertThat(sam.get(PAGE).getContentAsString()).contains("Save project", "Open Google Drive").doesNotContain("Cancel");

        var refused = sam.follow(sam.post(PAGE, "title", "Night Shift", "logline", "A night.", "videoUrl", "nope"));
        assertThat(refused.getContentAsString())
                .contains("Title, logline and a valid video URL are required", "value=\"Night Shift\"", "A night.");

        sam.post(PAGE, "title", "Night Shift", "logline", "A night.", "videoUrl", "https://drive.example/film");
        String saved = sam.get(PAGE).getContentAsString();
        assertThat(saved).contains("Saved", "Night Shift", "Watch submission", "href=\"https://drive.example/film\"", "href=\"?edit=true\"")
                .doesNotContain("Save project");

        assertThat(sam.get(PAGE + "?edit=true").getContentAsString()).contains("Save project", "Cancel", "value=\"Night Shift\"");
    }

    @Test
    void linksAreCheckedAsTheOldAppDid() {
        assertThat(Projects.isWebAddress("https://drive.google.com/file/d/1AbC/view?usp=sharing")).isTrue();
        assertThat(Projects.isWebAddress("HTTP://example.com")).isTrue();
        assertThat(Projects.isWebAddress("https://example.com/my film (final).mp4")).isTrue();
        assertThat(Projects.isWebAddress("https://")).isFalse();
        assertThat(Projects.isWebAddress("https:///no-host")).isFalse();
        assertThat(Projects.isWebAddress("ftp://example.com/film")).isFalse();
        assertThat(Projects.isWebAddress("see https://example.com")).isFalse();
    }
}
