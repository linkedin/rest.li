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

import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.pegasus.org.apache.zookeeper.AsyncCallback;
import com.linkedin.pegasus.org.apache.zookeeper.CreateMode;
import com.linkedin.pegasus.org.apache.zookeeper.Watcher;
import com.linkedin.pegasus.org.apache.zookeeper.data.ACL;

import java.util.List;

/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class SymlinkAwareRetryZooKeeper extends SymlinkAwareZooKeeper implements Retryable
{
  private final RetryZooKeeper _zk;

  public SymlinkAwareRetryZooKeeper(RetryZooKeeper zk, Watcher watcher)
  {
    this(zk, watcher, new DefaultSerializer());
  }

  public SymlinkAwareRetryZooKeeper(RetryZooKeeper zk, Watcher watcher, PropertySerializer<String> serializer)
  {
    super(zk, watcher, serializer);
    _zk = zk;
  }

  @Override
  public void createUniqueSequential(final String path, final byte[] data, final List<ACL> acl, final CreateMode createMode,
                                     final AsyncCallback.StringCallback cb, final Object ctx)
  {
    _zk.createUniqueSequential(path, data, acl, createMode, cb, ctx);
  }
}
