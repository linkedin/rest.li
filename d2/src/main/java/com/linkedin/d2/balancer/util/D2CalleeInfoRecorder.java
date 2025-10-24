package com.linkedin.d2.balancer.util;

import javax.annotation.Nonnull;


/**
 * Records the callee service information and periodically sends out the recorded information to a persistence medium.
 */
public interface D2CalleeInfoRecorder {
  /**
   * Starts the recorder to send periodic updates.
   */
  void start();
  /**
   * Records a callee service name.
   * @param serviceName the callee service name to record
   */
  void record(String serviceName);

  @Nonnull
  String getAppName();

  @Nonnull
  String getAppInstanceID();

  @Nonnull
  String getScope();

  /**
   * Stops periodic updates and shuts down the recorder.
   */
  void shutdown();
}