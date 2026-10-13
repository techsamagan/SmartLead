package com.samagan.smartlead.message;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionRateLimiterTests {

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) { now = now.plus(duration); }

        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    @Test
    void allowsUpToTheLimitThenBlocksUntilTheWindowPasses() {
        MutableClock clock = new MutableClock();
        SubmissionRateLimiter limiter = new SubmissionRateLimiter(2, clock);

        assertThat(limiter.tryAcquire("form|1.2.3.4")).isTrue();
        assertThat(limiter.tryAcquire("form|1.2.3.4")).isTrue();
        assertThat(limiter.tryAcquire("form|1.2.3.4")).isFalse();

        clock.advance(Duration.ofSeconds(61));
        assertThat(limiter.tryAcquire("form|1.2.3.4")).isTrue();
    }

    @Test
    void keysAreLimitedIndependently() {
        SubmissionRateLimiter limiter = new SubmissionRateLimiter(1, new MutableClock());

        assertThat(limiter.tryAcquire("form|1.2.3.4")).isTrue();
        assertThat(limiter.tryAcquire("form|5.6.7.8")).isTrue();
        assertThat(limiter.tryAcquire("form|1.2.3.4")).isFalse();
    }
}
