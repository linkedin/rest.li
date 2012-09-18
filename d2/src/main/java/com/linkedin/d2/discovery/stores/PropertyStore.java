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

package com.linkedin.d2.discovery.stores;

import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;

public interface PropertyStore<T>
{
  void put(String listenTo, T discoveryProperties) throws PropertyStoreException;

  void remove(String listenTo) throws PropertyStoreException;

  T get(String listenTo) throws PropertyStoreException;

  void start(Callback<None> callback);

  // TODO get rid of this in favor of the other shutdown
  void shutdown(PropertyEventShutdownCallback callback);
}
