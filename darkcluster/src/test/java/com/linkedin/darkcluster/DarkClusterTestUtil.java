package com.linkedin.darkcluster;

import com.linkedin.d2.DarkClusterConfig;

public class DarkClusterTestUtil
{
  /**
   * This creates the RelativeTrafficMultiplierConfig, which justs consists of
   * setting the multiplier.
   */
  public static DarkClusterConfig createRelativeTrafficMultiplierConfig(float multiplier)
  {
    return new DarkClusterConfig()
      .setMultiplier(multiplier);
  }
}
