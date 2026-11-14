package org.limelightcreatives.dashboard;

import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * A pretend browser for MockMvc on the real address: keeps the session cookie between requests like a real
 * one, and can be signed in as someone ({@link SignedIn}) for every request it sends.
 */
public class Visitor {

    public static final String COOKIE = "dashboard-session";

    private final MockMvc mvc;
    private RequestPostProcessor signedIn;
    public Cookie session;
    /** Any other cookies pages set, kept like a browser does. */
    private final Map<String, Cookie> others = new HashMap<>();

    public Visitor(MockMvc mvc) {
        this.mvc = mvc;
    }

    /** Every request from now on is from this person (see {@link SignedIn}). */
    public Visitor as(RequestPostProcessor signedIn) {
        this.signedIn = signedIn;
        return this;
    }

    /** {@code url} is an address as a browser has it (already encoded), e.g. from a redirect. */
    public MockHttpServletResponse get(String url, String... params) throws Exception {
        var request = MockMvcRequestBuilders.get(URI.create(url));
        for (int i = 0; i < params.length; i += 2) request.param(params[i], params[i + 1]);
        return send(request);
    }

    /** A form post, with the CSRF token the page would have had. */
    public MockHttpServletResponse post(String url, String... params) throws Exception {
        var request = MockMvcRequestBuilders.post(url).with(csrf());
        for (int i = 0; i < params.length; i += 2) request.param(params[i], params[i + 1]);
        return send(request);
    }

    public MockHttpServletResponse send(MockHttpServletRequestBuilder request) throws Exception {
        request.with(r -> {
            r.setSecure(true);
            r.setScheme("https");
            r.setServerName("dashboard.limelightcreatives.org");
            r.setServerPort(443);
            return r;
        });
        if (this.signedIn != null) request.with(this.signedIn);
        if (this.session != null) request.cookie(this.session);
        if (!this.others.isEmpty()) request.cookie(this.others.values().toArray(Cookie[]::new));
        MockHttpServletResponse response = this.mvc.perform(request).andReturn().getResponse();
        for (Cookie set : response.getCookies()) {
            boolean deleted = set.getMaxAge() == 0;
            if (set.getName().equals(COOKIE)) this.session = deleted ? null : set;
            else if (deleted) this.others.remove(set.getName());
            else this.others.put(set.getName(), set);
        }
        return response;
    }

    /** Opens where a redirect points, like a browser does. */
    public MockHttpServletResponse follow(MockHttpServletResponse redirect) throws Exception {
        String location = redirect.getRedirectedUrl();
        assertThat(location).as("a redirect").isNotNull();
        return get(location);
    }
}
