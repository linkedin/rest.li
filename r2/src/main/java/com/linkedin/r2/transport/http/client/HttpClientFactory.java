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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

  public static final String HTTP_QUERY_POST_THRESHOLD = "http.queryPostThreshold";
  public static final String HTTP_REQUEST_TIMEOUT = "http.requestTimeout";
  public static final String HTTP_MAX_RESPONSE_SIZE = "http.maxResponseSize";
  public static final String HTTP_POOL_SIZE = "http.poolSize";
  public static final String HTTP_IDLE_TIMEOUT = "http.idleTimeout";
  public static final String HTTP_SHUTDOWN_TIMEOUT = "http.shutdownTimeout";
  public static final String HTTP_SSL_CONTEXT = "http.sslContext";
  public static final String HTTP_SSL_PARAMS = "http.sslParams";

  public static final int DEFAULT_POOL_SIZE = 200;
  public static final int DEFAULT_REQUEST_TIMEOUT = 10000;
  public static final int DEFAULT_IDLE_TIMEOUT = 30000;
  public static final int DEFAULT_SHUTDOWN_TIMEOUT = 5000;
  public static final int DEFAULT_MAX_RESPONSE_SIZE = 1024 * 1024 * 2;

  /**
   * The string below this is deprecated so use the equivalent above.
   */
  public static final String OLD_GET_TIMEOUT_KEY = "getTimeout";
  public static final String OLD_POOL_SIZE_KEY = "poolSize";
  public static final String OLD_REQUEST_TIMEOUT_KEY = "requestTimeout";
  public static final String OLD_IDLE_TIMEOUT_KEY = "idleTimeout";
  public static final String OLD_SHUTDOWN_TIMEOUT_KEY = "shutdownTimeout";
  public static final String OLD_MAX_RESPONSE_SIZE = "maxResponseSize";
  public static final String OLD_SSL_CONTEXT = "sslContext";
  public static final String OLD_SSL_PARAMS = "sslParams";

  private final ClientSocketChannelFactory _channelFactory;
  private final ScheduledExecutorService   _executor;
  private final boolean                    _shutdownFactory;
  private final boolean                    _shutdownExecutor;
  private final FilterChain                _filters;

  private final AtomicBoolean              _finishingShutdown = new AtomicBoolean(false);
  private volatile ScheduledFuture<?>      _shutdownTimeoutTask;

  // All fields below protected by _mutex
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
    SSLContext sslContext;
    SSLParameters sslParameters;

    // Copy the properties map since we don't want to mutate the passed-in map by removing keys
    properties = new HashMap<String,Object>(properties);
    // TODO once references to old keys are eliminated, these should become calls to coerceAndRemoveFromMap().
    sslContext = coerceOldAndNew(OLD_SSL_CONTEXT, HTTP_SSL_CONTEXT, properties, SSLContext.class);
    sslParameters = coerceOldAndNew(OLD_SSL_PARAMS, HTTP_SSL_PARAMS, properties,
                                    SSLParameters.class);

    for (Map.Entry<String, ?> entry : properties.entrySet())
    {
      stringProperties.put(entry.getKey(), coerce(entry.getKey(), entry.getValue(), String.class));
    }

    return getClient(stringProperties, sslContext, sslParameters);
  }

  HttpNettyClient getRawClient(Map<String, String> properties)
  {
    return getRawClient(properties, null, null);
  }

  private static <T> T coerceOldAndNew(String oldKey, String newKey, Map<String, ?> props,
                                       Class<T> valueClass)
  {
    T oldValue = coerceAndRemoveFromMap(oldKey, props, valueClass);
    T value = coerceAndRemoveFromMap(newKey, props, valueClass);

    if (oldValue != null)
    {
      if (value != null)
      {
        LOG.warn("Ignoring obsolete key " + oldKey + " in favor of " + newKey);
      }
      else
      {
        LOG.warn("Using deprecated key " + oldKey + "; please switch to " + newKey);
        value = oldValue;
      }
    }
    return value;
  }

  private static <T> T coerceAndRemoveFromMap(String key, Map<String, ?> props, Class<T> valueClass)
  {
    return coerce(key, props.remove(key), valueClass);
  }

  private static <T> T coerce(String key, Object value, Class<T> valueClass)
  {
    if (value == null)
    {
      return null;
    }
    if (!valueClass.isInstance(value))
    {
      throw new IllegalArgumentException(
              "Property " + key + " is of type " + value.getClass().getName() +
              " but must be " + valueClass.getName());
    }
    return valueClass.cast(value);
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
   * helper method to get value from properties as well as to print log warning if the key is old
   * @param properties
   * @param propertyKey
   * @param newPropertyKey optional, if specified will be used to print log warning
   * @return null if property key can't be found, integer otherwise
   */
  private Integer getIntValue(Map<String, String> properties, String propertyKey, String newPropertyKey)
  {
    if (properties == null)
    {
      LOG.warn("passed a null raw client properties");
      return null;
    }
    if (properties.containsKey(propertyKey))
    {
      if (newPropertyKey != null)
      {
        LOG.warn("Obsolete key " + propertyKey + " is specified. Please use " + newPropertyKey + " instead");
      }
      return Integer.parseInt(properties.get(propertyKey));
    }
    else
    {
      return null;
    }
  }

  /**
   * Testing aid.
   */
  HttpNettyClient getRawClient(Map<String, String> properties,
                               SSLContext sslContext,
                               SSLParameters sslParameters)
  {
    Integer queryPostThreshold = getIntValue(properties, HTTP_QUERY_POST_THRESHOLD, null);
    Integer poolSize = getIntValue(properties, HTTP_POOL_SIZE, null);
    Integer requestTimeout = getIntValue(properties, HTTP_REQUEST_TIMEOUT, null);
    Integer idleTimeout = getIntValue(properties, HTTP_IDLE_TIMEOUT, null);
    Integer shutdownTimeout = getIntValue(properties, HTTP_SHUTDOWN_TIMEOUT, null);
    Integer maxResponseSize = getIntValue(properties, HTTP_MAX_RESPONSE_SIZE, null);

    //TODO these can go away when we migrate all obsolete config to new ones
    Integer oldGetTimeout = getIntValue(properties, OLD_GET_TIMEOUT_KEY, HTTP_REQUEST_TIMEOUT);
    Integer oldPoolSize = getIntValue(properties, OLD_POOL_SIZE_KEY, HTTP_POOL_SIZE);
    Integer oldRequestTimeout = getIntValue(properties, OLD_REQUEST_TIMEOUT_KEY, HTTP_REQUEST_TIMEOUT);
    Integer oldIdleTimeout = getIntValue(properties, OLD_IDLE_TIMEOUT_KEY, HTTP_IDLE_TIMEOUT);
    Integer oldShutdownTimeout = getIntValue(properties, OLD_SHUTDOWN_TIMEOUT_KEY, HTTP_SHUTDOWN_TIMEOUT);
    Integer oldMaxResponseSize = getIntValue(properties, OLD_MAX_RESPONSE_SIZE, HTTP_MAX_RESPONSE_SIZE);

    poolSize = chooseNewOverOldWithDefault(oldPoolSize, poolSize, DEFAULT_POOL_SIZE, OLD_POOL_SIZE_KEY,
                                           HTTP_POOL_SIZE);
    idleTimeout = chooseNewOverOldWithDefault(oldIdleTimeout, idleTimeout, DEFAULT_IDLE_TIMEOUT,
                                              OLD_IDLE_TIMEOUT_KEY, HTTP_IDLE_TIMEOUT);
    shutdownTimeout = chooseNewOverOldWithDefault(oldShutdownTimeout, shutdownTimeout, DEFAULT_SHUTDOWN_TIMEOUT,
                                                  OLD_SHUTDOWN_TIMEOUT_KEY, HTTP_SHUTDOWN_TIMEOUT);
    maxResponseSize = chooseNewOverOldWithDefault(oldMaxResponseSize, maxResponseSize, DEFAULT_MAX_RESPONSE_SIZE,
                                                  OLD_MAX_RESPONSE_SIZE, HTTP_MAX_RESPONSE_SIZE);
    queryPostThreshold = chooseNewOverOldWithDefault(queryPostThreshold, null, Integer.MAX_VALUE,
                                                     HTTP_QUERY_POST_THRESHOLD, null);
    //we have the getTimeout, oldRequestTimeOut and requestTimeOut. RequestTimeout has the highest priority and
    //getTimeout has the lowest priority.
    if (requestTimeout != null && (oldRequestTimeout != null || oldGetTimeout != null))
    {
      LOG.warn(HTTP_REQUEST_TIMEOUT + " was specified as well as " + OLD_GET_TIMEOUT_KEY + " or " +
      OLD_REQUEST_TIMEOUT_KEY + " so we will choose to use the value specified in " + HTTP_REQUEST_TIMEOUT );
    }
    else if (oldRequestTimeout != null && oldGetTimeout != null)
    {
      LOG.warn("Both " + OLD_REQUEST_TIMEOUT_KEY + " and " + OLD_GET_TIMEOUT_KEY +
                   " were specified so we will use the value specified in " + OLD_REQUEST_TIMEOUT_KEY);
    }
    if (requestTimeout == null)
    {
      if (oldRequestTimeout == null)
      {
        if (oldGetTimeout == null)
        {
          requestTimeout = DEFAULT_REQUEST_TIMEOUT;
        }
        else
        {
          requestTimeout = oldGetTimeout;
        }
      }
      else
      {
        requestTimeout = oldRequestTimeout;
      }
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
   * choose the new value over the old value. If both new and old are not specified, we'll use default value
   *
   * @param oldValue
   * @param newValue
   * @param defaultValue
   * @param oldName
   * @param newName
   * @return
   */
  private Integer chooseNewOverOldWithDefault(Integer oldValue, Integer newValue, Integer defaultValue,
                                              String oldName, String newName)
  {
    if (oldValue == null&& newValue == null)
    {
      return defaultValue;
    }
    else if (newValue != null && oldValue != null)
    {
      LOG.warn("Both " + oldName + " of "+
                   oldValue + " and "+ newName +" of "+ newValue +" were specified, ignoring obsolete " + oldName);
      return newValue;
    }
    else if (newValue != null && oldValue == null)
    {
      return  newValue;
    }
    else
    {
      return oldValue;
    }
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
    _shutdownTimeoutTask = _executor.schedule(new Runnable()
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
    if (!_finishingShutdown.compareAndSet(false, true))
    {
      return;
    }
    if (_shutdownTimeoutTask != null)
    {
      _shutdownTimeoutTask.cancel(false);
    }

    // Under some circumstances, this method will be executed on a Netty IO thread.  For example,
    // as the factory waits for clients to shutdown, if the final client shuts down due an IO
    // event (receives response, connection refused, etc.).  In that case, the call to
    // releaseExternalResources() below will throw an exception -- because
    // releaseExternalResources() blocks until all threads are shut down, it refuses to run
    // if called from one of its own threads.  Therefore, schedule this task to run on
    // a different thread pool.  That does mean _executor will be shut down from one of its
    // own threads, but since this call doesn't await termination it's OK.
    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        if (_shutdownFactory)
        {
          _channelFactory.releaseExternalResources();
          LOG.info("ChannelFactory shutdown complete");
        }
        if (_shutdownExecutor)
        {
          // Due to a bug in ScheduledThreadPoolExecutor, shutdownNow() returns cancelled
          // tasks as though they were still pending execution.  If the executor has a large
          // number of cancelled tasks, shutdownNow() could take a long time to copy the array
          // of tasks.  Calling shutdown() first will purge the cancelled tasks.  Bug filed with
          // Oracle; will provide bug number when available.  May be fixed in JDK7 already.
          _executor.shutdown();
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
    });
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
