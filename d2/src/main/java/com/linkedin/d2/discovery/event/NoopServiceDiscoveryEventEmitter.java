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


/**
 * Placeholder implementation of {@link ServiceDiscoveryEventEmitter} which has no operation.
 */
public class NoopServiceDiscoveryEventEmitter implements ServiceDiscoveryEventEmitter {
  @Override
  public void emitSDStatusActiveUpdateIntentEvent(List<String> clustersClaimed, StatusUpdateActionType actionType,
      boolean isNextGen, String tracingId, long timestamp) {
  }

  @Override
  public void emitSDStatusWriteEvent(String cluster, String host, int port, StatusUpdateActionType actionType, String serviceRegistry,
      String serviceRegistryKey, String serviceRegistryValue, Integer serviceRegistryVersion, String tracingId,
      boolean succeeded, long timestamp) {
  }

  @Override
  public void emitSDStatusUpdateReceiptEvent(String cluster, String host, int port, StatusUpdateActionType actionType, boolean isNextGen,
      String serviceRegistry, String serviceRegistryKey, String serviceRegistryValue, Integer serviceRegistryVersion,
      String tracingId, long timestamp) {
  }

  @Override
  public void emitSDStatusInitialRequestEvent(String cluster, boolean isNextGen, long duration, boolean succeeded) {
  }
}
