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

import com.linkedin.pegasus.org.apache.zookeeper.AsyncCallback;
import com.linkedin.pegasus.org.apache.zookeeper.CreateMode;
import com.linkedin.pegasus.org.apache.zookeeper.Watcher;
import com.linkedin.pegasus.org.apache.zookeeper.KeeperException;
import com.linkedin.pegasus.org.apache.zookeeper.data.ACL;
import com.linkedin.pegasus.org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *  This class extends the vanilla ZooKeeper and retries operations when ConnectionLossException happens.
 *  Asynchronous operation functions are overridden. More specifically, only the asynchronous operation
 *  functions used in d2 are overridden to retry when loss happens.
 *  <p>
 *  All other code that uses ZooKeeper directly won't be affected, except a few lines in ZKConnection.
 *  This avoids the work to modify the callbacks for all ZooKeeper operations invoked by other code.
 *  Instead, we wrap the original callbacks inside this Wrapper class to make the callbacks handle the
 *  ConnectionLoss situation.
 *  </p>
 *  <p>
 *  All read operations will be retried until the retry limit has been reached.
 *  For write operations,
 *    - setData and delete will be retried until success or reach the limit
 *    - create is a little more complicated
 *      - for non-sequential CreateMode, we will retry until success or reach the limit
 *      - for sequential CreateMode, we need to check the success of previous create by scanning the nodes
 *        owned by us; if previous create succeeded, return, otherwise retry until reach the limit
 *  </p>
 *  <p>
 *  Presumably, we may want to retry asap to avoid extra delay, but it may also help to step back and wait a
 *  while before retry (in case of network congestion). Hence, a exponential-backoff retry strategy is also
 *  provided.
 *  </p>
 */

public class RetryZooKeeper extends AbstractZooKeeper implements Retryable
{
  private static final Logger               _log = LoggerFactory.getLogger( RetryZooKeeper.class);
  // retry limit
  private final int                         _limit;
  private final ScheduledExecutorService    _scheduler;
  private final boolean                     _exponentialBackoff;
  private final long                        _initInterval;
  private long                              _interval;
  // UUID for this ZooKeeper instance, used in the name of the ephemeral nodes created by this instance
  // so that we can quickly identify the ephemeral nodes owned itself
  private final UUID                        _uuid = UUID.randomUUID();

  public RetryZooKeeper(String connectionString, int sessionTimeout, Watcher watcher, int limit)
      throws IOException
  {
    this(connectionString, sessionTimeout, watcher, limit, false, null, 0);
  }

  public RetryZooKeeper(String connectionString, int sessionTimeout, Watcher watcher, int limit,
                        boolean exponentialBackoff, ScheduledExecutorService scheduler, long initInterval)
      throws IOException
  {
    super(new VanillaZooKeeperAdapter(connectionString, sessionTimeout, watcher));
    _limit = limit;
    _exponentialBackoff = exponentialBackoff;
    _scheduler = scheduler;
    _initInterval = initInterval;
    _interval = _initInterval;
  }

  public RetryZooKeeper(ZooKeeper zk, int limit) throws IOException
  {
    this(zk, limit, false, null, 0);
  }

  public RetryZooKeeper(ZooKeeper zk, int limit, boolean exponentialBackoff, ScheduledExecutorService scheduler,
                        long initInterval) throws IOException
  {
    super(zk);
    _limit = limit;
    _exponentialBackoff = exponentialBackoff;
    _scheduler = scheduler;
    _initInterval = initInterval;
    _interval = _initInterval;
  }

  // for test
  public UUID getUuid()
  {
    return _uuid;
  }

  // for test
  public long getInterval()
  {
    return _interval;
  }

