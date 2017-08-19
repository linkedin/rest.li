/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.common;

import com.linkedin.data.template.RecordTemplate;

/**
 * Response containing both key and value.
 *
 * @author Boyang Chen
 */
public class IdEntityResponse<K, V extends RecordTemplate> extends IdResponse<K>
{
  private V _entity;

  public IdEntityResponse(K key, V entity)
  {
    super(key);
    _entity = entity;
  }

  public V getEntity()
  {
    return _entity;
  }

  @Override
  public String toString()
  {
    return "id: " + super.toString() + ", entity: " + (_entity == null ? "" : _entity);
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof IdEntityResponse)
    {
      IdEntityResponse<?, ?> that = (IdEntityResponse<?, ?>) obj;
      return super.equals(that) &&
          (this._entity == null ? that._entity == null : this._entity.equals(that._entity));
    }
    else
    {
      return false;
    }
  }

  @Override
  public int hashCode()
  {
    return super.hashCode() * 31 + (_entity == null ? 0 : _entity.hashCode());
  }
}
