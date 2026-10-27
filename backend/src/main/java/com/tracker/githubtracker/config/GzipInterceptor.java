package com.tracker.githubtracker.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Asks GitHub to compress its responses and transparently decompresses them.
 *
 * Java's built-in HTTP client does NOT request compression on its own, so
 * every GitHub response arrived as full-size JSON. The big ones - the
 * 100-repo list, the pull-request search (which includes PR descriptions)
 * and the three pages of the events feed - are hundreds of KB each, and
 * JSON compresses roughly 5-10x. On a typical home connection to a server
 * on another continent, the transfer time of those payloads is a large
 * part of the wait.
 */
public class GzipInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(org.springframework.http.HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set(HttpHeaders.ACCEPT_ENCODING, "gzip");
        ClientHttpResponse response = execution.execute(request, body);

        String encoding = response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
        if (encoding != null && encoding.trim().equalsIgnoreCase("gzip")) {
            return new GzipClientHttpResponse(response);
        }
        return response;
    }
}
