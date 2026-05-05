/*
   Copyright (c) 2026 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.linkedin.d2.balancer.clients;

/**
 * Describes what the per-call duration value from
 * {@link TrackerClient#setPerCallDurationListener(PerCallDurationListener)} represents, so
 * load-balancer metrics can keep REST / stream transport timing separate from streaming
 * time-to-first-byte (TTFB) measurements.
 *
 * <p>See {@link TrackerClientImpl} for the exact callback mapping.
 */
public enum PerCallDurationSemantics
{
  /**
   * Elapsed time from the start of the outbound request until the REST response callback, or until
   * a stream transport-level error (no response body / entity stream).
   */
  FULL_ROUND_TRIP,

  /**
   * Elapsed time from the start of the outbound request until the first byte of a successful
   * streaming response; used when the stream body completes in {@code onDone} or fails in
   * {@code onError} after headers/first bytes (D2 call-tracking policy).
   */
  TIME_TO_FIRST_BYTE
}
