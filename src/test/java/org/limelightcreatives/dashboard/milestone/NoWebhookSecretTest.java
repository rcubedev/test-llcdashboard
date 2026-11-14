package org.limelightcreatives.dashboard.milestone;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Without FILLOUT_WEBHOOK_SECRET nobody can post submissions, not even with an empty secret. */
@AutoConfigureMockMvc
class NoWebhookSecretTest extends DatabaseTest {

    @Autowired MockMvc mvc;

    @Test
    void everyCallIsRefused() throws Exception {
        for (String secret : new String[]{null, ""}) {
            var request = post("/api/fillout/webhook").contentType(MediaType.APPLICATION_JSON).content("{}");
            if (secret != null) request.param("secret", secret);
            assertThat(mvc.perform(request).andReturn().getResponse().getStatus()).isEqualTo(401);
        }
    }
}
