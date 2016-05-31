package com.linkedin.d2.balancer.util.healthcheck;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;


/**
 * HealthCheck defines the interface for client health checking.
 */

public interface HealthCheck
{
  void checkHealth(Callback<None> callback);
}

