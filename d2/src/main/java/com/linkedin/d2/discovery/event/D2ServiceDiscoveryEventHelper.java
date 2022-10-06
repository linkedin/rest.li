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

/**
 * D2-specific helper for emitting Service Discovery Status related events by calling the general
 * {@link ServiceDiscoveryEventEmitter}.
 */
public interface D2ServiceDiscoveryEventHelper {
  //---- d2 server-side events ----//

  /**
   * To emit ServiceDiscoveryStatusActiveUpdateIntentEvent and ServiceDiscoveryStatusWriteEvent.
   * @param cluster cluster name.
   * @param isMarkUp true for markUp, o.w for markDown.
   * @param succeeded true if the write succeeded, o.w failed.
   * @param startAt when the update intent is initiated (start time of the markUp/markDown).
   */
  void emitSDStatusActiveUpdateIntentAndWriteEvents(String cluster, boolean isMarkUp, boolean succeeded, long startAt);

  //---- d2 client-side events ----//

  /**
   * To emit ServiceDiscoveryStatusUpdateReceiptEvent.
   * @param cluster cluster name.
   * @param isMarkUp true for markUp, o.w for markDown.
   * @param nodePath path of the uri ephemeral znode.
   * @param nodeData data in the uri ephemeral znode.
   * @param timestamp when the update is received.
   */
  void emitSDStatusUpdateReceiptEvent(String cluster, String host, int port, boolean isMarkUp, String zkConnectString, String nodePath, String nodeData, long timestamp);

  /**
   * To emit ServiceDiscoveryStatusInitialRequestEvent, when a new service discovery request is sent for a cache miss,
   * (the first time of getting uris for a cluster).
   * @param cluster cluster name.
   * @param duration duration the request took.
   * @param succeeded true if the request succeeded, o.w failed.
   */
  void emitSDStatusInitialRequestEvent(String cluster, long duration, boolean succeeded);
}
