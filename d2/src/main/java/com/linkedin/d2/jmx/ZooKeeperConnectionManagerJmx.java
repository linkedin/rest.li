package com.linkedin.d2.jmx;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.servers.ZooKeeperConnectionManager;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import java.util.concurrent.TimeUnit;


public class ZooKeeperConnectionManagerJmx implements ZooKeeperConnectionManagerJmxMBean
{
  private final ZooKeeperConnectionManager _connectionManager;

  public ZooKeeperConnectionManagerJmx(ZooKeeperConnectionManager connectionManager)
  {
    _connectionManager = connectionManager;
  }

  @Override
  public void markUpAllServers() throws PropertyStoreException
  {
    FutureCallback<None> callback = new FutureCallback<>();
    _connectionManager.markUpAllServers(callback);
    try
    {
      callback.get(10, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      throw new PropertyStoreException(e);
    }
  }

  @Override
  public void markDownAllServers() throws PropertyStoreException
  {
    FutureCallback<None> callback = new FutureCallback<>();
    _connectionManager.markDownAllServers(callback);
    try
    {
      callback.get(10, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      throw new PropertyStoreException(e);
    }
  }

  @Override
  public boolean isSessionEstablished()
  {
    return _connectionManager.isSessionEstablished();
  }

  @Override
  public String getZooKeeperConnectString()
  {
    return _connectionManager.getZooKeeperConnectString();
  }

  @Override
  public String getZooKeeperBasePath() {
    return _connectionManager.getZooKeeperBasePath();
  }

  @Override
  public int getZooKeeperSessionTimeout() {
    return _connectionManager.getZooKeeperSessionTimeout();
  }
}
