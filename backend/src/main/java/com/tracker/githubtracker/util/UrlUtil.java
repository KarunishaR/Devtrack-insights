package com.tracker.githubtracker.util;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Builds fully-encoded URIs for GitHub search queries.
 *
 * Why this exists: search queries contain characters like spaces, ':',
 * '>' and (for languages like C++) '+'. Spring's UriComponentsBuilder
 * leaves '+' untouched in a query string, and servers read a bare '+' as
 * a SPACE - so "language:C++" silently became "language:C" and returned
 * the wrong results. Encoding every query value strictly (space -> %20,
 * '+' -> %2B) and handing RestTemplate a ready-made URI (which it will
 * not encode a second time) avoids both the double-encoding bug and the
 * '+' bug.
 */
public final class UrlUtil {

    private UrlUtil() {}

    public static URI buildUri(String baseUrl, String path, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(baseUrl).append(path);
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(first ? '?' : '&');
            first = false;
            sb.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return URI.create(sb.toString());
    }

    private static String encode(String value) {
        // URLEncoder encodes a space as '+'; in a query string that must be %20.
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
