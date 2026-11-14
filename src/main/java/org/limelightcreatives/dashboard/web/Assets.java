package org.limelightcreatives.dashboard.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves static asset paths with a content hash appended for cache busting
 *
 * <p>Paths are resolved relative to {@code static/}. External URLs and non-local
 * values are returned unchanged whilst paths without a corresponding file cause
 * an exception
 */
@Component
public class Assets {

    private record Version(long lastModified, String hash) {}

    private final Map<String, Version> versions = new ConcurrentHashMap<>();

    /**
     * Resolves an asset path, appending its content hash as a query parameter.
     *
     * @param path the asset path or external URL
     * @return the resolved URL, or the original value if it isn't a local path
     */
    public String url(String path) {
        if (path == null || !path.startsWith("/") || path.startsWith("//")) return path;
        return path + "?v=" + hash(path);
    }

    private String hash(String path) {
        var file = new ClassPathResource("static" + UriUtils.decode(path, StandardCharsets.UTF_8));
        if (!file.exists()) throw new IllegalStateException("Static asset not found: " + path);
        try {
            long lastModified = file.lastModified();
            Version known = this.versions.get(path);
            if (known != null && known.lastModified() == lastModified) return known.hash();

            String hash = sha256(file).substring(0, 10);
            this.versions.put(path, new Version(lastModified, hash));
            return hash;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha256(ClassPathResource file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];

            for (int read; (read = in.read(buffer)) != -1; )
                digest.update(buffer, 0, read);

            return HexFormat.of().formatHex(digest.digest());

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // every JVM has SHA-256
        }
    }
}
