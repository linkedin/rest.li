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
import java.util.List;

import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperStore;

public class ZooKeeperStoreJmx<T> implements ZooKeeperStoreJmxMBean
{
  private final ZooKeeperStore<T> _store;

  public ZooKeeperStoreJmx(ZooKeeperStore<T> store)
  {
    _store = store;
  }

  @Override
  public long getGetCount()
  {
    return _store.getGetCount();
  }

  @Override
  public long getPutCount()
  {
    return _store.getPutCount();
  }

  @Override
  public long getRemoveCount()
  {
    return _store.getRemoveCount();
  }

  @Override
  public String getPath()
  {
    return _store.getPath();
  }

  @Override
  public List<String> ls() throws PropertyStoreException
  {
    List<String> ls = _store.ls();

    return (ls != null) ? ls : null;
  }

  @Override
  public String get(String listenTo) throws PropertyStoreException
  {
    return _store.get(listenTo) + "";
  }

  @Override
  public void put(String listenTo, String discoveryProperties) throws PropertyStoreException
  {
    try
    {
      _store.put(listenTo, _store.getSerializer()
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

  @Override
  public void remove(String listenTo) throws PropertyStoreException
  {
    _store.remove(listenTo);
  }

  @Override
  public long getRegisterCount()
  {
    return _store.getRegisterCount();
  }

  @Override
  public long getUnregisterCount()
  {
    return _store.getUnregisterCount();
  }
}
