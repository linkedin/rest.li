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

package com.linkedin.restli.server.twitter;


import com.linkedin.parseq.promise.Promise;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.ResourceContextHolder;
import com.linkedin.restli.server.resources.SingleObjectResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Location;


/**
 * Simple resource that contains the location of a status.
 */
@RestLiSimpleResource(name="promiselocation", parent=PromiseStatusCollectionResource.class)
public class PromiseLocationResource extends ResourceContextHolder implements SingleObjectResource<Location>
{
  /**
   * Gets the location of the parent status.
   */
  @RestMethod.Get
  public Promise<Location> get()
  {
    throw new AssertionError("should be mocked");
  }

  /**
   * Updates the location of the parent status.
   */
  @RestMethod.Update
  public Promise<UpdateResponse> update(final Location entity)
  {
    throw new AssertionError("should be mocked");
  }

  /**
   * Updates the location of the parent status.
   */
  @RestMethod.PartialUpdate
  public Promise<UpdateResponse> update(final PatchRequest<Location> patch)
  {
    throw new AssertionError("should be mocked");
  }

  /**
   * Deletes the location of the parent status.
   */
  @RestMethod.Delete
  public Promise<UpdateResponse> delete()
  {
    throw new AssertionError("should be mocked");
  }
}
