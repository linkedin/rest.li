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
import com.linkedin.common.callback.CallbackAdapter;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.util.FileSystemDirectory;
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.file.FileStore;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.trace;

/**
 * The ZooKeeperEphemeralStore has two features:
 * 1) it allows to create ephemeral ZooKeeper nodes through the PropertyStore interface
 * 2) it allows to watch children of a node subscribing through the PropertyEventPublisher interface
 *
 * The store doesn't manage the lifecycle of the connection, which should be handled by the user of this class
 * All the callbacks will be executed through the single threaded ZK event thread
 *
 * Point 1: PropertyStore
 * Through the PropertyStore the user can create ephemeral ZK nodes that will live until the connection
 * assigned to the ZooKeeperEphemeralStore is alive
 *
 * Point 2: PropertyEventPublisher
 * This interface allows the user to subscribe to children of a specific node and their data.
 * There are several modes in which it can run:
 * - watching each node for data change enabling the watchChildNodes flag
 * - considering the children nodes immutable and watching only for membership changes
 */
public class ZooKeeperEphemeralStore<T> extends ZooKeeperStore<T>
{
  private static final Logger                                      _log =
                                                                         LoggerFactory.getLogger(ZooKeeperEphemeralStore.class);
  private static final Pattern PATH_PATTERN    = Pattern.compile("(.*)/(.*)$");

  private final ZooKeeperPropertyMerger<T>                      _merger;
  private final ConcurrentMap<String, EphemeralStoreWatcher> _ephemeralStoreWatchers = new ConcurrentHashMap<>();
  private final String _ephemeralNodesFilePath;
  private final boolean _watchChildNodes;

  //TODO: remove the following members after everybody has migrated to use EphemeralStoreWatcher.
  private final ZKStoreWatcher _zkStoreWatcher = new ZKStoreWatcher();
  private final boolean _useNewWatcher;
  private final ScheduledExecutorService _executorService;
  private final int _zookeeperReadWindowMs;

  public ZooKeeperEphemeralStore(ZKConnection client,
                                 PropertySerializer<T> serializer,
                                 ZooKeeperPropertyMerger<T> merger,
                                 String path)
  {
    this(client, serializer, merger, path, false, false, null);
  }

  public ZooKeeperEphemeralStore(ZKConnection client,
                                 PropertySerializer<T> serializer,
                                 ZooKeeperPropertyMerger<T> merger,
                                 String path,
                                 boolean watchChildNodes)
  {
    this(client, serializer, merger, path, watchChildNodes, false, null);
  }

  public ZooKeeperEphemeralStore(ZKConnection client,
                                 PropertySerializer<T> serializer,
                                 ZooKeeperPropertyMerger<T> merger,
                                 String path,
                                 boolean watchChildNodes,
                                 boolean useNewWatcher)
  {
    this(client, serializer, merger, path, watchChildNodes, useNewWatcher, null);
  }

  /**
   * @param watchChildNodes        if true, a watcher for each children node will be set (this have a large cost)
   * @param ephemeralNodesFilePath if a FS path is specified, children nodes are considered unmodifiable,
   *                               and a local cache for children nodes is enabled
   */
  @Deprecated
  public ZooKeeperEphemeralStore(ZKConnection client,
      PropertySerializer<T> serializer,
      ZooKeeperPropertyMerger<T> merger,
      String path,
      boolean watchChildNodes,
      boolean useNewWatcher,
      String ephemeralNodesFilePath)
  {
    this (client, serializer, merger, path, watchChildNodes, useNewWatcher, ephemeralNodesFilePath,
        null, DEFAULT_READ_WINDOW_MS);
  }

