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

package com.linkedin.restli.client;


import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.HostSet;
import com.linkedin.r2.message.RequestContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Only query-all type requests for partitioned services should call this API.
 * Nevertheless, it will still do the job if caller uses it for un-partitioned service
 * The possible benefit is that the caller is not required to know whether a service is partitioned or not
 * @author Zhenkai Zhu
 * @author Xialin Zhu
 * @version $Revision: $
 */
public class AllPartitionsRequestBuilder <T>
{
  private final String D2_URI_PREFIX = "d2://";
  private final KeyMapper _mapper;

  public AllPartitionsRequestBuilder(KeyMapper mapper)
  {
    _mapper = mapper;
  }

  /**
   * A convenience function for caller to issue search request with one call.
   * If finer-grain control is required, users should call buildRequestContexts instead and send requests by themselves
   *
   * @param client - the RestClient to use
   * @param request - the query-all request
   * @param requestContext - the original request context
   * @param callback - callback to be used for each request
   * @return the partition cover which informs the caller the RequestContexts, the total number of partitions and the available partition number
   * @throws ServiceUnavailableException
   */
  public HostSet sendRequests(RestClient client, Request<T> request, RequestContext requestContext, Callback<Response<T>> callback)
      throws ServiceUnavailableException
  {
    URI serviceUri;
    try
    {
      serviceUri = new URI(D2_URI_PREFIX + request.getServiceName());
    }
    catch(URISyntaxException e)
    {
      throw new IllegalArgumentException(e);
    }

    final Collection<RequestContext> queryAllRequestContext = new ArrayList<RequestContext>();
    final HostSet uriResult = _mapper.getAllPartitionsMultipleHosts(serviceUri, 1);
    for (URI targetHost : uriResult.getAllHosts())
    {
      RequestContext context = requestContext.clone();
      KeyMapper.TargetHostHints.setRequestContextTargetHost(context, targetHost);
      queryAllRequestContext.add(context);
      client.sendRequest(request, context, callback);
    }

    return uriResult;
  }
}
