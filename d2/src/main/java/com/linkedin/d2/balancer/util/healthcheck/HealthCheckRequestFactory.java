package com.linkedin.d2.balancer.util.healthcheck;

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * HealthCheckRequestFactory provides parameters for health checking requests.
 */
public class HealthCheckRequestFactory
{
  /**
   * @param method of the HttpRequest ({@link com.linkedin.r2.message.rest.RestMethod})
   * @param uri full URI of the request
   */
  public RestRequest buildRestRequest(String method, URI uri)
  {
    RestRequestBuilder requestBuilder = new RestRequestBuilder(uri);

    requestBuilder.setMethod(method);
    requestBuilder.setHeader("X-RestLi-Protocol-Version", "2.0.0");

    return requestBuilder.build();
  }

  /**
   * @deprecated Use {@link #buildRequestContextSupplier()} instead.
   */
  @Deprecated
  public RequestContext buildRequestContext()
  {
    return new RequestContext();
  }

  /**
   * @return RequestContext supplier.
   */
  public Supplier<RequestContext> buildRequestContextSupplier()
  {
    return RequestContext::new;
  }

  /**
   * @deprecated Use {@link #buildWireAttributesSupplier()} instead.
   */
  @Deprecated
  public Map<String, String> buildWireAttributes()
  {
    return new HashMap<>();
  }

  /**
   * @return Wire attributes supplier.
   */
  public Supplier<Map<String, String>> buildWireAttributesSupplier()
  {
    return HashMap::new;
  }
}
