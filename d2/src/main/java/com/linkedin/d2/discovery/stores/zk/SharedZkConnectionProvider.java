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

package com.linkedin.d2.discovery.stores.zk;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is used to dispatch ZkPersistentConnection based on the config provided in ZKConnectionBuilder.
 * This allows us to have a centralized place to generate connections to Zookeeper as well as share this connection
 * whenever possible.
 *
 * NOTE: this class is intended to be used during object initialization phase before starting/running the application.
 * This is because connection event listeners can only be added before connection start.
 *
 * Note on shared connection lifecycle:
 *
 * Shared connections to zookeeper are started by the first user trying to start it, after which new users trying to start it
 * will increase the active user count for that connection.
 *
 * To shutdown a connection, it is ensured that
 *  1. no active users are using it
 *  2. all the original connection requesters have finished using it.
 * Normally the last user would shutdown connection when it calls shutdown/close. If for some reason that one user who has obtained the connection
 * but never starts/uses it, {@code ensureConnectionClosed} should be called to ensure clean shutdown
 */
public class SharedZkConnectionProvider implements ZkConnectionProvider {
  private static final Logger LOG = LoggerFactory.getLogger(SharedZkConnectionProvider.class);

  private Map<ZKConnectionBuilder, ZKPersistentConnection> _sharedConnections;
  private LongAdder _requestCount;
  private volatile boolean _sharingEnabled;

  public SharedZkConnectionProvider() {
    _sharedConnections = new HashMap<>();
    _requestCount = new LongAdder();
    _sharingEnabled = true;
  }

  /**
   * Returns either a new connection to zookeeper if no connection is shareable or an old connection if the config is identical to one we had before
   * @param zkConnectionBuilder ZKConnectionBuilder with desired Zookeeper config values.
   * @return a ZKPersistentConnection
   */
  public ZKPersistentConnection getZKPersistentConnection(ZKConnectionBuilder zkConnectionBuilder) {
    if (!_sharingEnabled) {
      LOG.warn("Trying to obtain connections after application has been started!");
      return new ZKPersistentConnection(zkConnectionBuilder);
    }

    _requestCount.increment();

    final ZKConnectionBuilder builder= new ZKConnectionBuilder(zkConnectionBuilder);
    ZKPersistentConnection connection;

    synchronized (_sharedConnections){
      if (_sharedConnections.containsKey(builder)) {
        connection = _sharedConnections.get(builder);
        if (connection.isConnectionStarted())
        {
          LOG.warn("There is a connection with the same parameters that are already started. Opening a new connection now. Please consider constructing connections before startup.");
          return new ZKPersistentConnection(builder);
        }
      } else {
        connection = new ZKPersistentConnection(builder);
        _sharedConnections.put(builder, connection);
      }
    }
    connection.incrementShareCount();
    return connection;
  }

  /**
   * Since connections are shared, if registered users did not use the connection, the connection can't be closed unless we manually close it.
   * @Throws InterruptedException
   */
  public void ensureConnectionClosed() throws InterruptedException
  {
    synchronized (_sharedConnections) {
      Collection<ZKPersistentConnection> connectionList = _sharedConnections.values();
      for (ZKPersistentConnection connection : connectionList) {
        if (!connection.isConnectionStopped())
        {
          connection.forceShutdown();
        }
      }
    }
  }

  /**
   * Disable sharing from SharedZkConnectionProvider.
   */
  public void disableSharing() {
    _sharingEnabled = false;
  }

  /**
   * Returns the number of connections initialized by the SharedZkConnectionProvider
   */
  public int getZkConnectionCount() {
    synchronized (_sharedConnections) {
      return _sharedConnections.size();
    }
  }

  /**
   * Returns number of connection requests received by SharedZkConnectionProvider
   */
  public int getRequestCount()
  {
    return _requestCount.intValue();
  }
}
