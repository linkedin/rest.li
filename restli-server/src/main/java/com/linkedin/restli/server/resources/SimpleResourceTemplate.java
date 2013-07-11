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


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.UpdateResponse;


/**
 * Base {@link SimpleResource} implementation. All implementations should extend this.
 */
public class SimpleResourceTemplate<V extends RecordTemplate>
    extends ResourceContextHolder implements SimpleResource<V>
{
  /**
   * @see SimpleResource#get
   */
  @Override
  public V get()
  {
    throw new RoutingException("'get' not implemented", 400);
  }

  /**
   * @see SimpleResource#update
   */
  @Override
  public UpdateResponse update(final V entity)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  /**
   * @see SimpleResource#update
   */
  @Override
  public UpdateResponse update(PatchRequest<V> patch)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  /**
   * @see SimpleResource#delete
   */
  @Override
  public UpdateResponse delete()
  {
    throw new RoutingException("'delete' not implemented", 400);
  }
}