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
