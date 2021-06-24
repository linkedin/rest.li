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
import com.linkedin.restli.common.BatchCollectionResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.BatchCollectionResponseDecoder;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * A request for reading a resource collection by a batch of search criteria.
 *
 * @param <V> entity type for resource
 *
 * @author Jiaqi Guan
 */
public class BatchFindRequest<V extends RecordTemplate>
                  extends Request<BatchCollectionResponse<V>>
{
  private final CompoundKey _assocKey;

  public BatchFindRequest(Map<String, String> headers,
                  List<HttpCookie> cookies,
                  Class<V> templateClass,
                  ResourceSpec resourceSpec,
                  Map<String, Object> queryParams,
                  Map<String, Class<?>> queryParamClasses,
                  String name,
                  String baseUriTemplate,
                  Map<String, Object> pathKeys,
                  RestliRequestOptions requestOptions,
                  CompoundKey assocKey)
  {
    super(ResourceMethod.BATCH_FINDER,
        null,
        headers,
        cookies,
        new BatchCollectionResponseDecoder<>(templateClass),
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    BatchFindRequest<?> that = (BatchFindRequest<?>) o;
    return Objects.equals(_assocKey, that._assocKey);
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

  @Override
  public Set<PathSpec> getFields()
  {
    return super.getFields();
  }
}
