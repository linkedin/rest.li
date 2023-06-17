/*
   Copyright (c) 2023 LinkedIn Corp.

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

package com.linkedin.d2.balancer.dualread;

/**
 * A provider that dynamically determines which read mode to use in {@link DualReadLoadBalancer}
 */
public interface DualReadModeProvider
{
  enum DualReadMode
  {
    OLD_LB_ONLY,
    DUAL_READ,
    NEW_LB_ONLY
  }

  /**
   * @return The global read mode that applies to all D2 services if service-level read mode
   *         is not configured
   */
  DualReadMode getDualReadMode();

  /**
   * @return The service-level read mode for the given D2 service
   */
  default DualReadMode getDualReadMode(String d2ServiceName) {
    return getDualReadMode();
  };
}
