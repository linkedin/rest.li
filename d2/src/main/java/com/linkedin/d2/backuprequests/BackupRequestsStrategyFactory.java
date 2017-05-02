/*
   Copyright (c) 2017 LinkedIn Corp.

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
package com.linkedin.d2.backuprequests;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.d2.BoundedCostBackupRequests;
import com.linkedin.d2.balancer.properties.PropertyKeys;


/**
 * This class creates an instance of TrackingBackupRequestsStrategy from configuration.
 * <p>
 * See BackupRequestsConfiguration.pdsc for schema of configuration.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 */
class BackupRequestsStrategyFactory
{

  private static final Logger LOG = LoggerFactory.getLogger(BackupRequestsStrategyFactory.class);

  //object used to read default values directly from schema
  private static final BoundedCostBackupRequests BCBR = new BoundedCostBackupRequests();

  private BackupRequestsStrategyFactory()
  {
  }

  /**
   * Creates an instance of TrackingBackupRequestsStrategy from configuration.
   * @param backupRequestsConfiguration configuration, must not be null
   * @return new instance of TrackingBackupRequestsStrategy or null if it could not be created
   */
  public static TrackingBackupRequestsStrategy create(Map<String, Object> backupRequestsConfiguration)
  {
    try
    {
      return new TrackingBackupRequestsStrategy(tryCreate(backupRequestsConfiguration));
    } catch (Exception e)
    {
      LOG.error("Failed to create BackupRequestsStrategy from configuration: " + backupRequestsConfiguration, e);
    }
    return null;
  }

  static BackupRequestsStrategy tryCreate(Map<String, Object> backupRequestsConfiguration)
  {
    Map<String, Object> strategy = mapGet(backupRequestsConfiguration, PropertyKeys.STRATEGY);
    if (strategy.containsKey(BCBR.getClass().getName()))
    {
      return tryCreateBoundedCost(mapGet(strategy, BCBR.getClass().getName()));
    } else
    {
      throw new RuntimeException("Unrecognized type of BackupRequestsStrategy: " + strategy);
    }
  }

  static BoundedCostBackupRequestsStrategy tryCreateBoundedCost(Map<String, Object> properties)
  {
    int cost = mapGet(properties, PropertyKeys.COST);
    int historyLength = properties.containsKey(PropertyKeys.HISTORY_LENGTH)
        ? mapGet(properties, PropertyKeys.HISTORY_LENGTH) : BCBR.getHistoryLength();
    int requiredHistoryLength = properties.containsKey(PropertyKeys.REQUIRED_HISTORY_LENGTH)
        ? mapGet(properties, PropertyKeys.REQUIRED_HISTORY_LENGTH) : BCBR.getRequiredHistoryLength();
    int maxBurst = properties.containsKey(PropertyKeys.MAX_BURST) ? mapGet(properties, PropertyKeys.MAX_BURST)
        : BCBR.getMaxBurst();
    int minBackupDelayMs = properties.containsKey(PropertyKeys.MIN_BACKUP_DELAY_MS)
        ? mapGet(properties, PropertyKeys.MIN_BACKUP_DELAY_MS) : BCBR.getMinBackupDelayMs();
    return new BoundedCostBackupRequestsStrategy(cost, maxBurst, historyLength, requiredHistoryLength,
        minBackupDelayMs);
  }

  @SuppressWarnings("unchecked")
  private static <T> T mapGet(Map<String, Object> map, String key)
  {
    return (T) map.get(key);
  }
}
