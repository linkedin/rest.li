/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.internal.client.response;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;

import java.util.Map;


/**
 * Specialized {@link BatchKVResponse} whose value class is {@link EntityResponse}.
 *
 * @author Keren Jin
 */
public class BatchEntityResponse<K, E extends RecordTemplate> extends BatchKVResponse<K, EntityResponse<E>>
{
  private final TypeSpec<E> _entityType;

  public BatchEntityResponse(DataMap data,
                             TypeSpec<K> keyType,
                             TypeSpec<E> entityType,
                             Map<String, CompoundKey.TypeInfo> keyParts,
                             ComplexKeySpec<?, ?> complexKeyType,
                             ProtocolVersion version)
  {
    super(data);

    _entityType = entityType;

    createSchema(getEntityResponseValueClass());
    deserializeData(keyType, keyParts, complexKeyType, version);
  }

  @Override
  protected EntityResponse<E> deserializeValue(Object valueData)
  {
    return new EntityResponse<E>((DataMap) valueData, _entityType.getType());
  }

  private Class<EntityResponse<E>> getEntityResponseValueClass()
  {
    @SuppressWarnings("unchecked")
    final Class<EntityResponse<E>> valueClass = (Class<EntityResponse<E>>) (Object) EntityResponse.class;
    return valueClass;
  }
}
