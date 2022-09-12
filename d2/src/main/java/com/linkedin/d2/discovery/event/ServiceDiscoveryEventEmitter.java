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
 * Emitter for Service Discovery Status related events, to be called for both D2 and next-gen systems.
 * (Event schemas at: avro-schemas/avro-schemas/avro-schemas-tracking/schemas/monitoring/events/serviceDiscovery).
 */
public interface ServiceDiscoveryEventEmitter {

  /**
   * To emit ServiceDiscoveryStatusActiveUpdateIntentEvent, when the server instance actively intends to update its status.
   * @param clustersClaimed a list of clusters that the instance claimed to apply the update to.
   * @param actionType action type of updating the status.
   * @param isNextGen true for next-gen system, o.w for D2.
   * @param tracingId A unique tracing id to be used in joining other related events for tracing end-to-end status update related behaviors.
   *                  In current D2 system, it is the path of the Zookeeper ephemeral znode, e.g: /d2/uris/ClusterA/hostA-1234, in case of failure,
   *                  it will use a FAILURE path suffix: /d2/uris/ClusterA/hostA-FAILURE. In next-gen, it’s a UUID.
   * @param timestamp when the update intent is initiated. In D2, it's the start time of markUp/markDown. In next-gen, it's when an active status report is sent.
   */
  void emitSDStatusActiveUpdateIntentEvent(List<String> clustersClaimed, StatusUpdateActionType actionType, boolean isNextGen, String tracingId, long timestamp);

  /**
   * NOTE: ONLY for D2. In next-gen, writes will never happen on the server app, instead it will be on Service Registry Writer, so write event will never be
   * emitted from the server app.
   * To emit ServiceDiscoveryStatusWriteEvent, to trace a write request to Service Registry.
   * @param cluster cluster name of the status update.
   * @param host host name of the URI in the update.
   * @param port port number of the URI in the update.
   * @param actionType action type of updating the status.
   * @param serviceRegistry ID/URI that identifies the Service Registry (for ZK, it's the connect string).
   * @param serviceRegistryKey key of the service discovery status data stored in Service Registry (for mark-down, it's the key that was deleted).
   *                           Note: In current D2 system, this is the path of the created/deleted ephemeral znode.
   * @param serviceRegistryValue value of the service discovery status data stored in Service Registry (for mark-down, it's the data that was deleted).
   *                             Note: In current D2 system, this is UriProperties.
   * @param serviceRegistryVersion version of the status data in Service Registry. For writes, it will be the new data version after the write (for mark-down,
   *                               it's the data version that was deleted). Null if the write failed.
   *                               Note: In current D2 system, this is the version of the ephemeral znode, which is 0 for a new node, and increments for every data update.
   *                               But current D2 system does data update by removing the old node and recreating a new node, so the version is always 0.
   * @param tracingId same as the tracing id that was generated when the intent of this status update was made in ServiceDiscoveryStatusActiveUpdateIntentEvent.
   * @param succeeded true if the request succeeded, o.w failed.
   * @param timestamp when the write request is complete.
   */
  void emitSDStatusWriteEvent(String cluster, String host, int port, StatusUpdateActionType actionType, String serviceRegistry, String serviceRegistryKey,
      String serviceRegistryValue, Integer serviceRegistryVersion, String tracingId, boolean succeeded, long timestamp);

  /**
   * To emit ServiceDiscoveryStatusUpdateReceiptEvent, to trace when a status update is received by a subscriber.
   * NOTE: In current D2 system, this event is emitted from a client service instance. In next-gen, it could be from a client service instance,
   *       sidecar proxy, or Service Registry Observer.
   * @param cluster cluster name of the status update.
   * @param host host name of the URI in the update.
   * @param port port number of the URI in the update.
   * @param actionType action type of updating the status.
   * @param isNextGen true for next-gen system, o.w for D2.
   * @param serviceRegistry same as in method emitSDStatusWriteEvent.
   * @param serviceRegistryKey same as in method emitSDStatusWriteEvent.
   * @param serviceRegistryValue same as in method emitSDStatusWriteEvent.
   * @param serviceRegistryVersion same as in method emitSDStatusWriteEvent.
   * @param tracingId same as the tracing id that was generated when the intent of this status update was made in ServiceDiscoveryStatusActiveUpdateIntentEvent
   *                  or future next-gen HealthCheckStatusUpdateIntentEvent.
   * @param timestamp when the update is received.
   */
  void emitSDStatusUpdateReceiptEvent(String cluster, String host, int port, StatusUpdateActionType actionType, boolean isNextGen, String serviceRegistry,
      String serviceRegistryKey, String serviceRegistryValue, Integer serviceRegistryVersion, String tracingId, long timestamp);

  /**
   * To emit ServiceDiscoveryStatusInitialRequestEvent, when a new service discovery request is sent for a cache miss,
   * (the first time of getting uris for a cluster).
   * @param cluster cluster name.
   * @param isNextGen true for next-gen system, o.w for D2.
   * @param duration duration the request took.
   * @param succeeded true if the request succeeded, o.w failed.
   */
  void emitSDStatusInitialRequestEvent(String cluster, boolean isNextGen, long duration, boolean succeeded);

  // Action type of updating an app status.
  enum StatusUpdateActionType {
    // mark the app instance as ready to serve traffic (all infra and app-custom components which relate to service discovery are ready).
    MARK_READY,
    // Mark the app instance as running (still reachable/discoverable) but doesn't intend to take traffic (clients could still try with them for corner cases,
    // like when no ready ones are there). Note: Current D2 system doesn't save apps of this state to ZK, so this action won't be used in D2.
    MARK_RUNNING,
    // Mark the app instance as shut/shutting down or unreachable/unresponsive, could because of undeployment or outage incidences.
    MARK_DOWN,
    // Update the app status data, such as instance properties for custom routing, latencies, etc. Note: Current D2 system does data
    // update by removing the existing node and creating a new one, so this action won't be used in D2.
    UPDATE_DATA
  }
}
