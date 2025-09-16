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

import com.linkedin.d2.balancer.LoadBalancerServer;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.discovery.stores.PropertyStoreException;

import java.io.IOException;
import java.util.Map;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public interface ZooKeeperAnnouncerJmxMXBean
{

  void reset() throws PropertyStoreException;

  void markUp() throws PropertyStoreException;

  void markDown() throws PropertyStoreException;

  /**
   * Change the weight of an existing host.
   *
   * @param doNotSlowStart Flag to let clients know if slow start should be avoided for a host.
   */
  void changeWeight(boolean doNotSlowStart) throws PropertyStoreException;

  /**
   * Set {@link com.linkedin.d2.balancer.properties.PropertyKeys#DO_NOT_LOAD_BALANCE} for a given uri.
   *
   * @param doNotLoadBalance Flag to let clients know if load balancing should be disabled for a host.
   */
  void setDoNotLoadBalance(boolean doNotLoadBalance) throws PropertyStoreException;

  String getCluster();

  void setCluster(String cluster);

  String getUri();

  void setUri(String uri);

  void setWeight(double weight);

  Map<Integer, PartitionData> getPartitionData();

  void setPartitionDataUsingJson(String partitionDataJson)
      throws IOException;

  void setPartitionData(Map<Integer, PartitionData> partitionData);

  boolean isMarkUpFailed();

  /**
   * @return true if the announcer has completed sending a markup intent. NOTE THAT a mark-up intent sent does NOT mean the
   * announcement status on service discovery registry is up. Service discovery registry may further process the host
   * and determine its status. Check on service discovery registry for the final status.
   */
  boolean isMarkUpIntentSent();

  /**
   * @return true if the announcer has completed sending a dark warmup cluster markup intent.
   */
  boolean isDarkWarmupMarkUpIntentSent();

  /**
   * @return the times that the max weight has been breached.
   */
  int getMaxWeightBreachedCount();

  /**
   *
   * @return the times that the max number of decimal places on weight has been breached.
   */
  int getWeightDecimalPlacesBreachedCount();

  /**
   * @return the server announce mode corresponding to {@link LoadBalancerServer#getAnnounceMode()}
   */
  int getServerAnnounceMode();

  /**
   * @return the announcement status corresponding to
   * {@link com.linkedin.d2.balancer.servers.ReadinessStatusManager.AnnouncerStatus.AnnouncementStatus}
   */
  int getAnnouncementStatus();
}
