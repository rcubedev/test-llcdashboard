package org.limelightcreatives.dashboard.web;

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/** Addresses with a query string, for templates. */
public final class Links {

    private Links() {}

    /** {@code path?name=value&...} from name and value pairs, leaving out values that are null or empty. */
    public static String to(String path, String... pairs) {
        var link = new StringBuilder(path);
        char separator = '?';
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (pairs[i + 1] == null || pairs[i + 1].isEmpty()) continue;
            link.append(separator).append(pairs[i]).append('=')
                    .append(UriUtils.encodeQueryParam(pairs[i + 1], StandardCharsets.UTF_8));
            separator = '&';
        }
        return link.toString();
    }
}
