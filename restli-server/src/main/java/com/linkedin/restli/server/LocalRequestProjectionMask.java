/*
   Copyright (c) 2021 LinkedIn Corp.

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

import com.linkedin.data.transform.filter.request.MaskTree;
import javax.annotation.Nullable;


/**
 * Encapsulates projection {@link MaskTree} that can be set in local attributes of
 * {@link com.linkedin.r2.message.RequestContext} with the
 * {@link com.linkedin.restli.internal.server.ServerResourceContext#CONTEXT_PROJECTION_MASKS_KEY}.
 *
 * <p>This enables a nifty performance optimization that avoids serializing and deserializing the projection
 * masks for in-process request execution.</p>
 */
public class LocalRequestProjectionMask
{
  /**
   * Projection mask for root entity.
   */
  @Nullable
  private final MaskTree _projectionMask;

  /**
   * Projection mask for metadata.
   */
  @Nullable
  private final MaskTree _metadataProjectionMask;

  /**
   * Projection mask for paging.
   */
  @Nullable
  private final MaskTree _pagingProjectionMask;

  /**
   * Constructor
   *
   * @param projectionMask           Projection mask for root entity.
   * @param metadataProjectionMask   Projection mask for metadata.
   * @param pagingProjectionMask     Projection mask for paging.
   */
  public LocalRequestProjectionMask(@Nullable MaskTree projectionMask,
      @Nullable MaskTree metadataProjectionMask,
      @Nullable MaskTree pagingProjectionMask)
  {
    _projectionMask = projectionMask;
    _metadataProjectionMask = metadataProjectionMask;
    _pagingProjectionMask = pagingProjectionMask;
  }

  /**
   * @return Projection mask for root entity.
   */
  @Nullable
  public MaskTree getProjectionMask()
  {
    return _projectionMask;
  }

  /**
   * @return Projection mask for root entity.
   */
  @Nullable
  public MaskTree getMetadataProjectionMask()
  {
    return _metadataProjectionMask;
  }

  /**
   * @return Projection mask for root entity.
   */
  @Nullable
  public MaskTree getPagingProjectionMask()
  {
    return _pagingProjectionMask;
  }
}
