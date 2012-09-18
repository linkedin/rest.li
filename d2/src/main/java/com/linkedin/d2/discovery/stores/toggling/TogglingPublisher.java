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

package com.linkedin.d2.discovery.stores.toggling;

import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventPublisher;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.util.NullEventBus;
import com.linkedin.d2.discovery.stores.util.StoreEventPublisher;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.util.None;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TogglingPublisher<T>
{
  private final PublisherWithStatus<T> _primary;
  private final PublisherWithStatus<T> _backup;
  private final PropertyEventBus<T>       _eventBus;
  private final PropertyEventBus<T>       _nullBus = new NullEventBus<T>();

  public TogglingPublisher(PropertyEventPublisher<T> primary,
                           PropertyStore<T> backup,
                           PropertyEventBus<T> eventBus)
  {
    _primary = new PublisherWithStatus<T>(primary);
    _backup = new PublisherWithStatus<T>(new StoreEventPublisher<T>(backup));
    _eventBus = eventBus;
  }

  public PropertyEventBus<T> getEventBus()
  {
    return _eventBus;
  }

  public void enablePrimary(Callback<None> callback)
  {
    configure(callback, _primary, _backup);
  }

  public void enableBackup(Callback<None> callback)
  {
    configure(callback, _backup, _primary);
  }

  private void configure(final Callback<None> callback,
                         final PublisherWithStatus<T> activate,
                         final PublisherWithStatus<T> deactivate)
  {

    activate.start(new Callback<None>()
    {
      @Override
      public void onSuccess(None none)
      {
        PropertyEventPublisher<T> pubActivate = activate.getPublisher();
        pubActivate.setBus(_eventBus);
        _eventBus.setPublisher(pubActivate);

        if (deactivate.started())
        {
          PropertyEventPublisher<T> pubDeactivate = deactivate.getPublisher();
          pubDeactivate.setBus(_nullBus);
        }
        callback.onSuccess(None.none());
      }

      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }
    });
  }

  public void shutdown(final Callback<None> callback)
  {
    boolean primary = _primary.started();
    boolean backup = _backup.started();

    int count = (primary ? 1 : 0) + (backup ? 1 : 0);
    final Callback<None> multiCallback = Callbacks.countDown(callback, count);

    PropertyEventThread.PropertyEventShutdownCallback pcallback =
            new PropertyEventThread.PropertyEventShutdownCallback()
    {
      @Override
      public void done()
      {
        multiCallback.onSuccess(None.none());
      }
    };

    if (primary)
    {
      _primary.getPublisher().shutdown(pcallback);
    }
    if (backup)
    {
      _backup.getPublisher().shutdown(pcallback);
    }
  }

  private class PublisherWithStatus<T>
  {
    private final PropertyEventPublisher<T> _publisher;
    private final AtomicBoolean _started = new AtomicBoolean(false);

    private PublisherWithStatus(PropertyEventPublisher<T> publisher)
    {
      _publisher = publisher;
    }

    public void start(Callback<None> callback)
    {
      if (_started.compareAndSet(false, true))
      {
        _publisher.start(callback);
      }
      else
      {
        callback.onSuccess(None.none());
      }
    }

    public PropertyEventPublisher<T> getPublisher()
    {
      return _publisher;
    }

    public boolean started()
    {
      return _started.get();
    }
  }

}
