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
 * What a per-call duration measures when passed to
 * {@link PerCallDurationListener#accept(long, PerCallDurationSemantics)}.
 */
public enum PerCallDurationSemantics
{
  /** REST: full callback latency. Streaming: transport failure before any response body. */
  FULL_ROUND_TRIP,

  /** Streaming only: request start through first response byte (success or mid-stream error). */
  TIME_TO_FIRST_BYTE
}