  /**
   * @param watchChildNodes        if true, a watcher for each children node will be set (this have a large cost)
   * @param ephemeralNodesFilePath if a FS path is specified, children nodes are considered unmodifiable,
   *                               and a local cache for children nodes is enabled
   */
  public ZooKeeperEphemeralStore(ZKConnection client,
                                 PropertySerializer<T> serializer,
                                 ZooKeeperPropertyMerger<T> merger,
                                 String path,
                                 boolean watchChildNodes,
                                 boolean useNewWatcher,
                                 String ephemeralNodesFilePath,
                                 ScheduledExecutorService executorService,
                                 int zookeeperReadWindowMs)
  {
    super(client, serializer, path);

    if (watchChildNodes && useNewWatcher)
    {
      throw new IllegalArgumentException("watchChildNodes and useNewWatcher can not both be true.");
    }

    if (watchChildNodes && ephemeralNodesFilePath != null)
    {
      throw new IllegalArgumentException("watchChildNodes and ephemeralNodesFilePath, which enables a local cache for " +
        "ChildNodes, can not both be enabled together.");
    }

    if (ephemeralNodesFilePath != null && !useNewWatcher)
    {
      _log.warn("Forcing enabling useNewWatcher with ephemeralNodesFilePath!=null");
      useNewWatcher = true;
    }

    _merger = merger;
    _watchChildNodes = watchChildNodes;
    _useNewWatcher = useNewWatcher;
    _ephemeralNodesFilePath = ephemeralNodesFilePath;
    _executorService = executorService;
    _zookeeperReadWindowMs = zookeeperReadWindowMs;
  }

