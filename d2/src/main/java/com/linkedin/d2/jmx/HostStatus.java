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
package com.linkedin.d2.jmx;

/** Host status for {@code D2.RelativeLb.DegradedHostsCount} gauge attributes. */
public enum HostStatus
{
  /**
   * Hosts whose health score has been reduced due to high latency or error rate. Still receive
   * traffic at a reduced weight; not yet quarantined.
   */
  UNHEALTHY,

  /**
   * Hosts currently in quarantine. They are not receiving production traffic and are pending
   * health-check recovery before being re-admitted.
   */
  QUARANTINED
}
