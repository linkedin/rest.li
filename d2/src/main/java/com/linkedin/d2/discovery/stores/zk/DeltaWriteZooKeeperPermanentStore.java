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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.pegasus.org.apache.zookeeper.AsyncCallback;
import com.linkedin.pegasus.org.apache.zookeeper.KeeperException;
import com.linkedin.pegasus.org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Another version of ZooKeeperPermanentStore which only overwrite changed properties when calling
 * put.
 *
 * The atomic unit for comparison is Property here.
 * @author Yu Meng Zhang
 */

public class DeltaWriteZooKeeperPermanentStore<T> extends  ZooKeeperPermanentStore<T>
{
  private static final Logger _log = LoggerFactory.getLogger(DeltaWriteZooKeeperPermanentStore.class);

  public DeltaWriteZooKeeperPermanentStore(ZKConnection client,
                                           PropertySerializer<T> serializer,
                                           String path)
  {
    super(client, serializer, path);
  }

  /**
   * Returns callback of ensurePersistentNodeExists used in ZookeeperPermanentStore's put.
   * The callback would first try getting existing node's data. If it does not exist or is
   * different from the new value, it is overwritten. Otherwise nothing happens.
   *
   * @param listenTo
   * @param discoveryProperties
   * @param callback
   */
  @Override
  protected Callback<None> getExistsCallBack(final String listenTo, final T discoveryProperties, final Callback<None> callback)
  {
    final String path = getPath(listenTo);

    final AsyncCallback.StatCallback dataCallback = new AsyncCallback.StatCallback()
    {
      @Override
      public void processResult(int rc, String path, Object ctx, Stat stat)
      {
        KeeperException.Code code = KeeperException.Code.get(rc);
        switch (code)
        {
          case OK:
            callback.onSuccess(None.none());
            break;
          default:
            callback.onError(KeeperException.create(code));
            break;
        }
      }
    };

    final AsyncCallback.DataCallback getDataCallback = new AsyncCallback.DataCallback()
    {
      @Override
      public void processResult(int rc, String path, Object context, byte[] bytes, Stat stat)
      {
        _log.debug("Data callback got rc {} for path {}", rc, path);
        KeeperException.Code result = KeeperException.Code.get(rc);

        switch (result)
        {
          case OK:
            // Get property currently in store.
            T propertiesInStore = null;
            if (bytes != null)
            {
              try
              {
                propertiesInStore = _serializer.fromBytes(bytes);
              }
              catch (PropertySerializationException e)
              {
                _log.warn("Unable to de-serialize properties for {}, overwriting", path, e);
              }
            }

            // Compare with new property and only call setData if it is different.
            if (propertiesInStore == null || !propertiesInStore.equals(discoveryProperties))
            {
              _log.debug("Updating value for {}", path);
              _zk.setData(path, _serializer.toBytes(discoveryProperties), -1, dataCallback, null);
            }
            else
            {
              _log.debug("Node is up to date, skipping write for {}", path);
              callback.onSuccess(None.none());
            }

            break;
          default:
            callback.onError(KeeperException.create(result));
            break;
        }
      }

    };

    return new Callback<None>()
    {
      @Override
      public void onSuccess(None none)
      {
        _zk.getData(path, false, getDataCallback, null);
      }

      @Override
      public void onError(Throwable e)
      {
        _log.debug("Exist : failed for path {}", path);
        callback.onError(e);
      }
    };
  }
}
