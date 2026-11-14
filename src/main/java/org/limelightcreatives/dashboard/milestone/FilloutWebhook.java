package org.limelightcreatives.dashboard.milestone;

import org.limelightcreatives.dashboard.DashboardProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code POST /api/fillout/webhook?secret=...}: Fillout calls it for every milestone submission (set up in
 * Fillout, unchanged from the old app). The SUBMIT buttons add milestoneId, milestone, teamId and team to the
 * form's address, and Fillout sends them back as the submission's URL parameters.
 *
 * <p>It's the only page without a sign-in (SecurityConfig), so it checks the shared secret first.
 */
@RestController
class FilloutWebhook {

    private static final Logger log = LoggerFactory.getLogger(FilloutWebhook.class);

    private final MilestoneSubmissions submissions;
    private final DashboardProperties properties;
    private final JsonMapper json;

    FilloutWebhook(MilestoneSubmissions submissions, DashboardProperties properties, JsonMapper json) {
        this.submissions = submissions;
        this.properties = properties;
        this.json = json;
    }

    @PostMapping("/api/fillout/webhook")
    ResponseEntity<Map<String, Object>> receive(@RequestParam(required = false) String secret,
                                                @RequestBody(required = false) String body) {
        if (!secretMatches(secret)) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        JsonNode root;
        try {
            root = body == null || body.isBlank() ? null : this.json.readTree(body);
        } catch (RuntimeException e) {
            root = null;
        }
        if (root == null || !root.isObject()) return ResponseEntity.badRequest().body(Map.of("error", "Invalid JSON"));

        // Fillout puts it all under "submission"; the root too, just in case
        JsonNode submission = root.path("submission").isObject() ? root.path("submission") : root;
        Map<String, String> params = new HashMap<>();
        for (JsonNode param : submission.path("urlParameters")) {
            if (param.path("name").isString()) params.put(param.path("name").asString(), param.path("value").asString(""));
        }

        String milestoneId = params.get("milestoneId");
        String teamId = params.get("teamId");
        if (blank(milestoneId) || blank(teamId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing milestoneId or teamId"));
        }
        String label = blank(params.get("milestone")) ? null : params.get("milestone");
        String submissionId = submission.path("submissionId").isString() ? submission.path("submissionId").asString() : null;
        // the answers, as Fillout sends them: [{ id, name, type, value }]
        JsonNode questions = submission.path("questions").isArray()
                ? submission.path("questions") : JsonNodeFactory.instance.arrayNode();

        try {
            this.submissions.record(teamId, milestoneId, label, submissionId, this.json.writeValueAsString(questions));
        } catch (DataAccessException e) {
            // a teamId that isn't a team (or isn't a UUID at all)
            log.warn("Couldn't record a Fillout submission for team {}: {}", teamId, e.getMostSpecificCause().getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Could not record submission"));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private boolean secretMatches(String secret) {
        String expected = this.properties.fillout().webhookSecret();
        if (blank(expected) || secret == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
