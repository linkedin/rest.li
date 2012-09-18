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

package com.linkedin.d2.discovery.stores.util;

import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class NullStore<T> implements PropertyStore<T>
{
  @Override
  public void put(String listenTo, T discoveryProperties)
  {

  }

  @Override
  public void remove(String listenTo)
  {

  }

  @Override
  public T get(String listenTo)
  {
    return null;
  }

  @Override
  public void start(Callback<None> callback)
  {
    callback.onSuccess(None.none());
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback callback)
  {
    callback.done();
  }
}
