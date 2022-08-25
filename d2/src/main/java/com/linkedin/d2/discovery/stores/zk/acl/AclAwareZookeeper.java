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

package com.linkedin.d2.discovery.stores.zk.acl;

import com.linkedin.d2.discovery.stores.zk.AbstractZooKeeper;
import com.linkedin.d2.discovery.stores.zk.ZKPersistentConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeper;
import java.util.List;
import org.antlr.v4.runtime.misc.NotNull;
import com.linkedin.pegasus.org.apache.zookeeper.AsyncCallback;
import com.linkedin.pegasus.org.apache.zookeeper.CreateMode;
import com.linkedin.pegasus.org.apache.zookeeper.KeeperException;
import com.linkedin.pegasus.org.apache.zookeeper.Watcher;
import com.linkedin.pegasus.org.apache.zookeeper.ZooDefs;
import com.linkedin.pegasus.org.apache.zookeeper.data.ACL;
import com.linkedin.pegasus.org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Zookeeper wrapper that applies ACL from {@link ZKAclProvider} and add authentication information to zookeeper session.
 *
 * NOTE: If the method call carries Acl and the node created is ephemeral, it will be discarded if this wrapper is applied!
 */
public class AclAwareZookeeper extends AbstractZooKeeper
{
  private static final Logger LOG = LoggerFactory.getLogger(ZKPersistentConnection.class);
  private static final String DIGEST_AUTH_SCHEME = "digest";

  private final ZKAclProvider _aclProvider;

  public AclAwareZookeeper(@NotNull ZooKeeper zooKeeper, @NotNull ZKAclProvider aclProvider)
  {
    super(zooKeeper);
    _aclProvider = aclProvider;

    String authScheme = _aclProvider.getAuthScheme();
    byte[] authInfo = _aclProvider.getAuthInfo();

    if (authScheme != null && authScheme.equals(DIGEST_AUTH_SCHEME) && authInfo != null)
    {
      LOG.info("Adding authentication info when initiate connection to zookeeper");
      super.addAuthInfo(authScheme, authInfo);
    }
  }

  @Override
  public void addAuthInfo(String scheme, byte[] auth)
  {
    throw new UnsupportedOperationException(
        "This zookeeper client is managed by ZkAclProvider. Authentication Info to Zookeeper should be applied through ZKAclProvider");
  }

  @Override
  public String create(String path, byte[] data, List<ACL> acl, CreateMode createMode)
      throws KeeperException, InterruptedException
  {
    if (createMode == CreateMode.EPHEMERAL_SEQUENTIAL || createMode == CreateMode.EPHEMERAL)
    {
      return super.create(path, data, _aclProvider.getACL(), createMode);
    }
    else
    {
      return super.create(path, data, acl, createMode);
    }
  }

  @Override
  public void create(String path, byte[] data, List<ACL> acl, CreateMode createMode, AsyncCallback.StringCallback cb,
      Object ctx)
  {
    if (createMode == CreateMode.EPHEMERAL_SEQUENTIAL || createMode == CreateMode.EPHEMERAL)
    {
      super.create(path, data, _aclProvider.getACL(), createMode, cb, ctx);
    }
    else
    {
      super.create(path, data, acl, createMode, cb, ctx);
    }
  }

  @Override
  public Stat setACL(String path, List<ACL> acl, int version) throws KeeperException, InterruptedException
  {
    throw new UnsupportedOperationException(
        "This zookeeper client is managed by ZkAclProvider, all acls need to be set through the provider");
  }

  @Override
  public void setACL(String path, List<ACL> acl, int version, AsyncCallback.StatCallback cb, Object ctx)
  {
    throw new UnsupportedOperationException(
        "This zookeeper client is managed by ZkAclProvider, all acls need to be set through the provider");
  }
}
