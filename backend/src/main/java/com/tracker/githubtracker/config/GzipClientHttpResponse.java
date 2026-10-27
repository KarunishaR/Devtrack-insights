package com.tracker.githubtracker.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Presents a gzip-compressed response as a normal, readable one. The
 * body is decompressed on the fly, and the Content-Encoding /
 * Content-Length headers are dropped because they describe the
 * compressed bytes, not what callers will now read.
 */
class GzipClientHttpResponse implements ClientHttpResponse {

    private final ClientHttpResponse original;
    private final HttpHeaders headers = new HttpHeaders();
    private InputStream body;

    GzipClientHttpResponse(ClientHttpResponse original) {
        this.original = original;
        this.headers.putAll(original.getHeaders());
        this.headers.remove(HttpHeaders.CONTENT_ENCODING);
        this.headers.remove(HttpHeaders.CONTENT_LENGTH);
    }

    @Override
    public HttpStatusCode getStatusCode() throws IOException {
        return original.getStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return original.getStatusText();
    }

    @Override
    public void close() {
        original.close();
    }

    @Override
    public InputStream getBody() throws IOException {
        if (body == null) {
            body = new GZIPInputStream(original.getBody());
        }
        return body;
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }
}
