package com.linkedin.r2.transport.http.client.common;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.event.EventProviderRegistry;
import com.linkedin.r2.event.ChannelPoolEventProvider;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.PoolStats;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.pegasus.io.netty.channel.Channel;
import com.linkedin.pegasus.io.netty.channel.group.ChannelGroup;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;


/**
 * A decorator implementation of {@link ChannelPoolManagerFactory} that registers every
 * {@link ChannelPoolManager} created against a {@link EventProviderRegistry} and unregisters upon
 * the shutdown event of each ChannelPoolManager.
 */
public class EventAwareChannelPoolManagerFactory implements ChannelPoolManagerFactory
{
  private enum TransportMode
  {
    STREAM(true),
    REST(false);

    private final boolean _isStream;

    TransportMode(boolean isStream)
    {
      _isStream = isStream;
    }

    boolean isStream()
    {
      return _isStream;
    }
  }

  private final ChannelPoolManagerFactory _channelPoolManagerFactory;
  private final EventProviderRegistry _eventProviderRegistry;

  public EventAwareChannelPoolManagerFactory(
      ChannelPoolManagerFactory channelPoolManagerFactory,
      EventProviderRegistry eventProviderRegistry)
  {
    _channelPoolManagerFactory = channelPoolManagerFactory;
    _eventProviderRegistry = eventProviderRegistry;
  }

  @Override
  public ChannelPoolManager buildRest(ChannelPoolManagerKey channelPoolManagerKey)
  {
    return doBuild(_channelPoolManagerFactory::buildRest, channelPoolManagerKey, TransportMode.REST.isStream(), HttpProtocolVersion.HTTP_1_1);
  }

  @Override
  public ChannelPoolManager buildStream(ChannelPoolManagerKey channelPoolManagerKey)
  {
    return doBuild(_channelPoolManagerFactory::buildStream, channelPoolManagerKey, TransportMode.STREAM.isStream(), HttpProtocolVersion.HTTP_1_1);
  }

  @Override
  public ChannelPoolManager buildHttp2Stream(ChannelPoolManagerKey channelPoolManagerKey)
  {
    return doBuild(_channelPoolManagerFactory::buildHttp2Stream, channelPoolManagerKey, TransportMode.STREAM.isStream(), HttpProtocolVersion.HTTP_2);
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _channelPoolManagerFactory.shutdown(callback);
  }

  /**
   * Helper function that creates a new instance of {@link ChannelPoolManager} and register the
   * manager against the {@link EventProviderRegistry}.
   *
   * @param channelPoolManagerKey Channel pool manager key used to create the {@link ChannelPoolManager}
   * @param isStream Whether the channels created in the channel pool supports streaming
   * @param protocolVersion HTTP version, e.g. HTTP/1.1, HTTP/2
   * @return A new instance of {@link ChannelPoolManager} that has been registered against the {@link EventProviderRegistry}
   */
  private ChannelPoolManager doBuild(
      Function<ChannelPoolManagerKey, ChannelPoolManager> channelPoolManagerSupplier,
      ChannelPoolManagerKey channelPoolManagerKey,
      boolean isStream,
      HttpProtocolVersion protocolVersion)
  {
    String clusterName = channelPoolManagerKey.getPoolStatsNamePrefix();
    boolean isSecure = channelPoolManagerKey.getSslContext() != null;
    EventProviderManager eventProviderManager = new EventProviderManager(
        channelPoolManagerSupplier.apply(channelPoolManagerKey), clusterName, isStream, isSecure, protocolVersion);
    _eventProviderRegistry.registerChannelPoolEventProvider(eventProviderManager);
    return eventProviderManager;
  }

  /**
   * A decorator implementation of {@link ChannelPoolManager} and {@link ChannelPoolEventProvider} that
   * unregisters itself against the {@link EventProviderRegistry} during shutdown.
   */
  private class EventProviderManager implements ChannelPoolManager, ChannelPoolEventProvider
  {
    private final ChannelPoolManager _channelPoolManager;
    private final String _clusterName;
    private final boolean _isStream;
    private final boolean _isSecure;
    private final HttpProtocolVersion _protocolVersion;

    EventProviderManager(
        ChannelPoolManager channelPoolManager,
        String clusterName,
        boolean isStream,
        boolean isSecure,
        HttpProtocolVersion protocolVersion)
    {
      _channelPoolManager = channelPoolManager;
      _clusterName = clusterName;
      _isStream = isStream;
      _isSecure = isSecure;
      _protocolVersion = protocolVersion;
    }

    @Override
    public void shutdown(Callback<None> callback, Runnable callbackStopRequest, Runnable callbackShutdown,
        long shutdownTimeout)
    {
      _eventProviderRegistry.unregisterChannelPoolEventProvider(this);
      _channelPoolManager.shutdown(callback, callbackStopRequest, callbackShutdown, shutdownTimeout);
    }

    @Override
    public Collection<Callback<Channel>> cancelWaiters()
    {
      return _channelPoolManager.cancelWaiters();
    }

    @Override
    public AsyncPool<Channel> getPoolForAddress(SocketAddress address) throws IllegalStateException
    {
      return _channelPoolManager.getPoolForAddress(address);
    }

    @Override
    public Map<String, PoolStats> getPoolStats()
    {
      return _channelPoolManager.getPoolStats();
    }

    @Override
    public String getName()
    {
      return _channelPoolManager.getName();
    }

    @Override
    public ChannelGroup getAllChannels()
    {
      return _channelPoolManager.getAllChannels();
    }

    @Override
    public String clusterName()
    {
      return _clusterName;
    }

    @Override
    public boolean isStream()
    {
      return _isStream;
    }

    @Override
    public boolean isSecure()
    {
      return _isSecure;
    }

    @Override
    public HttpProtocolVersion protocolVersion()
    {
      return _protocolVersion;
    }
  }
}
