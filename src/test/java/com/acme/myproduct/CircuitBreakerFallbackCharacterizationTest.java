package com.acme.myproduct;

// step 13:
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FallbackFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Circuit-breaker characterization knot (Step 12a) — the net-widening §5/§8 require BEFORE the
 * Netflix cliff. The app carries the Netflix/Hystrix/OpenFeign stack on its classpath
 * (feign-hystrix:10.12, hystrix-core:1.5.18) but wires no live @HystrixCommand / @FeignClient
 * fallback, so this pins the CONTRACT the next hops must preserve rather than any incidental wiring:
 *
 *   feign.hystrix.FallbackFactory<T> { T create(Throwable cause); }
 *
 * i.e. on a downstream failure the factory (1) is handed the triggering Throwable and (2) returns a
 * T fallback that serves a degraded value. This is the green reference for:
 *   - Step 13: Hystrix -> Resilience4j re-architecture (the fallback contract must survive the port;
 *              feign.hystrix.FallbackFactory -> org.springframework.cloud.openfeign.FallbackFactory).
 *   - Step 14: Boot 2.4 / Spring Cloud 2020.0, where netflix-hystrix is removed from the train.
 * If either step breaks the "factory sees the cause, returns a degraded fallback" behaviour, this
 * test goes red.
 */
public class CircuitBreakerFallbackCharacterizationTest {

    /** The thin downstream contract a Feign client would expose. */
    interface GreetingClient {
        String greet(String name);
    }

    /**
     * The Hystrix-era degradation primitive: a FallbackFactory that, given the failure cause,
     * builds a fallback client serving a degraded ("offline") greeting.
     */
    static final class GreetingFallbackFactory implements FallbackFactory<GreetingClient> {
        Throwable seenCause;   // characterizes that the factory is handed the cause

        @Override
        public GreetingClient create(Throwable cause) {
            this.seenCause = cause;
            return name -> "offline:" + name;
        }
    }

    @Test
    public void factoryReceivesTheTriggeringCause() {
        GreetingFallbackFactory factory = new GreetingFallbackFactory();
        RuntimeException downstreamFailure = new RuntimeException("503 from greeting-service");

        factory.create(downstreamFailure);

        // The distinguishing property of FallbackFactory over a plain fallback: it sees the cause.
        assertSame(downstreamFailure, factory.seenCause);
    }

    @Test
    public void fallbackServesDegradedValueOnFailure() {
        GreetingFallbackFactory factory = new GreetingFallbackFactory();

        GreetingClient fallback = factory.create(new RuntimeException("downstream down"));

        // The degraded path is taken instead of the (unavailable) live call.
        assertEquals("offline:acme", fallback.greet("acme"));
    }
}
