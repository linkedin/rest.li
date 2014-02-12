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

/**
 * $Id: $
 */

package com.linkedin.d2.jmx;


/**
 * @author David Hoa
 * @version $Revision: $
 */

public interface DegraderLoadBalancerStrategyV2JmxMBean
{
  /**
   *
   * @return the current overrideClusterDropRate
   */
  double getOverrideClusterDropRate();

  /**
   *
   * @return String representation of this strategy
   */
  String toString();

  /**
   *
   * @return number of total points in hash ring
   */
  int getTotalPointsInHashRing();

  /**
   *
   * @return the hash ring points mapping between URI --> #points
   */
  String getPointsMap();

  /**
   * This method assumes unhealthy clients are clients whose hash ring points are below
   * the default value for healthy client. (This value is points_per_weight * weight of the client)
   * We assume that the weight is defaulted to 1.
   *
   * @return String representation of pair of unhealthy client's URI : # of points / # points for perfect health
   */
  String getUnhealthyClientsPoints();

}
