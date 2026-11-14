package org.limelightcreatives.dashboard.sheets;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

/**
 * A Google service account: signs the short-lived JWT that Google swaps for an access token
 * (https://developers.google.com/identity/protocols/oauth2/service-account#httprest).
 */
record ServiceAccount(String email, PrivateKey key, Clock clock) {

    static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    static final String SCOPE = "https://www.googleapis.com/auth/spreadsheets";
    static final Duration LIFETIME = Duration.ofHours(1);

    /** The signed JWT to post to {@link #TOKEN_URL} as the {@code assertion}. */
    String assertion() {
        long now = clock.instant().getEpochSecond();
        String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String claims = "{\"iss\":\"" + jsonEscape(email) + "\",\"scope\":\"" + SCOPE + "\",\"aud\":\"" + TOKEN_URL
                + "\",\"iat\":" + now + ",\"exp\":" + (now + LIFETIME.toSeconds()) + "}";
        String unsigned = base64Url(header.getBytes(StandardCharsets.UTF_8)) + "." + base64Url(claims.getBytes(StandardCharsets.UTF_8));
        try {
            var signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(unsigned.getBytes(StandardCharsets.US_ASCII));
            return unsigned + "." + base64Url(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Couldn't sign the Google service account token", e);
        }
    }

    /**
     * The key from the service account's JSON file ({@code private_key}), however it was pasted into an
     * environment variable: the PEM text, with real or literal {@code \n} line breaks and maybe stray quotes,
     * or that PEM in base64 (what GOOGLE_PRIVATE_KEY_B64 holds).
     */
    static PrivateKey parsePrivateKey(String value) {
        String pem = value.strip();
        if (!pem.contains("-----BEGIN")) {
            try {
                pem = new String(Base64.getMimeDecoder().decode(pem), StandardCharsets.UTF_8).strip();
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("The Google private key is neither PEM text nor base64", e);
            }
        }
        pem = pem.replaceAll("^[\"']|[\"']$", "").replace("\\n", "\n");
        String base64 = pem.replaceAll("-----(BEGIN|END) [A-Z ]*PRIVATE KEY-----", "").replaceAll("\\s", "");
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("The Google private key isn't a valid RSA key (copy private_key from the JSON file)", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
