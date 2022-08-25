/*
   Copyright (c) 2014 LinkedIn Corp.

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

import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.pegasus.org.apache.zookeeper.AsyncCallback;
import com.linkedin.pegasus.org.apache.zookeeper.CreateMode;
import com.linkedin.pegasus.org.apache.zookeeper.KeeperException;
import com.linkedin.pegasus.org.apache.zookeeper.WatchedEvent;
import com.linkedin.pegasus.org.apache.zookeeper.Watcher;
import com.linkedin.pegasus.org.apache.zookeeper.data.ACL;
import com.linkedin.pegasus.org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class extends the vanilla ZooKeeper and is aware of symbolic links in the input path. Here we define symbolic
 * link(symlink) as a special znode whose name is prefixed with a dollar sign '$' (eg. /$symlink1, /foo/bar/$symlink2),
 * and its data field stores the real path this symlink points to. A symlink znode can be created, updated or deleted
 * via regular {@link ZKConnection} which is not necessarily symlink aware. However, any client wants to operate
 * transparently on symlink has to use the overridden operation provided by this class. More specifically, only
 * asynchronous read operations are overridden to resolve symlinks. All write operations are leaving untouched since it
 * adds more complications and we don't have any use case in d2 requires it.
 *
 * The read operation to symlink without a watcher is effectively the same as read operation to the real znode, except
 * the path name user sees in the callback is the raw symlink path instead of the real path. If a watcher is associated
 * with a read operation, then the watcher will be attached to not only the real znode but also all the intermediate
 * symlinks. However, only the first watch event we receive will be processed per read operation in order to maintain
 * the definition of "one-time trigger" for ZooKeeper Watches.
 *
 * See: http://zookeeper.apache.org/doc/trunk/zookeeperProgrammers.html#ch_zkWatches.
 *
 * @author Ang Xu
 * @version $Revision: $
 */
public class SymlinkAwareZooKeeper extends AbstractZooKeeper
{
  private static final Logger LOG = LoggerFactory.getLogger(SymlinkAwareZooKeeper.class);
  private final PropertySerializer<String>  _serializer;
  private final Watcher                     _defaultWatcher;

  public  SymlinkAwareZooKeeper(ZooKeeper zk, Watcher watcher)
  {
    this(zk, watcher, new DefaultSerializer());
  }

  public SymlinkAwareZooKeeper(ZooKeeper zk, Watcher watcher, PropertySerializer<String> serializer)
  {
    super(zk);
    _defaultWatcher = watcher;
    _serializer = serializer;
  }

  @Override
  public void exists(final String path, final boolean watch, final AsyncCallback.StatCallback cb, final Object ctx)
  {
    if (!SymlinkUtil.containsSymlink(path))
    {
      _zk.exists(path, watch, cb, ctx);
    }
    else
    {
      SymlinkStatCallback compositeCallback = new SymlinkStatCallback(path, _defaultWatcher, cb);
      exists0(path, watch ? compositeCallback : null, compositeCallback, ctx);
    }
  }

  @Override
  public void exists(final String path, final Watcher watcher, final AsyncCallback.StatCallback cb, final Object ctx)
  {
    if (!SymlinkUtil.containsSymlink(path))
    {
      _zk.exists(path, watcher, cb, ctx);
    }
    else
    {
      SymlinkStatCallback compositeCallback = new SymlinkStatCallback(path, watcher, cb);
      exists0(path, watcher != null ? compositeCallback : null, compositeCallback, ctx);
    }
  }

  @Override
  public void getChildren(final String path, final boolean watch, final AsyncCallback.ChildrenCallback cb, final Object ctx)
  {
    if (!SymlinkUtil.containsSymlink(path))
    {
      _zk.getChildren(path, watch, cb, ctx);
    }
    else
    {
      SymlinkChildrenCallback compositeCallback = new SymlinkChildrenCallback(path, _defaultWatcher, cb);
      getChildren0(path, watch ? compositeCallback : null, compositeCallback, ctx);
    }
  }

  @Override
  public void getChildren(final String path, final Watcher watcher, final AsyncCallback.ChildrenCallback cb, final Object ctx)
  {
    if (!SymlinkUtil.containsSymlink(path))
    {
      _zk.getChildren(path, watcher, cb, ctx);
    }
    else
    {
      SymlinkChildrenCallback compositeCallback = new SymlinkChildrenCallback(path, watcher, cb);
      getChildren0(path, watcher != null ? compositeCallback : null, compositeCallback, ctx);
    }
  }

  @Override
  public void getChildren(final String path, Watcher watcher, AsyncCallback.Children2Callback cb, Object ctx)
  {
    if (!SymlinkUtil.containsSymlink(path))
    {
      _zk.getChildren(path, watcher, cb, ctx);
    }
    else
    {
      SymlinkChildren2Callback compositeCallback = new SymlinkChildren2Callback(path, watcher, cb);
      getChildren2(path, watcher != null ? compositeCallback : null, compositeCallback, ctx);
    }
  }

