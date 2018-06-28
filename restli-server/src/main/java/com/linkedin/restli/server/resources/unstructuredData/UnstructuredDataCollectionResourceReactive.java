/*
 * Copyright (c) 2017 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.restli.server.resources.unstructuredData;

import com.linkedin.common.callback.Callback;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.UnstructuredDataReactiveReader;
import com.linkedin.restli.server.UnstructuredDataReactiveResult;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.UnstructuredDataReactiveReaderParam;
import com.linkedin.restli.server.resources.BaseResource;


/**
 * This is a counterpart of {@link UnstructuredDataCollectionResource} with reactive streaming interface.
 */
public interface UnstructuredDataCollectionResourceReactive<K> extends BaseResource, KeyUnstructuredDataResource<K>
{
  /**
   * Respond an unstructured data with reactive streaming.
   *
   * @param key the key of the data requested
   * @param callback the response callback
   */
  default void get(K key, @CallbackParam Callback<UnstructuredDataReactiveResult> callback)
  {
    throw new RoutingException("'get' is not implemented", 400);
  }

  default void create(@UnstructuredDataReactiveReaderParam UnstructuredDataReactiveReader reader, @CallbackParam final Callback<CreateResponse> responseCallback)
  {
    throw new RoutingException("'create' is not implemented", 400);
  }

  default void update(K key, @UnstructuredDataReactiveReaderParam UnstructuredDataReactiveReader reader, @CallbackParam final Callback<UpdateResponse> responseCallback)
  {
    throw new RoutingException("'update' is not implemented", 400);
  }

  default void delete(K key, @CallbackParam Callback<UpdateResponse> callback)
  {
    throw new RoutingException("'delete' is not implemented", 400);
  }
}