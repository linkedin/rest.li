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

package com.linkedin.d2.jmx;

import java.io.UnsupportedEncodingException;

import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;

public class ZooKeeperEphemeralStoreJmx<T> extends ZooKeeperStoreJmx<T> implements
    ZooKeeperEphemeralStoreJmxMBean
{
  private final ZooKeeperEphemeralStore<T> _store;

  public ZooKeeperEphemeralStoreJmx(ZooKeeperEphemeralStore<T> store)
  {
    super(store);

    _store = store;
  }

  @Override
  public int getListenerCount()
  {
    return _store.getListenerCount();
  }

  @Override
  public void removePartial(String listenTo, String discoveryProperties) throws
          PropertyStoreException
  {
    try
    {
      _store.removePartial(listenTo,
                           _store.getSerializer()
                                 .fromBytes(discoveryProperties.getBytes("UTF-8")));
    }
    catch (PropertySerializationException e)
    {
      throw new PropertyStoreException(e);
    }
    catch (UnsupportedEncodingException e)
    {
      throw new RuntimeException("UTF-8 should never fail", e);
    }
  }
}
