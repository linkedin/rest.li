/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.server;


import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;


/**
 * This servlet provides the support for asynchronous request handling with streaming. It can only be used with
 * containers supporting Servlet API 3.0 or greater.
 *
 * @author Zhenkai Zhu
 *
 */
public class AsyncR2StreamServlet extends AbstractAsyncR2StreamServlet
{
  private static final long serialVersionUID = 0L;

  private final HttpDispatcher _dispatcher;


  public AsyncR2StreamServlet(HttpDispatcher dispatcher,
                              long timeout)
  {
    super(timeout);
    _dispatcher = dispatcher;
  }

  /**
   * Creates the AsyncR2Servlet.
   */
  public AsyncR2StreamServlet(TransportDispatcher dispatcher,
                              long timeout)
  {

    this(new HttpDispatcher(dispatcher), timeout);
  }

  @Override
  protected HttpDispatcher getDispatcher()
  {
    return _dispatcher;
  }
}
