package com.linkedin.d2.balancer.subsetting;

/**
 * Provides deterministic subsetting strategy with the peer cluster metadata needed
 */
public interface DeterministicSubsettingMetadataProvider
{
  DeterministicSubsettingMetadata getSubsettingMetadata();
}
