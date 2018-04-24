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

package com.linkedin.r2.transport.http.client.common;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.r2.event.EventProviderRegistry;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

public class TestEventChannelPoolManagerFactory
{
  @Test
  public void testBuildChannelPoolManagers()
  {
    ChannelPoolManagerFactory channelPoolManagerFactory = mock(ChannelPoolManagerFactory.class);
    ChannelPoolManager expectedRestManager = mock(ChannelPoolManager.class);
    ChannelPoolManager expectedStreamManager = mock(ChannelPoolManager.class);
    ChannelPoolManager expectedHttp2StreamManager = mock(ChannelPoolManager.class);
    when(channelPoolManagerFactory.buildRest(any())).thenReturn(expectedRestManager);
    when(channelPoolManagerFactory.buildStream(any())).thenReturn(expectedStreamManager);
    when(channelPoolManagerFactory.buildHttp2Stream(any())).thenReturn(expectedHttp2StreamManager);

    EventProviderRegistry eventProviderRegistry = mock(EventProviderRegistry.class);

    ChannelPoolManagerKey anyChannelPoolManagerKey = mock(ChannelPoolManagerKey.class);

    EventAwareChannelPoolManagerFactory factory = new EventAwareChannelPoolManagerFactory(
        channelPoolManagerFactory, eventProviderRegistry);

    ChannelPoolManager actualRestManager = factory.buildRest(anyChannelPoolManagerKey);
    ChannelPoolManager actualStreamManager = factory.buildStream(anyChannelPoolManagerKey);
    ChannelPoolManager actualHttp2StreamManager = factory.buildHttp2Stream(anyChannelPoolManagerKey);

    // Expects event provider to have been registered for three times and none is unregistered
    verify(eventProviderRegistry, times(3)).registerChannelPoolEventProvider(any());
    verify(eventProviderRegistry, times(0)).unregisterChannelPoolEventProvider(any());

    actualRestManager.shutdown(Callbacks.empty(), mock(Runnable.class), mock(Runnable.class), 0L);
    actualStreamManager.shutdown(Callbacks.empty(), mock(Runnable.class), mock(Runnable.class), 0L);
    actualHttp2StreamManager.shutdown(Callbacks.empty(), mock(Runnable.class), mock(Runnable.class), 0L);

    // Expects event provider to have been registered for three times and unregistered for three times
    verify(eventProviderRegistry, times(3)).registerChannelPoolEventProvider(any());
    verify(eventProviderRegistry, times(3)).unregisterChannelPoolEventProvider(any());
  }
}
