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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer.zkfs;

import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.common.callback.Callback;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZKFSDirectory implements Directory
{
  private final String _basePath;
  private volatile ZKConnection _connection;

  public ZKFSDirectory(String basePath)
  {
    _basePath = basePath;
  }

  @Override
  public void getServiceNames(final Callback<List<String>> callback)
  {
    final ZooKeeper zk = _connection.getZooKeeper();
    final String path = ZKFSUtil.servicePath(_basePath);
    zk.getChildren(path, false, new AsyncCallback.Children2Callback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children, Stat stat)
      {
        KeeperException.Code code = KeeperException.Code.get(rc);
        switch (code)
        {
          case OK:
            callback.onSuccess(children);
            break;
          case NONODE:
            callback.onSuccess(Collections.<String>emptyList());
            break;
          default:
            callback.onError(KeeperException.create(code));
            break;
        }
      }
    }, null);
  }

  @Override
  public void getClusterNames(final Callback<List<String>> callback)
  {
    final ZooKeeper zk = _connection.getZooKeeper();
    final String path = ZKFSUtil.clusterPath(_basePath);
    zk.getChildren(path, false, new AsyncCallback.Children2Callback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children, Stat stat)
      {
        KeeperException.Code code = KeeperException.Code.get(rc);
        switch (code)
        {
          case OK:
            callback.onSuccess(children);
            break;
          case NONODE:
            callback.onSuccess(Collections.<String>emptyList());
            break;
          default:
            callback.onError(KeeperException.create(code));
            break;
        }
      }
    }, null);

  }

  public void setConnection(ZKConnection connection)
  {
    _connection = connection;
  }
}
