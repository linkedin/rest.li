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

package com.linkedin.restli.internal.client.response;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.ComplexKeySpec;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.UpdateEntityStatus;
import java.util.Map;


/**
 * Specialized {@link BatchKVResponse} whose value class is {@link UpdateEntityStatus}. Used for BATCH_PARTIAL_UPDATE
 * responses that return the patched entities.
 *
 * @author Evan Williams
 */
public class BatchUpdateEntityResponse<K, E extends RecordTemplate> extends BatchKVResponse<K, UpdateEntityStatus<E>>
{
  private final TypeSpec<E> _entityType;

  public BatchUpdateEntityResponse(DataMap data,
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
  protected UpdateEntityStatus<E> deserializeValue(Object valueData)
  {
    DataMap valueDataMap = (DataMap) valueData;
    E entity = valueDataMap.containsKey(UpdateEntityStatus.ENTITY) ?
        DataTemplateUtil.wrap(((DataMap) valueData).getDataMap(UpdateEntityStatus.ENTITY), _entityType.getType()) :
        null;
    return new UpdateEntityStatus<>((DataMap) valueData, entity);
  }

  private Class<UpdateEntityStatus<E>> getEntityResponseValueClass()
  {
    @SuppressWarnings("unchecked")
    final Class<UpdateEntityStatus<E>> valueClass = (Class<UpdateEntityStatus<E>>) (Object) UpdateEntityStatus.class;
    return valueClass;
  }
}
