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

import com.linkedin.d2.discovery.stores.zk.acl.AclAwareZookeeper;
import com.linkedin.d2.discovery.stores.zk.acl.ZKAclProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class AclAwareZookeeperTest
{
  private final int ZK_PORT = 2120;
  private final int ZK_TIMEOUT = 2000;
  private final int ZK_RETRY_LIMIT = 10;

  private ZKServer _zkServer;
  private ZooKeeper _verificationZKClient;

  @BeforeMethod
  public void setup() throws Exception
  {
    _zkServer = new ZKServer(ZK_PORT);
    _zkServer.startup();
    final ZKConnection zkconnection = new ZKConnectionBuilder("localhost:" + ZK_PORT)
      .setTimeout(ZK_TIMEOUT)
      .setWaitForConnected(true)
      .build();
    zkconnection.start();
    _verificationZKClient = zkconnection.getZooKeeper();
  }

  @AfterMethod
  public void teardown() throws IOException
  {
    _zkServer.shutdown();
  }

  private ZooKeeper getAclAwareZookeeper(List<ACL> providedAcls, byte[] authInfo, String scheme) throws IOException
  {
    MockAclProvider aclProvider = new MockAclProvider();
    aclProvider.setAcl(providedAcls);
    aclProvider.setAuthScheme(scheme);
    aclProvider.setAuthInfo(authInfo);
    ZooKeeper newSession = new VanillaZooKeeperAdapter("localhost:" + ZK_PORT, ZK_TIMEOUT, new TestWatcher());
    ZooKeeper retryZk = new RetryZooKeeper(newSession, ZK_RETRY_LIMIT);
    ZooKeeper aclAwareZk = new AclAwareZookeeper(retryZk, aclProvider);
    return aclAwareZk;
  }

  private ACL getACLItem(String scheme, String id, int perm)
  {
    Id userId = new Id(scheme, id);
    return new ACL(perm, userId);
  }

  @Test
  public void TestAclApply() throws IOException, KeeperException, InterruptedException
  {
    List<ACL> acls = new ArrayList<>();
    acls.addAll(ZooDefs.Ids.READ_ACL_UNSAFE);
    acls.addAll(ZooDefs.Ids.CREATOR_ALL_ACL);
    ZooKeeper aclAwareZk = getAclAwareZookeeper(acls, "test:123".getBytes(), "digest");
    aclAwareZk.create("/d2", "data".getBytes(), null, CreateMode.EPHEMERAL);

    // now try getting the Acls from a bystander
    Stat stat = new Stat();
    List<ACL> retrievedAcls = _verificationZKClient.getACL("/d2", stat);
    Assert.assertEquals(acls.size(), retrievedAcls.size());
    int version = stat.getVersion();
    // Acl should already being enforced
    Assert.assertThrows(() -> _verificationZKClient.setData("/d2", "newdata".getBytes(), version));
  }

  @Test
  public void TestNoAuth() throws IOException, KeeperException, InterruptedException
  {
    List<ACL> acls = new ArrayList<>();
    acls.addAll(ZooDefs.Ids.OPEN_ACL_UNSAFE);
    ZooKeeper aclAwareZk = getAclAwareZookeeper(acls, null, null);
    aclAwareZk.create("/d2", "data".getBytes(), null, CreateMode.EPHEMERAL);

    List<ACL> retrievedAcls = _verificationZKClient.getACL("/d2", new Stat());
    Assert.assertTrue(retrievedAcls.equals(ZooDefs.Ids.OPEN_ACL_UNSAFE));
  }

  @Test
  public void TestAclNoApply() throws IOException, KeeperException, InterruptedException
  {
    List<ACL> acls = new ArrayList<>();
    acls.addAll(ZooDefs.Ids.READ_ACL_UNSAFE);
    acls.addAll(ZooDefs.Ids.CREATOR_ALL_ACL);
    ZooKeeper aclAwareZk = getAclAwareZookeeper(acls, null, null);
    aclAwareZk.create("/d2", "data".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

    // if createMode is persistent, the user provided acl will be used
    List<ACL> retrievedAcls = _verificationZKClient.getACL("/d2", new Stat());
    Assert.assertTrue(retrievedAcls.size() == 1);
    Assert.assertTrue(retrievedAcls.equals(ZooDefs.Ids.OPEN_ACL_UNSAFE));
  }

  @Test
  public void TestIPAcl() throws IOException, KeeperException, InterruptedException
  {
    List<ACL> acls = new ArrayList<>();
    acls.add(getACLItem("ip", "127.0.0.1", ZooDefs.Perms.ALL));
    ZooKeeper aclAwareZk = getAclAwareZookeeper(acls, null, null);
    aclAwareZk.create("/d2", "data".getBytes(), null, CreateMode.EPHEMERAL);

    Stat stat = new Stat();
    List<ACL> retrievedAcls = _verificationZKClient.getACL("/d2", stat);
    Assert.assertTrue(retrievedAcls.equals(acls));

    // verification client should be able to delete since both creator and verificator are on localhost
    _verificationZKClient.delete("/d2", stat.getVersion());
  }

  /**
   * Open Acl from external source should be removed if wrapper is used
   */
  @Test
  public void TestAclRemoval() throws IOException, KeeperException, InterruptedException
  {
    ACL readOnlyAcl = getACLItem("world", "anyone", ZooDefs.Perms.READ);
    ZooKeeper aclAwareZk =
        getAclAwareZookeeper(Collections.singletonList(readOnlyAcl), "test:123".getBytes(), "digest");
    aclAwareZk.create("/d2", "data".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

    List<ACL> acls = _verificationZKClient.getACL("/d2", new Stat());
    Assert.assertTrue(acls.equals(Collections.singletonList(readOnlyAcl)));
  }

  private class TestWatcher implements Watcher
  {

    @Override
    public void process(WatchedEvent event)
    {
      return;
    }
  }

  private class MockAclProvider implements ZKAclProvider
  {
    private List<ACL> _acls;
    private String _authScheme;
    private byte[] _authInfo;

    public void setAcl(List<ACL> acls)
    {
      _acls = acls;
    }

    public void setAuthScheme(String scheme)
    {
      _authScheme = scheme;
    }

    public void setAuthInfo(byte[] authInfo)
    {
      _authInfo = authInfo;
    }

    @Override
    public List<ACL> getACL()
    {
      return _acls;
    }

    @Override
    public String getAuthScheme()
    {
      return _authScheme;
    }

    @Override
    public byte[] getAuthInfo()
    {
      return _authInfo;
    }
  }
}
