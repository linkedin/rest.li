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

package com.linkedin.restli.server;


import com.linkedin.data.template.RecordTemplate;

import java.util.List;
import java.util.Map;


/**
 * An abstraction that encapsulates incoming request data.
 *
 * @author nshankar
 *
 */
public interface RestLiRequestData
{
  /**
   * Determine if the data corresponds to a batch request.
   *
   * @return true if the request is a batch request; else false.
   */
  boolean isBatchRequest();

  /**
   * Determine if the request has a (single) key.
   *
   * @return true if the request has a (single) key; else false
   */
  boolean hasKey();

  /**
   * Determine if the request has batch keys.
   *
   * @return true if the request has batch keys; else false
   */
  boolean hasBatchKeys();

  /**
   * Determine if the request has a (single) entity.
   *
   * @return true if the request has a (single) entity; else false
   */
  boolean hasEntity();

  /**
   * Determine if the request has batch entities.
   *
   * @return true if the request has batch entities; else false
   */
  boolean hasBatchEntities();

  /**
   * Determine if the request has batch entities.
   *
   * @return true if the request has batch entities; else false
   */
  boolean hasBatchKeyEntityMap();

  /**
   * Obtain the key.
   *
   * @return the key is one exits; else null.
   */
  Object getKey();

  /**
   * Set the key.
   *
   * @param key
   *          New value of the key.
   */
  void setKey(Object key);

  /**
   * Obtain a mutable {@link List} of batch keys.
   *
   * @return List of keys if exists; else null.
   */
  List<?> getBatchKeys();

  /**
   * Obtain the entity.
   *
   * @return the entity if one exists; else null.
   */
  RecordTemplate getEntity();

  /**
   * Set the entity.
   *
   * @param entity
   *          New value of the entity.
   */
  void setEntity(RecordTemplate entity);

  /**
   * Obtain a mutable {@link List} of batch entities.
   *
   * @return List of entities if one exists; else null.
   */
  List<? extends RecordTemplate> getBatchEntities();

  /**
   * Obtain a mutable key entity map. This map is populated primarily for batch update and batch
   * partial update requests.
   *
   * @return mutable key entity map.
   */
  Map<?, ? extends RecordTemplate> getBatchKeyEntityMap();
}
