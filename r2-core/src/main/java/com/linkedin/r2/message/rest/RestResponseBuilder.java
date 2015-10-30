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
import com.linkedin.r2.message.BaseResponseBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.util.ArgumentUtil;

/**
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 */
public final class RestResponseBuilder extends BaseResponseBuilder<RestResponseBuilder> implements RestMessageBuilder<RestResponseBuilder>
{
  private ByteString _entity = ByteString.empty();

  public RestResponseBuilder() {}

  public RestResponseBuilder(RestResponse response)
  {
    super(response);
    _entity = response.getEntity();
  }

  public RestResponseBuilder(StreamResponse response)
  {
    super(response);
  }

  public RestResponseBuilder setEntity(ByteString entity)
  {
    ArgumentUtil.notNull(entity, "entity");

    _entity = entity;
    return this;
  }

  public RestResponseBuilder setEntity(byte[] entity)
  {
    ArgumentUtil.notNull(entity, "entity");

    _entity = ByteString.copy(entity);
    return this;
  }

  public ByteString getEntity()
  {
    return _entity;
  }

  public RestResponse build()
  {
    return new RestResponseImpl(_entity, getHeaders(), getCookies(), getStatus());
  }

  public RestResponse buildCanonical()
  {
    return new RestResponseImpl(_entity, getCanonicalHeaders(), getCanonicalCookies(), getStatus());
  }
}
