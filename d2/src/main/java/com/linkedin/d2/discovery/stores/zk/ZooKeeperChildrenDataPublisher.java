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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.pegasus.org.apache.zookeeper.AsyncCallback;
import com.linkedin.pegasus.org.apache.zookeeper.KeeperException;
import com.linkedin.pegasus.org.apache.zookeeper.WatchedEvent;
import com.linkedin.pegasus.org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.linkedin.d2.discovery.util.LogUtil.trace;

/**
 * This class provides the ability to listen to a whole bunch of children under a
 * certain directory in Zookeeper and notify the consumer whenever there is a change
 * of data/membership of any child.
 *
 * Despite the fact that this class extends ZookeeperStore, it should only be used as
 * PropertyEventPublisher. The get/put/remove operation is intentionally not supported to
 * keep the internal data structure lock-free.
 *
 * @author Ang Xu
 * @version $Revision: $
 */
public class ZooKeeperChildrenDataPublisher<T extends Map<String, V>, V> extends ZooKeeperStore<T>
{
  private static final Logger _log = LoggerFactory.getLogger(ZooKeeperChildrenDataPublisher.class);
  private static final Pattern PATH_PATTERN    = Pattern.compile("(.*)/(.*)$");

  private final ZKDataWatcher _zkDataWatcher = new ZKDataWatcher();
  private final ZKChildWatcher _zkChildWatcher = new ZKChildWatcher();
  private final PropertySerializer<V> _childSerializer;



  public ZooKeeperChildrenDataPublisher(ZKConnection client,
                                        PropertySerializer<V> childSerializer,
                                        String path)
  {
    super(client, null, path);
    _childSerializer = childSerializer;
  }

  @Override
  public void startPublishing(String prop)
  {
    trace(_log, "register: ", prop);

    if (_eventBus == null)
    {
      throw new IllegalStateException("_eventBus must not be null when publishing");
    }

    // Zookeeper callbacks (see the listener below) will be executed by the Zookeeper
    // notification thread, in order.  See:
    // http://zookeeper.apache.org/doc/current/zookeeperProgrammers.html#Java+Binding
    // This call occurs on a different thread, the PropertyEventBus callback thread.
    //
    // Publication to the event bus always occurs in the callback which is executed by the
    // ZooKeeper notification thread.  Since ZK guarantees the callbacks will be executed in
    // the same order as the requests were made, we will never publish a stale value to the bus,
    // even if there was a watch set on this property before this call to startPublishing().

    _zkChildWatcher.addWatch(prop);
    _zk.getChildren(getPath(prop), _zkChildWatcher, _zkChildWatcher, true);
  }

  @Override
  public void stopPublishing(String prop)
  {
    trace(_log, "unregister: ", prop);

    _zkChildWatcher.cancelWatch(prop);
    _zkDataWatcher.cancelAllWatches();
  }

  @Override
  public void put(String prop, T value, Callback<None> callback)
  {
    callback.onError(new UnsupportedOperationException("put is not supported"));
  }

  @Override
  public void get(String prop, Callback<T> callback)
  {
    callback.onError(new UnsupportedOperationException("get is not supported"));
  }

  @Override
  public void remove(String prop, Callback<None> callback)
  {
    callback.onError(new UnsupportedOperationException("remove is not supported"));
  }


