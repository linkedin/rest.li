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

package com.linkedin.d2.balancer;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.http.client.HttpClientFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


/**
 * Build a {@link D2Client} with basic ZooKeeper setup to connect D2 protocol.
 * The client could be further wrapped by other client classes.
 */
public class D2ClientBuilder
{
  /**
   * @return {@link D2Client} that is not started yet. Call start(Callback) to start it.
   */
  public D2Client build()
  {
    final Map<String, TransportClientFactory> transportClientFactories = (_config.clientFactories == null) ?
        createDefaultTransportClientFactories() :  // if user didn't provide transportClientFactories we'll use default ones
        _config.clientFactories;

    final LoadBalancerWithFacilitiesFactory loadBalancerFactory = (_config.lbWithFacilitiesFactory == null) ?
        new ZKFSLoadBalancerWithFacilitiesFactory() :
        _config.lbWithFacilitiesFactory;

    final D2ClientConfig cfg = new D2ClientConfig(_config.zkHosts,
                  _config.zkSessionTimeoutInMs,
                  _config.zkStartupTimeoutInMs,
                  _config.lbWaitTimeout,
                  _config.lbWaitUnit,
                  _config.flagFile,
                  _config.basePath,
                  _config.fsBasePath,
                  _config.componentFactory,
                  transportClientFactories,
                  _config.lbWithFacilitiesFactory,
                  _config.sslContext,
                  _config.sslParameters,
                  _config.isSSLEnabled,
                  _config.shutdownAsynchronously,
                  _config.isSymlinkAware,
                  _config.clientServicesConfig,
                  _config.d2ServicePath);

    final LoadBalancerWithFacilities loadBalancer = loadBalancerFactory.create(cfg);

    D2Client d2Client = new DynamicClient(loadBalancer, loadBalancer);

    /**
     * If we created default transport client factories, we need to shut them down when d2Client
     * is being shut down.
     */
    if (_config.clientFactories != transportClientFactories)
    {
      d2Client = new TransportClientFactoryAwareD2Client(d2Client, transportClientFactories.values());
    }
    return d2Client;
  }

  public D2ClientBuilder setZkHosts(String zkHosts)
  {
    _config.zkHosts = zkHosts;
    return this;
  }

  public D2ClientBuilder setZkSessionTimeout(long zkSessionTimeout, TimeUnit unit)
  {
    _config.zkSessionTimeoutInMs = unit.toMillis(zkSessionTimeout);
    return this;
  }

  public D2ClientBuilder setZkStartupTimeout(long zkStartupTimeout, TimeUnit unit)
  {
    _config.zkStartupTimeoutInMs = unit.toMillis(zkStartupTimeout);
    return this;
  }

  public D2ClientBuilder setLbWaitTimeout(long lbWaitTimeout, TimeUnit unit)
  {
    _config.lbWaitTimeout = lbWaitTimeout;
    _config.lbWaitUnit = unit;
    return this;
  }

  public D2ClientBuilder setFlagFile(String flagFile)
  {
    _config.flagFile = flagFile;
    return this;
  }

  public D2ClientBuilder setBasePath(String basePath)
  {
    _config.basePath = basePath;
    return this;
  }

  public D2ClientBuilder setFsBasePath(String fsBasePath)
  {
    _config.fsBasePath = fsBasePath;
    return this;
  }

  public D2ClientBuilder setComponentFactory(ZKFSTogglingLoadBalancerFactoryImpl.ComponentFactory componentFactory)
  {
    _config.componentFactory = componentFactory;
    return this;
  }

  public D2ClientBuilder setSSLContext(SSLContext sslContext)
  {
    _config.sslContext = sslContext;
    return this;
  }

  public D2ClientBuilder setSSLParameters(SSLParameters sslParameters)
  {
    _config.sslParameters = sslParameters;
    return this;
  }

  public D2ClientBuilder setIsSSLEnabled(boolean isSSLEnabled)
  {
    _config.isSSLEnabled = isSSLEnabled;
    return this;
  }

  public D2ClientBuilder setShutdownAsynchronously(boolean shutdownAsynchronously)
  {
    _config.shutdownAsynchronously = shutdownAsynchronously;
    return this;
  }

  public D2ClientBuilder setIsSymlinkAware(boolean isSymlinkAware)
  {
    _config.isSymlinkAware = isSymlinkAware;
    return this;
  }

  public D2ClientBuilder setClientServicesConfig(Map<String, Map<String, Object>> clientServicesConfig)
  {
    _config.clientServicesConfig = clientServicesConfig;
    return this;
  }

  public D2ClientBuilder setD2ServicePath(String d2ServicePath)
  {
    _config.d2ServicePath = d2ServicePath;
    return this;
  }

  /**
   * Specify {@link TransportClientFactory} to generate the client for specific protocol.
   * Caller is responsible to maintain the life cycle of the factories.
   * If not specified, the default client factory map will be used, which is suboptimal in performance
   */
  public D2ClientBuilder setClientFactories(Map<String, TransportClientFactory> clientFactories)
  {
    _config.clientFactories = clientFactories;
    return this;
  }

  public D2ClientBuilder setLoadBalancerWithFacilitiesFactory(LoadBalancerWithFacilitiesFactory lbWithFacilitiesFactory)
  {
    _config.lbWithFacilitiesFactory = lbWithFacilitiesFactory;
    return this;
  }

  private Map<String, TransportClientFactory> createDefaultTransportClientFactories()
  {
    final Map<String, TransportClientFactory> clientFactories = new HashMap<String, TransportClientFactory>();
    clientFactories.put("http", new HttpClientFactory());
    return clientFactories;
  }

  private final D2ClientConfig _config = new D2ClientConfig();

  private class TransportClientFactoryAwareD2Client implements D2Client
  {
    TransportClientFactoryAwareD2Client(D2Client d2Client, Collection<TransportClientFactory> clientFactories)
    {
      _d2Client = d2Client;
      _clientFactories = clientFactories;
    }

    @Override
    public Facilities getFacilities()
    {
      return _d2Client.getFacilities();
    }

    @Override
    public void start(Callback<None> callback)
    {
      _d2Client.start(callback);
    }

    @Override
    public Future<RestResponse> restRequest(RestRequest request)
    {
      return _d2Client.restRequest(request);
    }

    @Override
    public Future<RestResponse> restRequest(RestRequest request, RequestContext requestContext)
    {
      return _d2Client.restRequest(request, requestContext);
    }

    @Override
    public void restRequest(RestRequest request, Callback<RestResponse> callback)
    {
      _d2Client.restRequest(request, callback);
    }

    @Override
    public void restRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      _d2Client.restRequest(request, requestContext, callback);
    }

    @Override
    public void streamRequest(StreamRequest request, Callback<StreamResponse> callback)
    {
      _d2Client.streamRequest(request, callback);
    }

    @Override
    public void streamRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
    {
      _d2Client.streamRequest(request, requestContext, callback);
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      _d2Client.shutdown(callback);

      for (TransportClientFactory clientFactory: _clientFactories)
      {
        clientFactory.shutdown(new FutureCallback<None>());
      }
    }

    @Override
    public Map<String, Object> getMetadata(URI uri)
    {
      return _d2Client.getMetadata(uri);
    }

    private D2Client _d2Client;
    private Collection<TransportClientFactory> _clientFactories;
  }
}
