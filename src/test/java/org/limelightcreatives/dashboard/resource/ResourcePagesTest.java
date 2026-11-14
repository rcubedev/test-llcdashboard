package org.limelightcreatives.dashboard.resource;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.limelightcreatives.dashboard.resource.ResourcesTest.input;

@AutoConfigureMockMvc
class ResourcePagesTest extends DatabaseTest {

    @Autowired MockMvc mvc;
    @Autowired Resources resources;

    @Test
    void participantsReadPublishedOnes() throws Exception {
        var sam = new Visitor(mvc).as(SignedIn.participant("sam"));
        assertThat(sam.get("/participant/film-fest/resources").getContentAsString())
                .contains("No resources have been published yet. Check back soon.");

        resources.save(new ResourceInput(null, "Lighting 101", "The basics", "First paragraph\nhttps://example.com/more\n\nSecond",
                "https://youtu.be/dQw4w9WgXcQ", "", "Camera", "video", "all", true, 0));
        resources.save(input("Mic tips", "Sound", "all", true, "Clip it close.", ""));
        resources.save(input("Draft", "Sound", "all", false, "x", ""));

        String page = sam.get("/participant/film-fest/resources").getContentAsString();
        assertThat(page).contains("Lighting 101", "The basics", "Camera · Updated ", "▶ Video",
                "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg", ">Watch</a>", ">Read</span>",
                "First paragraph", "href=\"https://example.com/more\"", "Second", "Mic tips",
                "All · 2", "Camera · 1", "Sound · 1",
                "href=\"/participant/film-fest/resources?category=Sound\"")
                .doesNotContain("Draft");

        assertThat(sam.get("/participant/film-fest/resources?category=Sound").getContentAsString())
                .contains("Mic tips").doesNotContain("Lighting 101");
        assertThat(sam.get("/participant/film-fest/resources?q=clip").getContentAsString())
                .contains("Mic tips").doesNotContain("Lighting 101");
        assertThat(sam.get("/participant/film-fest/resources?q=zzz").getContentAsString()).contains("No resources match.");
    }

    @Test
    void staffWriteAndPublish() throws Exception {
        var staff = new Visitor(mvc).as(SignedIn.staff("ontik"));
        assertThat(staff.get("/staff/resources").getContentAsString()).contains("No resources yet.");
        assertThat(staff.get("/staff/resources/new").getContentAsString()).contains("New resource", "value=\"General\"");

        var refused = staff.follow(staff.post("/staff/resources", "title", "Lighting", "category", "Camera",
                "kind", "guide", "audience", "all", "published", "true", "body", "", "sortOrder", "oops"));
        assertThat(refused.getContentAsString()).contains("Add some content or a link before publishing.",
                "value=\"Lighting\"", "value=\"Camera\"");

        assertThat(staff.post("/staff/resources", "title", "Lighting", "category", "Camera", "kind", "guide",
                "audience", "all", "body", "Use a key light.", "sortOrder", "2").getRedirectedUrl()).isEqualTo("/staff/resources");
        var saved = resources.all().getFirst();
        assertThat(saved.published()).isFalse();
        assertThat(saved.sortOrder()).isEqualTo(2);

        String list = staff.get("/staff/resources").getContentAsString();
        assertThat(list).contains("Lighting", "Camera · Everyone · Updated", "Draft", "Drafts · 1", ">Publish</button>");

        staff.post("/staff/resources/" + saved.id() + "/published", "published", "true");
        assertThat(resources.find(saved.id()).orElseThrow().published()).isTrue();
        assertThat(staff.get("/staff/resources?filter=published").getContentAsString()).contains("Lighting", ">Unpublish</button>");

        assertThat(staff.get("/staff/resources/" + saved.id()).getContentAsString())
                .contains("Edit resource", "Use a key light.", "data-confirm=\"Delete “Lighting”?");
        staff.post("/staff/resources/" + saved.id() + "/delete");
        assertThat(resources.all()).isEmpty();
    }

    @Test
    void theEditorIsForStaff() throws Exception {
        var sam = new Visitor(mvc).as(SignedIn.participant("sam"));
        assertThat(sam.get("/staff/resources/new").getRedirectedUrl()).isEqualTo("/participant");
        assertThat(sam.post("/staff/resources", "title", "x").getRedirectedUrl()).isEqualTo("/participant");
        assertThat(resources.all()).isEmpty();
    }
}
