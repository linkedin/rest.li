package com.linkedin.d2.jmx;

import com.linkedin.d2.discovery.stores.PropertyStoreException;


public interface ZooKeeperConnectionManagerJmxMBean
{
  void markUpAllServers() throws PropertyStoreException;

  void markDownAllServers() throws PropertyStoreException;

  boolean isSessionEstablished();

  String getZooKeeperConnectString();

  String getZooKeeperBasePath();

  int getZooKeeperSessionTimeout();
}
