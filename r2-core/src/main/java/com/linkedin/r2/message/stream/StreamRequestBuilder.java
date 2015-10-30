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
package com.linkedin.r2.message.stream;

import com.linkedin.r2.message.BaseRequestBuilder;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.stream.entitystream.EntityStream;

import java.net.URI;

/**
 * @author Zhenkai Zhu
 */
public final class StreamRequestBuilder extends BaseRequestBuilder<StreamRequestBuilder>
    implements StreamMessageBuilder<StreamRequestBuilder>
{
  /**
   * Constructs a new builder using the given uri.
   *
   * @param uri the URI for the resource involved in the request
   */
  public StreamRequestBuilder(URI uri)
  {
    super(uri);
  }

  /**
   * Copies the values from the supplied request. Changes to this builder will not be reflected
   * in the original message.
   *
   * @param request the request to copy
   */
  public StreamRequestBuilder(Request request)
  {
    super(request);
  }

  @Override
  public StreamRequest build(EntityStream entityStream)
  {
    return new StreamRequestImpl(entityStream, getHeaders(), getCookies(), getURI(), getMethod());
  }

  @Override
  public StreamRequest buildCanonical(EntityStream entityStream)
  {
    return new StreamRequestImpl(entityStream, getCanonicalHeaders(), getCanonicalCookies(), getURI().normalize(), getMethod());
  }
}
