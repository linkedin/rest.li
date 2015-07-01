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
public class IdEntityResponse<K, V extends RecordTemplate> extends RecordTemplate
{
  private K _key;
  private V _entity;

  public IdEntityResponse(K key, V entity)
  {
    super(null, null);
    _key = key;
    _entity = entity;
  }

  public K getId()
  {
    return _key;
  }

  public Object getEntity()
  {
    return _entity;
  }

  @Override
  public String toString()
  {
    return "id: " + (_key == null ? "" : _key) + ", entity: " + (_entity == null ? "" : _entity);
  }

  @Override
  public boolean equals(Object that)
  {
    if (that instanceof IdEntityResponse)
    {
      IdEntityResponse<?, ?> thatIdResponse = (IdEntityResponse<?, ?>) that;
      boolean keyEquals = (this._key == null)? thatIdResponse._key == null : this._key.equals(thatIdResponse._key);
      boolean entityEquals = (this._entity == null)? thatIdResponse._entity == null : this._entity.equals(thatIdResponse._entity);
      return keyEquals && entityEquals;
    }
    else
    {
      return false;
    }
  }

  @Override
  public int hashCode()
  {
    return (_key == null ? 0 : _key.hashCode()) + (_entity == null ? 0 : _entity.hashCode());
  }
}
