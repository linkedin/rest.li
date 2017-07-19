/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.common;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;

/**
 * Create Request that keeps track of Resource's Key and Value in Batch status.
 *
 * @author Boyang Chen
 */
public class CreateIdEntityStatus<K, V extends RecordTemplate> extends CreateIdStatus<K>
{
  private final V _entity;

  public CreateIdEntityStatus(DataMap dataMap, K key, V entity)
  {
    super(dataMap, key);
    _entity = entity;
  }

  public CreateIdEntityStatus(int status, K key, V entity, ErrorResponse error, ProtocolVersion version)
  {
    super(createDataMap(status, key, entity, null, error, version), key);
    _entity = entity;
  }

  public CreateIdEntityStatus(int status, K key, V entity, String location, ErrorResponse error, ProtocolVersion version)
  {
    super(createDataMap(status, key, entity, location, error, version), key);
    _entity = entity;
  }

  private static DataMap createDataMap(int status, Object key, RecordTemplate entity, String location, ErrorResponse error, ProtocolVersion version)
  {
    DataMap idStatusMap = CreateIdStatus.createDataMap(status, key, location, error, version);
    if (entity != null)
    {
      idStatusMap.put("entity", entity.data());
    }
    return idStatusMap;
  }

  public V getEntity()
  {
    return _entity;
  }
}