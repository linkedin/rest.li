/*
   Copyright (c) 2018 LinkedIn Corp.

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
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Partial update request that keeps track of the entity's key and template class. Meant for resource methods
 * that return the patched entity, so it supports decoding the entity in the response.
 *
 * @param <T> entity class
 *
 * @author Evan Williams
 */
public class PartialUpdateEntityRequest<T extends RecordTemplate> extends Request<T>
{
  private final Object _id;

  public PartialUpdateEntityRequest(PatchRequest<T> input,
                        Map<String, String> headers,
                        List<HttpCookie> cookies,
                        EntityResponseDecoder<T> decoder,
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
          decoder,
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

    PartialUpdateEntityRequest<?> other = (PartialUpdateEntityRequest<?>) obj;

    return _id != null ? _id.equals(other._id) : other._id == null;
  }
}