  // Create sequential node with retry.
  // To make retry efficient, a uuid of this ZooKeeper will be appended to the path passed in.
  // The appended path will be returned in case the caller need to know the full prefix of the sequential node.
  @Override
  public void createUniqueSequential(final String path, final byte[] data, final List<ACL> acl, final CreateMode createMode,
                                       final AsyncCallback.StringCallback cb, final Object ctx)
  {
    if(!createMode.isSequential())
    {
      create(path, data, acl, createMode, cb, ctx);
    }

    final String retryPath = path + "-" + _uuid.toString() + "-";
    final RetryCallback callback = new RetryCallback() {
      @Override
      protected void retry() {
        final String parentPath = path.substring(0, path.lastIndexOf('/'));
        final StringCallback stringCallback = this;

        final ChildrenCallback childrenCallback = new ChildrenCallback(){
          @Override
          public void processResult(int ccRC, String ccPath, Object ccCtx, List<String> ccChildren)
          {
            KeeperException.Code code = KeeperException.Code.get(ccRC);
            // we don't have to handle CONNECTIONLOSS here; it would be handled by the retry version of getChildren
            switch(code)
            {
              case OK:
                List<String> ourChildren = new ArrayList<>();
                for(final String child : ccChildren)
                {
                  if(child.contains(_uuid.toString()))
                  {
                    ourChildren.add(child);
                  }
                }
                if (ourChildren.size() > 0)
                {
                  ChildrenInspector inspector = new ChildrenInspector(ourChildren.size());
                  for (final String ourChild : ourChildren)
                  {
                    // user retry version of getData here
                    getData(parentPath + "/" + ourChild, false, inspector, null);
                  }
                }
                else
                {
                  // no children belong to us found, retry create directly
                  _log.info("Retry create operation: path = " + retryPath + "data length: " + getDataLength(data));
                  zkCreate(retryPath, data, acl, createMode, stringCallback, ctx);
                }
                break;
              default:
                _log.error("Retry create aborted in getChildren. KeeperException code: " + code);
                break;
            }
          }

          class ChildrenInspector implements DataCallback{
            private int _count;
            ChildrenInspector(int count)
            {
              _count = count;
            }
            @Override
            public void processResult(int dcRC, String dcPath, Object dcCtx, byte[] dcData, Stat dcStat)
            {
              KeeperException.Code code = KeeperException.Code.get(dcRC);
              // we don't have to handle CONNECTIONLOSS here
              switch (code)
              {
                case OK:
                  if(Arrays.equals(data, dcData))
                  {
                    // we find the data we wanted to create
                    // do not decrement _count
                    // retry create won't be triggered as a result
                  }
                  else
                  {
                    // this is not the data we wanted to create
                    _count--;
                    if (_count == 0)
                    {
                      // this is the last child to be inspected
                      // all previous children do not have the data we wanted to create
                      // trigger retry create
                      _log.info("Retry create operation: path = " + retryPath + "data length: " + getDataLength(data));
                      zkCreate(retryPath, data, acl, createMode, stringCallback, ctx);
                    }
                  }
                  break;
                default:
                  _log.error("Retry create stopped in getData. KeeperException code: " + code);
                  break;
              }
            }
          }
        };
        // use retry version of getChildren
        getChildren(parentPath, false, childrenCallback, null);
      }

      @Override
      protected void processStringResult(int cbRC, String cbPath, Object cbCtx, String cbName)
      {
        cb.processResult(cbRC, cbPath, cbCtx, cbName);
      }
    };

    zkCreate(retryPath, data, acl, createMode, callback, ctx);
  }

  @Override
  public void create(final String path, final byte[] data, final List<ACL> acl, final CreateMode createMode,
                     final AsyncCallback.StringCallback cb, final Object ctx)
  {
    final RetryCallback callback = new RetryCallback() {
      @Override
      protected void retry() {
        if (!createMode.isSequential())
        {
          // it's always safe to retry create for non-sequential names
          _log.info("Retry create operation: path = " + path + " data length " + getDataLength(data));
          zkCreate(path, data, acl, createMode, this, ctx);
        }
        else
        {
          _log.error("Connection lost during create operation of sequential node. " +
              "Consider using createUniqueSequential() instead");
        }
      }

      @Override
      protected void processStringResult(int cbRC, String cbPath, Object cbCtx, String cbName)
      {
        cb.processResult(cbRC, cbPath, cbCtx, cbName);
      }
    };

    zkCreate(path, data, acl, createMode, callback, ctx);
  }

  @Override
  public void delete(final String path, final int version, final AsyncCallback.VoidCallback cb, final Object ctx)
  {
    final RetryCallback callback = new RetryCallback() {
      @Override
      protected void retry() {
        _log.info("Retry delete operation: path = " + path + " version = " + version);
        zkDelete(path, version, this, ctx);
      }
      @Override
      protected void processVoidResult(int cbRC, String cbPath, Object cbCtx)
      {
        cb.processResult(cbRC, cbPath, cbCtx);
      }
    };
    zkDelete(path, version, callback, ctx) ;
  }

  @Override
  public void exists(final String path, final Watcher watcher, final AsyncCallback.StatCallback cb, final Object ctx)
  {
    final RetryCallback callback = new RetryCallback() {
      @Override
      protected void retry() {
        _log.info("Retry exists operation: path = " + path);
        zkExists(path, watcher, this, ctx);
      }
      @Override
      protected void processStatResult(int cbRC, String cbPath, Object cbCtx, Stat cbStat)
      {
        cb.processResult(cbRC, cbPath, cbCtx, cbStat);
      }
    };
    zkExists(path, watcher, callback, ctx);
  }

