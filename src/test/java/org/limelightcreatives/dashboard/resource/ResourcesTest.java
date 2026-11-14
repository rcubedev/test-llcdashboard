package org.limelightcreatives.dashboard.resource;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourcesTest extends DatabaseTest {

    @Autowired Resources resources;

    static ResourceInput input(String title, String category, String audience, boolean published, String body, String url) {
        return new ResourceInput(null, title, "", body, url, "", category, "guide", audience, published, 0);
    }

    @Test
    void participantsSeePublishedOnesForThemByCategoryOrderAndTitle() {
        resources.save(input("Zebra", "Sound", "all", true, "b", ""));
        resources.save(new ResourceInput(null, "Apple", "", "b", "", "", "Sound", "guide", "client", true, 5));
        resources.save(new ResourceInput(null, "Banana", "", "b", "", "", "Sound", "guide", "all", true, 5));
        resources.save(input("Draft", "Sound", "all", false, "b", ""));
        resources.save(input("Staff only", "Sound", "organiser", true, "b", ""));
        resources.save(input("Camera", "Camera", "all", true, "b", ""));

        assertThat(resources.forParticipants()).extracting(Resource::title)
                .containsExactly("Camera", "Zebra", "Apple", "Banana");
        assertThat(resources.all()).hasSize(6);
    }

    @Test
    void theOldRules() {
        assertThatThrownBy(() -> resources.save(input(" ", "x", "all", false, "", ""))).hasMessage("Add a title before saving.");
        assertThatThrownBy(() -> resources.save(input("t", "x", "all", false, "", "ftp://x")))
                .hasMessage("The link must start with http:// or https://.");
        assertThatThrownBy(() -> resources.save(input("t", "x", "everyone", false, "", "")))
                .hasMessage("Pick a valid type and audience.");
        assertThatThrownBy(() -> resources.save(new ResourceInput(null, "t", "", "", "", "javascript:x", "x", "guide", "all", false, 0)))
                .hasMessage("The thumbnail link must start with http:// or https://.");
        assertThatThrownBy(() -> resources.save(input("t", "x", "all", true, "  ", "")))
                .hasMessage("Add some content or a link before publishing.");
    }

    @Test
    void anEmptyCategoryIsGeneralAndSlugsAreUnique() {
        String first = resources.save(input("How to Light a Scene!", "", "all", false, "", ""));
        String second = resources.save(input("How to light a scene", "", "all", false, "", ""));
        var a = resources.find(first).orElseThrow();
        var b = resources.find(second).orElseThrow();
        assertThat(a.category()).isEqualTo("General");
        assertThat(a.slug()).isEqualTo("how-to-light-a-scene");
        assertThat(b.slug()).startsWith("how-to-light-a-scene-").isNotEqualTo(a.slug());
        assertThat(Resources.slug("¡Ça va!")).isEqualTo("ca-va");
        assertThat(Resources.slug("!!!")).isEqualTo("resource");
    }

    @Test
    void editingPublishingAndDeleting() {
        String id = resources.save(input("Lighting", "Camera", "all", false, "Body", ""));
        resources.save(new ResourceInput(id, "Lighting 2", "Short", "Body", "", "", "Camera", "video", "client", true, 3));
        var saved = resources.find(id).orElseThrow();
        assertThat(saved.title()).isEqualTo("Lighting 2");
        assertThat(saved.kind()).isEqualTo("video");
        assertThat(saved.sortOrder()).isEqualTo(3);
        assertThat(saved.slug()).isEqualTo("lighting"); // kept

        resources.setPublished(id, false);
        assertThat(resources.find(id).orElseThrow().published()).isFalse();
        resources.delete(id);
        assertThat(resources.find(id)).isEmpty();
        assertThat(resources.find("not-a-uuid")).isEmpty();
    }

    @Test
    void youtubeLinksGetTheirThumbnail() {
        assertThat(Resource.thumbnail("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=1", ""))
                .isEqualTo("https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg");
        assertThat(Resource.thumbnail("https://youtu.be/dQw4w9WgXcQ", "")).contains("dQw4w9WgXcQ");
        assertThat(Resource.thumbnail("https://youtube.com/shorts/dQw4w9WgXcQ", "")).contains("dQw4w9WgXcQ");
        assertThat(Resource.thumbnail("https://youtu.be/dQw4w9WgXcQ", "https://img.example/own.jpg")).isEqualTo("https://img.example/own.jpg");
        assertThat(Resource.thumbnail("https://example.com", "")).isNull();
    }
}