  @Override
  public void put(final String prop, final T value, final Callback<None> callback)
  {
    _putStats.inc();

    trace(_log, "put ", prop, ": ", value);

    final String path = getPath(prop);
    _zkConn.ensurePersistentNodeExists(path, new Callback<None>()
    {
      @Override
      public void onSuccess(None none)
      {
        final String ephemeralPath = path + "/ephemoral-";

        AsyncCallback.StringCallback stringCallback = new AsyncCallback.StringCallback()
        {
          @Override
          public void processResult(int rc, String path, Object ctx, String name)
          {
            KeeperException.Code code = KeeperException.Code.get(rc);
            switch (code)
            {
              case OK:
                callback.onSuccess(None.none());
                break;
              default:
                callback.onError(KeeperException.create(code));
                break;
            }
          }
        };

        if (_zk instanceof Retryable)
        {
          ((Retryable) _zk).createUniqueSequential(ephemeralPath, _serializer.toBytes(value), ZooDefs.Ids.OPEN_ACL_UNSAFE,
              CreateMode.EPHEMERAL_SEQUENTIAL, stringCallback, null);
        }
        else
        {
          _zk.create(ephemeralPath, _serializer.toBytes(value), ZooDefs.Ids.OPEN_ACL_UNSAFE,
              CreateMode.EPHEMERAL_SEQUENTIAL, stringCallback, null);
        }
      }

      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }
    });
  }

  @Override
  public void remove(String prop, Callback<None> callback)
  {
    _removeStats.inc();

    trace(_log, "remove: ", prop);

    String path = getPath(prop);
    _zkConn.removeNodeUnsafeRecursive(path, callback);

  }

  public void removePartial(String listenTo, T discoveryProperties) throws PropertyStoreException
  {
    FutureCallback<None> callback = new FutureCallback<None>();
    removePartial(listenTo, discoveryProperties, callback);
    getUninterruptibly(callback);
  }

  public void removePartial(final String prop, final T value, final Callback<None> callback)
  {
    final String path = getPath(prop);

    trace(_log, "remove partial ", prop, ": ", value);

    final Callback<Map<String,T>> childrenCallback = new Callback<Map<String,T>>()
    {
      @Override
      public void onSuccess(Map<String,T> children)
      {
        String delete = _merger.unmerge(prop, value, children);

        //delete string maybe null if children map is empty. Which could happen if
        //someone else deletes the child asynchronously while we're sending
        //a message to delete. So the ChildCollector will not be able to populate the children map
        //with the node that we want to delete.
        if (delete != null)
        {
          _zkConn.removeNodeUnsafe(path + "/" + delete.toString(), callback);
        }
        else
        {
          // node was already deleted.
          callback.onSuccess(None.none());
        }
      }
      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }
    };

    _zk.getChildren(path, false, new AsyncCallback.ChildrenCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children)
      {
        KeeperException.Code code = KeeperException.Code.get(rc);
        switch (code)
        {
          case OK:
            if (children.size() > 0)
            {
              ChildCollector collector = new ChildCollector(children.size(), childrenCallback);
              for (String child : children)
              {
                _zk.getData(path + "/" + child, false, collector, null);
              }
            }
            else
            {
              _log.warn("Ignoring request to removePartial with no children: {}", path);
              callback.onSuccess(None.none());
            }
            break;
          default:
            callback.onError(KeeperException.create(code));
            break;
        }

      }
    }, null);
  }

  @Override
  public void get(final String listenTo, final Callback<T> callback)
  {
    String path = getPath(listenTo);
    // Note ChildrenCallback is compatible with a ZK 3.2 server; Children2Callback is
    // compatible only with ZK 3.3+ server.
    AsyncCallback.ChildrenCallback zkCallback = new AsyncCallback.ChildrenCallback()
    {
      @Override
      public void processResult(int rc, String path, Object context, List<String> children)
      {
        KeeperException.Code result = KeeperException.Code.get(rc);
        switch (result)
        {
          case NONODE:
            callback.onSuccess(null);
            break;

          case OK:
            getMergedChildren(path, children, null, callback);
            break;

          default:
            callback.onError(KeeperException.create(result));
            break;
        }
      }
    };
    _zk.getChildren(path, null, zkCallback, null);
  }

  private void getMergedChildren(String path, List<String> children, ZKStoreWatcher watcher, final Callback<T> callback)
  {
    final String propertyName = getPropertyForPath(path);
    if (children.size() > 0)
    {
      _log.debug("getMergedChildren: collecting {}", children);
      ChildCollector collector = new ChildCollector(children.size(), new CallbackAdapter<T,Map<String,T>>(callback)
      {
        @Override
        protected T convertResponse(Map<String,T> response) throws Exception
        {
          return _merger.merge(propertyName, response.values());
        }
      });
      for (String child : children)
      {
        _zk.getData(path + "/" + child, (_watchChildNodes) ? watcher : null, collector, null);
      }
    }
    else
    {
      _log.debug("getMergedChildren: no children");
      callback.onSuccess(_merger.merge(propertyName, Collections.emptyList()));
    }
  }

  /**
   * Gets children data asynchronously. If the given children collection is empty, the callback is fired
   * immediately.
   */
  private void getChildrenData(String path, Collection<String> children, Callback<Map<String, T>> callback)
  {
    if (children.size() > 0)
    {
      _log.debug("getChildrenData: collecting {}", children);
      ChildCollector collector = new ChildCollector(children.size(), callback);
      children.forEach(child -> _zk.getData(path + "/" + child, null, collector, null));
    }
    else
    {
      _log.debug("getChildrenData: no children");
      callback.onSuccess(Collections.emptyMap());
    }
  }

  @Override
  public void startPublishing(final String prop)
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

    if (_useNewWatcher)
    {
      EphemeralStoreWatcher watcher = _ephemeralStoreWatchers.computeIfAbsent(prop, k -> new EphemeralStoreWatcher(prop));
      watcher.addWatch(prop);
      _zk.getChildren(getPath(prop), watcher, watcher, true);
    }
    else
    {
      _zkStoreWatcher.addWatch(prop);
      _zk.getChildren(getPath(prop), _zkStoreWatcher, _zkStoreWatcher, true);
    }
  }

  @Override
  public void stopPublishing(String prop)
  {
    trace(_log, "unregister: ", prop);

    if (_useNewWatcher)
    {
      EphemeralStoreWatcher watcher = _ephemeralStoreWatchers.remove(prop);
      if (watcher != null)
      {
        watcher.cancelAllWatches();
      }
    }
    else
    {
      _zkStoreWatcher.cancelWatch(prop);
    }
  }

  public int getListenerCount()
  {
    return _useNewWatcher ? _ephemeralStoreWatchers.size() : _zkStoreWatcher.getWatchCount();
  }

  // Note ChildrenCallback is compatible with a ZK 3.2 server; Children2Callback is
  // compatible only with ZK 3.3+ server.
  // TODO: this watcher has an known issue to generate too many zk reads when only a small portion
  // of the children nodes have been changed. We are currently in the process of migrating
  // everybody to the new EphemeralStoreWatcher. After the migration is done, we should remove
  // this class.
  private class ZKStoreWatcher extends ZooKeeperStore<T>.ZKStoreWatcher
    implements AsyncCallback.ChildrenCallback, AsyncCallback.StatCallback
  {
    // Helper function to get parent path
    private String getParentPath(String inputPath)
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
          return parent;
        }
        if (parent != null && parent.isEmpty() && child != null && !child.isEmpty() )
        {
          // this can happen if we were at a child of the root, ie /foo.
          // return the root in this case.
          return "/";
        }
      }
      // Any other case, such as inputPath = "/" or bad path, return null.
      return null;
    }

    @Override
    protected String watchedPropertyPath(String inputPath)
    {
      // given a path, return the path if we're watching it, or the parent
      // if we're watching the parent.
      if (containsWatch(getPropertyForPath(inputPath)))
      {
        return inputPath;
      }
      String parentPath = getParentPath(inputPath);
      // it is possible that parent path equals to base path. Here we check this in advance
      // to avoid IllegalArgumentException when calling getPropertyForPath().
      if (_path.equals(parentPath))
      {
        return null;
      }
      if (parentPath != null && containsWatch(getPropertyForPath(parentPath)))
      {
        return parentPath;
      }
      // if we get here, we weren't watching the node or it's parent
      return null;
    }

    @Override
    public void processWatch(final String propertyName, WatchedEvent watchedEvent)
    {
      // Reset the watch
      _zk.getChildren(getPath(propertyName), this, this, false);
    }

    /**
     * Callback for children call
     */
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
          getMergedChildren(path, children, this, new Callback<T>()
          {
            @Override
            public void onSuccess(T value)
            {
              if (init)
              {
                _eventBus.publishInitialize(property, value);
                _log.debug("{}: published init", path);
              }
              else
              {
                _eventBus.publishAdd(property, value);
                _log.debug("{}: published add", path);
              }
            }

            @Override
            public void onError(Throwable e)
            {
              _log.error("Failed to merge children for path " + path, e);
              if (init)
              {
                _eventBus.publishInitialize(property, null);
                _log.debug("{}: published init", path);
              }
            }
          });

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

    /**
     * Callback for exist call
     */
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

  /**
   * A Children watcher that can be attached to a znode whose children are all ephemeral nodes.
   * It will publish new merged property using {@link ZooKeeperPropertyMerger} whenever the
   * children membership changed. It, however, does NOT capture any data updates on the children
   * node and should NOT be used when {@link this#_watchChildNodes} is {@code true}.
   */
  private class EphemeralStoreWatcher extends ZooKeeperStore<T>.ZKStoreWatcher
      implements AsyncCallback.Children2Callback, AsyncCallback.StatCallback
  {
    // map from child to its data
    private final Map<String, T> _childrenMap = new HashMap<>();

    // property that is being watched
    private final String _prop;

    // id of the transaction that caused the parent node to be created
    private long _czxid = 0;

    // FileStore to save unmodifiable nodes' data
    private FileStore<T> _fileStore = null;

    EphemeralStoreWatcher(String prop)
    {
      _prop = prop;
    }

    @Override
    protected void processWatch(String propertyName, WatchedEvent event)
    {
      // Reset the watch
      if (_zookeeperReadWindowMs > 0 && _executorService != null)
      {
        // Delay setting the watch based on configured _readWindowMs
        int midPoint = _zookeeperReadWindowMs / 2;
        int delay = midPoint + ThreadLocalRandom.current().nextInt(midPoint);
        _executorService.schedule(() -> _zk.getChildren(getPath(propertyName), this, this, false),
            delay, TimeUnit.MILLISECONDS);
      }
      else
      {
        // Set watch Immediately
        _zk.getChildren(getPath(propertyName), this, this, false);
      }
    }

    /**
     * Children callback
     */
    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children, Stat stat)
    {
      KeeperException.Code code = KeeperException.Code.get(rc);
      _log.debug("{}: getChildren returned {}: {}", new Object[]{path, code, children});
      final boolean init = (Boolean)ctx;
      final String property = getPropertyForPath(path);
      switch (code)
      {
        case OK:
        {
          initCurrentNode(stat);
          Set<String> newChildren = calculateChildrenDeltaAndUpdateState(children);
          getChildrenData(path, newChildren, getChildrenDataCallback(path, init, property));
          break;
        }
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
          if (_fileStore != null)
          {
            _fileStore.removeDirectory();
          }
          break;

        default:
          _log.error("getChildren: unexpected error: {}: {}", code, path);
          break;
      }
    }

    private Callback<Map<String, T>> getChildrenDataCallback(String path, boolean init, String property)
    {
      return new Callback<Map<String, T>>()
      {
        @Override
        public void onError(Throwable e)
        {
          _log.error("Failed to merge children for path " + path, e);
          if (init)
          {
            _eventBus.publishInitialize(property, null);
          }
          _log.debug("{}: published init", path);
        }

        @Override
        public void onSuccess(Map<String, T> result)
        {
          _childrenMap.putAll(result);
          if (_fileStore != null)
          {
            result.forEach(_fileStore::put);
          }
          if (init)
          {
            _eventBus.publishInitialize(property, _merger.merge(property, _childrenMap.values()));
            _log.debug("{}: published init", path);
          }
          else
          {
            _eventBus.publishAdd(property, _merger.merge(property, _childrenMap.values()));
            _log.debug("{}: published add", path);
          }
        }
      };
    }

    private void initCurrentNode(Stat stat)
    {
      // in the case of startup or the node gets recreated, create a new file store
      if (_czxid != stat.getCzxid())
      {
        // if node==0 it means that it is just booting up, if it !=0 it means that the node has been recreated
        if (_czxid != 0)
        {
          _childrenMap.clear();
          if (_ephemeralNodesFilePath != null)
          {
            // The file structure for each children saved is: myBasePath/nodeWatchedProp/zkNodeId123/ephemeral-2
            // When the node of nodeWatchedProp gets deleted and recreated, the new directory will be:
            // myBasePath/nodeWatchedProp/zkNodeId234/
            // Therefore we need to clean up from old nodes the directory myBasePath/nodeWatchedProp, to avoid
            // storing unused data, removing the entire directory before creating zkNodeId234
            FileStore.removeDirectory((_ephemeralNodesFilePath + File.separator + _prop));
          }
        }

        _czxid = stat.getCzxid();
        if (_ephemeralNodesFilePath != null)
        {
          _fileStore = new FileStore<>(_ephemeralNodesFilePath + File.separator + _prop + File.separator
            + _czxid, FileSystemDirectory.FILE_STORE_EXTENSION, _serializer);
          _fileStore.start();
          _childrenMap.putAll(_fileStore.getAll());
        }
      }
    }

    private Set<String> calculateChildrenDeltaAndUpdateState(List<String> children)
    {
      // remove children that have been evicted from the map
      Set<String> oldChildren = new HashSet<>(_childrenMap.keySet());
      oldChildren.removeAll(children);
      oldChildren.forEach(_childrenMap::remove);
      if (_fileStore != null)
      {
        oldChildren.forEach(_fileStore::remove);
      }
      Set<String> newChildren = new HashSet<>(children);
      newChildren.removeAll(_childrenMap.keySet());
      return newChildren;
    }

    /**
     * Exist callback
     */
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


  private class ChildCollector implements AsyncCallback.DataCallback
  {
    private int _count;
    private final Map<String,T> _properties;
    private final Callback<Map<String,T>> _callback;

    private ChildCollector(int count, Callback<Map<String,T>> callback)
    {
      _count = count;
      _properties = new HashMap<String,T>(_count);
      _callback = callback;
    }

    @Override
    public void processResult(int rc, String s, Object o, byte[] bytes, Stat stat)
    {
      _count--;
      KeeperException.Code result = KeeperException.Code.get(rc);
      switch (result)
      {
        case OK:
          try
          {
            String childPath = s.substring(s.lastIndexOf('/') + 1);
            T value = _serializer.fromBytes(bytes);
            _properties.put(childPath, value);
            if (_count == 0)
            {
              _callback.onSuccess(_properties);
            }
          }
          catch (PropertySerializationException e)
          {
            _count = 0;
            _callback.onError(e);
          }
          break;

        case NONODE:
          if (_count == 0)
          {
            _callback.onSuccess(_properties);
          }
          _log.debug("{} doesn't exist, count={}", s, _count);
          break;

        default:
          // Set count = 0 so we don't invoke the callback again
          _count = 0;
          _callback.onError(KeeperException.create(KeeperException.Code.get(rc)));
          break;
      }
    }
  }

}