  @Override
  public void getData(final String path, final boolean watch, final AsyncCallback.DataCallback cb, final Object ctx)
  {
    if (!SymlinkUtil.containsSymlink(path))
    {
      _zk.getData(path, watch, cb, ctx);
    }
    else
    {
      SymlinkDataCallback compositeCallback = new SymlinkDataCallback(path, _defaultWatcher, cb);
      getData0(path, watch ? compositeCallback : null, compositeCallback, ctx);
    }
  }

  @Override
  public void getData(final String path, final Watcher watcher, final AsyncCallback.DataCallback cb, final Object ctx)
  {
    if (!SymlinkUtil.containsSymlink(path))
    {
      _zk.getData(path, watcher, cb, ctx);
    }
    else
    {
      SymlinkDataCallback compositeCallback = new SymlinkDataCallback(path, watcher, cb);
      getData0(path, watcher != null ? compositeCallback : null, compositeCallback, ctx);
    }
  }

  public void rawGetData(String path, boolean watch, AsyncCallback.DataCallback cb, Object ctx)
  {
    _zk.getData(path, watch, cb, ctx);
  }

  public void rawGetData(String path, Watcher watcher, AsyncCallback.DataCallback cb, Object ctx)
  {
    _zk.getData(path, watcher, cb, ctx);
  }

  public void rawExists(String path, boolean watch, AsyncCallback.StatCallback cb, Object ctx)
  {
    _zk.exists(path, watch, cb, ctx);
  }

  public void rawExists(String path, Watcher watcher, AsyncCallback.StatCallback cb, Object ctx)
  {
    _zk.exists(path, watcher, cb, ctx);
  }

  public void rawGetChildren(String path, boolean watch, AsyncCallback.ChildrenCallback cb, Object ctx)
  {
    _zk.getChildren(path, watch, cb, ctx);
  }

  public void rawGetChildren(String path, Watcher watcher, AsyncCallback.ChildrenCallback cb, Object ctx)
  {
    _zk.getChildren(path, watcher, cb, ctx);
  }

  private void getData0(final String path, final SymlinkWatcher watcher, final AsyncCallback.DataCallback cb, final Object ctx)
  {
    int index = SymlinkUtil.firstSymlinkIndex(path);
    if (index < 0)
    {
      _zk.getData(path, watcher, cb, ctx);
    }
    else
    {
      String symlink = path.substring(0, index);
      final String remainPath = path.substring(index);
      // TODO: instead of resolving the symlink everytime it is requested, we probably can cache
      // the resolve results and rely on the watch event associated with the symlink to invalidate the cache.
      AsyncCallback.DataCallback resolveCallback = new AsyncCallback.DataCallback()
      {
        @Override
        public void processResult(int rc, String path, Object ctx, byte data[], Stat stat)
        {
          KeeperException.Code result = KeeperException.Code.get(rc);
          switch (result)
          {
            case OK:
              try
              {
                String realPath = _serializer.fromBytes(data);
                getData0(realPath + remainPath, watcher, cb, ctx);
              }
              catch (Exception e)
              {
                if (watcher != null) watcher.disable();
                cb.processResult(KeeperException.Code.NONODE.intValue(), path, ctx, null, null);
                LOG.warn("Exception when resolving symlink: " + path, e);
              }
              break;

            default:
              if (watcher != null) watcher.disable();
              cb.processResult(rc, path, ctx, data, stat);
              break;
          }
        }
      };
      _zk.getData(symlink, watcher, resolveCallback, ctx);
    }
  }

