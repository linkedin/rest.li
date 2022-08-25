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
import com.linkedin.pegasus.org.apache.zookeeper.data.ACL;

import java.util.List;

/**
 *
 * @author Ang Xu
 * @version $Revision: $
 */
public interface Retryable
{
  // Create sequential node with retry.
  public void createUniqueSequential(String path, byte[] data, List<ACL> acl, CreateMode createMode,
                                     AsyncCallback.StringCallback cb, Object ctx);
}
