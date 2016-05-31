package com.linkedin.d2.balancer.util.healthcheck;

import com.linkedin.r2.message.rest.RestResponse;


/**
 * HealthCheck response validate interface
 * @return: 'true' if the response contents are correct. 'false' otherwise.
 */

public interface HealthCheckResponseValidator
{
  boolean validateResponse(RestResponse response);
}
