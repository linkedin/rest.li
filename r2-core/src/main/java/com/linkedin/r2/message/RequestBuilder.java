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
public interface RequestBuilder<B extends RequestBuilder<B>> extends MessageBuilder<B>
{
  /**
   * Sets the URI for this request.
   *
   * @param uri the URI to set
   * @return this builder
   */
  B setURI(URI uri);

  /**
   * Returns the URI for this request.
   *
   * @return the URI for this request
   */
  URI getURI();

  /**
   * Constructs an immutable {@link Request} using the settings configured in this builder.
   * Subsequent changes to this builder will not change this request. The concrete
   * type of this builder (for example {@link com.linkedin.r2.message.rpc.RpcRequestBuilder}) will
   * be used to build the appropriate concrete type.
   *
   * @return a Request from the settings in this builder
   */
  @Override
  Request build();

  /**
   * Similar to {@link #build}, but the returned Message is in canonical form.
   *
   * @return a Request from the settings in this builder.
   */
  @Override
  Request buildCanonical();
}
