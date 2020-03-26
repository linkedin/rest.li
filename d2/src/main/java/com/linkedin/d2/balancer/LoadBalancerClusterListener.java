/*
   Copyright (c) 2020 LinkedIn Corp.

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

/**
 * Cluster Listeners will get notified for ALL cluster changes. It is up to the users
 * to filter/act upon just the clusters they are interested in. This is done this way
 * because it is very likely that users of the listener will be interested in more than
 * one cluster, and so they will only need one listener with this approach, even if they add or
 * remove clusters that they are interested in.
 */
public interface LoadBalancerClusterListener
{
  /**
   * Take appropriate action if interested in this cluster, otherwise, ignore.
   */
  void onClusterAdded(String clusterName);

  /**
   * Take appropriate action if interested in this cluster, otherwise, ignore.
   */
  void onClusterRemoved(String clusterName);
}