  private class ZKDataWatcher extends ZooKeeperStore<T>.ZKStoreWatcher
      implements AsyncCallback.DataCallback
  {
    // initialCount and childMap can only be accessed via Zookeeper callbacks.
    // Since zookeeper callbacks will be executed in order, one at a time, no
    // extra synchronization effort is needed.
    // See:
    // http://zookeeper.apache.org/doc/current/zookeeperProgrammers.html#Java+Binding
    private volatile int _initialCount;
    private volatile Map<String, V> _childMap = new HashMap<>();

    private void initialize(String path, List<String> children)
    {
      _initialCount = children.size();
      _childMap = new HashMap<>();
      for (String child : children)
      {
        String childPath = path + "/" + child;
        addWatch(getPropertyForPath(childPath));
        _zk.getData(childPath, this, this, true);
      }
    }

    private void processNewChildren(String path, List<String> children)
    {
      for (String child : children)
      {
        String childPath = path + "/" + child;
        // Notice that for the case where a child node gets deleted, it will also trigger the
        // data watch on that child and will be handled by child's getData() callback automatically.
        // So here we just need to handle the case where new child node is created.
        if (!containsWatch(getPropertyForPath(childPath)))
        {
          addWatch(getPropertyForPath(childPath));
          _zk.getData(childPath, this, this, false);
        }
      }
    }

    // Helper function to get parent path
    private String getPropertyToPublish(String inputPath)
    {
      Matcher m = PATH_PATTERN.matcher(inputPath);

      if (m.matches())
      {
        String parent = m.group(1);
        String child = m.group(2);
        if ( parent != null && !parent.isEmpty() && child != null && !child.isEmpty() )
        {
          // we have both a non-empty parent and a non-empty child, e.g.
          // for "/foo/bar", parent = "/foo", child = "bar".
          // return the parent.
          return getPropertyForPath(parent);
        }
      }
      // Any other case, such as inputPath = "/" or bad path, return null.
      return null;
    }


    @Override
    @SuppressWarnings("unchecked")
    // this callback could be invoked either in the initial phase or in the normal phase.
    // The steps of processing result are slightly different between two phases.
    // - Initial Phase
    //  1. count down the initialCount
    //  2. serialize the child data and put it into childMap
    //  3. if initialCount goes down to 0, meaning we've finished initialization, we call
    //     eventBus#publishInitialize(). Otherwise, we do nothing.
    // - Normal Phase
    //  1. serialize the child data and update the childMap
    //  2. do eventBus#publishAdd() anyway.
    public void processResult(int rc, String path, Object ctx, byte[] bytes, Stat stat)
    {

      KeeperException.Code code = KeeperException.Code.get(rc);
      _log.debug("{}: getData returned {}", path, code);
      final String propToPublish = getPropertyToPublish(path);
      final boolean init = (Boolean)ctx;
      if (init) _initialCount--;
      switch (code)
      {
        case OK:
          try
          {
            V value = _childSerializer.fromBytes(bytes);
            _childMap.put(getPropertyForPath(path), value);
            if (init)
            {
              if (_initialCount == 0)
              {
                _eventBus.publishInitialize(propToPublish, (T) new HashMap<>(_childMap));
                _log.debug("{}: published initialize", propToPublish);
              }
            }
            else
            {
              _eventBus.publishAdd(propToPublish, (T) new HashMap<>(_childMap));
              _log.debug("{}: published add", propToPublish);
            }
          }
          catch (PropertySerializationException e)
          {
            _log.error("Failed to getData for path " + path, e);
            if (init)
            {
              _initialCount = 0;
              _eventBus.publishInitialize(propToPublish, null);
            }
          }
          break;

        case NONODE:
          cancelWatch(getPropertyForPath(path));
          _childMap.remove(getPropertyForPath(path));
          if (init)
          {
            if (_initialCount == 0)
            {
              _eventBus.publishInitialize(propToPublish,(T) new HashMap<>(_childMap));
              _log.debug("{}: published initialize", propToPublish);
            }
          }
          else
          {
            _eventBus.publishAdd(propToPublish, (T) new HashMap<>(_childMap));
            _log.debug("{}: published add", propToPublish);
          }
          break;

        default:
          _log.error("getData: unexpected error: {}: {}", code, path);
          if (init)
          {
            _initialCount = 0;
            _eventBus.publishInitialize(propToPublish, null);
          }
          break;
      }
    }

    @Override
    public void processWatch(final String propertyName, WatchedEvent watchedEvent)
    {
      // Reset the watch
      _zk.getData(getPath(propertyName), this, this, false);
    }
  }

  private class ZKChildWatcher extends ZooKeeperStore<T>.ZKStoreWatcher
      implements AsyncCallback.ChildrenCallback, AsyncCallback.StatCallback
  {
    @Override
    public void processWatch(final String propertyName, WatchedEvent watchedEvent)
    {
      // Reset the watch
      _zk.getChildren(getPath(propertyName), this, this, false);
    }

    @Override
    public void processResult(int rc, final String path, Object ctx, List<String> children)
    {
      KeeperException.Code code = KeeperException.Code.get(rc);
      _log.debug("{}: getChildren returned {}: {}", new Object[]{path, code, children});
      final boolean init = (Boolean)ctx;
      final String property = getPropertyForPath(path);
      switch (code)
      {
        case OK:
          if (init)
          {
            _zkDataWatcher.initialize(path, children);
          }
          else
          {
            _zkDataWatcher.processNewChildren(path, children);
          }
          break;

        case NONODE:
          // The node whose children we are monitoring is gone; set an exists watch on it
          _log.debug("{}: node is not present, calling exists", path);
          _zk.exists(path, this, this, false);
          if (init)
          {
            _eventBus.publishInitialize(property, null);
            _log.debug("{}: published init", path);
          }
          else
          {
            _eventBus.publishRemove(property);
            _log.debug("{}: published remove", path);
          }
          break;

        default:
          _log.error("getChildren: unexpected error: {}: {}", code, path);
          break;
      }
    }

    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat)
    {
      KeeperException.Code code = KeeperException.Code.get(rc);
      _log.debug("{}: exists returned {}", path, code);
      switch (code)
      {
        case OK:
          // The node is back, get children and set child watch
          _log.debug("{}: calling getChildren", path);
          _zk.getChildren(path, this, this, false);
          break;

        case NONODE:
          // The node doesn't exist; OK, the watch is set so now we wait.
          _log.debug("{}: set exists watch", path);
          break;

        default:
          _log.error("exists: unexpected error: {}: {}", code, path);
          break;
      }
    }
  }
}
