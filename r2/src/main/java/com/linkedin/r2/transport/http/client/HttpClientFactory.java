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

/* $Id$ */
package com.linkedin.r2.transport.http.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.transport.FilterChainClient;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.util.NamedThreadFactory;

/**
 * A factory for HttpNettyClient instances.
 *
 * All clients created by the factory will share the same resources, in particular the
 * {@link ClientSocketChannelFactory} and {@link ScheduledExecutorService}.
 *
 * In order to shutdown cleanly, all clients issued by the factory should be shutdown via
 * {@link TransportClient#shutdown(com.linkedin.common.callback.Callback)} and the factory
 * itself should be shut down via one of the following two methods:
 * <ul>
 * <li>{@link #shutdown(com.linkedin.common.callback.Callback)}</li>
 * <li>
 * {@link #shutdown(com.linkedin.common.callback.Callback, long, java.util.concurrent.TimeUnit)}
 * </li>
 * </ul>
 *
 * See the method descriptions for more details. Note that factory shutdown and shutdown
 * of the clients can be initiated in any order.
 *
 * @author Chris Pettitt
 * @author Steven Ihde
 * @version $Revision$
 */
public class HttpClientFactory implements TransportClientFactory
{
  private static final Logger LOG = LoggerFactory.getLogger(HttpClientFactory.class);

  public static final String               POOL_SIZE_KEY        = "poolSize";
  public static final String               REQUEST_TIMEOUT_KEY  = "requestTimeout";
  public static final String               IDLE_TIMEOUT_KEY     = "idleTimeout";
  public static final String               SHUTDOWN_TIMEOUT_KEY = "shutdownTimeout";
  public static final String               MAX_RESPONSE_SIZE    = "maxResponseSize";
  public static final String               SSL_CONTEXT          = "sslContext";
  public static final String               SSL_PARAMS           = "sslParams";
  public static final String               QUERY_POST_THRESHOLD = "queryPostThreshold";
  /**
   * @deprecated use REQUEST_TIMEOUT_KEY instead
   */
  @Deprecated
  public static final String               GET_TIMEOUT_KEY      = "getTimeout";

  private final ClientSocketChannelFactory _channelFactory;
  private final ScheduledExecutorService   _executor;
  private final boolean                    _shutdownFactory;
  private final boolean                    _shutdownExecutor;
  private final FilterChain                _filters;

  private final Object                     _mutex               = new Object();
  private boolean                          _running             = true;
  private int                              _clientsOutstanding  = 0;
  private Callback<None>                   _factoryShutdownCallback;

  /**
   * Construct a new instance using an empty filter chain.
   */
  public HttpClientFactory()
  {
    this(FilterChains.empty());
  }

