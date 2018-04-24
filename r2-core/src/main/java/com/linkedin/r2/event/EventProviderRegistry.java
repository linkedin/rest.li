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

package com.linkedin.r2.event;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A registry implementation that allows registering and unregistering of event providers. Registered
 * event providers can be iterated through exposed get providers methods.
 */
public class EventProviderRegistry
{
  private final Set<ChannelPoolEventProvider> _channelPoolEventProviders = ConcurrentHashMap.newKeySet();

  public void registerChannelPoolEventProvider(ChannelPoolEventProvider channelPoolEventProvider)
  {
    _channelPoolEventProviders.add(channelPoolEventProvider);
  }

  public void unregisterChannelPoolEventProvider(ChannelPoolEventProvider channelPoolEventProvider)
  {
    _channelPoolEventProviders.remove(channelPoolEventProvider);
  }

  public Collection<ChannelPoolEventProvider> getChannelPoolEventProviders()
  {
    return Collections.unmodifiableSet(_channelPoolEventProviders);
  }
}
