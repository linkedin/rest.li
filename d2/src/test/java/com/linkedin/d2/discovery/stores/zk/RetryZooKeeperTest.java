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


import com.linkedin.test.util.retry.ThreeRetries;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMockBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class RetryZooKeeperTest {
  private static final int _connectionLossRC = KeeperException.Code.CONNECTIONLOSS.intValue();
  private static final int _okRC = KeeperException.Code.OK.intValue();
  private static final byte[] _dummyData = {'d', 'u', 'm', 'm', 'y'};
  private static final String _dummyPath = "/dummy/path";
  private static final String _dummyParentPath = "/dummy";
  private static final int _dummyVersion = 1;
  private static final List<String> _dummyList = new ArrayList<String>();
  private static final List<ACL> _dummyACL = new ArrayList<ACL>();
  private static final Object _dummyCtx = new Object();
  private static final Stat _dummyStat = new Stat();

  private static Constructor<RetryZooKeeper> _rzkCstr1;
  private static Constructor<RetryZooKeeper> _rzkCstr2;
  private static Watcher _noopWatcher =  new Watcher() {
    public void process(WatchedEvent event)
    {
      return;
    }
  };

  private static final AsyncCallback.DataCallback _dummyDataCallback = new AsyncCallback.DataCallback() {
    @Override
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat)
    {
      return;
    }
  };

  private static final AsyncCallback.ChildrenCallback _dummyChildrenCallback = new AsyncCallback.ChildrenCallback() {
    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children)
    {
      return;
    }
  };

  private static final AsyncCallback.StatCallback _dummyStatCallback = new AsyncCallback.StatCallback() {
    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
      return;
    }
  };

  private static final AsyncCallback.VoidCallback _dummyVoidCallback = new AsyncCallback.VoidCallback() {
    @Override
    public void processResult(int rc, String path, Object ctx) {
      return;
    }
  };

  private static final AsyncCallback.StringCallback _dummyStringCallback = new AsyncCallback.StringCallback() {
    @Override
    public void processResult(int rc, String path, Object ctx, String name) {
      return;
    }
  };

  private void expectGetChildCallbackWithCode(final int rc, final List<String> children)
  {
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable
      {
        AsyncCallback.ChildrenCallback callback = (AsyncCallback.ChildrenCallback) EasyMock.getCurrentArguments() [2];
        callback.processResult(rc, _dummyPath, _dummyCtx, children);
        return null;
      }
    });
  }

  private void expectGetDataCallbackWithCode(final int rc, final byte[] data)
  {
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable
      {
        AsyncCallback.DataCallback callback = (AsyncCallback.DataCallback) EasyMock.getCurrentArguments() [2];
        callback.processResult(rc, _dummyPath, _dummyCtx, data, _dummyStat);
        return null;
      }
    });
  }

  private void expectExistCallbackWithCode(final int rc)
  {
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable
      {
        AsyncCallback.StatCallback callback = (AsyncCallback.StatCallback) EasyMock.getCurrentArguments() [2];
        callback.processResult(rc, _dummyPath, _dummyCtx, _dummyStat);
        return null;
      }
    });
  }

  private void expectSetDataCallbackWithCode(final int rc)
  {
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable
      {
        AsyncCallback.StatCallback callback = (AsyncCallback.StatCallback) EasyMock.getCurrentArguments() [3];
        callback.processResult(rc, _dummyPath, _dummyCtx, _dummyStat);
        return null;
      }
    });
  }

  private void expectDeleteCallbackWithCode(final int rc)
  {
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable
      {
        AsyncCallback.VoidCallback callback = (AsyncCallback.VoidCallback) EasyMock.getCurrentArguments() [2];
        callback.processResult(rc, _dummyPath, _dummyCtx);
        return null;
      }
    });
  }

  private void expectCreateCallbackWithCode(final int rc)
  {
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable
      {
        AsyncCallback.StringCallback callback = (AsyncCallback.StringCallback) EasyMock.getCurrentArguments() [4];
        callback.processResult(rc, _dummyPath, _dummyCtx, _dummyPath);
        return null;
      }
    });
  }

  @BeforeTest
  public void setUp() throws NoSuchMethodException
  {
    _rzkCstr1 = RetryZooKeeper.class.getDeclaredConstructor(String.class,
                                                            int.class,
                                                            Watcher.class,
                                                            int.class);
    _rzkCstr2 = RetryZooKeeper.class.getDeclaredConstructor(String.class,
                                                            int.class,
                                                            Watcher.class,
                                                            int.class,
                                                            boolean.class,
                                                            ScheduledExecutorService.class,
                                                            long.class);
  }

  @Test
  public void testRetryGetChildren() throws NoSuchMethodException
  {
    final RetryZooKeeper rzkPartialMock = createMockObject(
        RetryZooKeeper.class.getMethod("zkGetChildren",
                                       String.class,
                                       boolean.class,
                                       AsyncCallback.ChildrenCallback.class,
                                       Object.class));

    // Here we only test the getChildren without supplying watcher
    // the alternative getChildren with a watcher should behave the same
    // as the watcher does not affect this operation
    // mock up zkGetChildren, which wrapper's ZooKeeper's getChildren
    rzkPartialMock.zkGetChildren((String) EasyMock.anyObject(),
                                 EasyMock.anyBoolean(),
                                 (AsyncCallback.ChildrenCallback) EasyMock.anyObject(),
                                 EasyMock.anyObject());

    // first try, we got "connection loss"
    expectGetChildCallbackWithCode(_connectionLossRC, _dummyList);
    // second try, we got "ok"
    expectGetChildCallbackWithCode(_okRC, _dummyList);
    EasyMock.replay(rzkPartialMock);
    rzkPartialMock.getChildren(_dummyPath, false, _dummyChildrenCallback, _dummyCtx);
    EasyMock.verify(rzkPartialMock);

  }

  @Test
  public void testRetryGetData() throws NoSuchMethodException
  {
    final RetryZooKeeper rzkPartialMock = createMockObject(
        RetryZooKeeper.class.getMethod("zkGetData",
                                       String.class,
                                       boolean.class,
                                       AsyncCallback.DataCallback.class,
                                       Object.class));

    // Similarly, only getData without watcher is tested.
    // mock up zkGetData, which wrapper's ZooKeeper's getData
    rzkPartialMock.zkGetData((String) EasyMock.anyObject(),
                             EasyMock.anyBoolean(),
                             (AsyncCallback.DataCallback) EasyMock.anyObject(),
                             EasyMock.anyObject());

    // first try, "connection loss" happens
    expectGetDataCallbackWithCode(_connectionLossRC, _dummyData);
    // second try, get the data
    expectGetDataCallbackWithCode(_okRC, _dummyData);
    EasyMock.replay(rzkPartialMock);
    rzkPartialMock.getData(_dummyPath, false, _dummyDataCallback, _dummyCtx);
    EasyMock.verify(rzkPartialMock);
  }

  @Test
  public void testRetryExists() throws NoSuchMethodException
  {
    final RetryZooKeeper rzkPartialMock = createMockObject(
        RetryZooKeeper.class.getMethod("zkExists",
                                       String.class,
                                       boolean.class,
                                       AsyncCallback.StatCallback.class,
                                       Object.class));

    // as before, only exists without watcher is tested.
    // mock up zkExists, which wrapper's ZooKeeper's exists
    rzkPartialMock.zkExists(
        (String) EasyMock.anyObject(),
        EasyMock.anyBoolean(),
        (AsyncCallback.StatCallback) EasyMock.anyObject(),
        EasyMock.anyObject());

    // first try, "connection loss"
    expectExistCallbackWithCode(_connectionLossRC);
    expectExistCallbackWithCode(_connectionLossRC);
    expectExistCallbackWithCode(_connectionLossRC);
    expectExistCallbackWithCode(_okRC);
    EasyMock.replay(rzkPartialMock);
    rzkPartialMock.exists(_dummyPath, false, _dummyStatCallback, _dummyCtx);
    EasyMock.verify(rzkPartialMock);
  }

  @Test
  public void testRetrySetData() throws NoSuchMethodException
  {
    final RetryZooKeeper rzkPartialMock = createMockObject(
        RetryZooKeeper.class.getMethod("zkSetData",
                                       String.class,
                                       byte[].class,
                                       int.class,
                                       AsyncCallback.StatCallback.class,
                                       Object.class));

    // mock up zkSetData, which wrapper's ZooKeeper's setData
    rzkPartialMock.zkSetData((String) EasyMock.anyObject(),
                             EasyMock.aryEq(_dummyData),
                             EasyMock.anyInt(),
                             (AsyncCallback.StatCallback) EasyMock.anyObject(),
                             EasyMock.anyObject());

    // first try, "connection loss"
    expectSetDataCallbackWithCode(_connectionLossRC);
    // second try, still "connection loss"
    expectSetDataCallbackWithCode(_connectionLossRC);
    // finally, success
    expectSetDataCallbackWithCode(_okRC);
    EasyMock.replay(rzkPartialMock);
    rzkPartialMock.setData(_dummyPath, _dummyData, _dummyVersion, _dummyStatCallback, _dummyCtx);
    EasyMock.verify(rzkPartialMock);
  }

  @Test
  public void testDelete() throws NoSuchMethodException
  {
    final RetryZooKeeper rzkPartialMock = createMockObject(
        RetryZooKeeper.class.getMethod("zkDelete",
                                       String.class,
                                       int.class,
                                       AsyncCallback.VoidCallback.class,
                                       Object.class));

    // mock up zkDelete, which wrapper's ZooKeeper's delete
    rzkPartialMock.zkDelete(
        (String) EasyMock.anyObject(),
        EasyMock.anyInt(),
        (AsyncCallback.VoidCallback) EasyMock.anyObject(),
        EasyMock.anyObject());

    // first try, "connection loss"
    expectDeleteCallbackWithCode(_connectionLossRC);
    // second try, "no node"
    expectDeleteCallbackWithCode(KeeperException.Code.NONODE.intValue());
    EasyMock.replay(rzkPartialMock);
    rzkPartialMock.delete(_dummyPath, _dummyVersion, _dummyVoidCallback, _dummyCtx);
    EasyMock.verify(rzkPartialMock);
  }

  @Test
  public void testCreate() throws NoSuchMethodException
  {
    testCreateHelper(_dummyData);
  }

  @Test
  public void testCreateNullData() throws NoSuchMethodException
  {
    testCreateHelper(null);
  }

  @SuppressWarnings("unchecked")
  public void testCreateHelper(byte[] data) throws NoSuchMethodException
  {
    final RetryZooKeeper rzkPartialMock = createMockObject(
        RetryZooKeeper.class.getMethod("zkCreate",
                                       String.class,
                                       byte[].class,
                                       List.class,
                                       CreateMode.class,
                                       AsyncCallback.StringCallback.class,
                                       Object.class));

    /**
     * Testing nonsequential create, PERSISTENT type, but the ephemeral one should be the same
     */
    // mock up zkCreate, which wrapper's ZooKeeper's create
    rzkPartialMock.zkCreate(
        (String) EasyMock.anyObject(),
        (byte []) EasyMock.anyObject(),
        (List<ACL>) EasyMock.anyObject(),
        (CreateMode) EasyMock.anyObject(),
        (AsyncCallback.StringCallback) EasyMock.anyObject(),
        EasyMock.anyObject());

    // first try, "connection loss"
    expectCreateCallbackWithCode(_connectionLossRC);
    // second try, "connection loss"
    expectCreateCallbackWithCode(_connectionLossRC);
    // third try, "ok"
    expectCreateCallbackWithCode(_okRC);
    EasyMock.replay(rzkPartialMock);
    rzkPartialMock.create(_dummyPath, data, _dummyACL, CreateMode.PERSISTENT, _dummyStringCallback, _dummyCtx);
    EasyMock.verify(rzkPartialMock);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCreateSequential() throws NoSuchMethodException
  {
    final RetryZooKeeper rzkPartialMock = createMockObject(
        RetryZooKeeper.class.getMethod("zkCreate",
                                       String.class,
                                       byte[].class,
                                       List.class,
                                       CreateMode.class,
                                       AsyncCallback.StringCallback.class,
                                       Object.class),
        RetryZooKeeper.class.getMethod("zkGetData",
                                       String.class,
                                       boolean.class,
                                       AsyncCallback.DataCallback.class,
                                       Object.class),
        RetryZooKeeper.class.getMethod("zkGetChildren",
                                       String.class,
                                       boolean.class,
                                       AsyncCallback.ChildrenCallback.class,
                                       Object.class));

    // mock up zkCreate, which wrapper's ZooKeeper's create
    rzkPartialMock.zkCreate(
        (String) EasyMock.anyObject(),
        (byte[]) EasyMock.anyObject(),
        (List<ACL>) EasyMock.anyObject(),
        (CreateMode) EasyMock.anyObject(),
        (AsyncCallback.StringCallback) EasyMock.anyObject(),
        EasyMock.anyObject());
    // connection loss in create
    expectCreateCallbackWithCode(_connectionLossRC);

    List<String> children = new ArrayList<String>();
    children.add("ephemeral-3.14159");
    children.add("ephemeral-6.26");
    rzkPartialMock.zkGetChildren(
        (String) EasyMock.anyObject(),
        EasyMock.anyBoolean(),
        (AsyncCallback.ChildrenCallback) EasyMock.anyObject(),
        EasyMock.anyObject());
    // getChildren succeeded
    expectGetChildCallbackWithCode(_okRC, children);

    // no children belong to us, so zkCreate would be triggered
    rzkPartialMock.zkCreate(
        (String) EasyMock.anyObject(),
        (byte[]) EasyMock.anyObject(),
        (List<ACL>) EasyMock.anyObject(),
        (CreateMode) EasyMock.anyObject(),
        (AsyncCallback.StringCallback) EasyMock.anyObject(),
        EasyMock.anyObject());
    // connection loss in create, again
    expectCreateCallbackWithCode(_connectionLossRC);

    List<String> childrenWithOurChild = new ArrayList<String>();
    childrenWithOurChild.add("ephemeral-3.14159");
    childrenWithOurChild.add("ephemeral-6.26");
    childrenWithOurChild.add("ephemeral" + rzkPartialMock.getUuid() + "1");
    childrenWithOurChild.add("ephemeral" + rzkPartialMock.getUuid() + "2");
    rzkPartialMock.zkGetChildren(
        (String) EasyMock.anyObject(),
        EasyMock.anyBoolean(),
        (AsyncCallback.ChildrenCallback) EasyMock.anyObject(),
        EasyMock.anyObject());
    // getChildren succeeded
    expectGetChildCallbackWithCode(_okRC, childrenWithOurChild);

    rzkPartialMock.zkGetData(
        (String) EasyMock.anyObject(),
        EasyMock.anyBoolean(),
        (AsyncCallback.DataCallback) EasyMock.anyObject(),
        EasyMock.anyObject());
    byte[] randomData = {'r', 'a', 'n', 'd', 'o', 'm'};
    // connection loss in getData
    expectGetDataCallbackWithCode(_connectionLossRC, _dummyData);
    // we get some random data, not the one we wanted to create
    expectGetDataCallbackWithCode(_okRC, randomData);
    expectGetDataCallbackWithCode(_okRC, randomData);

    rzkPartialMock.zkCreate(
        (String) EasyMock.anyObject(),
        (byte[]) EasyMock.anyObject(),
        (List<ACL>) EasyMock.anyObject(),
        (CreateMode) EasyMock.anyObject(),
        (AsyncCallback.StringCallback) EasyMock.anyObject(),
        EasyMock.anyObject());
    // connection loss in create, again
    expectCreateCallbackWithCode(_connectionLossRC);

    List<String> childrenWithThatKid = new ArrayList<String>();
    childrenWithThatKid.add("ephemeral-3.14159");
    childrenWithThatKid.add("ephemeral-6.26");
    childrenWithThatKid.add("ephemeral" + rzkPartialMock.getUuid() + "1");
    childrenWithThatKid.add("ephemeral" + rzkPartialMock.getUuid() + "2");
    childrenWithThatKid.add("ephemeral" + rzkPartialMock.getUuid() + "3");
    rzkPartialMock.zkGetChildren(
        (String) EasyMock.anyObject(),
        EasyMock.anyBoolean(),
        (AsyncCallback.ChildrenCallback) EasyMock.anyObject(),
        EasyMock.anyObject());
    // getChildren succeeded
    expectGetChildCallbackWithCode(_okRC, childrenWithThatKid);

    rzkPartialMock.zkGetData(
        (String) EasyMock.anyObject(),
        EasyMock.anyBoolean(),
        (AsyncCallback.DataCallback) EasyMock.anyObject(),
        EasyMock.anyObject());

    // we found the data we wanted to create
    expectGetDataCallbackWithCode(_okRC, _dummyData);
    expectGetDataCallbackWithCode(_okRC, randomData);
    expectGetDataCallbackWithCode(_okRC, randomData);


    // so we should have confirmed the success of previous create and returned
    // without unnecessary and potentially harmful retry
    EasyMock.replay(rzkPartialMock);
    rzkPartialMock.createUniqueSequential(_dummyPath, _dummyData, _dummyACL,
        CreateMode.EPHEMERAL_SEQUENTIAL, _dummyStringCallback, _dummyCtx);
    EasyMock.verify(rzkPartialMock);
  }

  @Test
  public void testRetryLimit() throws NoSuchMethodException
  {
    // retry limit is set to 2
    final RetryZooKeeper rzkPartialMock = EasyMock.createMockBuilder(RetryZooKeeper.class)
        .withConstructor(_rzkCstr1)
        .withArgs("127.0.0.1:11711",
                  5000000,
                  _noopWatcher,
                  1)
        .addMockedMethod(RetryZooKeeper.class.getMethod("zkExists",
                                                        String.class,
                                                        boolean.class,
                                                        AsyncCallback.StatCallback.class,
                                                        Object.class))
        .createMock();

    rzkPartialMock.zkExists(
        (String) EasyMock.anyObject(),
        EasyMock.anyBoolean(),
        (AsyncCallback.StatCallback) EasyMock.anyObject(),
        EasyMock.anyObject());

    expectExistCallbackWithCode(_connectionLossRC);
    expectExistCallbackWithCode(_connectionLossRC);
    EasyMock.replay(rzkPartialMock);
    rzkPartialMock.exists(_dummyPath, false, _dummyStatCallback, _dummyCtx);
    EasyMock.verify(rzkPartialMock);
  }

  @Test(retryAnalyzer = ThreeRetries.class)
  public void testRetryBackoff() throws NoSuchMethodException, InterruptedException
  {
    final RetryZooKeeper rzkPartialMock = EasyMock.createMockBuilder(RetryZooKeeper.class)
        .withConstructor(_rzkCstr2)
        .withArgs("127.0.0.1:11711",
                  5000000,
                  _noopWatcher,
                  10,
                  true,
                  Executors.newScheduledThreadPool(1),
                  20L)
        .addMockedMethod(RetryZooKeeper.class.getMethod("zkExists",
                                                        String.class,
                                                        boolean.class,
                                                        AsyncCallback.StatCallback.class,
                                                        Object.class))
        .createMock();

    rzkPartialMock.zkExists(
        (String) EasyMock.anyObject(),
        EasyMock.anyBoolean(),
        (AsyncCallback.StatCallback) EasyMock.anyObject(),
        EasyMock.anyObject());

    // have to set thread-safe in order for mock to be
    // run by two threads
    EasyMock.makeThreadSafe(rzkPartialMock, true);
    expectExistCallbackWithCode(_connectionLossRC);
    expectExistCallbackWithCode(_connectionLossRC);
    expectExistCallbackWithCode(_connectionLossRC);
    expectExistCallbackWithCode(_connectionLossRC);
    expectExistCallbackWithCode(_okRC);
    EasyMock.replay(rzkPartialMock);

    rzkPartialMock.exists(_dummyPath, false, _dummyStatCallback, _dummyCtx);
    Thread.sleep(100);
    Assert.assertEquals(rzkPartialMock.getInterval(), 80);
    Thread.sleep(100);
    Assert.assertEquals(rzkPartialMock.getInterval(), 160);
    Thread.sleep(200);
    Assert.assertEquals(rzkPartialMock.getInterval(), 20);
    EasyMock.verify(rzkPartialMock);
  }

  private static RetryZooKeeper createMockObject(Method... methods)
  {
    final IMockBuilder<RetryZooKeeper> mockBuilder = EasyMock.<RetryZooKeeper>createMockBuilder(RetryZooKeeper.class)
        .withConstructor(_rzkCstr1)
        .withArgs("127.0.0.1:11711",
                  5000000,
                  _noopWatcher,
                  10);
    for (Method m: methods)
    {
      mockBuilder.addMockedMethod(m);
    }

    return mockBuilder.createMock();
  }
}
