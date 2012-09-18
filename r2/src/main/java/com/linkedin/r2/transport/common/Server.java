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

/* $Id$ */
package com.linkedin.r2.transport.common;

import java.io.IOException;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface Server
{
  /**
   * Starts the server. The server startup will be completed before this method returns.
   *
   * @throws IOException if an error occurred during startup.
   */
  void start() throws IOException;

  /**
   * Starts a shutdown of the server. This method may return before shutdown has been completed.
   * Use {@link #waitForStop()} to block until the server has completely stopped.
   *
   * @throws IOException if an error occurred during shutdown.
   */
  void stop() throws IOException;

  /**
   * Blocks until the server has stopped. This method does not initiate the shutdown - use
   * {@link #stop()} to start shutdown.
   *
   * @throws InterruptedException if the thread is interrupted while waiting for the server to
   *                              shutdown.
   */
  public void waitForStop() throws InterruptedException;
}
