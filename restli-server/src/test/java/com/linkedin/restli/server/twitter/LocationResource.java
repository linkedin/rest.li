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


import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Location;


/**
 * Simple resource that contains the location of a status.
 */
@RestLiSimpleResource(name="location", parent=StatusCollectionResource.class)
public class LocationResource extends SimpleResourceTemplate<Location>
{
  /**
   * Gets the location of the parent status.
   */
  @Override
  public Location get()
  {
    return null;
  }

  /**
   * Updates the location of the parent status.
   */
  @Override
  public UpdateResponse update(Location location)
  {
    return null;
  }

  /**
   * Updates the location of the parent status.
   */
  @Override
  public UpdateResponse update(PatchRequest<Location> patch)
  {
    return null;
  }

  /**
   * Deletes the location of the parent status.
   */
  @Override
  public UpdateResponse delete()
  {
    return null;
  }

  /**
   * Replies to all followers nearby.
   */
  @Action(name="new_status_from_location")
  public void newStatusFromLocation(@ActionParam("status") String status)
  {

  }
}