  private void exists0(final String path, final SymlinkWatcher watcher, final AsyncCallback.StatCallback cb, final Object ctx)
  {
    int index = SymlinkUtil.firstSymlinkIndex(path);
    if (index < 0)
    {
      _zk.exists(path, watcher, cb, ctx);
    }
    else
    {
      String symlink = path.substring(0, index);
      final String remainPath = path.substring(index);
      AsyncCallback.DataCallback resolveCallback = new AsyncCallback.DataCallback()
      {
        @Override
        public void processResult(int rc, String p, Object c, byte data[], Stat s)
        {
          KeeperException.Code result = KeeperException.Code.get(rc);
          switch (result)
          {
            case OK:
              try
              {
                String realPath = _serializer.fromBytes(data);
                exists0(realPath + remainPath, watcher, cb, ctx);
              }
              catch (Exception e)
              {
                // we don't want to disable watch here because NONODE is not an exception
                // in exists() call, so the watch is still valid.
                cb.processResult(KeeperException.Code.NONODE.intValue(), path, ctx, null);
                LOG.warn("Exception when resolving symlink: " + path, e);
              }
              break;

            case NONODE:
              // the intermediate symlink znode doesn't exist. we should attach an existsWatcher to this znode,
              // otherwise we won't get notified once it is back later.
              //
              // Note that we don't have to do this for getData0() and getChildren0() because NONODE is considered
              // as exception for these two operations and by definition no watcher should be attached if an exception
              // is thrown.
              AsyncCallback.StatCallback existsCallback = new AsyncCallback.StatCallback()
              {
                @Override
                public void processResult(int rc, String p, Object c, Stat s)
                {
                  KeeperException.Code code = KeeperException.Code.get(rc);
                  switch (code)
                  {
                    case OK:
                      // the symlink znode is back, resume the original call.
                      exists0(path, watcher, cb, ctx);
                      break;

                    case NONODE:
                      // the existsWatcher has been attached. we just need to invoke the user callback in this case.
                      cb.processResult(rc, path, ctx, s);
                      break;

                    default:
                      // the call failed with some other reasons. disable the watcher and invoke the user callback.
                      if (watcher != null) watcher.disable();
                      cb.processResult(rc, path, ctx, s);
                      break;
                  }
                }
              };
              _zk.exists(p, watcher, existsCallback, ctx);
              break;

            default:
              if (watcher != null) watcher.disable();
              cb.processResult(rc, path, ctx, s);
              break;
          }
        }
      };
      _zk.getData(symlink, watcher, resolveCallback, ctx);
    }
  }

  private void getChildren0(final String path, final SymlinkWatcher watcher, final AsyncCallback.ChildrenCallback cb, final Object ctx)
  {
    int index = SymlinkUtil.firstSymlinkIndex(path);
    if (index < 0)
    {
      _zk.getChildren(path, watcher, cb, ctx);
    }
    else
    {
      String symlink = path.substring(0, index);
      final String remainPath = path.substring(index);
      AsyncCallback.DataCallback resolveCallback = new AsyncCallback.DataCallback()
      {
        @Override
        public void processResult(int rc, String path, Object ctx, byte data[], Stat stat)
        {
          KeeperException.Code result = KeeperException.Code.get(rc);
          switch (result)
          {
            case OK:
              try
              {
                String realPath = _serializer.fromBytes(data);
                getChildren0(realPath + remainPath, watcher, cb, ctx);
              }
              catch (Exception e)
              {
                if (watcher != null) watcher.disable();
                cb.processResult(KeeperException.Code.NONODE.intValue(), path, ctx, Collections.<String>emptyList());
                LOG.warn("Exception when resolving symlink: " + path, e);
              }
              break;

            default:
              if (watcher != null) watcher.disable();
              cb.processResult(rc, path, ctx, Collections.<String>emptyList());
              break;
          }
        }
      };
      _zk.getData(symlink, watcher, resolveCallback, ctx);
    }
  }

  private void getChildren2(final String path, final SymlinkWatcher watcher, final AsyncCallback.Children2Callback cb, final Object ctx)
  {
    int index = SymlinkUtil.firstSymlinkIndex(path);
    if (index < 0)
    {
      _zk.getChildren(path, watcher, cb, ctx);
    }
    else
    {
      String symlink = path.substring(0, index);
      final String remainPath = path.substring(index);
      AsyncCallback.DataCallback resolveCallback = new AsyncCallback.DataCallback()
      {
        @Override
        public void processResult(int rc, String path, Object ctx, byte data[], Stat stat)
        {
          KeeperException.Code result = KeeperException.Code.get(rc);
          switch (result)
          {
            case OK:
              try
              {
                String realPath = _serializer.fromBytes(data);
                getChildren2(realPath + remainPath, watcher, cb, ctx);
              }
              catch (Exception e)
              {
                if (watcher != null) watcher.disable();
                cb.processResult(KeeperException.Code.NONODE.intValue(), path, ctx, Collections.<String>emptyList(), null);
                LOG.warn("Exception when resolving symlink: " + path, e);
              }
              break;

            default:
              if (watcher != null) watcher.disable();
              cb.processResult(rc, path, ctx, Collections.<String>emptyList(), null);
              break;
          }
        }
      };
      _zk.getData(symlink, watcher, resolveCallback, ctx);
    }
  }


  private abstract class SymlinkWatcher implements Watcher
  {
    protected final Watcher _watch;
    protected final String _rawPath;
    protected AtomicBoolean _disabled = new AtomicBoolean();
    protected volatile WatchedEvent _pendingEvent;
    protected volatile boolean _callbackInvoked = false;

    public SymlinkWatcher(Watcher watch, String rawPath)
    {
      _watch = watch;
      _rawPath = rawPath;
      _pendingEvent = null;
    }

