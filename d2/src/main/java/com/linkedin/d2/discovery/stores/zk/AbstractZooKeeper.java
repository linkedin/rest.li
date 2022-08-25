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

import com.linkedin.pegasus.org.apache.zookeeper.AsyncCallback;
import com.linkedin.pegasus.org.apache.zookeeper.CreateMode;
import com.linkedin.pegasus.org.apache.zookeeper.KeeperException;
import com.linkedin.pegasus.org.apache.zookeeper.Watcher;
import com.linkedin.pegasus.org.apache.zookeeper.data.ACL;
import com.linkedin.pegasus.org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * An abstract class that provides the default implementation of {@link com.linkedin.d2.discovery.stores.zk.ZooKeeper}
 * The intention of this class is to reduce the verbosity if we want to create a class that only overrides partial of
 * ZooKeeper operations.
 *
 * @author Ang Xu
 * @version $Revision: $
 */
public abstract class AbstractZooKeeper implements ZooKeeper
{
  protected final ZooKeeper _zk;

  public AbstractZooKeeper(ZooKeeper zk)
  {
    _zk = zk;
  }

  @Override
  public long getSessionId()
  {
    return _zk.getSessionId();
  }

  @Override
  public byte[] getSessionPasswd()
  {
    return _zk.getSessionPasswd();
  }

  @Override
  public int getSessionTimeout()
  {
    return _zk.getSessionTimeout();
  }

  @Override
  public void addAuthInfo(String scheme, byte auth[])
  {
    _zk.addAuthInfo(scheme, auth);
  }

  @Override
  public void register(Watcher watcher)
  {
    _zk.register(watcher);
  }

  @Override
  public void close() throws InterruptedException
  {
    _zk.close();
  }

  @Override
  public String create(final String path, byte data[], List<ACL> acl, CreateMode createMode)
      throws KeeperException, InterruptedException
  {
    return _zk.create(path, data, acl, createMode);
  }

  @Override
  public void create(final String path, byte data[], List<ACL> acl,
                     CreateMode createMode,  AsyncCallback.StringCallback cb, Object ctx)
  {
    _zk.create(path, data, acl, createMode, cb, ctx);
  }

  @Override
  public void delete(final String path, int version)
      throws InterruptedException, KeeperException
  {
    _zk.delete(path, version);
  }

  @Override
  public void delete(final String path, int version, AsyncCallback.VoidCallback cb, Object ctx)
  {
    _zk.delete(path, version, cb, ctx);
  }

  @Override
  public Stat exists(final String path, Watcher watcher)
      throws KeeperException, InterruptedException
  {
    return _zk.exists(path, watcher);
  }

  @Override
  public Stat exists(String path, boolean watch) throws KeeperException,
      InterruptedException
  {
    return _zk.exists(path, watch);
  }

  @Override
  public byte[] getData(final String path, Watcher watcher, Stat stat)
      throws KeeperException, InterruptedException
  {
    return _zk.getData(path, watcher, stat);
  }

  @Override
  public byte[] getData(String path, boolean watch, Stat stat)
      throws KeeperException, InterruptedException
  {
    return _zk.getData(path, watch, stat);
  }

  @Override
  public Stat setData(final String path, byte data[], int version)
      throws KeeperException, InterruptedException
  {
    return _zk.setData(path, data, version);
  }

  @Override
  public void setData(final String path, byte data[], int version, AsyncCallback.StatCallback cb, Object ctx)
  {
    _zk.setData(path, data, version, cb, ctx);
  }

  @Override
  public List<ACL> getACL(final String path, Stat stat)
      throws KeeperException, InterruptedException
  {
    return _zk.getACL(path, stat);
  }

  @Override
  public void getACL(final String path, Stat stat, AsyncCallback.ACLCallback cb, Object ctx)
  {
    _zk.getACL(path, stat, cb, ctx);
  }

  @Override
  public Stat setACL(final String path, List<ACL> acl, int version)
      throws KeeperException, InterruptedException
  {
    return _zk.setACL(path, acl, version);
  }

  @Override
  public void setACL(final String path, List<ACL> acl, int version, AsyncCallback.StatCallback cb, Object ctx)
  {
    _zk.setACL(path, acl, version, cb, ctx);
  }

  @Override
  public List<String> getChildren(final String path, Watcher watcher)
      throws KeeperException, InterruptedException
  {
    return _zk.getChildren(path, watcher);
  }

  @Override
  public List<String> getChildren(String path, boolean watch)
      throws KeeperException, InterruptedException
  {
    return _zk.getChildren(path, watch);
  }

  @Override
  public List<String> getChildren(final String path, Watcher watcher, Stat stat)
      throws KeeperException, InterruptedException
  {
    return _zk.getChildren(path, watcher, stat);
  }

  @Override
  public List<String> getChildren(String path, boolean watch, Stat stat)
      throws KeeperException, InterruptedException
  {
    return _zk.getChildren(path, watch, stat);
  }

  @Override
  public void sync(final String path, AsyncCallback.VoidCallback cb, Object ctx)
  {
    _zk.sync(path, cb, ctx);
  }

  @Override
  public com.linkedin.pegasus.org.apache.zookeeper.ZooKeeper.States getState()
  {
    return _zk.getState();
  }

  @Override
  public void getChildren(final String path, Watcher watcher, AsyncCallback.Children2Callback cb, Object ctx)
  {
    _zk.getChildren(path, watcher, cb, ctx);
  }

  @Override
  public void getChildren(String path, boolean watch, AsyncCallback.Children2Callback cb, Object ctx)
  {
    _zk.getChildren(path, watch, cb, ctx);
  }

  @Override
  public void exists(String path, boolean watch, AsyncCallback.StatCallback cb, Object ctx)
  {
    _zk.exists(path, watch, cb, ctx);
  }

  @Override
  public void exists(String path, Watcher watcher, AsyncCallback.StatCallback cb, Object ctx)
  {
    _zk.exists(path, watcher, cb, ctx);
  }

  @Override
  public void getChildren(String path, boolean watch, AsyncCallback.ChildrenCallback cb, Object ctx)
  {
    _zk.getChildren(path, watch, cb, ctx);
  }

  @Override
  public void getChildren(String path, Watcher watcher, AsyncCallback.ChildrenCallback cb, Object ctx)
  {
    _zk.getChildren(path, watcher, cb, ctx);
  }

  @Override
  public void getData(String path, boolean watch, AsyncCallback.DataCallback cb, Object ctx)
  {
    _zk.getData(path, watch, cb, ctx);
  }

  @Override
  public void getData(String path, Watcher watcher, AsyncCallback.DataCallback cb, Object ctx)
  {
    _zk.getData(path, watcher, cb, ctx);
  }

  @Override
  public String toString()
  {
    return _zk.toString();
  }
}
