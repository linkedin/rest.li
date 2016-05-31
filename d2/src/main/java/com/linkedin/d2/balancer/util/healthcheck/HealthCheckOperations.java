package com.linkedin.d2.balancer.util.healthcheck;

/**
 * HealthCheck allows to define/update heath checking method by
 * using different health checking requests or augmenting the response
 * validating method
 */

public class HealthCheckOperations extends HealthCheckRequestFactory
{
  public HealthCheckResponseValidator buildResponseValidate()
  {
    return response -> true;
  }
}
