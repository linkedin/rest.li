package com.linkedin.r2.transport.http.client.ratelimiter;

/**
 * Used by a RateLimiter to track execution of callbacks pending in its internal buffer.
 */
public interface RateLimiterExecutionTracker {

    /**
     * Unpauses execution on RateLimiter if applicable. Increments the number of pending callbacks by 1.
     * @return whether or not the RateLimiter was paused when method call happened.
     */
    boolean getPausedAndIncrement();

    /**
     * Pauses execution on RateLimiter if applicable. Decrements the number of pending callbacks by 1.
     * @return whether on not the RateLimiter was paused as a result of the call happening.
     */
    boolean decrementAndGetPaused();

    /**
     * Pauses execution on the RateLimiter.
     */
    void pauseExecution();

    /**
     * @return whether or not execution on the RateLimiter is currently paused.
     */
    boolean isPaused();

    /**
     * @return outstanding number of callbacks pending to be executed in the RateLimiter
     */
    int getPending();

    /**
     * @return maximum number of callbacks that can be stored in the RateLimiter
     */
    int getMaxBuffered();
}
