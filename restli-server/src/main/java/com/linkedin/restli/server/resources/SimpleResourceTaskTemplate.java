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
import com.linkedin.parseq.Task;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.annotations.RestLiTemplate;


/**
 * Base template class for building async Rest.li simple resources that return {@link Task}s
 *
 * @author kparikh
 */
@RestLiTemplate(expectedAnnotation = RestLiSimpleResource.class)
public class SimpleResourceTaskTemplate<V extends RecordTemplate>
  extends ResourceContextHolder implements SimpleResourceTask<V>
{
  @Override
  public Task<V> get()
  {
    throw new RoutingException("'get' not implemented", 400);
  }

  @Override
  public Task<UpdateResponse> update(V entity)
  {
    throw new RoutingException("'update' not implemented", 400);
  }

  @Override
  public Task<UpdateResponse> update(PatchRequest<V> patch)
  {
    throw new RoutingException("'partial_update' not implemented", 400);
  }

  @Override
  public Task<UpdateResponse> delete()
  {
    throw new RoutingException("'delete' not implemented", 400);
  }
}
