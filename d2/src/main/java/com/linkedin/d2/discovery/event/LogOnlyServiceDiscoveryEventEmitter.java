/*
   Copyright (c) 2022 LinkedIn Corp.

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

package com.linkedin.d2.discovery.event;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link ServiceDiscoveryEventEmitter} which only logs about the events for debugging purpose. NO event is emitted.
 */
public class LogOnlyServiceDiscoveryEventEmitter implements ServiceDiscoveryEventEmitter
{
  private static final Logger _log = LoggerFactory.getLogger(LogOnlyServiceDiscoveryEventEmitter.class);

  @Override
  public void emitSDStatusActiveUpdateIntentEvent(List<String> clustersClaimed, StatusUpdateActionType actionType,
      boolean isNextGen, String tracingId, long timestamp)
  {
    _log.debug(String.format("[LOG ONLY] emit ServiceDiscoveryStatusActiveUpdateIntentEvent: "
            + "{"
            + "clustersClaimed: %s,"
            + " actionType: %s,"
            + " isNextGen: %s,"
            + " tracingId: %s,"
            + " timestamp: %d"
            + "}",
        clustersClaimed, actionType, isNextGen, tracingId, timestamp));
  }

  @Override
  public void emitSDStatusWriteEvent(String cluster, String host, int port, StatusUpdateActionType actionType,
      String serviceRegistry, String serviceRegistryKey, String serviceRegistryValue, Integer serviceRegistryVersion,
      String tracingId, boolean succeeded, long timestamp)
  {
    _log.debug(String.format("[LOG ONLY] emit ServiceDiscoveryStatusWriteEvent for update: "
            + "{"
            + "%s,"
            + " succeeded: %s"
            + "}",
        formatStatusUpdate(cluster, host, port, actionType, serviceRegistry, serviceRegistryKey,
            serviceRegistryValue, serviceRegistryVersion, tracingId, timestamp),
        succeeded));
  }

  @Override
  public void emitSDStatusUpdateReceiptEvent(String cluster, String host, int port, StatusUpdateActionType actionType,
      boolean isNextGen, String serviceRegistry, String serviceRegistryKey, String serviceRegistryValue,
      Integer serviceRegistryVersion, String tracingId, long timestamp)
  {
    _log.debug(String.format("[LOG ONLY] emit ServiceDiscoveryStatusUpdateReceiptEvent for update: "
        + "{"
        + "%s,"
        + " isNextGen: %s"
        + "}",
        formatStatusUpdate(cluster, host, port, actionType, serviceRegistry, serviceRegistryKey,
            serviceRegistryValue, serviceRegistryVersion, tracingId, timestamp),
        isNextGen));
  }

  @Override
  public void emitSDStatusInitialRequestEvent(String cluster, boolean isNextGen, long duration, boolean succeeded)
  {
    _log.debug(String.format("[LOG ONLY] emit ServiceDiscoveryStatusInitialRequestEvent: "
        + "{"
        + "cluster: %s,"
        + " duration: %d,"
        + " isNextGen: %s,"
        + " succeeded: %s"
        + "}",
        cluster, duration, isNextGen, succeeded));
  }

  private String formatStatusUpdate(String cluster, String host, int port, StatusUpdateActionType actionType,
      String serviceRegistry, String serviceRegistryKey, String serviceRegistryValue, Integer serviceRegistryVersion,
      String tracingId, long timestamp)
  {
    return String.format("cluster: %s,"
        + " host: %s,"
        + " port: %d,"
        + " actionType: %s,"
        + " serviceRegistry: %s,"
        + " serviceRegistryKey: %s,"
        + " serviceRegistryValue: %s,"
        + " serviceRegistryVersion: %s,"
        + " tracingId: %s,"
        + " timestamp: %d",
        cluster, host, port, actionType, serviceRegistry, serviceRegistryKey, serviceRegistryValue, serviceRegistryVersion,
        tracingId, timestamp);
  }
}
