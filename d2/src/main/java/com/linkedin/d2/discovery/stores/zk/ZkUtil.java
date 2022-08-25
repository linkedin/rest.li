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

package com.linkedin.d2.discovery.stores.zk;

import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.common.callback.Callback;
import com.linkedin.pegasus.org.apache.zookeeper.AsyncCallback;
import com.linkedin.pegasus.org.apache.zookeeper.KeeperException;
import com.linkedin.pegasus.org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZkUtil
{
  private static final Logger LOG = LoggerFactory.getLogger(ZkUtil.class);

  private ZkUtil()
  {
  }

  public static <T> AsyncCallback.DataCallback zkDataCallback(final Callback<T> callback, final PropertySerializer<T> serializer)
  {
    return new AsyncCallback.DataCallback()
    {
      @Override
      public void processResult(int rc, String path, Object context, byte[] bytes, Stat stat)
      {
        LOG.trace("Data callback got rc {} for path {}", rc, path);
        KeeperException.Code result = KeeperException.Code.get(rc);
        switch (result)
        {
          case OK:
            try
            {
              callback.onSuccess(serializer.fromBytes(bytes));
            }
            catch (PropertySerializationException e)
            {
              callback.onError(e);
            }
            break;
          case NONODE:
            callback.onSuccess(null);
            break;
          default:
            callback.onError(KeeperException.create(result));
            break;
        }
      }
    };
  }

}
