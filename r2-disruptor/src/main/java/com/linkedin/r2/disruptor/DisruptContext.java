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


/**
 * Abstract implementation of different disrupt contexts.
 *
 * @author Sean Sheng
 * @version $Revision$
 */
public abstract class DisruptContext
{
  /**
   * Key used to access the R2 disrupt source field in {@link RequestContext}. The
   * disrupt source described if the {@link DisruptMode} of a request has already
   * been evaluated by some source.
   */
  public static final String DISRUPT_SOURCE_KEY = "R2_DISRUPT_SOURCE";

  /**
   * Key used to access the R2 disrupt context field in {@link RequestContext}
   */
  public static final String DISRUPT_CONTEXT_KEY = "R2_DISRUPT_CONTEXT";

  /**
   * Key used to access the R2 disrupt request start time field in {@link RequestContext}
   */
  public static final String DISRUPT_REQUEST_START_TIME_KEY = "R2_DISRUPT_REQUEST_START";

  private final DisruptMode _mode;

  public DisruptContext(DisruptMode mode)
  {
    _mode = mode;
  }

  public DisruptMode mode()
  {
    return _mode;
  }
}