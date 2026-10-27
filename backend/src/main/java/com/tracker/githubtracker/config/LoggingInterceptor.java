package com.tracker.githubtracker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Logs every outgoing GitHub call with its status and how long it took
 * (handy for spotting what's slow), and logs the response BODY only
 * when the call failed.
 *
 * Previously this read and buffered the full body of every response -
 * including the 100-repo list - purely to print the first 500
 * characters. That was pure overhead on every request.
 */
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        long startedAt = System.currentTimeMillis();
        ClientHttpResponse response = execution.execute(request, body);
        long tookMs = System.currentTimeMillis() - startedAt;
        int status = response.getStatusCode().value();

        if (status >= 400) {
            String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            String shortBody = responseBody.length() > 400 ? responseBody.substring(0, 400) + "..." : responseBody;
            log.warn("{} {} -> {} in {}ms | {}", request.getMethod(), request.getURI(), status, tookMs, shortBody);
            // The stream was consumed above, so hand Spring a re-readable copy.
            return new BufferedClientHttpResponse(response, responseBody.getBytes(StandardCharsets.UTF_8));
        }

        log.info("{} {} -> {} in {}ms", request.getMethod(), request.getURI(), status, tookMs);
        return response;
    }
}
