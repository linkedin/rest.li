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

package com.linkedin.r2.disruptor;

import com.linkedin.r2.message.RequestContext;
import java.util.function.Supplier;


/**
 * Abstract implementation of different disrupt contexts.
 *
 * @author Sean Sheng
 * @version $Revision$
 */
public abstract class DisruptContext
{
  /**
   * Key used to access the R2 disrupt source field in {@link RequestContext}. The value for this key is the canonical
   * class name of the disruptor controller that was invoked.
   *
   * Presence of this key in a {@link RequestContext} means that no other disrupt controllers should be invoked. It
   * does <b>not</b> imply presence of {@link #DISRUPT_CONTEXT_KEY}, which may be unset if the controller determines
   * that no disruption is to take place.
   */
  public static final String DISRUPT_SOURCE_KEY = "R2_DISRUPT_SOURCE";

  /**
   * Key used to access the R2 disrupt context field in {@link RequestContext}. The value for this key is the
   * {@link DisruptContext} instance that should be used to disrupt a request, if any.
   *
   * When this key is set in a {@link RequestContext}, the {@link #DISRUPT_SOURCE_KEY} must be set as well.
   */
  public static final String DISRUPT_CONTEXT_KEY = "R2_DISRUPT_CONTEXT";

  private final DisruptMode _mode;

  public DisruptContext(DisruptMode mode)
  {
    _mode = mode;
  }

  public DisruptMode mode()
  {
    return _mode;
  }

  /**
   * If there was no previous disruptor called, adds the DisruptContext given by disruptContextSupplier to the
   * requestContext.
   * @param context The request context to which the disrupt context should be added.
   * @param controllerClass The class which is controlling the disruption. Used as the identifier in the request context
   *                        so that later disruptor calls will be skipped.
   * @param disruptContextSupplier Called to provide the DisruptContext. If it returns null, the disruptor is still
   *                               considered to be set, preventing other DisruptContexts from being added later.
   */
  public static void addDisruptContextIfNotPresent(RequestContext context, Class<?> controllerClass,
      Supplier<DisruptContext> disruptContextSupplier) {
    if (context.getLocalAttr(DISRUPT_SOURCE_KEY) != null)
    {
      return;
    }
    context.putLocalAttr(DISRUPT_SOURCE_KEY, controllerClass.getCanonicalName());
    DisruptContext disruptContext = disruptContextSupplier.get();
    if (disruptContext == null) {
      return;
    }
    context.putLocalAttr(DISRUPT_CONTEXT_KEY, disruptContext);
  }
}