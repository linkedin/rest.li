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


import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.EntityResponseDecoder;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A request for reading an entity resource.
 *
 * @param <T> entity template class
 *
 * @author Eran Leshem
 */
public class GetRequest<T extends RecordTemplate> extends Request<T>
{
  private final Class<T> _templateClass;
  private final Object   _id;

  public GetRequest(Map<String, String> headers,
             List<HttpCookie> cookies,
             Class<T> templateClass,
             Object id,
             Map<String, Object> queryParams,
             Map<String, Class<?>> queryParamClasses,
             ResourceSpec resourceSpec,
             String baseUriTemplate,
             Map<String, Object> pathKeys,
             RestliRequestOptions requestOptions)
  {
    super(ResourceMethod.GET,
          null,
          headers,
          cookies,
          new EntityResponseDecoder<T>(templateClass),
          resourceSpec,
          queryParams,
          queryParamClasses,
          null,
          baseUriTemplate,
          pathKeys,
          requestOptions,
          null);

    _templateClass = templateClass;
    _id = id;
    validateKeyPresence(_id);
  }

  public Class<T> getEntityClass()
  {
    return _templateClass;
  }

  public Object getObjectId()
  {
    return _id;
  }

  @Override
  public Set<PathSpec> getFields()
  {
    return super.getFields();
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

    GetRequest<?> other = (GetRequest<?>)obj;

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
    sb.append(", _templateClass=");
    sb.append(_templateClass);
    sb.append("}");
    return sb.toString();
  }
}
