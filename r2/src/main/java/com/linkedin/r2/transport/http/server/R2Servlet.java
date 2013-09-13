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

package com.linkedin.r2.transport.http.server;

import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class R2Servlet extends AbstractR2Servlet
{
  private static final long serialVersionUID = 0L;
  private final HttpDispatcher _dispatcher;

  public R2Servlet(HttpDispatcher dispatcher)
  {
    this(dispatcher, false, -1);
  }

  public R2Servlet(TransportDispatcher dispatcher)
  {
    this(new HttpDispatcher(dispatcher));
  }

  /**
   * Initialize the R2Servlet.
   * @see AbstractR2Servlet#AbstractR2Servlet(boolean, int)
   */
  public R2Servlet(HttpDispatcher dispatcher,
                    boolean useAsync,
                    int timeOut)
  {
    super(useAsync, timeOut);
    _dispatcher = dispatcher;
  }

  /**
   * Initialize the R2Servlet.
   * @see AbstractR2Servlet#AbstractR2Servlet(boolean, int)
   */
  @Deprecated
  public R2Servlet(TransportDispatcher dispatcher,
                    boolean useAsync,
                    int timeOut,
                    int timeOutDelta)
  {
    this(dispatcher, useAsync, timeOut);
  }

  /**
   * Initialize the R2Servlet.
   * @see AbstractR2Servlet#AbstractR2Servlet(boolean, int)
   */
  public R2Servlet(TransportDispatcher dispatcher,
                    boolean useAsync,
                    int timeOut)
  {
    this(new HttpDispatcher(dispatcher), useAsync, timeOut);
  }


  @Override
  protected HttpDispatcher getDispatcher()
  {
    return _dispatcher;
  }
}
