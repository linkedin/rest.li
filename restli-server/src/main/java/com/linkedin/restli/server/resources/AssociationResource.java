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

package com.linkedin.restli.server.resources;

import java.util.Map;
import java.util.Set;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.UpdateResponse;



/**
 *
 * @author dellamag
 */
public interface AssociationResource<V extends RecordTemplate>
        extends BaseResource, KeyValueResource<CompoundKey, V>
{
  /**
   * Gets a batch of resources from this collection.
   *
   * @param ids the ids to get
   * @return a Map from K id to V value
   */
  Map<CompoundKey, V> batchGet(Set<CompoundKey> ids);

  /**
   * Gets this resource.
   *
   * @param key resource {@link CompoundKey}
   * @return null if resource was not found
   */
  V get(CompoundKey key);

  /**
   * Update this resource.
   *
   * @param key resource {@link CompoundKey}
   * @param entity value to update the resource with
   * @return the status or null if for a default, 204 No Content response
   */
  UpdateResponse update(CompoundKey key, V entity);

  /**
   * Partially update this resource given a Patch Request.
   *
   * @param key resource {@link CompoundKey}
   * @param patch to update the resource with
   * @return the status or null if for a default, 204 No Content response
   */
  UpdateResponse update(CompoundKey key, PatchRequest<V> patch);

  /**
   * Deletes this resource.
   *
   * @param key resource {@link CompoundKey}
   * @return the status or null if for a default, 204 No Content response
   */
  UpdateResponse delete(CompoundKey key);

  /**
   * Updates a batch of entities in this collection.
   *
   * @param entities - a map of entity keys and their new values
   * @return - update results
   */
  BatchUpdateResult<CompoundKey, V> batchUpdate(BatchUpdateRequest<CompoundKey, V> entities);

  /**
   * Partially updates a batch of entities in this collection.
   *
   * @param patches - a map of entity keys and the deltas to apply to them
   * @return - update results
   */
  BatchUpdateResult<CompoundKey, V> batchUpdate(BatchPatchRequest<CompoundKey, V> patches);

  /**
   * Creates a batch of entities in this collection.
   *
   * @param entities - a list of entities to be created
   * @return - creation results
   */
  BatchCreateResult<CompoundKey, V> batchCreate(BatchCreateRequest<CompoundKey, V> entities);

  /**
   * Deletes a batch of entities in this collection.
   *
   * @param ids - the keys of the entities to be deleted
   * @return - deletion results
   */
  BatchUpdateResult<CompoundKey, V> batchDelete(BatchDeleteRequest<CompoundKey, V> ids);

}
