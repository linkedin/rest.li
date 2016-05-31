package com.linkedin.d2.balancer.util.healthcheck;

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;


/**
 * HealthCheckRequestFactory provides parameters for health checking requests.
 */


public class HealthCheckRequestFactory
{
  public RestRequest buildRestRequest(String method, URI uri)
  {
    RestRequestBuilder requestBuilder = new RestRequestBuilder(uri);

    requestBuilder.setMethod(method);

    requestBuilder.setHeader("X-RestLi-Protocol-Version", "2.0.0");

    return requestBuilder.build();
  }

  public RequestContext buildRequestContext()
  {
    return new RequestContext();
  }

  public Map<String, String> buildWireAttributes()
  {
    return new HashMap<>();
  }
}
