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

package com.linkedin.common.callback;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.linkedin.common.util.None;

/**
 * A Callback which accumulates a specified number of success or failure calls before
 * invoking the original callback with success or failure as appropriate.
 *
 * A useful variant might be a MultiCallback<T> which accepts a Callback<Collection<T>>
 * and returns the accumulated results on success.
 *
 * @author Steven Ihde
 * @version $Revision: $
 */

public class MultiCallback implements Callback<None>
{
  private final Callback<None>        _callback;
  private final AtomicInteger         _count;
  private final Collection<Throwable> _exceptions;

  public MultiCallback(final Callback<None> orig, final int count)
  {
    if (count < 1)
    {
      throw new IllegalArgumentException();
    }
    _count = new AtomicInteger(count);
    _exceptions = new ConcurrentLinkedQueue<>();
    _callback = orig;
  }

  @Override
  public void onSuccess(final None t)
  {
    checkDone();
  }

  @Override
  public void onError(final Throwable e)
  {
    _exceptions.add(e);
    checkDone();
  }

  private void checkDone()
  {
    if (_count.decrementAndGet() == 0)
    {
      if (_exceptions.isEmpty())
      {
        _callback.onSuccess(None.none());
      }
      else
      {
        _callback.onError(new MultiException(_exceptions));
      }
    }
  }
}
