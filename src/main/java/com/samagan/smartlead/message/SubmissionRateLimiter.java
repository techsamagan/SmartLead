package com.samagan.smartlead.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Fixed one-minute window per key, held in memory. Enough for a single instance;
// several instances would need a shared store such as Redis.
@Component
public class SubmissionRateLimiter {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Window(long startedAt, int count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limitPerMinute;
    private final Clock clock;

    @Autowired
    public SubmissionRateLimiter(@Value("${smartlead.public-form.rate-limit-per-minute:10}") int limitPerMinute) {
        this(limitPerMinute, Clock.systemUTC());
    }

    SubmissionRateLimiter(int limitPerMinute, Clock clock) {
        this.limitPerMinute = limitPerMinute;
        this.clock = clock;
    }

    public boolean tryAcquire(String key) {
        long now = clock.millis();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(window -> now - window.startedAt() >= WINDOW_MILLIS);
        }

        Window updated = windows.compute(key, (k, window) ->
                window == null || now - window.startedAt() >= WINDOW_MILLIS
                        ? new Window(now, 1)
                        : new Window(window.startedAt(), window.count() + 1));
        return updated.count() <= limitPerMinute;
    }
}
