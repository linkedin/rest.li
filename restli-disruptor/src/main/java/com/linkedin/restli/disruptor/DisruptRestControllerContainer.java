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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Singleton container of a {@link DisruptRestController} implementation.
 *
 * <p> The goal of using this class is to achieve a backward compatible way to enable Rest.li disruptor
 * without code change. We should consider removing this class once the disruptor implementation has
 * been widely adopted.
 *
 * @author Sean Sheng
 * @version $Revision$
 */
public class DisruptRestControllerContainer
{
  private static final Logger LOG = LoggerFactory.getLogger(DisruptRestControllerContainer.class);

  private static final AtomicReference<DisruptRestController> INSTANCE = new AtomicReference<>();

  private DisruptRestControllerContainer()
  {
  }

  public static DisruptRestController getInstance()
  {
    return INSTANCE.get();
  }

  public static void setInstance(DisruptRestController instance)
  {
    if (!INSTANCE.compareAndSet(null, instance))
    {
      LOG.warn("Ignored because instance has already been set. Invoke resetInstance before setInstance again.");
    }
  }

  /* package private */ static void resetInstance()
  {
    INSTANCE.set(null);
  }
}
