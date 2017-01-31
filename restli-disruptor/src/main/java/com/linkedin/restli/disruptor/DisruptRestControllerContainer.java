/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.restli.disruptor;

import java.util.concurrent.atomic.AtomicReference;


/**
 * Singleton container of a {@link DisruptRestController} implementation
 *
 * @author Sean Sheng
 * @version $Revision$
 */
public class DisruptRestControllerContainer
{
  private static final AtomicReference<DisruptRestController> _instance = new AtomicReference<>();

  private DisruptRestControllerContainer()
  {
  }

  public static DisruptRestController getInstance()
  {
    return _instance.get();
  }

  public static void setInstance(DisruptRestController instance)
  {
    if (!_instance.compareAndSet(null, instance))
    {
      throw new IllegalStateException("Instance has already been set");
    }
  }

  /* package private */ static void resetInstance()
  {
    _instance.set(null);
  }
}
