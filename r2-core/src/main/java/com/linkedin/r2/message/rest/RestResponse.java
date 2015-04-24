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
package com.linkedin.r2.message.rest;


import java.util.Collections;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.Response;


/**
 * An object that contains details of a REST response.<p/>
 *
 * Instances of RestResponse are immutable and thread-safe. New instances can be created using the
 * {@link RestResponseBuilder}. An existing RestResponse can be used as a prototype for
 * building a new RestResponse using the {@link #builder()} method.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface RestResponse extends RestMessage, Response
{
  RestResponse NO_RESPONSE = new RestResponseImpl(
      ByteString.empty(), Collections.<String, String>emptyMap(), Collections.<String>emptyList(), 0);

  /**
   * Returns the status for this response.
   *
   * @return the status for this response
   * @see com.linkedin.r2.message.rest.RestStatus
   */
  int getStatus();

  /**
   * Returns a {@link RestResponseBuilder}, which provides a means of constructing a new
   * response using this response as a starting point. Changes made with the builder are
   * not reflected by this response instance.
   *
   * @return a builder for this response
   */
  @Override
  RestResponseBuilder builder();
}
