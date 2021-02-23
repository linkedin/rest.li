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

/**
 * $Id: $
 */

package com.linkedin.restli.client;


import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.EmptyResponseDecoder;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;


/**
 * @author Josh Walker
 * @version $Revision: $
 */
public class PartialUpdateRequest<T> extends Request<EmptyRecord>
{
  private final Object _id;

  public PartialUpdateRequest(PatchRequest<T> input,
                       Map<String, String> headers,
                       List<HttpCookie> cookies,
                       ResourceSpec resourceSpec,
                       Map<String, Object> queryParams,
                       Map<String, Class<?>> queryParamClasses,
                       String baseUriTemplate,
                       Map<String, Object> pathKeys,
                       RestliRequestOptions requestOptions,
                       Object id,
                       List<Object> streamingAttachments)
  {
    super(ResourceMethod.PARTIAL_UPDATE,
          input,
          headers,
          cookies,
          new EmptyResponseDecoder(),
          resourceSpec,
          queryParams,
          queryParamClasses,
          null,
          baseUriTemplate,
          pathKeys,
          requestOptions,
          streamingAttachments);
    _id = id;
    validateKeyPresence(_id);
  }

  public Object getId()
  {
    return _id;
  }

  @Override
  public int hashCode()
  {
    final int idHashCode = (_id != null ? _id.hashCode() : 0);
    return 31 * super.hashCode() + idHashCode;
  }

  @Override
  public boolean equals(Object obj)
  {
    boolean superEquals = super.equals(obj);

    if (!superEquals)
    {
      return false;
    }

    PartialUpdateRequest<?> other = (PartialUpdateRequest<?>)obj;

    if ((_id != null) ? !_id.equals(other._id) : other._id != null)
    {
      return false;
    }

    return true;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.append(", {_id=");
    sb.append(_id);
    sb.append("}");
    return sb.toString();
  }
}