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

package com.linkedin.d2.balancer.strategies;

/**
 * Mixin for {@link LoadBalancerStrategy} implementations that need to know which URI scheme
 * (e.g. {@code "http"} / {@code "https"}) they are serving so they can tag per-strategy telemetry.
 *
 * Callers that want to set the scheme should always check first:
 *
 * {@code
 * if (strategy instanceof SchemeAware) {
 *   ((SchemeAware) strategy).setScheme(scheme);
 * }
 * }
 */
public interface SchemeAware
{

  String NO_VALUE = "-";

  /**
   * Sets the URI scheme this strategy is associated with. Used for OpenTelemetry metric tagging
   * so the consumer can attribute samples to the correct (service, scheme) pair.
   *
   * Implementations should treat {@code null} and {@link #NO_VALUE} as "no change" so that
   * late, repeated, or partially-initialized callers don't clobber a previously-set scheme.
   *
   * @param scheme the load-balancer scheme (e.g. {@code "http"}, {@code "https"})
   */
  void setScheme(String scheme);
}
