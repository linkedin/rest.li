package com.linkedin.d2.balancer.strategies;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import java.net.URI;
import java.util.Map;
import javax.annotation.Nullable;


/**
 *
 */
public interface ClientSelector
{
  /**
   * @param request
   * @param requestContext
   * @param ring
   * @param trackerClients
   * @return
   */
  @Nullable
  TrackerClient getTrackerClient(Request request, RequestContext requestContext, Ring<URI> ring,
      Map<URI, TrackerClient> trackerClients);

  HashFunction<Request> getRequestHashFunction();
}
