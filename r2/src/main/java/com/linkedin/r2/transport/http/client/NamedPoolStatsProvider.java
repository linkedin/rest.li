package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.transport.http.client.PoolStats;

import java.util.Map;

/**
 * @author Ang Xu
 * @version $Revision$
 */
public interface NamedPoolStatsProvider
{
  /**
   * Get statistics from each underlying pool inside pool stats provider. The map keys represent pool names.
   * The values are the corresponding {@link com.linkedin.r2.transport.http.client.PoolStats} objects.
   * @return A map of pool names and statistics.
   */
  public Map<String, PoolStats> getPoolStats();

  /**
   * Get the name of pool stats provider
   * @return Name of the pool stats provider
   */
  public String getName();
}
