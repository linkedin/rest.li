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


import com.linkedin.common.callback.Callback;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.server.UpdateResponse;


/**
 * Interface for asynchronous implementations of Rest.li Simple Resources. Most
 * applications should extend SimpleResourceAsyncTemplate for convenience, rather
 * than implementing this interface directly.
 */
public interface SimpleResourceAsync<V extends RecordTemplate> extends BaseResource, SingleObjectResource<V>
{
  /**
   * Gets this resource
   *
   * @param callback The callback for the asynchronous get call.
   */
  void get(Callback<V> callback);

  /**
   * Updates this resource
   *
   * @param entity the value to update the resource with.
   * @param callback the callback for the asynchronous update call.
   */
  void update(V entity, Callback<UpdateResponse> callback);

  /**
   * Deletes this resource.
   *
   * @param callback the callback for the asynchronous delete call.
   */
  void delete(Callback<UpdateResponse> callback);
}