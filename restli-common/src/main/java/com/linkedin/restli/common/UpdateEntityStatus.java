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

package com.linkedin.restli.common;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;


/**
 * Extension of {@link UpdateStatus} that supports returning the entity in the response for a batch update request.
 * Supported for BATCH_PARTIAL_UPDATE.
 *
 * @author Evan Williams
 */
public class UpdateEntityStatus<V extends RecordTemplate> extends UpdateStatus
{
  public static final String ENTITY = "entity";

  private final V _entity;

  /**
   * Creates an instance with the given underlying data matching the schema of {@link UpdateStatus}, with the entity
   * to be returned in addition.
   * <p>This constructor is for internal use only.</p>
   *
   * @param dataMap the underlying DataMap of the {@link UpdateStatus} response. This data should fit the {@link UpdateStatus} schema.
   * @param entity the patched entity being returned
   */
  public UpdateEntityStatus(DataMap dataMap, V entity)
  {
    super(createDataMap(dataMap, entity));
    _entity = entity;
  }

  /**
   * Creates an instance with the given status and patched entity.
   * @param status the individual http status
   * @param entity the patched entity being returned
   */
  public UpdateEntityStatus(int status, V entity)
  {
    super(createDataMap(status, entity));
    _entity = entity;
  }

  /**
   * Create a DataMap matching the schema of {@link UpdateStatus} with the given data.
   * @param dataMap the data for the underlying record
   * @param entity the patched entity being returned
   * @return a {@link DataMap} containing the given data
   */
  private static DataMap createDataMap(DataMap dataMap, RecordTemplate entity)
  {
    UpdateStatus updateStatus = new UpdateStatus(dataMap);
    DataMap newDataMap = updateStatus.data();
    if (entity != null)
    {
      newDataMap.put(ENTITY, entity.data());
    }
    return newDataMap;
  }

  /**
   * Create a DataMap matching the schema of {@link UpdateStatus} with the given data.
   * @param status the individual http status
   * @param entity the patched entity being returned
   * @return a {@link DataMap} containing the given data
   */
  private static DataMap createDataMap(int status, RecordTemplate entity)
  {
    UpdateStatus updateStatus = new UpdateStatus();
    updateStatus.setStatus(status);
    final DataMap dataMap = updateStatus.data();
    if (entity != null)
    {
      dataMap.put(ENTITY, entity.data());
    }
    return dataMap;
  }

  public boolean hasEntity()
  {
    return _entity != null;
  }

  public V getEntity()
  {
    return _entity;
  }
}
