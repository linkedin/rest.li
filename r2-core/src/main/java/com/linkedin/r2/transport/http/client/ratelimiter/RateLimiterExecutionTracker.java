/*
   Copyright (c) 2021 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

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
