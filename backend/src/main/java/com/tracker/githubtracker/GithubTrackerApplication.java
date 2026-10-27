package com.tracker.githubtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GithubTrackerApplication {

    public static void main(String[] args) {
        // The JDK keeps only 5 idle connections per host by default. We make many
        // parallel calls to api.github.com, so allow more to be reused instead of
        // paying for a fresh TLS handshake each time.
        System.setProperty("http.maxConnections", "32");
        SpringApplication.run(GithubTrackerApplication.class, args);
    }
}
