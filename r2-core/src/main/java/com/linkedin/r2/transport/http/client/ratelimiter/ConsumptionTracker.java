package com.linkedin.r2.transport.http.client.ratelimiter;


public interface ConsumptionTracker {

    boolean getPausedAndIncrement();

    boolean decrementAndGetPaused();

    void pauseConsumption();

    boolean isPaused();

    int getPending();

    int getMaxBuffered();
}
