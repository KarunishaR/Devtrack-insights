package com.tracker.githubtracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Provides a single, reusable RestTemplate bean used by GitHubServiceImpl
 * to talk to https://api.github.com
 *
 * IMPORTANT: GitHub's REST API rejects requests that don't send a
 * User-Agent header (it returns 403 Forbidden, which can look like an
 * "unauthorized" error). Java's RestTemplate doesn't send one by default,
 * so we add it here for every request, along with the recommended
 * GitHub API version header.
 *
 * A LoggingInterceptor is also attached so the real status code and
 * response body from GitHub (or any proxy in between) show up in your
 * server console - this is the fastest way to diagnose "unauthorized"
 * style errors instead of guessing.
 *
 * OPTIONAL TOKEN: unauthenticated requests are limited to 60/hour PER IP
 * ADDRESS by GitHub - shared with everyone else on the same network. If
 * you set github.api.token in application.properties (a personal access
 * token, no scopes needed for public data), it's attached server-side
 * only and raises the limit to 5,000/hour. This never asks end users of
 * the app to log in or share credentials - it's purely the server
 * authenticating itself to GitHub.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                      @Value("${github.api.token:}") String githubToken) {

        RestTemplateBuilder configured = builder
                .defaultHeader(HttpHeaders.USER_AGENT, "student-open-source-contribution-tracker")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                // Timeouts: without these, one stalled GitHub call could hang a page indefinitely.
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(20))
                .additionalInterceptors(new LoggingInterceptor(), new GzipInterceptor());

        if (githubToken != null && !githubToken.isBlank()) {
            configured = configured.additionalInterceptors((request, body, execution) -> {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken);
                return execution.execute(request, body);
            });
        }

        return configured.build();
    }
}
