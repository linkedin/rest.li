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

package com.linkedin.d2.balancer.subsetting;

import java.util.Objects;


/**
 * The metadata required by the deterministic subsetting strategy.
 */
public final class DeterministicSubsettingMetadata
{
  private final int _instanceId;
  private final int _totalInstanceCount;

  public DeterministicSubsettingMetadata(int instanceId, int totalInstanceCount)
  {
    _instanceId = instanceId;
    _totalInstanceCount = totalInstanceCount;
  }

  /**
   * Get the ID of current client instance. In the peer cluster, the IDs should start from 0 and be contiguous
   */
  public int getInstanceId()
  {
    return _instanceId;
  }

  /**
   * Get the total number of instances in the peer client cluster.
   */
  public int getTotalInstanceCount()
  {
    return _totalInstanceCount;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeterministicSubsettingMetadata that = (DeterministicSubsettingMetadata) o;
    return _instanceId == that._instanceId && _totalInstanceCount == that._totalInstanceCount;
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(_instanceId, _totalInstanceCount);
  }

  @Override
  public String toString()
  {
    return "DeterministicSubsettingMetadata{" + "_instanceId=" + _instanceId + ", _totalInstanceCount="
        + _totalInstanceCount + '}';
  }
}
