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
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.annotations.Context;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Trending;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * CollectionResource containing all trending regions of a parent global trending resource
 *
 * @author dellamag
 */
@RestLiCollection(name="trendRegions",
                  keyName="trendRegionId",
                  parent=TrendingResource.class)
public class TrendRegionsCollectionResource extends CollectionResourceTemplate<String, Trending>
{
  /**
   * Returns the trending regions sorted by popularity.
   */
  @Finder("get_trending_by_popularity")
  public List<Trending> getTrendingByPopularity(@Context PagingContext pagingContext)
  {
    return null;
  }

  /**
   * Creates a new trending region
   */
  @Override
  public CreateResponse create(Trending entity)
  {
    return null;
  }

  /**
   * Returns a batch of trending regions.
   */
  @Override
  public Map<String, Trending> batchGet(Set<String> ids)
  {
    return null;
  }

  /**
   * Gets a single trending region resource
   */
  @Override
  public Trending get(String key)
  {
    return null;
  }

  /**
   * Deletes a trending region resource
   */
  @Override
  public UpdateResponse delete(String key)
  {
    return null;
  }

  /**
   * Partially updates a trending region resource
   */
  @Override
  public UpdateResponse update(String key, PatchRequest<Trending> request)
  {
    return null;
  }

  /**
   * Updates (overwrites) a trending region resource
   */
  @Override
  public UpdateResponse update(String key, Trending entity)
  {
    return null;
  }

  /**
   * Batch updates (overwrites) trending region resources
   */
  @Override
  public BatchUpdateResult<String, Trending> batchUpdate(
      BatchUpdateRequest<String, Trending> entities)
  {
    return null;
  }

  /**
   * Batch patches trending region resources
   */
  @Override
  public BatchUpdateResult<String, Trending> batchUpdate(
      BatchPatchRequest<String, Trending> entityUpdates)
  {
    return null;
  }

  /**
   * Batch creates (overwrites) trending region resources
   */
  @Override
  public BatchCreateResult<String, Trending> batchCreate(
      BatchCreateRequest<String, Trending> entities)
  {
    return null;
  }

  /**
   * Batch deletes trending region resources
   */
  @Override
  public BatchUpdateResult<String, Trending> batchDelete(
      BatchDeleteRequest<String, Trending> ids)
  {
    return null;
  }
}
