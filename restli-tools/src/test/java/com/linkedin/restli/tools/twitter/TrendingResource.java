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

package com.linkedin.restli.tools.twitter;


import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;
import com.linkedin.restli.tools.twitter.TwitterTestDataModels.Trending;


/**
 * Simple resource that contains the location of a status.
 */
@RestLiSimpleResource(name="trending")
public class TrendingResource extends SimpleResourceTemplate<Trending>
{
  /**
   * Gets the global trending topics information.
   */
  @Override
  public Trending get()
  {
    return null;
  }

  /**
   * Updates the global trending topics information.
   */
  @Override
  public UpdateResponse update(Trending trending)
  {
    return null;
  }

  /**
   * Updates the global trending topics information.
   */
  @Override
  public UpdateResponse update(PatchRequest<Trending> patch)
  {
    return null;
  }

  /**
   * Deletes the global trending topics information.
   */
  @Override
  public UpdateResponse delete()
  {
    return null;
  }
}