    @Override
    public void process(WatchedEvent event)
    {
      // We disable the watch once we receive the first watched event. Otherwise
      // the user watcher may receive more than 1 event, which breaks the definition
      // of "one-time trigger".
      // See: http://zookeeper.apache.org/doc/trunk/zookeeperProgrammers.html#ch_zkWatches.
      if (_disabled.getAndSet(true)) return;

      WatchedEvent newEvent = newWatchedEvent(event);
      // In vanilla ZooKeeper, we can only receive a watch event after the
      // callback associated with the request has been successfully invoked.
      // While it is entirely possible in our implementation that a watch event
      // triggered by an intermediate symlink arrives in advance. In such case,
      // we should defer the watch event until the request callback is invoked.
      if (_callbackInvoked)
      {
        _watch.process(newEvent);
      }
      else
      {
        _pendingEvent = newEvent;
      }
    }

    public void disable()
    {
      _disabled.set(true);
      _pendingEvent = null;
    }

    protected WatchedEvent newWatchedEvent(WatchedEvent event)
    {
      return new WatchedEvent(event.getType(), event.getState(), _rawPath);
    }
  }

  private class SymlinkDataCallback extends SymlinkWatcher implements AsyncCallback.DataCallback
  {
    private final AsyncCallback.DataCallback _callback;

    public SymlinkDataCallback (String rawPath, Watcher watch, AsyncCallback.DataCallback cb)
    {
      super(watch, rawPath);
      _callback = cb;
    }

    @Override
    public void processResult(int rc, String path, Object ctx, byte[] bytes, Stat stat)
    {
      _callback.processResult(rc, _rawPath, ctx, bytes, stat);
      _callbackInvoked = true;
      // flush out the pending watch event if necessary.
      if (_pendingEvent != null)
      {
        _watch.process(_pendingEvent);
      }
    }
  }

  private class SymlinkStatCallback extends SymlinkWatcher implements AsyncCallback.StatCallback
  {
    private final StatCallback _callback;

    public SymlinkStatCallback(String rawPath, Watcher watch, StatCallback cb)
    {
      super(watch, rawPath);
      _callback = cb;
    }

    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat)
    {
      _callback.processResult(rc, _rawPath, ctx, stat);
      _callbackInvoked = true;
      // flush out the pending watch event if necessary.
      if (_pendingEvent != null)
      {
        _watch.process(_pendingEvent);
      }
    }
  }

  private class SymlinkChildrenCallback extends SymlinkWatcher implements AsyncCallback.ChildrenCallback
  {
    private final ChildrenCallback _callback;

    public SymlinkChildrenCallback(String rawPath, Watcher watch, ChildrenCallback cb)
    {
      super(watch, rawPath);
      _callback = cb;
    }

    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children)
    {
      _callback.processResult(rc, _rawPath, ctx, children);
      _callbackInvoked = true;
      // flush out the pending watch event if necessary.
      if (_pendingEvent != null)
      {
        _watch.process(_pendingEvent);
      }
    }

    @Override
    public WatchedEvent newWatchedEvent(WatchedEvent event)
    {
      if (event.getType() == Event.EventType.NodeDataChanged)
      {
        return new WatchedEvent(Event.EventType.NodeChildrenChanged, event.getState(), _rawPath);
      }
      else
      {
        return new WatchedEvent(event.getType(), event.getState(), _rawPath);
      }
    }
  }

  private class SymlinkChildren2Callback extends SymlinkWatcher implements AsyncCallback.Children2Callback
  {
    private final Children2Callback _callback;

    public SymlinkChildren2Callback(String rawPath, Watcher watch, Children2Callback cb)
    {
      super(watch, rawPath);
      _callback = cb;
    }

    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children, Stat stat)
    {
      _callback.processResult(rc, _rawPath, ctx, children, stat);
      _callbackInvoked = true;
      // flush out the pending watch event if necessary.
      if (_pendingEvent != null)
      {
        _watch.process(_pendingEvent);
      }
    }

    @Override
    public WatchedEvent newWatchedEvent(WatchedEvent event)
    {
      if (event.getType() == Event.EventType.NodeDataChanged)
      {
        return new WatchedEvent(Event.EventType.NodeChildrenChanged, event.getState(), _rawPath);
      }
      else
      {
        return new WatchedEvent(event.getType(), event.getState(), _rawPath);
      }
    }
  }

  public static class DefaultSerializer implements PropertySerializer<String>
  {
    @Override
    public String fromBytes(byte[] bytes) throws PropertySerializationException
    {
      try
      {
        return new String(bytes, "UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
        throw new PropertySerializationException(e);
      }
    }

    @Override
    public byte[] toBytes(String property)
    {
      try
      {
        return property.getBytes("UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
        throw new RuntimeException(e);
      }
    }
  }
}