  @Override
  public void exists(final String path, final boolean watch, final AsyncCallback.StatCallback cb, final Object ctx)
  {
    final RetryCallback callback = new RetryCallback() {
      @Override
      protected void retry() {
        _log.info("Retry exists operation: path = " + path);
        zkExists(path, watch, this, ctx);
      }
      @Override
      protected void processStatResult(int cbRC, String cbPath, Object cbCtx, Stat cbStat)
      {
        cb.processResult(cbRC, cbPath, cbCtx, cbStat);
      }
    };
    zkExists(path, watch, callback, ctx);
  }

  @Override
  public void getChildren(final String path, final Watcher watcher, final AsyncCallback.ChildrenCallback cb, final Object ctx)
  {
    final RetryCallback callback = new RetryCallback() {
      @Override
      protected void retry() {
        _log.info("Retry getChildren operation: path = " + path);
        zkGetChildren(path, watcher, this, ctx);
      }
      @Override
      protected void processChildrenResult(int cbRC, String cbPath, Object cbCtx, List<String> cbChildren)
      {
        cb.processResult(cbRC, cbPath, cbCtx, cbChildren);
      }

    };
    zkGetChildren(path, watcher, callback, ctx);
  }

  @Override
  public void getChildren(final String path, final boolean watch, final AsyncCallback.ChildrenCallback cb, final Object ctx)
  {
    final RetryCallback callback = new RetryCallback() {
      @Override
      protected void retry() {
        _log.info("Retry getChildren operation: path = " + path);
        zkGetChildren(path, watch, this, ctx);
      }
      @Override
      protected void processChildrenResult(int cbRC, String cbPath, Object cbCtx, List<String> cbChildren)
      {
        cb.processResult(cbRC, cbPath, cbCtx, cbChildren);
      }

    };
    zkGetChildren(path, watch, callback, ctx);
  }

  @Override
  public void getData(final String path, final Watcher watcher, final AsyncCallback.DataCallback cb, final Object ctx)
  {
    final RetryCallback callback = new RetryCallback() {
      @Override
      protected void retry() {
        _log.info("Retry getData operation: path = " + path);
        zkGetData(path, watcher, this, ctx);
      }
      @Override
      protected void processDataResult(int cbRC, String cbPath, Object cbCtx, byte cbData[], Stat cbStat)
      {
        cb.processResult(cbRC, cbPath, cbCtx, cbData, cbStat);
      }
    };
    zkGetData(path, watcher , callback , ctx);
  }

  @Override
  public void getData(final String path, final boolean watch, final AsyncCallback.DataCallback cb, final Object ctx)
  {
    final RetryCallback callback = new RetryCallback() {
      @Override
      protected void retry() {
        _log.info("Retry getData operation: path = " + path);
        zkGetData(path, watch, this, ctx);
      }
      @Override
      protected void processDataResult(int cbRC, String cbPath, Object cbCtx, byte cbData[], Stat cbStat)
      {
        cb.processResult(cbRC, cbPath, cbCtx, cbData, cbStat);
      }
    };
    zkGetData(path, watch , callback , ctx);

  }

  @Override
  public void setData(final String path, final byte[] data, final int version,
                      final AsyncCallback.StatCallback cb, final Object ctx)
  {
    final RetryCallback callback = new RetryCallback() {
      @Override
      protected void retry() {
        _log.info("Retry setData operation: path = " + path + " version = " + version + " data length " + getDataLength(data));
        zkSetData(path, data, version, this, ctx);
      }
      @Override
      protected void processStatResult(int cbRC, String cbPath, Object cbCtx, Stat cbStat)
      {
        cb.processResult(cbRC, cbPath, cbCtx, cbStat);
      }
    };
    zkSetData(path, data, version, callback, ctx);
  }

  private int getDataLength(byte [] data)
  {
    return data == null ? 0 : data.length;
  }

