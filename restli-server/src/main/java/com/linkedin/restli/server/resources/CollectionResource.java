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
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.UpdateResponse;

/**
 * Interface for implementations of Rest.li Collection Resources. Most applications should
 * extend CollectionResourceTemplate for convenience, rather than implementing this
 * interface directly.
 *
 * @author dellamag
 */
public interface CollectionResource<K, V extends RecordTemplate> extends
    BaseResource,
    KeyValueResource<K, V>
{
  /**
   * Creates a new resource within this collection.
   *
   * @param entity the entity to create
   * @return the HTTP status or null to return 201 Created (the default)
   */
  CreateResponse create(V entity);

  /**
   * Gets a batch of resources from this collection.
   *
   * @param ids the ids to get
   * @return a Map from K id to V value
   */
  Map<K, V> batchGet(Set<K> ids);

  /**
   * Updates a batch of entities in this collection.
   *
   * @param entities - a map of entity keys and their new values
   * @return - update results
   */
  BatchUpdateResult<K, V> batchUpdate(BatchUpdateRequest<K, V> entities);

  /**
   * Partially updates a batch of entities in this collection.
   *
   * @param patches - a map of entity keys and the deltas to apply to them
   * @return - update results
   */
  BatchUpdateResult<K, V> batchUpdate(BatchPatchRequest<K, V> patches);

  /**
   * Creates a batch of entities in this collection.
   *
   * @param entities - a list of entities to be created
   * @return - creation results. Results are ordered, and should be correlated
   *         positionally with their respective requests.
   */
  BatchCreateResult<K, V> batchCreate(BatchCreateRequest<K, V> entities);

  /**
   * Deletes a batch of entities in this collection.
   *
   * @param ids - the keys of the entities to be deleted
   * @return - deletion results
   */
  BatchUpdateResult<K, V> batchDelete(BatchDeleteRequest<K, V> ids);

  /**
   * Gets this resource.
   *
   * @param key resource key
   * @return null if resource was not found
   */
  V get(K key);

  /**
   * Update this resource.
   *
   * @param key resource key
   * @param entity value to update the resource with
   * @return the status or null if for a default, 204 No Content response
   */
  UpdateResponse update(K key, V entity);

  /**
   * Partially update this resource given a Patch Request.
   *
   * @param key resource key
   * @param patch value to update the resource with
   * @return the status or null if for a default, 204 No Content response
   */
  UpdateResponse update(K key, PatchRequest<V> patch);

  /**
   * Deletes this resource.
   *
   * @param key resource key
   * @return the status or null if for a default, 204 No Content response
   */
  UpdateResponse delete(K key);

}
