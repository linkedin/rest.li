/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

/**
 * $Id: $
 */

package com.linkedin.d2.contrib;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * RouteLookupClient can be used to send normal RestRequests as well as request that require a
 * RouteLookup. A RouteLookup can do any action, as long as they implement the interface.
 * Currently, only RestRequests are supported in RouteLookupClient when using a RouteLookup.
 *
 * @author David Hoa
 * @version $Revision: $
 */

public class RouteLookupClient implements RoutingAwareClient
{

  // this is the normal R2 Client that can route ordinary requests
  private final Client _client;

  // this is  the async user provided action that will be executed first to get a d2 service name
  // back in the callback, which will then be used to execute the request.
  private final RouteLookup _routeLookup;

  // The routing group that this client is sending requests from.
  private final String _routingGroup;

  public RouteLookupClient(Client client, RouteLookup routeLookup, String routingGroup)
  {
    _client = client;
    _routeLookup = routeLookup;
    _routingGroup = routingGroup;
  }

  @Override
  public void restRequest(RestRequest request, Callback<RestResponse> callback, String routeKey)
  {
    this.restRequest(request, new RequestContext(), callback, routeKey);
  }

  @Override
  public void restRequest(final RestRequest request, final RequestContext requestContext,
                          final Callback<RestResponse> callback, String routeKey)
  {
    String originalServiceName = request.getURI().getAuthority();
    Callback<String> routeLookupCallback = new Callback<String>() {

      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }

      @Override
      public void onSuccess(String resultServiceName)
      {
        RestRequest resultRequest = createNewRequestWithNewServiceName(request, resultServiceName);
        _client.restRequest(resultRequest, requestContext, callback);
      }
    };

    _routeLookup.run(originalServiceName, _routingGroup, routeKey, routeLookupCallback);
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request, String routeKey)
  {
    return this.restRequest(request, new RequestContext(), routeKey);
  }

  @Override
  public Future<RestResponse> restRequest(final RestRequest request, final RequestContext requestContext,
                                          String routekey)
  {
    final FutureCallback<String> futureCallback = new FutureCallback<String>();
    String originalServiceName = LoadBalancerUtil.getServiceNameFromUri(request.getURI());
    String resultServiceName;
    _routeLookup.run(originalServiceName, _routingGroup, routekey, futureCallback);

    try
    {
      resultServiceName = futureCallback.get();
    }
    catch (InterruptedException e)
    {
      throw new RuntimeException(e);
    }
    catch (ExecutionException e)
    {
      throw new RuntimeException(e);
    }

    RestRequest resultRequest = createNewRequestWithNewServiceName(request, resultServiceName);
    Future<RestResponse> resultFuture = _client.restRequest(resultRequest, requestContext);
    return resultFuture;
  }

  /**
   * Creates a new request identical to the original request, but changes the d2 service name to
   * the one passed in.
   * @param origRequest original request to copy
   * @param newServiceName the new service name to rewrite the request with
   * @return a new RestRequest
   */
  private static RestRequest createNewRequestWithNewServiceName(RestRequest origRequest, String newServiceName)
  {
    RestRequestBuilder modifiedBuilder = origRequest.builder();
    URI originalURI = modifiedBuilder.getURI();
    if (!("d2".equals(originalURI.getScheme())))
    {
      throw new IllegalArgumentException("Unsupported scheme in URI: " + originalURI);
    }
    UriBuilder modifiedUriBuilder = UriBuilder.fromUri(originalURI);
    modifiedUriBuilder.host(newServiceName);
    URI resultUri = modifiedUriBuilder.build();
    modifiedBuilder.setURI(resultUri);
    RestRequest resultRequest = modifiedBuilder.build();
    return resultRequest;
  }
}
