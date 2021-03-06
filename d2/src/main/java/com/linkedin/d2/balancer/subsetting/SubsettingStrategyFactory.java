/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.d2.balancer.subsetting;

import com.linkedin.d2.balancer.properties.ServiceProperties;
import java.net.URI;


public interface SubsettingStrategyFactory
{
  /**
   * get retrieves the {@link SubsettingStrategy} corresponding to the serviceName and partition Id.
   * @return {@link SubsettingStrategy} or {@code null} if minClusterSubsetSize is less than or equal to 0.
   */
   SubsettingStrategy<URI> get(String serviceName, int minClusterSubsetSize, int partitionId);
}
