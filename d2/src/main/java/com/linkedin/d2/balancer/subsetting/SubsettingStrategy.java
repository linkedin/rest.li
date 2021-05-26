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

import java.util.Map;


/**
 * Picks a subset from a collection of items. Items in the subset can be picked with
 * different probabilities, proportional to their weights.
 */
public interface SubsettingStrategy<T>
{
  boolean DEFAULT_ENABLE_CLUSTER_SUBSETTING = false;
  int DEFAULT_CLUSTER_SUBSET_SIZE = -1;

  /**
   * Get the version of peer cluster
   */
  long getPeerClusterVersion();

  /**
   * Picks a subset from a collection of items
   *
   * @param weightMap Maps each item to its weight on a scale of 0.0 to 1.0.
   * @return A subset that maps each item to its weight on a scale of 0.0 to 1.0.
   */
  Map<T, Double> getWeightedSubset(Map<T, Double> weightMap);
}
