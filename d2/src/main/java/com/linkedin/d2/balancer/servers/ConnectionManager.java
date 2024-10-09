package com.linkedin.d2.balancer.servers;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.util.None;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConnectionManager is an abstract class responsible for managing connections to external systems.
 * It can be extended to handle specific service registries (e.g., Zookeeper).
 * For example, see {@link com.linkedin.d2.balancer.servers.ZooKeeperConnectionManager} for managing Zookeeper connections during D2 server announcements.
 * This class provides basic functionalities such as start, shutdown, markDownAllServers, and markUpAllServers which
 * is called during D2 server announcements/de-announcement.
 */
public abstract class ConnectionManager
{
  private final ZooKeeperAnnouncer[] _servers;

  private static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

  protected ConnectionManager(ZooKeeperAnnouncer[] servers)
  {
    _servers = servers;
  }

  abstract public void start(Callback<None> callback);

  abstract public void shutdown(final Callback<None> callback);

  abstract public String getAnnouncementTargetIdentifier();

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
}
