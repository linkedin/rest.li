/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.r2.netty.common;

import com.linkedin.r2.netty.common.ChannelPoolManager;
import com.linkedin.r2.netty.common.ChannelPoolManagerFactory;
import com.linkedin.r2.netty.common.ChannelPoolManagerKey;
import com.linkedin.r2.netty.common.ConnectionSharingChannelPoolManagerFactory;
import com.linkedin.r2.netty.common.EventAwareChannelPoolManagerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.event.EventProviderRegistry;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestEventChannelPoolManagerFactory
{
  @Test
  public void testBuildChannelPoolManagers()
  {
    ChannelPoolManagerFactory channelPoolManagerFactory = getChannelPoolManagerFactory();

    EventProviderRegistry eventProviderRegistry = mock(EventProviderRegistry.class);

    ChannelPoolManagerKey anyChannelPoolManagerKey = mock(ChannelPoolManagerKey.class);

    EventAwareChannelPoolManagerFactory factory = new EventAwareChannelPoolManagerFactory(
        channelPoolManagerFactory, eventProviderRegistry);

    ChannelPoolManager actualHttp1StreamManager = factory.buildHttp1Stream(anyChannelPoolManagerKey);
    ChannelPoolManager actualHttp2StreamManager = factory.buildHttp2Stream(anyChannelPoolManagerKey);

    // Expects event provider to have been registered for three times and none is unregistered
    verify(eventProviderRegistry, times(2)).registerChannelPoolEventProvider(any());
    verify(eventProviderRegistry, times(0)).unregisterChannelPoolEventProvider(any());

    actualHttp1StreamManager.shutdown(Callbacks.empty(), mock(Runnable.class), mock(Runnable.class), 0L);
    actualHttp2StreamManager.shutdown(Callbacks.empty(), mock(Runnable.class), mock(Runnable.class), 0L);

    // Expects event provider to have been registered for three times and unregistered for three times
    verify(eventProviderRegistry, times(2)).registerChannelPoolEventProvider(any());
    verify(eventProviderRegistry, times(2)).unregisterChannelPoolEventProvider(any());
  }

  @DataProvider(name = "connectionSharingFactoriesDecorator")
  public Object[][] connectionSharingFactoriesDecorator()
  {
    // need to create a list first, otherwise Java doesn't allow to create a Lambda but wants the full implementation of the interface.
    List<Function<ChannelPoolManagerFactory, ChannelPoolManagerFactory>> functions = Arrays.asList(
      channelPoolManagerFactory -> channelPoolManagerFactory,
      channelPoolManagerFactory -> new ConnectionSharingChannelPoolManagerFactory(channelPoolManagerFactory),
      channelPoolManagerFactory -> new EventAwareChannelPoolManagerFactory(channelPoolManagerFactory, mock(EventProviderRegistry.class))
    );

    Object[][] res = new Object[functions.size()][1];
    int index = 0;
    for (Function<ChannelPoolManagerFactory, ChannelPoolManagerFactory> factoryFunction : functions)
    {
      res[index++] = new Object[]{factoryFunction};
    }
    return res;
  }

  @Test(dataProvider = "connectionSharingFactoriesDecorator")
  public final void testChannelPoolManagerLifecycle(Function<ChannelPoolManagerFactory, ChannelPoolManagerFactory> factoryFunction)
  {
    ChannelPoolManagerFactory channelPoolManagerFactory = getChannelPoolManagerFactory();

    ChannelPoolManagerFactory extendedChannelPoolManagerFactory = factoryFunction.apply(channelPoolManagerFactory);

    // build some channelPoolManager. The extendedChannelPoolManagerFactory might be stateful and we have to ensure that it shuts down correctly
    ChannelPoolManagerKey anyChannelPoolManagerKey = mock(ChannelPoolManagerKey.class);
    extendedChannelPoolManagerFactory.buildHttp1Stream(anyChannelPoolManagerKey);
    extendedChannelPoolManagerFactory.buildHttp2Stream(anyChannelPoolManagerKey);

    FutureCallback<None> callback = new FutureCallback<>();
    extendedChannelPoolManagerFactory.shutdown(callback);
    try
    {
      callback.get(5, TimeUnit.SECONDS);
    }
    catch (InterruptedException | ExecutionException | TimeoutException e)
    {
      Assert.fail("It should be able to shutdown without exception", e);
    }
  }

  // ############################# Util Section #############################

  @SuppressWarnings("unchecked")
  private ChannelPoolManagerFactory getChannelPoolManagerFactory()
  {
    ChannelPoolManagerFactory channelPoolManagerFactory = mock(ChannelPoolManagerFactory.class);

    //need to create the channelPoolManager outside the thenReturn otherwise mockito complains
    ChannelPoolManager channelPoolManager = getChannelPoolManager();

    ChannelPoolManager channelPoolManager2 = getChannelPoolManager();
    when(channelPoolManagerFactory.buildHttp1Stream(any())).thenReturn(channelPoolManager2);

    ChannelPoolManager channelPoolManager3 = getChannelPoolManager();
    when(channelPoolManagerFactory.buildHttp2Stream(any())).thenReturn(channelPoolManager3);

    doAnswer(invocation -> {
      Callback<None> callback = ((Callback<None>) invocation.getArguments()[0]);
      callback.onSuccess(None.none());
      return null;
    })
      .when(channelPoolManagerFactory).shutdown(any(Callback.class));
    return channelPoolManagerFactory;
  }

  @SuppressWarnings("unchecked")
  private ChannelPoolManager getChannelPoolManager()
  {
    ChannelPoolManager expectedChannelPoolManager = mock(ChannelPoolManager.class);
    AtomicBoolean alreadyCalled = new AtomicBoolean(false);

    doAnswer(invocation -> {
      Callback<None> callback = ((Callback<None>) invocation.getArguments()[0]);
      if (alreadyCalled.compareAndSet(false, true))
      {
        callback.onSuccess(None.none());
      }
      else
      {
        callback.onError(new IllegalStateException("It has been called shutdown at least twice on the same ChannelPoolManager. " +
          "This means there is probably an error in the shutdown logic of the component. Check which ChannelPoolManagerFactory decorator " +
          "was applied and fix it"));
      }
      return null;
    })
      .when(expectedChannelPoolManager).shutdown(any(Callback.class), any(Runnable.class), any(Runnable.class), anyLong());
    return expectedChannelPoolManager;
  }
}
