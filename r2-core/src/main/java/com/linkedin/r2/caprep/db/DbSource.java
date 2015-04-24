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
package com.linkedin.r2.caprep.db;

import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.Response;

/**
 * Interface for replaying captured request/response pairs.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface DbSource
{
  /**
   * Obtain the response for a given request.
   *
   * @param req the request to use as a search key.
   * @param <T> a subclass of Request.
   * @return the Response object for the specified request, or null if no response is found.
   */
  <T extends Response> T replay(Request req);
}
