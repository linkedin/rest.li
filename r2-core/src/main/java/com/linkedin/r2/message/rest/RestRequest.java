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
package com.linkedin.r2.message.rest;

import com.linkedin.r2.message.Request;
import java.net.URI;


/**
 * An object that contains details of a REST request.
 *
 * Instances of RestRequest are immutable and thread-safe. New instances can be created using the
 * {@link RestRequestBuilder}. An existing RestRequest can be used as a prototype for
 * building a new RestRequest using the {@link #builder()} method.
 *
 * RestRequest is a request with full entity.
 *
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 */
public interface RestRequest extends Request, RestMessage
{
  /**
   * Returns a {@link RestRequestBuilder}, which provides a means of constructing a new request using
   * this request as a starting point. Changes made with the builder are not reflected by this
   * request instance.
   *
   * @return a builder for this request
   */
  @Override
  RestRequestBuilder builder();

  /**
   * @return A copy of this request with just the URI replaced.
   */
  default RestRequest copyWithURI(URI uri)
  {
    return builder().setURI(uri).build();
  }
}
