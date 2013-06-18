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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.EntityResponseDecoder;

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
  private final URI      _baseURI;
  private final Object   _id;

  GetRequest(URI fullURI,
             Map<String, String> headers,
             Class<T> templateClass,
             URI baseURI,
             Object id,
             DataMap queryParams,
             ResourceSpec resourceSpec,
             List<String> resourcePath)
  {
    super(fullURI,
          ResourceMethod.GET,
          null,
          headers,
          new EntityResponseDecoder<T>(templateClass),
          resourceSpec,
          queryParams,
          resourcePath);

    _baseURI = baseURI;
    _templateClass = templateClass;
    _id = id;
  }

  public Class<T> getEntityClass()
  {
    return _templateClass;
  }

  public URI getBaseURI()
  {
    return _baseURI;
  }

  public Object getIdObject()
  {
    if (_id == null)
    {
      return null;
    }
    else if (_id instanceof ComplexResourceKey)
    {
      return _id;
    }
    else
    {
      return AbstractRequestBuilder.stringifySimpleValue(_id);
    }
  }

  @Deprecated
  public String getId()
  {
    return _id == null ? null : _id.toString();
  }

  @Override
  public Set<PathSpec> getFields()
  {
    return super.getFields();
  }
}
