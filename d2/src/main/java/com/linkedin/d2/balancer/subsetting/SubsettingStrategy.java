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
  public static final int DEFAULT_CLUSTER_SUBSET_SIZE = -1;

  Map<T, Double> getWeightedSubset(Map<T, Double> pointsMap);
}
