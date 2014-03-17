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

package com.linkedin.restli.server;


import com.linkedin.data.template.RecordTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author nshankar
 *
 */
public class RestLiRequestDataImpl implements RestLiRequestData
{
  private Object _key;
  private RecordTemplate _entity;
  private final List<?> _keys;
  private final List<? extends RecordTemplate> _entities;
  private final Map<?, ? extends RecordTemplate> _keyEntityMap;

  @Override
  public Object getKey()
  {
    return _key;
  }

  @Override
  public void setKey(Object key)
  {
    _key = key;
  }

  @Override
  public List<?> getBatchKeys()
  {
    return _keys;
  }

  @Override
  public RecordTemplate getEntity()
  {
    return _entity;
  }

  @Override
  public void setEntity(RecordTemplate entity)
  {
    _entity = entity;
  }

  @Override
  public List<? extends RecordTemplate> getBatchEntities()
  {
    return _entities;
  }

  @Override
  public Map<?, ? extends RecordTemplate> getBatchKeyEntityMap()
  {
    return _keyEntityMap;
  }

  @Override
  public boolean hasKey()
  {
    return _key != null;
  }

  @Override
  public boolean hasBatchKeys()
  {
    return !_keys.isEmpty();
  }

  @Override
  public boolean hasEntity()
  {
    return _entity != null;
  }

  @Override
  public boolean hasBatchEntities()
  {
    return !_entities.isEmpty();

  }

  @Override
  public boolean hasBatchKeyEntityMap()
  {
    return !_keyEntityMap.isEmpty();
  }

  @Override
  public boolean isBatchRequest()
  {
    return hasBatchEntities() || hasBatchKeys() || hasBatchKeyEntityMap();
  }

  private RestLiRequestDataImpl(Object key,
                                RecordTemplate entity,
                                List<?> keys,
                                List<? extends RecordTemplate> entities,
                                Map<?, ? extends RecordTemplate> keyEntityMap)
  {
    _key = key;
    _entity = entity;
    _keys = keys;
    _entities = entities;
    _keyEntityMap = keyEntityMap;
  }

  public static class Builder
  {
    private Object _key;
    private RecordTemplate _entity;
    private List<?> _keys;
    private List<? extends RecordTemplate> _entities;
    private Map<?, ? extends RecordTemplate> _keyEntityMap;

    public Builder()
    {
      _keys = new ArrayList<Object>();
      _entities = new ArrayList<RecordTemplate>();
      _keyEntityMap = new HashMap<Object, RecordTemplate>();
    }

    public Builder key(Object key)
    {
      _key = key;
      return this;
    }

    public Builder entity(RecordTemplate entity)
    {
      _entity = entity;
      return this;
    }

    public Builder batchKeys(Collection<?> keys)
    {
      _keys = new ArrayList<Object>(keys);
      return this;
    }

    public Builder batchEntities(Collection<? extends RecordTemplate> entities)
    {
      _entities = new ArrayList<RecordTemplate>(entities);
      return this;
    }

    public Builder batchKeyEntityMap(Map<?, ? extends RecordTemplate> map)
    {
      _keyEntityMap = new HashMap<Object, RecordTemplate>(map);
      return this;
    }

    public RestLiRequestData build()
    {
      return new RestLiRequestDataImpl(_key, _entity, _keys, _entities, _keyEntityMap);
    }
  }
}
