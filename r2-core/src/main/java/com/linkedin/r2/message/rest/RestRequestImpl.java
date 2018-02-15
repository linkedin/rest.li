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
import com.linkedin.r2.message.BaseRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 */
/* package private */ final class RestRequestImpl extends BaseRequest implements RestRequest
{
  private final ByteString _entity;

  /* package private */ RestRequestImpl(ByteString entity, Map<String, String> headers,
                                        List<String> cookies, URI uri, String method)
  {
    super(headers, cookies, uri, method);
    _entity = entity;
  }

  @Override
  public ByteString getEntity()
  {
    return _entity;
  }

  @Override
  public RestRequestBuilder builder()
  {
    return new RestRequestBuilder(this);
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (!(o instanceof RestRequestImpl))
    {
      return false;
    }
    if (!super.equals(o))
    {
      return false;
    }

    RestRequestImpl that = (RestRequestImpl) o;
    return _entity.equals(that._entity);
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = result * 31 +  _entity.hashCode();
    return result;
  }

  @Override
  public String toString()
  {
    return "RestRequest[headers=" + getHeaders() + ",cookies=" + getCookies() + ",uri=" + getURI() + ",method="
        + getMethod() + ",entityLength=" + _entity.length() + "]";
  }

}
