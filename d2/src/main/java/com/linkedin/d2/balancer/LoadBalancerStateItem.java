/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.d2.balancer;

import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;

public class LoadBalancerStateItem<P>
{
  private final P    _property;
  private final long _version;
  private final long _lastUpdate;
  private final CanaryDistributionProvider.Distribution _distribution;

  public LoadBalancerStateItem(P property, long version, long lastUpdate)
  {
    this(property, version, lastUpdate, CanaryDistributionProvider.Distribution.STABLE);
  }

  public LoadBalancerStateItem(
      P property,
      long version,
      long lastUpdate,
      CanaryDistributionProvider.Distribution distribution)
  {
    _property = property;
    _version = version;
    _lastUpdate = lastUpdate;
    _distribution = distribution;
  }

  public P getProperty()
  {
    return _property;
  }

  public long getVersion()
  {
    return _version;
  }

  public long getLastUpdate()
  {
    return _lastUpdate;
  }

  /**
   * Get the canary state of the underlying property object.
   */
  public CanaryDistributionProvider.Distribution getDistribution()
  {
    return _distribution;
  }

  @Override
  public String toString()
  {
    return "LoadBalancerStateItem [_lastUpdate=" + _lastUpdate + ", _property="
        + _property + ", _version=" + _version + "]";
  }
}
