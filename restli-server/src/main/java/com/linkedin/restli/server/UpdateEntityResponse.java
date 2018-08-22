/*
   Copyright (c) 2018 LinkedIn Corp.

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

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;


/**
 * A key-value response extension of {@link com.linkedin.restli.server.UpdateResponse} with id and entity fields.
 * This response can be used if the resource wants to return the patched entity in the response of partial update.
 *
 * @param <V> - the value type of the resource.
 *
 * @author Evan Williams
 */
public class UpdateEntityResponse<V extends RecordTemplate> extends UpdateResponse
{
  private final V _entity;

  public UpdateEntityResponse(final HttpStatus status, final V entity)
  {
    super(status);
    _entity = entity;
  }

  public boolean hasEntity()
  {
    return _entity != null;
  }

  public V getEntity()
  {
    return _entity;
  }
}