  /*
  This is the master abstract class for AsyncCallbacks that can handle retry.
  We keep the states (retry times, intervals) by ourselves, rather than relying on context object
  passed to ZooKeeper, as the original callback may rely on the context object and we don't want
  to touch that.
  */
  private abstract class RetryCallback implements AsyncCallback.DataCallback, AsyncCallback.ChildrenCallback,
      AsyncCallback.StatCallback, AsyncCallback.StringCallback, AsyncCallback.VoidCallback
  {
    private int             _retry = 0;

    // subclass implements retry according to their own strategy.
    protected abstract void retry();

    // subclasses should override one of the following method to invoke the
    // processResult method in the original callback
    protected void processDataResult(int rc, String path, Object ctx, byte data[], Stat stat)
    {
      throw new UnsupportedOperationException("Must override use processDataResult");
    }
    protected void processChildrenResult(int rc, String path, Object ctx, List<String> children)
    {
      throw new UnsupportedOperationException("Must override use processChildResult");
    }
    protected void processStatResult(int rc, String path, Object ctx, Stat stat)
    {
      throw new UnsupportedOperationException("Must override use processStatResult");
    }
    protected void processStringResult(int rc, String path, Object ctx, String name)
    {
      throw new UnsupportedOperationException("Must override use processStringResult");
    }
    protected void processVoidResult(int rc, String path, Object ctx)
    {
      throw new UnsupportedOperationException("Must override use processVoidResult");
    }

    private void retryLimitedTimes()
    {
      _retry++;
      if (_retry > _limit)
      {
        // reset retry interval
        _interval = _initInterval;
        _log.error("Connection lost. Give up after " + _limit + " retries." );
        return;
      }

      if (! _exponentialBackoff)
      {
        retry();
      }
      else
      {
        Runnable retryHandler = new Runnable() {
          public void run()
          {
            _interval *= 2;
            retry();
          }
        };
        _scheduler.schedule(retryHandler, _interval, TimeUnit.MILLISECONDS);
      }
    }

    // for DataCallback
    @Override
    public void processResult(int rc, String path, Object ctx, byte data[], Stat stat)
    {
      KeeperException.Code result = KeeperException.Code.get(rc);
      if (result != KeeperException.Code.CONNECTIONLOSS)
      {
        // reset backoff interval
        _interval = _initInterval;
        // subclass should invoke original callback's processResult method here.
        processDataResult(rc, path, ctx, data, stat) ;
      }
      else
      {
        retryLimitedTimes();
      }
    }

    // for ChildrenCallback
    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children)
    {
      KeeperException.Code result = KeeperException.Code.get(rc);
      if (result != KeeperException.Code.CONNECTIONLOSS)
      {
        // reset backoff interval
        _interval = _initInterval;
        // subclass should invoke original callback's processResult method here.
        processChildrenResult(rc, path, ctx, children);
      }
      else
      {
        retryLimitedTimes();
      }
    }

    // for StatCallback
    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat)
    {
      KeeperException.Code result = KeeperException.Code.get(rc);
      if (result != KeeperException.Code.CONNECTIONLOSS)
      {
        // reset backoff interval
        _interval = _initInterval;
        // subclass should invoke original callback's processResult method here.
        processStatResult(rc, path, ctx, stat);
      }
      else
      {
        retryLimitedTimes();
      }
    }

    // for StringCallback
    @Override
    public void processResult(int rc, String path, Object ctx, String name)
    {
      KeeperException.Code result = KeeperException.Code.get(rc);
      switch(result)
      {
        case CONNECTIONLOSS:
          retryLimitedTimes();
          break;
        default:
          // reset backoff interval
          _interval = _initInterval;
          processStringResult(rc, path, ctx, name);
          break;
      }
    }

    // for VoidCallback
    @Override
    public void processResult(int rc, String path, Object ctx)
    {
      KeeperException.Code result = KeeperException.Code.get(rc);
      switch(result)
      {
        case CONNECTIONLOSS:
          retryLimitedTimes();
          break;
        default:
          // reset backoff interval
          _interval = _initInterval;
          processVoidResult(rc, path, ctx);
          break;
      }
    }
  }


  // The following delegation methods are created for the testability using EasyMock's partial mock
  // Also, they provide a way to call the vanilla methods in ZooKeeper if for some reason
  // retry when connection loss happens is undesired
  public void zkCreate(final String path, byte[] data, List<ACL> acl, CreateMode createMode,
                        AsyncCallback.StringCallback cb, Object ctx )
  {
    _zk.create(path, data, acl, createMode, cb, ctx);
  }

  public void zkDelete(final String path, int version, AsyncCallback.VoidCallback cb, Object ctx)
  {
    _zk.delete(path, version, cb, ctx);
  }

  public void zkExists(String path, boolean watch, AsyncCallback.StatCallback cb, Object ctx)
  {
    _zk.exists(path, watch, cb, ctx);
  }

  public void zkExists(final String path, Watcher watcher, AsyncCallback.StatCallback cb, Object ctx)
  {
    _zk.exists(path, watcher, cb, ctx);
  }

  public void zkGetChildren(String path, boolean watch, AsyncCallback.ChildrenCallback cb, Object ctx)
  {
    _zk.getChildren(path, watch, cb, ctx) ;
  }

  public void zkGetChildren(final String path, Watcher watcher, AsyncCallback.ChildrenCallback cb, Object ctx)
  {
    _zk.getChildren(path, watcher, cb, ctx) ;
  }

  public void zkGetData(String path, boolean watch, AsyncCallback.DataCallback cb, Object ctx)
  {
    _zk.getData(path, watch, cb, ctx);
  }

  public void zkGetData(String path, Watcher watcher, AsyncCallback.DataCallback cb, Object ctx)
  {
    _zk.getData(path, watcher, cb, ctx);
  }

  public void zkSetData(String path, byte[] data, int version, AsyncCallback.StatCallback cb, Object ctx)
  {
    _zk.setData(path, data, version, cb, ctx);
  }

}
