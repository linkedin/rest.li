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

package com.linkedin.d2.discovery.stores.zk;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventPublisher;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.util.AbstractPropertyStoreAsync;
import com.linkedin.d2.discovery.util.Stats;

import static com.linkedin.d2.discovery.util.LogUtil.debug;
import static com.linkedin.d2.discovery.util.LogUtil.info;

public abstract class ZooKeeperStore<T> extends AbstractPropertyStoreAsync<T>
        implements
    PropertyEventPublisher<T>,
    PropertyStore<T>
{
  private static final Logger           _log =
                                                 LoggerFactory.getLogger(ZooKeeperStore.class);

  protected PropertyEventBus<T>         _eventBus;
  protected final ZKConnection          _zkConn;
  protected final String                _path;
  protected final PropertySerializer<T> _serializer;
  protected final Stats                 _getStats;
  protected final Stats                 _putStats;
  protected final Stats                 _removeStats;
  protected final Stats                 _registerStats;
  protected final Stats                 _unregisterStats;
  protected ZooKeeper             _zk;

  public ZooKeeperStore(ZKConnection client, PropertySerializer<T> serializer, String path)
  {
    _getStats = new Stats(60000);
    _putStats = new Stats(60000);
    _removeStats = new Stats(60000);
    _registerStats = new Stats(60000);
    _unregisterStats = new Stats(60000);
    _zkConn = client;
    _path = path;
    _serializer = serializer;

  }

  @Override
  public void start(Callback<None> callback)
  {
    _zk = _zkConn.getZooKeeper();
    _zkConn.ensurePersistentNodeExists(_path, callback);
  }

  @Override
  public void setBus(PropertyEventBus<T> bus)
  {
    _eventBus = bus;
  }

  /**
   * Subclasses could override if they need to do something specific, such as delete nodes, etc.
   * @param callback callback
   */
  @Override
  public void shutdown(Callback<None> callback)
  {
    debug(_log, "shutting down");

    callback.onSuccess(None.none());

    info(_log, "shutdown complete");
  }

  public static String getListenTo(String path)
  {
    return new File(path).getName();
  }

  public ZKConnection getClient()
  {
    return _zkConn;
  }

  protected String getPath(String listenTo)
  {
    if(!(_path.equals("/")))
    {
      return _path + "/" + listenTo;
    }
    else
    {
      // path was just a "/", don't add an additional "/"
      return _path + listenTo;
    }
  }

  protected String getPropertyForPath(String path)
  {
    if (path.startsWith(_path + "/"))
    {
      // Return just the property, removing the base path. Need to also handle
      // the case where the base path was just '/'.
      return path.substring(_path.length() + 1);
    }
    if (_path.equals("/"))
    {
      return path.substring(1);
    }
    throw new IllegalArgumentException(path + " is not under " + _path);
  }

  public String getPath()
  {
    return _path;
  }

  public PropertySerializer<T> getSerializer()
  {
    return _serializer;
  }

  public List<String> ls() throws PropertyStoreException
  {
    try
    {
      return _zk.getChildren(_path, false);
    }
    catch (Exception e)
    {
      throw new PropertyStoreException("Failed to list children of path " + _path, e);
    }
  }

  public long getGetCount()
  {
    return _getStats.getCount();
  }

  public long getPutCount()
  {
    return _putStats.getCount();
  }

  public long getRemoveCount()
  {
    return _removeStats.getCount();
  }

  public long getRegisterCount()
  {
    return _registerStats.getCount();
  }

  public long getUnregisterCount()
  {
    return _unregisterStats.getCount();
  }

  protected abstract class ZKStoreWatcher implements Watcher
  {
    private final Object      _mutex        = new Object();
    private final Set<String> _watches  = new HashSet<String>();
    private volatile int      _watchCount;

    public void addWatch(String propertyName)
    {
      String path = getPath(propertyName);
      synchronized (_mutex)
      {
        _watches.add(path);
        _watchCount++;
      }
    }
    public void cancelWatch(String propertyName)
    {
      String path = getPath(propertyName);
      synchronized (_mutex)
      {
        _watches.remove(path);
        _watchCount--;
      }
    }
    public int getWatchCount()
    {
      return _watchCount;
    }

    protected boolean containsWatch(String prop)
    {
      synchronized (_mutex)
      {
        if(_watches.contains(prop))
        {
          return true;
        }
      }
      return false;
    }

    @Override
    public final void process(WatchedEvent watchedEvent)
    {
      _log.debug("Session {}: Received watch type {} for path {} ", new Object[] {
          _zk.getSessionId(), watchedEvent.getType(), watchedEvent.getPath() });
      Event.EventType type = watchedEvent.getType();
      if (type != Event.EventType.None)
      {
        String path = watchedPropertyPath(watchedEvent.getPath());
        if (path == null)
        {
          // Just return and let the watch expire
          _log.debug("Ignoring watch for path {}: {}", path, watchedEvent);
          return;
        }
        _log.debug("processing watch for path: {}", path);
        processWatch(getPropertyForPath(path), watchedEvent);
      }
    }

    /**
     * Return the path that is being watched, or null if the path is
     * not being watched.
     *
     * @param inputPath
     * @return outputString
     */
    protected String watchedPropertyPath(String inputPath)
    {
      // the default is to just return the path that the watchedEvent
      // was for.
      if(containsWatch(inputPath))
      {
        return inputPath;
      }
      return null;
    }

    /**
     * Note it is the responsibility of processWatch to reinstall the watch by
     * making another ZK call that installs the watch.
     *
     * @param propertyName
     * @param event
     */
    protected abstract void processWatch(String propertyName, WatchedEvent event);
  }

}
