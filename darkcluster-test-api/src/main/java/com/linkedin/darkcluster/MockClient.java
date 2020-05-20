/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.darkcluster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.Client;

import org.apache.commons.lang.NotImplementedException;

/**
 * MockClient that allows failing requests and recording url authority
 */
public class MockClient implements Client
{
  private final boolean _failRequests;
  public final Map<String, AtomicInteger> requestAuthorityMap = new ConcurrentHashMap<>();

  public MockClient(boolean failRequests)
  {
    _failRequests = failRequests;
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request)
  {
    throw new NotImplementedException();
  }

  @Override
  public Future<RestResponse> restRequest(RestRequest request, RequestContext requestContext)
  {
    throw new NotImplementedException();
  }

  @Override
  public void restRequest(RestRequest request, Callback<RestResponse> callback)
  {
    restRequest(request, new RequestContext(), callback);
  }

  @Override
  public void restRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
  {
    if (request != null && request.getURI() != null)
    {
      String authority = request.getURI().getAuthority();
      if (authority != null)
      {
        // only store the authority if the requestURI has one.
        if (!requestAuthorityMap.containsKey(authority))
        {
          requestAuthorityMap.putIfAbsent(authority, new AtomicInteger());
        }
        requestAuthorityMap.get(authority).incrementAndGet();
      }
    }

    if (_failRequests)
    {
      callback.onError(new RuntimeException("test"));
    }
    else
    {
      callback.onSuccess(new RestResponseBuilder().build());
    }
  }

  @Override
  public void shutdown(Callback<None> callback)
  {

  }
}
