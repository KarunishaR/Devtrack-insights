package com.tracker.githubtracker.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A tiny in-memory cache with a time-to-live, plus "single-flight"
 * loading: if several requests ask for the same key at the same moment
 * (e.g. the Overview page and the Skills page both need skills), only
 * ONE of them does the expensive GitHub work - the others wait for that
 * result instead of duplicating it. This both speeds the app up and
 * keeps GitHub API usage (and rate-limit risk) down.
 *
 * A short TTL (minutes) keeps data effectively live while avoiding
 * hammering GitHub on every click.
 */
public class SimpleTtlCache<K, V> {

    private record Entry<V>(V value, long expiresAtMillis) {}

    private final ConcurrentHashMap<K, Entry<V>> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, Object> locks = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public SimpleTtlCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public V get(K key) {
        Entry<V> entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expiresAtMillis()) {
            store.remove(key);
            return null;
        }
        return entry.value();
    }

    public void put(K key, V value) {
        store.put(key, new Entry<>(value, System.currentTimeMillis() + ttlMillis));
    }

    /**
     * Returns the cached value, or computes it with the loader. Only one
     * thread computes a given key at a time; concurrent callers wait and
     * then reuse the result. If the loader throws, nothing is cached and
     * the exception propagates (so errors are never "remembered").
     */
    public V getOrCompute(K key, Supplier<V> loader) {
        V cached = get(key);
        if (cached != null) {
            return cached;
        }
        Object lock = locks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            cached = get(key);
            if (cached != null) {
                return cached;
            }
            V value = loader.get();
            if (value != null) {
                put(key, value);
            }
            return value;
        }
    }
}
