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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreAsync;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public abstract class AbstractPropertyStoreAsync<T> implements PropertyStoreAsync<T>, PropertyStore<T>
{
  @Override
  public final void put(String name, T value) throws PropertyStoreException
  {
    FutureCallback<None> callback = new FutureCallback<None>();
    put(name, value, callback);
    getUninterruptibly(callback);
  }

  @Override
  public final void remove(String name) throws PropertyStoreException
  {
    FutureCallback<None> callback = new FutureCallback<None>();
    remove(name, callback);
    getUninterruptibly(callback);
  }

  @Override
  public final T get(String name) throws PropertyStoreException
  {
    FutureCallback<T> callback = new FutureCallback<T>();
    get(name, callback);
    return getUninterruptibly(callback);
  }

  @Override
  public void shutdown(final Callback<None> callback)
  {
    shutdown(callback);
  }

  protected static <U> U getUninterruptibly(Future<U> future) throws PropertyStoreException
  {
    boolean interrupted = false;
    for (;;)
    {
      try
      {
        U result = future.get();
        if (interrupted)
        {
          Thread.currentThread().interrupt();
        }
        return result;
      }
      catch (InterruptedException e)
      {
        interrupted = true;
      }
      catch (ExecutionException e)
      {
        throw new PropertyStoreException(e);
      }
    }
  }
}
