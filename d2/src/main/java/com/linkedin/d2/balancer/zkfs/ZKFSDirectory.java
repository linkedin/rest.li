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

package com.linkedin.d2.balancer.zkfs;

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeper;
import java.util.Collections;
import java.util.List;
import com.linkedin.pegasus.org.apache.zookeeper.AsyncCallback;
import com.linkedin.pegasus.org.apache.zookeeper.KeeperException;
import com.linkedin.pegasus.org.apache.zookeeper.data.Stat;

/**
 * @author Steven Ihde
 */
public class ZKFSDirectory implements Directory
{
  private final String _basePath;
  private final String _d2ServicePath;
  private volatile ZKConnection _connection;

  public ZKFSDirectory(String basePath)
  {
    this(basePath, ZKFSUtil.SERVICE_PATH);
  }

  public ZKFSDirectory(String basePath, String d2ServicePath)
  {
    _basePath = basePath;
    _d2ServicePath = d2ServicePath;
  }

  @Override
  public void getServiceNames(final Callback<List<String>> callback)
  {
    final ZooKeeper zk = _connection.getZooKeeper();
    final String path = ZKFSUtil.servicePath(_basePath, _d2ServicePath);
    zk.getChildren(path, false, new ChildrenCallback(callback), null);
  }

  @Override
  public void getClusterNames(final Callback<List<String>> callback)
  {
    final ZooKeeper zk = _connection.getZooKeeper();
    final String path = ZKFSUtil.clusterPath(_basePath);
    zk.getChildren(path, false, new ChildrenCallback(callback), null);
  }

  class ChildrenCallback implements AsyncCallback.Children2Callback
  {
    private Callback<List<String>> _callback;

    ChildrenCallback(final Callback<List<String>> callback)
    {
      _callback = callback;
    }

    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children, Stat stat)
    {
      KeeperException.Code code = KeeperException.Code.get(rc);
      switch (code)
      {
        case OK:
          _callback.onSuccess(children);
          break;
        case NONODE:
          _callback.onSuccess(Collections.<String>emptyList());
          break;
        default:
          _callback.onError(KeeperException.create(code));
          break;
      }
    }
  }

  public void setConnection(ZKConnection connection)
  {
    _connection = connection;
  }
}
