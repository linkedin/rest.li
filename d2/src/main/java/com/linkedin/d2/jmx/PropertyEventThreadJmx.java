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

import com.linkedin.d2.discovery.event.PropertyEventThread;

public class PropertyEventThreadJmx implements PropertyEventThreadJmxMBean
{
  private PropertyEventThread _thread;

  public PropertyEventThreadJmx(PropertyEventThread thread)
  {
    _thread = thread;
  }

  @Override
  public int getQueuedMessageCount()
  {
    return _thread.getQueuedMessageCount();
  }

  @Override
  public int getRemainingCapacity()
  {
    return _thread.getRemainingCapacity();
  }

  @Override
  public boolean isAlive()
  {
    return _thread.isAlive();
  }
}
