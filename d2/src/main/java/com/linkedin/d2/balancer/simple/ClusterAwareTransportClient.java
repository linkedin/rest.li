/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.d2.balancer.simple;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionValidator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


/**
 * A {@link TransportClient} wrapper that is {@link ClusterProperties} aware
 *
 * This client can check the updated ClusterProperty values before sending the request
 *
 * @author cxu
 */
public class ClusterAwareTransportClient implements TransportClient
{
  private final String _clusterName;
  private final TransportClient _wrappedClient;
  private final Map<String, ClusterInfoItem> _clusterInfo;
  private final SslSessionValidatorFactory _sslSessionValidatorFactory;
  private final AtomicLong _cachedClusterVersion;

  private volatile SslSessionValidator _cachedSslSessionValidator;

  public ClusterAwareTransportClient(String clusterName, TransportClient client, Map<String, ClusterInfoItem> clusterInfo,
      SslSessionValidatorFactory sessionValidatorFactory)
  {
    _clusterName = clusterName;
    _wrappedClient = client;
    _clusterInfo = clusterInfo;
    _sslSessionValidatorFactory = sessionValidatorFactory;

    // No need to construct SslSessionValidator in each request.
    _cachedClusterVersion = new AtomicLong(-1);
    _cachedSslSessionValidator = null;
  }

  @Override
  public void restRequest(RestRequest request,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          TransportCallback<RestResponse> callback)
  {
    updateRequestContext(requestContext);
    getWrappedClient().restRequest(request, requestContext, wireAttrs, callback);
  }

  @Override
  public void restRequestStreamResponse(RestRequest request, RequestContext requestContext,
      Map<String, String> wireAttrs, TransportCallback<StreamResponse> callback) {
    updateRequestContext(requestContext);
    getWrappedClient().restRequestStreamResponse(request, requestContext, wireAttrs, callback);
  }

  @Override
  public void streamRequest(StreamRequest request,
                          RequestContext requestContext,
                          Map<String, String> wireAttrs,
                          TransportCallback<StreamResponse> callback)
  {
    updateRequestContext(requestContext);
    getWrappedClient().streamRequest(request, requestContext, wireAttrs, callback);
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    getWrappedClient().shutdown(callback);
  }

  private void updateRequestContext(RequestContext requestContext)
  {
    SslSessionValidator sslSessionValidator = getValidator();
    if (sslSessionValidator != null)
    {
      requestContext.putLocalAttr(R2Constants.REQUESTED_SSL_SESSION_VALIDATOR, sslSessionValidator);
    }
  }

  private TransportClient getWrappedClient()
  {
    return _wrappedClient;
  }

  /**
   * Since the validator has validationStrings build in, the only time it needs to update is when the validationStrings
   * change. So we always use the cached validator unless the clusterProperties change. This avoid repeatedly creating
   * new sslSessionValidator object.
   */
  private SslSessionValidator getValidator()
  {
    ClusterInfoItem clusterInfoItem = _clusterInfo.get(_clusterName);
    if (clusterInfoItem == null || clusterInfoItem.getClusterPropertiesItem() == null)
    {
      return null;
    }
    long cachedVersion = _cachedClusterVersion.get();
    long currentVersion = clusterInfoItem.getClusterPropertiesItem().getVersion();
    if ( currentVersion > cachedVersion &&
        _cachedClusterVersion.updateAndGet(prev -> clusterInfoItem.getClusterPropertiesItem().getVersion()) > cachedVersion)
    {
      ClusterProperties clusterProperties = clusterInfoItem.getClusterPropertiesItem().getProperty();
      if (clusterProperties != null)
      {
        _cachedSslSessionValidator = _sslSessionValidatorFactory.getSessionValidator(clusterProperties.getSslSessionValidationStrings());
      }
    }
    return _cachedSslSessionValidator;
  }
}
