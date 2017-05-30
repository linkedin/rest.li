package com.linkedin.d2.balancer.util.healthcheck;

import com.linkedin.r2.message.rest.RestResponse;


/**
 * HealthCheck response validate interface
 */
public interface HealthCheckResponseValidator
{
  /**
   * @return 'true' if the response contents are correct. 'false' otherwise.
   */
  boolean validateResponse(RestResponse response);
}
