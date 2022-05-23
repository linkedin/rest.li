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
package com.linkedin.d2.balancer.clients;

import com.linkedin.d2.balancer.clusterfailout.FailoutConfig;
import java.net.URI;
import java.util.Set;


/**
 * A provider for rewriting of D2 URIs when necessary to reroute away from/around failed out clusters.
 */
public interface FailoutRedirectStrategy {
  /**
   * Rewrites a D2 URI to avoid a failed out cluster.
   *
   * @param failoutConfig the failout configuration for the cluster.
   * @param uri the D2 URI to rewrite.
   *
   * @return A new URI to another destination.
   */
  URI redirect(FailoutConfig failoutConfig, URI uri);
}
