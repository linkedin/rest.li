package com.linkedin.d2.balancer.servers;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.util.None;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConnectionManager
{
  private final ZooKeeperAnnouncer[] _servers;
  private final String _zkConnectString;
  private final int _zkSessionTimeout;
  private final String _zkBasePath;

  private static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

  protected ConnectionManager(ZooKeeperAnnouncer[] servers, String zkConnectString, int zkSessionTimeout, String zkBasePath)
  {
    _servers = servers;
    _zkConnectString = zkConnectString;
    _zkSessionTimeout = zkSessionTimeout;
    _zkBasePath = zkBasePath;
  }

  abstract public void start(Callback<None> callback);

  abstract public void shutdown(final Callback<None> callback);

  public void markDownAllServers(final Callback<None> callback)
  {
    Callback<None> markDownCallback;
    if (callback != null)
    {
      markDownCallback = callback;
    }
    else
    {
      markDownCallback = new Callback<None>()
      {
        @Override
        public void onError(Throwable e)
        {
          LOG.error("failed to mark down servers", e);
        }

        @Override
        public void onSuccess(None result)
        {
          LOG.info("mark down all servers successful");
        }
      };
    }
    Callback<None> multiCallback = Callbacks.countDown(markDownCallback, _servers.length);
    for (ZooKeeperAnnouncer server : _servers)
    {
      server.markDown(multiCallback);
    }
  }

  public void markUpAllServers(final Callback<None> callback)
  {
    Callback<None> markUpCallback;
    if (callback != null)
    {
      markUpCallback = callback;
    }
    else
    {
      markUpCallback = new Callback<None>()
      {
        @Override
        public void onError(Throwable e)
        {
          LOG.error("failed to mark up servers", e);
        }

        @Override
        public void onSuccess(None result)
        {
          LOG.info("mark up all servers successful");
        }
      };
    }
    Callback<None> multiCallback = Callbacks.countDown(markUpCallback, _servers.length);
    for (ZooKeeperAnnouncer server : _servers)
    {
      server.markUp(multiCallback);
    }

  }

  public ZooKeeperAnnouncer[] getAnnouncers()
  {
    return _servers;
  }

  abstract public boolean isSessionEstablished();

  public String getZooKeeperConnectString()
  {
    return _zkConnectString;
  }

  public int getZooKeeperSessionTimeout()
  {
    return _zkSessionTimeout;
  }

  public String getZooKeeperBasePath()
  {
    return _zkBasePath;
  }
}
