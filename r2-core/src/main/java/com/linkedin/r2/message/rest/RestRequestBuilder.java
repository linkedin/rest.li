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

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.BaseRequestBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.util.ArgumentUtil;

import java.net.URI;

/**
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 */
public final class RestRequestBuilder extends BaseRequestBuilder<RestRequestBuilder> implements RestMessageBuilder<RestRequestBuilder>
{
  private ByteString _entity = ByteString.empty();

  public RestRequestBuilder(URI uri)
  {
    super(uri);
  }

  public RestRequestBuilder(RestRequest request)
  {
    super(request);
    _entity = request.getEntity();
  }

  public RestRequestBuilder(StreamRequest request)
  {
    super(request);
  }

  public RestRequestBuilder setEntity(ByteString entity)
  {
    ArgumentUtil.notNull(entity, "entity");

    _entity = entity;
    return this;
  }

  public RestRequestBuilder setEntity(byte[] entity)
  {
    ArgumentUtil.notNull(entity, "entity");

    _entity = ByteString.copy(entity);
    return this;
  }

  public ByteString getEntity()
  {
    return _entity;
  }

  public RestRequest build()
  {
    return new RestRequestImpl(_entity, getHeaders(), getCookies(), getURI(), getMethod());
  }

  public RestRequest buildCanonical()
  {
    return new RestRequestImpl(_entity, getCanonicalHeaders(), getCanonicalCookies(), getURI(), getMethod());
  }
}
