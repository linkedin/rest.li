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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Entity;


/**
 * This class is used to dispatch ZkPersistentConnection based on the config provided in ZKConnectionBuilder.
 * This allows us to have a centralized place to generate connections to Zookeeper as well as share this connection
 * whenever possible.
 *
 * NOTE: this class is intended to be used during object initialization phase before starting/running the application.
 * This is because connection event listeners can only be added before connection start.
 */
public class ZkConnectionDealer {
  private static final Logger LOG = LoggerFactory.getLogger(ZkConnectionDealer.class);

  private Map<ZKConnectionBuilder, ZKPersistentConnection> _sharedConnections;


  public ZkConnectionDealer() {
    _sharedConnections = new HashMap<>();
  }

  /**
   * Returns either a new connection to zookeeper if no connection is shareable or an old connection if the config is identical to one we had before
   * @param zkConnectionBuilder ZKConnectionBuilder with desired Zookeeper config values.
   * @return a ZKPersistentConnection
   */
  public ZKPersistentConnection getZKPersistentConnection(ZKConnectionBuilder zkConnectionBuilder) {
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
   * @throws InterruptedException
   */
  public void ensureConnectionClosed() throws InterruptedException
  {
    Collection<ZKPersistentConnection> connectionList = _sharedConnections.values();
    for (ZKPersistentConnection connection : connectionList) {
      if (!connection.isConnectionStopped())
      {
        connection.forceShutdown();
      }
    }
  }

  public int getZkConnectionCount() {
    return _sharedConnections.size();
  }
}
