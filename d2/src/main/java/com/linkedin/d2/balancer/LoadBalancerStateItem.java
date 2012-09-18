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

public class LoadBalancerStateItem<P>
{
  private final P    _property;
  private final long _version;
  private final long _lastUpdate;

  public LoadBalancerStateItem(P property, long version, long lastUpdate)
  {
    _property = property;
    _version = version;
    _lastUpdate = lastUpdate;
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

  @Override
  public String toString()
  {
    return "LoadBalancerStateItem [_lastUpdate=" + _lastUpdate + ", _property="
        + _property + ", _version=" + _version + "]";
  }
}
