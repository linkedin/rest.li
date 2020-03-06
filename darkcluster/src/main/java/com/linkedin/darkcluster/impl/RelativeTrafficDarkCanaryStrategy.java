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

package com.linkedin.darkcluster.impl;

import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.balancer.util.ClusterInfoProvider;

public class RelativeTrafficDarkCanaryStrategy extends AbstractDarkClusterStrategyImpl
{
  public RelativeTrafficDarkCanaryStrategy(ClusterInfoProvider clusterInfoProvider, String clusterName,
                                           BaseDarkClusterDispatcher baseDarkClusterDispatcher)
  {
    super(clusterInfoProvider, clusterName, baseDarkClusterDispatcher);
  }

  @Override
  public int getNumDuplicateRequests(String darkClusterName, String originalClusterName, DarkClusterConfig darkClusterConfig, float randomNum)
  {
    //        float multiplier = darkClusterConfigEntry.getValue().getMultiplier();
    //        float multiplierDecimalPart = multiplier % 1;
    //        int numRequestDuplicates = randomNum < multiplierDecimalPart ? (int) multiplier + 1 : (int) multiplier;
    // Not yet implemented
    return 1;
  }
}
