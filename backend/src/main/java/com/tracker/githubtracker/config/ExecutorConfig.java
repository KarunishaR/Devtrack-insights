package com.tracker.githubtracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A dedicated thread pool for fan-out GitHub calls (per-repo language
 * lookups, parallel searches, parallel event pages). These calls are
 * network-bound, so a larger pool than the CPU count is appropriate.
 *
 * Previously these ran on Java's shared ForkJoinPool, whose size is
 * (CPU cores - 1) - on a small machine that can be 1-3 threads, which
 * quietly turned "parallel" calls back into nearly sequential ones.
 */
@Configuration
public class ExecutorConfig {

    @Bean(name = "githubExecutor", destroyMethod = "shutdown")
    public ExecutorService githubExecutor() {
        AtomicInteger counter = new AtomicInteger();
        return Executors.newFixedThreadPool(16, runnable -> {
            Thread thread = new Thread(runnable, "github-io-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * A second pool used ONLY for "coordinator" work - tasks that start other
     * tasks and then wait for them (e.g. building suggestions needs skills,
     * PRs and repos at the same time). Keeping these separate from the
     * GitHub-call pool above means coordinators can never occupy every
     * thread while the work they are waiting on has nowhere to run.
     */
    @Bean(name = "orchestratorExecutor", destroyMethod = "shutdown")
    public ExecutorService orchestratorExecutor() {
        AtomicInteger counter = new AtomicInteger();
        return Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "github-orchestrator-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }
}
