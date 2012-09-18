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
package com.linkedin.r2.message;

import java.net.URI;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface Request extends Message
{
  /**
   * Returns the URI for this request.
   *
   * @return the URI for this request
   */
  URI getURI();

  /**
   * Returns a {@link RequestBuilder}, which provides a means of constructing a new request using
   * this request as a starting point. Changes made with the builder are not reflected by this
   * request instance. The concrete type (for example {@link com.linkedin.r2.message.rpc.RpcRequest})
   * is preserved when building the new request.
   *
   * @return a builder for this request
   */
  RequestBuilder<? extends RequestBuilder<?>> requestBuilder();
}
