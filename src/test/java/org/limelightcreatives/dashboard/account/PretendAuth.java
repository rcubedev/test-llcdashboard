package org.limelightcreatives.dashboard.account;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Just enough of the auth app's OpenID Connect provider for the dashboard to sign someone in against it: a
 * token endpoint that swaps one code for tokens, the keys they're signed with, and userinfo. It signs like auth:
 * an EC P-256 and an RSA key named by their thumbprints, ID tokens with the algorithm the app was registered with
 * (RS256 by default) and access tokens with ES256.
 */
class PretendAuth implements AutoCloseable {

    private final HttpServer server;
    private final ECKey ecKey;
    private final RSAKey rsaKey;
    final String issuer;

    /** What the dashboard's ID tokens are signed with, as chosen on auth's apps page. */
    volatile JWSAlgorithm idTokenAlgorithm = JWSAlgorithm.RS256;

    /** Who the next code signs in, and the nonce the dashboard sent with it. */
    private volatile Map<String, Object> next = Map.of();
    /** The form of the last token request, e.g. code_verifier, and its Authorization header. */
    volatile Map<String, String> lastTokenRequest = Map.of();
    volatile String lastTokenAuthorization;

    PretendAuth() throws Exception {
        this.ecKey = new ECKeyGenerator(Curve.P_256).keyIDFromThumbprint(true).generate();
        this.rsaKey = new RSAKeyGenerator(2048).keyIDFromThumbprint(true).generate();
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.issuer = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/oauth2/token", this::token);
        server.createContext("/oauth2/jwks", exchange ->
                send(exchange, new JWKSet(List.<JWK>of(ecKey.toPublicJWK(), rsaKey.toPublicJWK())).toString()));
        server.createContext("/userinfo", this::userInfo);
        server.start();
    }

    /** The next sign-in is this person, for the authorization request that carried {@code nonce}. */
    void willSignIn(String id, String role, String nonce) {
        var claims = new HashMap<String, Object>();
        claims.put("sub", id);
        claims.put("email", id + "@example.com");
        claims.put("name", "Sam Lee");
        claims.put("role", role);
        claims.put("nonce", nonce);
        this.next = claims;
    }

    /** How long access tokens last, in seconds. Under a minute counts as run out already, so every page renews it. */
    volatile int expiresIn = 600;
    /** Whether refresh tokens are turned down, as when someone signed out everywhere. */
    volatile boolean refuseRefresh;
    private int tokens;

    /** What userinfo (and the next tokens) say from now on. */
    void nowIs(String role, String name) {
        Map<String, Object> claims = new java.util.HashMap<>(next);
        claims.put("role", role);
        claims.put("name", name);
        this.next = claims;
    }

    /** How many times a refresh token was swapped for new tokens. */
    volatile int refreshes;

    private synchronized void token(HttpExchange exchange) throws IOException {
        lastTokenAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
        lastTokenRequest = form(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        try {
            Instant now = Instant.now();
            String idToken = signed(idTokenAlgorithm, claims(now, 600));
            String accessToken = signed(JWSAlgorithm.ES256, claims(now, expiresIn).claim("scope", "openid profile email"));
            boolean refresh = "refresh_token".equals(lastTokenRequest.get("grant_type"));
            String presented = lastTokenRequest.get("refresh_token");
            // like auth, a refresh token works once: each use replaces it
            boolean used = refresh && !("refresh-" + tokens).equals(presented);
            if (refresh && !used) refreshes++;
            if (refresh && (refuseRefresh || used)) {
                byte[] refused = "{\"error\":\"invalid_grant\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, refused.length);
                try (var out = exchange.getResponseBody()) {
                    out.write(refused);
                }
                return;
            }
            send(exchange, json(Map.of("access_token", accessToken, "token_type", "Bearer", "expires_in", expiresIn,
                    "refresh_token", "refresh-" + ++tokens, "scope", "openid profile email", "id_token", idToken)));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private JWTClaimsSet.Builder claims(Instant now, int lifetime) {
        var claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(List.of("app_test"))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(lifetime)));
        next.forEach(claims::claim);
        return claims;
    }

    private String signed(JWSAlgorithm algorithm, JWTClaimsSet.Builder claims) throws JOSEException {
        boolean ec = JWSAlgorithm.ES256.equals(algorithm);
        JWSSigner signer = ec ? new ECDSASigner(ecKey) : new RSASSASigner(rsaKey);
        String keyId = ec ? ecKey.getKeyID() : rsaKey.getKeyID();
        var jwt = new SignedJWT(new JWSHeader.Builder(algorithm).keyID(keyId).build(), claims.build());
        jwt.sign(signer);
        return jwt.serialize();
    }

    /** Like auth's, only for an access token it signed that hasn't run out. */
    private void userInfo(HttpExchange exchange) throws IOException {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        boolean valid;
        try {
            var token = SignedJWT.parse(authorization == null ? "" : authorization.replaceFirst("^Bearer ", ""));
            valid = token.verify(new ECDSAVerifier(ecKey.toPublicJWK()))
                    && token.getJWTClaimsSet().getExpirationTime().toInstant().isAfter(Instant.now());
        } catch (ParseException | JOSEException e) {
            valid = false;
        }
        if (!valid) {
            exchange.getResponseHeaders().set("WWW-Authenticate", "Bearer error=\"invalid_token\"");
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }
        send(exchange, json(next));
    }

    private static void send(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String json(Map<String, ?> values) {
        var json = new StringBuilder("{");
        values.forEach((name, value) -> {
            if (json.length() > 1) json.append(',');
            json.append('"').append(name).append("\":");
            json.append(value instanceof Number ? value.toString() : "\"" + value + "\"");
        });
        return json.append('}').toString();
    }

    private static Map<String, String> form(String body) {
        var values = new HashMap<String, String>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) values.put(parts[0], URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return values;
    }

    static String basic(String id, String secret) {
        return "Basic " + Base64.getEncoder().encodeToString((id + ":" + secret).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