  /**
   * Construct a new instance using the specified filter chain.
   *
   * @param filters the {@link FilterChain} shared by all Clients created by this factory.
   */
  public HttpClientFactory(FilterChain filters)
  {
    // TODO Disable Netty's thread renaming so that the names below are the ones that actually
    // show up in log messages; need to coordinate with Espresso team (who also have netty threads)
    this(filters,
         new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(new NamedThreadFactory("R2 Netty IO Boss")),
            Executors.newCachedThreadPool(new NamedThreadFactory("R2 Netty IO Worker"))),
         true,
         Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("R2 Netty Scheduler")),
         true);
  }

  /**
   * Creates a new HttpClientFactory.
   *
   * @param filters the filter chain shared by all Clients created by this factory
   * @param channelFactory the ClientSocketChannelFactory that all Clients created by this
   *          factory will share
   * @param shutdownFactory if true, the channelFactory will be shut down when this
   *          factory is shut down
   * @param executor an executor shared by all Clients created by this factory to schedule
   *          tasks
   * @param shutdownExecutor if true, the executor will be shut down when this factory is
   *          shut down
   */
  public HttpClientFactory(FilterChain filters,
                           ClientSocketChannelFactory channelFactory,
                           boolean shutdownFactory,
                           ScheduledExecutorService executor,
                           boolean shutdownExecutor)
  {
    _filters = filters;
    _channelFactory = channelFactory;
    _shutdownFactory = shutdownFactory;
    _executor = executor;
    _shutdownExecutor = shutdownExecutor;
  }

  @Override
  public TransportClient getClient(Map<String, ? extends Object> properties)
  {
    // Translate the new Map<String, Object> into the old Map<String, String> + SSLContext
    // and SSLParameters params.
    Map<String, String> stringProperties = new HashMap<String, String>(properties.size());
    SSLContext sslContext = null;
    SSLParameters sslParameters = null;

    if (properties != null)
    {
      if (properties.containsKey(SSL_CONTEXT))
      {
        Object sslContextObj = properties.get(SSL_CONTEXT);
        if (sslContextObj instanceof SSLContext)
        {
          sslContext = (SSLContext) sslContextObj;
          properties.remove(SSL_CONTEXT);
        }
        else
        {
          throw new IllegalArgumentException(SSL_CONTEXT + " property is of type "
              + sslContextObj.getClass().getSimpleName()
              + " while must be SSLContext");
        }
      }

      if (properties.containsKey(SSL_PARAMS))
      {
        Object sslParametersObj = properties.get(SSL_PARAMS);
        if (sslParametersObj instanceof SSLParameters)
        {
          sslParameters = (SSLParameters) sslParametersObj;
          properties.remove(SSL_PARAMS);
        }
        else
        {
          throw new IllegalArgumentException(SSL_PARAMS + " property is of type "
              + sslParametersObj.getClass().getSimpleName()
              + " while must be SSLParameters");
        }
      }

      for (Map.Entry<String, ? extends Object> entry : properties.entrySet())
      {
        if (entry.getValue() instanceof String)
        {
          stringProperties.put(entry.getKey(), (String) entry.getValue());
        }
        else
        {
          throw new IllegalArgumentException("Property " + entry.getKey() + "is of type "
              + entry.getValue().getClass().getSimpleName() + " while must be String");
        }
      }
    }

    return getClient(stringProperties, sslContext, sslParameters);
  }

  HttpNettyClient getRawClient(Map<String, String> properties)
  {
    return getRawClient(properties, null, null);
  }


  /**
   * Create a new {@link TransportClient} with the specified properties,
   * {@link SSLContext} and {@link SSLParameters}
   *
   * @param properties map of properties for the {@link TransportClient}
   * @param sslContext {@link SSLContext} to be used for requests over SSL/TLS.
   * @param sslParameters {@link SSLParameters} to configure secure connections.
   * @return an appropriate {@link TransportClient} instance, as specified by the properties.
   */
  private TransportClient getClient(Map<String, String> properties,
                                   SSLContext sslContext,
                                   SSLParameters sslParameters)
  {
    LOG.info("Getting a client with configuration {} and SSLContext {}",
             properties,
             sslContext);
    TransportClient client = getRawClient(properties, sslContext, sslParameters);

    client = new FilterChainClient(client, _filters);
    client = new FactoryClient(client);
    synchronized (_mutex)
    {
      if (!_running)
      {
        throw new IllegalStateException("Factory is shutting down");
      }
      _clientsOutstanding++;
      return client;
    }
  }

  /**
   * Testing aid.
   */
  HttpNettyClient getRawClient(Map<String, String> properties,
                               SSLContext sslContext,
                               SSLParameters sslParameters)
  {
    int poolSize = 200;
    int requestTimeout = 10000;
    int idleTimeout = 30000;
    int shutdownTimeout = 5000;
    int maxResponseSize = 1024 * 1024 * 2;
    int queryPostThreshold = Integer.MAX_VALUE;

    // These can go away when support for getTimeout is completely removed
    Integer getTimeoutObj = null;
    Integer requestTimeoutObj = null;

    for (Map.Entry<String, String> e : properties.entrySet())
    {
      String name = e.getKey();
      String value = e.getValue();
      if (name.equals(POOL_SIZE_KEY))
      {
        poolSize = Integer.parseInt(value);
      }
      else if (name.equals(GET_TIMEOUT_KEY))
      {
        getTimeoutObj = Integer.parseInt(value);
      }
      else if (name.equals(REQUEST_TIMEOUT_KEY))
      {
        requestTimeoutObj = Integer.parseInt(value);
      }
      else if (name.equals(IDLE_TIMEOUT_KEY))
      {
        idleTimeout = Integer.parseInt(value);
      }
      else if (name.equals(SHUTDOWN_TIMEOUT_KEY))
      {
        shutdownTimeout = Integer.parseInt(value);
      }
      else if (name.equals(MAX_RESPONSE_SIZE))
      {
        maxResponseSize = Integer.parseInt(value);
      }
      else if (name.equals(QUERY_POST_THRESHOLD))
      {
        queryPostThreshold = Integer.parseInt(value);
      }
    }

    if (requestTimeoutObj != null)
    {
      requestTimeout = requestTimeoutObj;
      if (getTimeoutObj != null && !getTimeoutObj.equals(requestTimeoutObj))
      {
        LOG.warn("Both requestTimeout of {} and getTimeout of {} were specified, ignoring obsolete getTimeout",
                 requestTimeoutObj,
                 getTimeoutObj);
      }
    }
    else if (getTimeoutObj != null)
    {
      requestTimeout = getTimeoutObj;
      LOG.warn("Obsolete getTimeout was specified, please specify requestTimeout as well");
    }

    return new HttpNettyClient(_channelFactory,
                               _executor,
                               poolSize,
                               requestTimeout,
                               idleTimeout,
                               shutdownTimeout,
                               maxResponseSize,
                               sslContext,
                               sslParameters,
                               queryPostThreshold);
  }

  /**
   * Initiates an orderly shutdown of the factory wherein no more clients will be created,
   * and the shutdown will complete when all existing clients have been shut down.  If some
   * clients fail to shutdown, the factory will never shut down.  Shutdown of the clients must
   * be initiated independently, but can occur before or after factory shutdown is initiated.
   *
   * After all clients have shut down, the ClientSocketChannelFactory and ScheduledExecutorService
   * will be shut down, if these options were selected at construction time.
   *
   * @param callback invoked after all outstanding clients and this factory have completed shutdown
   */
  @Override
  public void shutdown(final Callback<None> callback)
  {
    final int count;
    synchronized (_mutex)
    {
      _running = false;
      count = _clientsOutstanding;
      _factoryShutdownCallback = callback;
    }

    if (count == 0)
    {
      finishShutdown();
    }
    else
    {
      LOG.info("Awaiting shutdown of {} outstanding clients", count);
    }
  }

  /**
   * Initiates an orderly shutdown similar to
   * {@link #shutdown(com.linkedin.common.callback.Callback)}. However, in the case that
   * some clients fail to shutdown, the factory shutdown will still complete after the
   * specified timeout.
   *
   * @param callback invoked after all clients shutdown (or the timeout expires) and the
   *          factory has shut down
   * @param timeout the timeout
   * @param timeoutUnit the timeout unit
   */
  public void shutdown(Callback<None> callback, long timeout, TimeUnit timeoutUnit)
  {
    // Schedule a timeout in case shutdown does not happen normally
    _executor.schedule(new Runnable()
    {
      @Override
      public void run()
      {
        LOG.warn("Shutdown timeout exceeded, proceeding with shutdown");
        finishShutdown();
      }
    }, timeout, timeoutUnit);

    // Initiate orderly shutdown
    shutdown(callback);
  }

  private void finishShutdown()
  {
    if (_shutdownFactory)
    {
      _channelFactory.releaseExternalResources();
      LOG.info("ChannelFactory shutdown complete");
    }
    if (_shutdownExecutor)
    {
      _executor.shutdownNow();
      LOG.info("Scheduler shutdown complete");
    }
    final Callback<None> callback;
    synchronized (_mutex)
    {
      callback = _factoryShutdownCallback;
    }
    LOG.info("Shutdown complete");
    callback.onSuccess(None.none());
  }

  private void clientShutdown()
  {
    final boolean done;
    synchronized (_mutex)
    {
      _clientsOutstanding--;
      done = !_running && _clientsOutstanding == 0;
    }
    if (done)
    {
      finishShutdown();
    }
  }

  /**
   * The FactoryClient is a wrapper that simply does reference counting for all clients
   * issued by this factory, so that we can know when all outstanding clients have been
   * shut down completely.
   *
   * It introduces no synchronization overhead in the per-request code path, only the
   * shutdown code path.
   */
  private class FactoryClient implements TransportClient
  {
    private final TransportClient _client;

    private FactoryClient(TransportClient client)
    {
      _client = client;
    }

    @Override
    public void restRequest(RestRequest request, RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            TransportCallback<RestResponse> callback)
    {
      _client.restRequest(request, requestContext, wireAttrs, callback);
    }

    @Override
    public void rpcRequest(RpcRequest request, RequestContext requestContext,
                           Map<String, String> wireAttrs, TransportCallback<RpcResponse> callback)
    {
      _client.rpcRequest(request, requestContext, wireAttrs, callback);
    }

    @Override
    public void shutdown(final Callback<None> callback)
    {
      _client.shutdown(new Callback<None>()
      {
        @Override
        public void onSuccess(None none)
        {
          try
          {
            callback.onSuccess(none);
          }
          finally
          {
            clientShutdown();
          }
        }

        @Override
        public void onError(Throwable e)
        {
          try
          {
            callback.onError(e);
          }
          finally
          {
            clientShutdown();
          }
        }
      });
    }
  }
}
