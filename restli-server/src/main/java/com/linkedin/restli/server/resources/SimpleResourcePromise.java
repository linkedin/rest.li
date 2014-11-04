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

package com.linkedin.restli.server.resources;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.UpdateResponse;


/**
 * @author kparikh
 */
public interface SimpleResourcePromise<V extends RecordTemplate> extends BaseResource, SingleObjectResource<V>
{
  /**
   * Gets this resource.
   *
   * @return null if resource was not found
   */
  Promise<V> get();

  /**
   * Updates this resource.
   *
   * @param entity value to update the resource with
   * @return the status or null if for a default, 204 No Content response
   */
  Promise<UpdateResponse> update(V entity);

  /**
   * Partially update this resource given a Patch Request.
   *
   * @param patch value to update the resource with
   * @return the status or null if for a default, 204 No Content response
   */
  Promise<UpdateResponse> update(PatchRequest<V> patch);

  /**
   * Deletes this resource.
   *
   * @return the status or null if for a default, 204 No Content response
   */
  Promise<UpdateResponse> delete();
}
