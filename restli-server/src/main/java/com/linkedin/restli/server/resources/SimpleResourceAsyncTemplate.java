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
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.annotations.RestLiTemplate;


/**
 *  Base {@link SimpleResourceAsync} implementation. All implementations should extend this.
 */
@RestLiTemplate(expectedAnnotation = RestLiSimpleResource.class)
public class SimpleResourceAsyncTemplate<V extends RecordTemplate>
    extends ResourceContextHolder implements SimpleResourceAsync<V>
{
  /**
   * @see SimpleResourceAsync#get
   */
  @Override
  public void get(final Callback<V> callback)
  {
    throw new RoutingException("'get' not implemented", 400);
  }

  /**
   * @see SimpleResourceAsync#update
   */
  @Override
  public void update(final V entity, final Callback<UpdateResponse> callback)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  /**
   * @see SimpleResourceAsync#update
   */
  @Override
  public void update(final PatchRequest<V> patch, final Callback<UpdateResponse> callback)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  /**
   * SimpleResourceAsync#delete
   */
  @Override
  public void delete(final Callback<UpdateResponse> callback)
  {
    throw new RoutingException("'delete' not implemented", 400);
  }
}
