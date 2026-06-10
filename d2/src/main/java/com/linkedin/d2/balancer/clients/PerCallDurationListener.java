/*
   Copyright (c) 2026 LinkedIn Corp.

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
package com.linkedin.d2.balancer.clients;

/**
 * Per-call duration listener for {@link TrackerClient}.
 *
 * <p>Primitive-{@code long} specialization of a {@code BiConsumer<Long, PerCallDurationSemantics>}
 * to avoid boxing the duration on every completed call. Equivalent in spirit to
 * {@link java.util.function.LongConsumer}, extended with a second
 * {@link PerCallDurationSemantics} argument that the JDK has no built-in functional interface for.
 *
 * <p><b>Threading and contract.</b> The listener fires <em>synchronously</em> on the thread that
 * completes the underlying transport call &mdash; for the REST path it runs immediately before the
 * wrapped {@code TransportCallback} is invoked, and for the streaming path it runs from the entity
 * stream's {@code onDone}/{@code onError} (typically a transport / event-loop thread). Therefore
 * implementations <b>MUST NOT block</b> (no I/O, no locks held by slow code paths, no waiting on
 * other threads) and <b>MUST NOT throw checked work</b> onto the caller. Any blocking behaviour
 * here directly stalls request completion and downstream user callbacks.
 *
 * <p>Implementations should also be cheap and allocation-free where possible; they are on the
 * per-request hot path. Exceptions thrown from {@link #accept(long, PerCallDurationSemantics)}
 * are caught by {@link TrackerClient} and rate-limited in the logs &mdash; they will not propagate
 * to the wrapped callback, but they will cause the duration sample to be dropped.
 */
@FunctionalInterface
public interface PerCallDurationListener
{
  /**
   * Records the perceived per-call duration for one completed call.
   *
   * @param durationMs duration in milliseconds the server was perceived to have contributed
   * @param semantics what {@code durationMs} represents (full round-trip vs. streaming TTFB);
   *                  see {@link PerCallDurationSemantics}
   */
  void accept(long durationMs, PerCallDurationSemantics semantics);
}
