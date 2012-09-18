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
package com.linkedin.r2.caprep;

import java.io.IOException;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface CapRepAdmin
{
  /**
   * Puts the CapRep system into capture mode. Requests and responses will be
   * written to {@code directory}. If {@code directory} does not exist, it is
   * created.
   *
   * @param directory the directory in which to store captured requests and
   *                  responses.
   * @throws IOException if an error occurs while switching to capture mode.
   *                     In this case the CapRep system will fall back to
   *                     passThrough().
   */
  void capture(String directory) throws IOException;

  /**
   * Puts the CapRep system into replay mode. If a request is issued that was
   * previously recorded then its recorded response will be used instead of
   * sending the request over the network.
   *
   * @param directory the directory from which to read requests and responses.
   *                  This should be the same directory used in
   *                  {@link #capture}.
   * @throws IOException if an error occurs while switching to replay mode.
   *                     In this case the CapRep system will fall back to
   *                     passThrough().
   */
  void replay(String directory) throws IOException;

  /**
   * Puts the CapRep system into pass-through mode. In this mode requests and
   * responses are sent as during normal operation - they are not altered or
   * captured.
   */
  void passThrough();

  /**
   * Returns a String representation of the current mode that the CapRep system
   * is in.
   *
   * @return String representation of the current mode.
   */
  String getMode();
}
