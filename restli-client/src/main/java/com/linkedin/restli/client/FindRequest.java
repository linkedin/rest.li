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

package com.linkedin.restli.client;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.CollectionResponseDecoder;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;


/**
 * A request for reading a resource collection.
 *
 * @param <T> entity template class
 *
 * @author Eran Leshem
 */
public class FindRequest<T extends RecordTemplate>
                extends Request<CollectionResponse<T>>
{
  private final CompoundKey _assocKey;

  FindRequest(Map<String, String> headers,
              List<HttpCookie> cookies,
              Class<T> templateClass,
              ResourceSpec resourceSpec,
              Map<String, Object> queryParams,
              Map<String, Class<?>> queryParamClasses,
              String name,
              String baseUriTemplate,
              Map<String, Object> pathKeys,
              RestliRequestOptions requestOptions,
              CompoundKey assocKey)
  {
    super(ResourceMethod.FINDER,
          null,
          headers,
          cookies,
          new CollectionResponseDecoder<T>(templateClass),
          resourceSpec,
          queryParams,
          queryParamClasses,
          name,
          baseUriTemplate,
          pathKeys,
          requestOptions,
          null);
    _assocKey = assocKey;
  }

  public CompoundKey getAssocKey()
  {
    return _assocKey;
  }

  @Override
  public int hashCode()
  {
    final int assocKeyHashCode = (_assocKey != null ? _assocKey.hashCode() : 0);
    return 31 * super.hashCode() + assocKeyHashCode;
  }

  @Override
  public boolean equals(Object obj)
  {
    boolean superEquals = super.equals(obj);

    if (!superEquals)
    {
      return false;
    }

    FindRequest<?> other = (FindRequest<?>)obj;

    if ((_assocKey != null) ? !_assocKey.equals(other._assocKey) : other._assocKey != null)
    {
      return false;
    }

    return true;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.append(", {_assocKey=");
    sb.append(_assocKey);
    sb.append("}");
    return sb.toString();
  }
}
