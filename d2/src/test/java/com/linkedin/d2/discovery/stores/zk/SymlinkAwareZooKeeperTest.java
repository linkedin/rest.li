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

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class SymlinkAwareZooKeeperTest
{
  private ZKConnection  _zkClient;
  private ZKServer      _zkServer;
  private int           _port;

  @BeforeSuite
  public void setup() throws InterruptedException, ExecutionException, IOException
  {
    _port = 11830;

    try
    {
      _zkServer = new ZKServer(_port);
      _zkServer.startup();
      _zkClient = new ZKConnection("localhost:" + _port, 5001, 0, false, null, 0, false, true);
      _zkClient.start();
    }
    catch (IOException e)
    {
      Assert.fail("unable to instantiate real zk server on port " + _port);
    }
    initTestData();
  }


  @AfterSuite
  public void tearDown() throws IOException, InterruptedException
  {
    _zkClient.shutdown();
    _zkServer.shutdown();
  }

  private void initTestData() throws ExecutionException, InterruptedException, UnsupportedEncodingException
  {
    FutureCallback<None> callback;
    for (int i = 1; i <= 10; i++)
    {
      callback = new FutureCallback<None>();
      _zkClient.ensurePersistentNodeExists("/foo/bar/" + i, callback);
      callback.get();
      callback = new FutureCallback<None>();
      _zkClient.setDataUnsafe("/foo/bar/" + i, String.valueOf(i).getBytes("UTF-8"), callback);
      callback.get();
    }

    for (int i=11; i <= 15; i++)
    {
      callback = new FutureCallback<None>();
      _zkClient.ensurePersistentNodeExists("/bar/foo/" + i, callback);
      callback.get();
      callback = new FutureCallback<None>();
      _zkClient.setDataUnsafe("/bar/foo/" + i, String.valueOf(i).getBytes("UTF-8"), callback);
      callback.get();
    }
    callback = new FutureCallback<None>();
    _zkClient.createSymlink("/foo/$link", "/foo/bar", callback);
    callback.get();
    callback = new FutureCallback<None>();
    _zkClient.createSymlink("/$bar", "/foo", callback);
    callback.get();
  }


  @Test
  public void testSymlinkExists() throws InterruptedException
  {
    final CountDownLatch latch = new CountDownLatch(1);
    AsyncCallback.StatCallback callback = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        KeeperException.Code result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.OK);
        Assert.assertEquals(path, "/foo/$link");
        Assert.assertNotNull(stat);
        latch.countDown();
      }
    };
    _zkClient.getZooKeeper().exists("/foo/$link", null, callback, null);
    latch.await(30, TimeUnit.SECONDS);
  }

  @Test
  public void testSymlinkGetChildren() throws InterruptedException, ExecutionException, IOException
  {
    final CountDownLatch latch = new CountDownLatch(1);
    AsyncCallback.ChildrenCallback callback = new AsyncCallback.ChildrenCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children)
      {
        KeeperException.Code result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.OK);
        Assert.assertEquals(path, "/foo/$link");
        Assert.assertEquals(children.size(), 10);
        latch.countDown();
      }
    };
    // symlink: /foo/$link -> /foo/bar
    _zkClient.getZooKeeper().getChildren("/foo/$link", null, callback, null);
    latch.await(30, TimeUnit.SECONDS);
  }

  @Test
  public void testMultiSymlink() throws InterruptedException
  {
    final CountDownLatch latch = new CountDownLatch(1);
    AsyncCallback.ChildrenCallback callback = new AsyncCallback.ChildrenCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children)
      {
        KeeperException.Code result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.OK);
        Assert.assertEquals(path, "/$bar/$link");
        Assert.assertEquals(children.size(), 10);
        latch.countDown();
      }
    };
    // symlink: /$bar -> /foo
    // symlink: /foo/$link -> /foo/bar
    _zkClient.getZooKeeper().getChildren("/$bar/$link", null, callback, null);
    latch.await(30, TimeUnit.SECONDS);
  }

  @Test
  public void testSymlinkGetData() throws InterruptedException
  {
    final CountDownLatch latch = new CountDownLatch(1);
    AsyncCallback.DataCallback callback = new AsyncCallback.DataCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat)
      {
        String value = null;
        try
        {
          value = new String(data, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
          Assert.fail(e.getMessage());
        }
        finally
        {
          KeeperException.Code result = KeeperException.Code.get(rc);
          Assert.assertEquals(result, KeeperException.Code.OK);
          Assert.assertEquals(value, (String)ctx);
          Assert.assertNotNull(stat);
          latch.countDown();
        }
      }
    };
    // symlink: /foo/$link/1 -> /foo/bar/1
    _zkClient.getZooKeeper().getData("/foo/$link/1", null, callback, "1");
    latch.await(30, TimeUnit.SECONDS);
  }

  @Test
  public void testSymlinkGetChildrenAndData() throws InterruptedException
  {
    final CountDownLatch latch = new CountDownLatch(10);

    AsyncCallback.ChildrenCallback callback = new AsyncCallback.ChildrenCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children)
      {
        AsyncCallback.DataCallback dataCallback = new AsyncCallback.DataCallback()
        {
          @Override
          public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat)
          {
            String value = null;
            try
            {
              value = new String(data, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
              Assert.fail(e.getMessage());
            }
            finally
            {
              Assert.assertEquals(value, (String)ctx);
              latch.countDown();
            }
          }
        };
        for (String child : children)
        {
          _zkClient.getZooKeeper().getData(path + "/" + child, null, dataCallback, child);
        }
      }
    };
    _zkClient.getZooKeeper().getChildren("/foo/$link", null, callback, null);
    latch.await(30, TimeUnit.SECONDS);
  }

  @Test
  public void testNonexistentSymlink() throws ExecutionException, InterruptedException
  {
    final CountDownLatch latch = new CountDownLatch(3);
    AsyncCallback.StatCallback statCallback = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        KeeperException.Code result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.NONODE);
        latch.countDown();
      }
    };
    AsyncCallback.DataCallback dataCallback = new AsyncCallback.DataCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat)
      {
        KeeperException.Code result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.NONODE);
        latch.countDown();
      }
    };
    AsyncCallback.ChildrenCallback childrenCallback = new AsyncCallback.ChildrenCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children)
      {
        KeeperException.Code result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.NONODE);
        latch.countDown();
      }
    };
    // symlink: /foo/$badlink -> null
    _zkClient.getZooKeeper().exists("/foo/$badlink", false, statCallback, null);
    _zkClient.getZooKeeper().getData("/foo/$badlink", false, dataCallback, null);
    _zkClient.getZooKeeper().getChildren("/foo/$badlink", false, childrenCallback, null);
    latch.await(30, TimeUnit.SECONDS);
  }

  @Test
  public void testSymlinkWithExistWatch() throws InterruptedException, ExecutionException
  {
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);
    final AsyncCallback.StatCallback existCallback = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        KeeperException.Code result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.OK);
        latch.countDown();
      }
    };
    Watcher existWatch = new Watcher()
    {
      @Override
      public void process(WatchedEvent event)
      {
        Assert.assertEquals(event.getType(), Event.EventType.NodeCreated);
        _zkClient.getZooKeeper().exists(event.getPath(), null, existCallback, null);
      }
    };
    AsyncCallback.StatCallback existCallback2 = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        KeeperException.Code  result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.NONODE);
        latch2.countDown();
      }
    };
    // symlink: /foo/$link/newNode -> /foo/bar/newNode
    _zkClient.getZooKeeper().exists("/foo/$link/newNode", existWatch, existCallback2, null);
    latch2.await(30, TimeUnit.SECONDS);
    _zkClient.ensurePersistentNodeExists("/foo/bar/newNode", new FutureCallback<None>());
    latch.await(30, TimeUnit.SECONDS);
    _zkClient.removeNodeUnsafe("/foo/bar/newNode", new FutureCallback<None>());
  }

  @Test
  public void testSymlinkWithExistWatch2() throws InterruptedException, ExecutionException
  {
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);
    final AsyncCallback.StatCallback existCallback = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        KeeperException.Code result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.OK);
        latch.countDown();
      }
    };
    Watcher existWatch = new Watcher()
    {
      @Override
      public void process(WatchedEvent event)
      {
        Assert.assertEquals(event.getType(), Event.EventType.NodeDataChanged);
        _zkClient.getZooKeeper().exists(event.getPath(), null, existCallback, null);
      }
    };
    AsyncCallback.StatCallback existCallback2 = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        KeeperException.Code  result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.NONODE);
        latch2.countDown();
      }
    };
    // symlink: /foo/$link/foo -> /foo/bar/foo, which doesn't exist
    _zkClient.getZooKeeper().exists("/foo/$link/foo", existWatch, existCallback2, null);
    latch2.await(30, TimeUnit.SECONDS);
    // update symlink. now it points to /bar/foo, which does exist.
    _zkClient.setSymlinkData("/foo/$link", "/bar", new FutureCallback<None>());
    latch.await(30, TimeUnit.SECONDS);
    // restore symlink
    _zkClient.setSymlinkData("/foo/$link", "/foo/bar", new FutureCallback<None>());
  }

  @Test
  public void testSymlinkWithExistWatch3() throws InterruptedException, ExecutionException
  {
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);
    final AsyncCallback.StatCallback existCallback = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        KeeperException.Code result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.OK);
        latch.countDown();
      }
    };
    Watcher existWatch = new Watcher()
    {
      @Override
      public void process(WatchedEvent event)
      {
        Assert.assertEquals(event.getType(), Event.EventType.NodeCreated);
        _zkClient.getZooKeeper().exists(event.getPath(), null, existCallback, null);
      }
    };
    AsyncCallback.StatCallback existCallback2 = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        KeeperException.Code  result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.NONODE);
        latch2.countDown();
      }
    };
    // symlink /$link doesn't exist.
    _zkClient.getZooKeeper().exists("/$link", existWatch, existCallback2, null);
    latch2.await(30, TimeUnit.SECONDS);
    // create symlink /$link -> /foo/bar. existWatch should be notified.
    _zkClient.createSymlink("/$link", "/foo/bar", new FutureCallback<None>());
    latch.await(30, TimeUnit.SECONDS);
    // delete symlink /$link
    _zkClient.removeNodeUnsafe("/$link", new FutureCallback<None>());
  }

  @Test
  public void testSymlinkWithChildrenWatch() throws InterruptedException
  {
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);
    final AsyncCallback.ChildrenCallback childrenCallback = new AsyncCallback.ChildrenCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children)
      {
        KeeperException.Code result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.OK);
        Assert.assertEquals(children.size(), 11);
        latch.countDown();
      }
    };
    Watcher childrenWatch = new Watcher()
    {
      @Override
      public void process(WatchedEvent event)
      {
        Assert.assertEquals(event.getType(), Event.EventType.NodeChildrenChanged);
        _zkClient.getZooKeeper().getChildren(event.getPath(), null, childrenCallback, null);
      }
    };
    AsyncCallback.ChildrenCallback childrenCallback2 = new AsyncCallback.ChildrenCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children)
      {
        KeeperException.Code result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.OK);
        latch2.countDown();
      }
    };
    // symlink: /foo/$link -> /foo/bar
    _zkClient.getZooKeeper().getChildren("/foo/$link", childrenWatch, childrenCallback2, null);
    latch2.await(30, TimeUnit.SECONDS);
    _zkClient.ensurePersistentNodeExists("/foo/bar/newNode", new FutureCallback<None>());
    latch.await(30, TimeUnit.SECONDS);
    _zkClient.removeNodeUnsafe("/foo/bar/newNode", new FutureCallback<None>());
  }

  @Test
  public void testSymlinkWithChildrenWatcher2() throws ExecutionException, InterruptedException
  {
    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);
    final AsyncCallback.ChildrenCallback callback2 = new AsyncCallback.ChildrenCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children)
      {
        Assert.assertEquals(path, "/foo/$link");
        Assert.assertEquals(children.size(), 5);
        latch2.countDown();
      }
    };
    Watcher watcher = new Watcher()
    {
      @Override
      public void process(WatchedEvent event)
      {
        Assert.assertEquals(event.getType(), Event.EventType.NodeChildrenChanged);
        _zkClient.getZooKeeper().getChildren(event.getPath(), null, callback2, null);
      }
    };
    AsyncCallback.ChildrenCallback callback = new AsyncCallback.ChildrenCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children)
      {
        latch1.countDown();
      }
    };
    // set watcher to /foo/$link
    _zkClient.getZooKeeper().getChildren("/foo/$link", watcher, callback, null);
    latch1.await(30, TimeUnit.SECONDS);
    // update symlink
    _zkClient.setSymlinkData("/foo/$link", "/bar/foo", new FutureCallback<None>());
    latch2.await(30, TimeUnit.SECONDS);
    FutureCallback<None> fcb = new FutureCallback<None>();
    // restore symlink
    _zkClient.setSymlinkData("/foo/$link", "/foo/bar", fcb);
    fcb.get();
  }

  @Test
  public void testInvalidSymlinkWithExists() throws ExecutionException, InterruptedException
  {
    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);
    final AsyncCallback.StatCallback callback = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        Assert.assertEquals(path, "/foo/$link");
        KeeperException.Code result = KeeperException.Code.get(rc);
        Assert.assertEquals(result, KeeperException.Code.NONODE);
        latch1.countDown();
      }
    };
    final Watcher watcher = new Watcher()
    {
      @Override
      public void process(WatchedEvent event)
      {
        Assert.assertEquals(event.getType(), Event.EventType.NodeDataChanged);
        latch2.countDown();
      }
    };
    FutureCallback<None> fcb = new FutureCallback<None>();
    _zkClient.setSymlinkData("/foo/$link", "INVALID", fcb);
    fcb.get();
    _zkClient.getZooKeeper().exists("/foo/$link", watcher, callback, null);
    latch1.await(30, TimeUnit.SECONDS);
    _zkClient.setSymlinkData("/foo/$link", "/foo/bar", fcb);
    if (!latch2.await(30, TimeUnit.SECONDS))
    {
      Assert.fail("Exists Watch is not triggered");
    }
  }
}
