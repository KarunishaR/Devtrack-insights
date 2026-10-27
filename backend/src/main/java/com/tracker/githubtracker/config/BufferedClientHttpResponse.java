package com.tracker.githubtracker.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps an already-consumed ClientHttpResponse so its body can be
 * re-read further down the chain (needed because LoggingInterceptor
 * reads the stream once to log it).
 */
class BufferedClientHttpResponse implements ClientHttpResponse {

    private final ClientHttpResponse original;
    private final byte[] body;

    BufferedClientHttpResponse(ClientHttpResponse original, byte[] body) {
        this.original = original;
        this.body = body;
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
    public InputStream getBody() {
        return new ByteArrayInputStream(body);
    }

    @Override
    public HttpHeaders getHeaders() {
        return original.getHeaders();
    }
}
