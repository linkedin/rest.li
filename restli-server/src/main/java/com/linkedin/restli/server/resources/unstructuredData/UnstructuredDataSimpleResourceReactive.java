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
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.UnstructuredDataReactiveReader;
import com.linkedin.restli.server.UnstructuredDataReactiveResult;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.UnstructuredDataReactiveReaderParam;
import com.linkedin.restli.server.resources.BaseResource;


/**
 * This is a counterpart of {@link UnstructuredDataSimpleResource} with reactive streaming interface.
 */
public interface UnstructuredDataSimpleResourceReactive extends BaseResource, SingleUnstructuredDataResource
{
  /**
   * Respond an unstructured data with reactive streaming.
   *
   * @param callback The response callback
   */
  default void get(@CallbackParam Callback<UnstructuredDataReactiveResult> callback)
  {
    throw new RoutingException("'get' is not implemented", 400);
  }

  default void update(@UnstructuredDataReactiveReaderParam UnstructuredDataReactiveReader reader, @CallbackParam Callback<UpdateResponse> callback)
  {
    throw new RoutingException("'update' is not implemented", 400);
  }

  default void delete(@CallbackParam Callback<UpdateResponse> callback)
  {
    throw new RoutingException("'delete' is not implemented", 400);
  }
}
