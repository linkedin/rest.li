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

package com.linkedin.data.template;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Abstract {@link DataTemplate} for maps.
 *
 * @param <V> is the value type of map.
 */
public abstract class AbstractMapTemplate<V> extends AbstractMap<String ,V> implements DataTemplate<DataMap>
{
  /**
   * Constructor.
   *
   * @param map is the underlying {@link DataMap} that will be proxied by this {@link AbstractMapTemplate}.
   * @param schema is the {@link DataSchema} of the map.
   * @param valueClass is the class of elements returned by this {@link AbstractMapTemplate}.
   */
  protected AbstractMapTemplate(DataMap map, MapDataSchema schema, Class<V> valueClass, Class<?> dataClass)
  {
    _map = map;
    _schema = schema;
    _valueClass = valueClass;
    _dataClass = dataClass;
  }

  @Override
  public MapDataSchema schema()
  {
    return _schema;
  }

  @Override
  public DataMap data()
  {
    return _map;
  }

  @Override
  public abstract boolean containsValue(Object value);

  @Override
  public abstract Set<Map.Entry<String, V>> entrySet();

  @Override
  public abstract V get(Object key);

  @Override
  public abstract V put(String key, V value);

  @Override
  public abstract V remove(Object key);

  @Override
  public boolean containsKey(Object key)
  {
    return _map.containsKey(key);
  }

  @Override
  public void clear()
  {
    _map.clear();
  }

  @Override
  public boolean isEmpty()
  {
    return _map.isEmpty();
  }

  @Override
  public Set<String> keySet()
  {
    return _map.keySet();
  }

  @Override
  public int size()
  {
    return _map.size();
  }

  @Override
  public boolean equals(Object object)
  {
    if (this == object)
    {
      return true;
    }
    if (object != null && object instanceof AbstractMapTemplate)
    {
      return ((AbstractMapTemplate<?>) object).data().equals(_map);
    }
    return super.equals(object);
  }

  /**
   * Return the underlying {@link DataMap}'s hash code.
   *
   * @return the underlying {@link DataMap}'s hash code.
   */
  @Override
  public int hashCode()
  {
    return _map.hashCode();
  }

  @Override
  public String toString()
  {
    return _map.toString();
  }

  /**
   * Return a clone that is backed by a shallow copy of the underlying {@link DataMap}.
   *
   * @return a clone that is backed by a shallow copy of the underlying {@link DataMap}.
   * @throws CloneNotSupportedException if this object cannot be cloned.
   */
  @Override
  public AbstractMapTemplate<V> clone() throws CloneNotSupportedException
  {
    @SuppressWarnings("unchecked")
    AbstractMapTemplate<V> clone = (AbstractMapTemplate<V>) super.clone();
    clone._map = clone._map.clone();
    return clone;
  }

  @Override
  public AbstractMapTemplate<V> copy() throws CloneNotSupportedException
  {
    @SuppressWarnings("unchecked")
    AbstractMapTemplate<V> copy = (AbstractMapTemplate<V>) super.clone();
    copy._map = copy._map.copy();
    return copy;
  }

  protected Class<V> valueClass()
  {
    return _valueClass;
  }

  /**
   * {@link AbstractSet} to assist in implementing the EntrySet required by the
   * {@link AbstractMap} class.
   */
  protected abstract class AbstractEntrySet extends AbstractSet<Map.Entry<String, V>>
  {
    @Override
    public abstract boolean add(Map.Entry<String, V> entry);

    @Override
    public abstract boolean contains(Object entry);

    @Override
    public abstract Iterator<Map.Entry<String, V>> iterator();

    @Override
    public abstract boolean remove(Object entry);

    @Override
    public abstract boolean removeAll(Collection<?> c);

    @Override
    public abstract boolean retainAll(Collection<?> c);

    @Override
    public void clear()
    {
      _map.entrySet().clear();
    }

    @Override
    public boolean isEmpty()
    {
      return _map.isEmpty();
    }

    @Override
    public int size()
    {
      return _map.size();
    }

    @Override
    public boolean equals(Object object)
    {
      if (object != null && object instanceof AbstractMapTemplate.AbstractEntrySet)
      {
        @SuppressWarnings("unchecked")
        AbstractEntrySet set = (AbstractEntrySet) object;
        return _map.equals(set.dataMap());
      }
      return super.equals(object);
    }

    @Override
    public int hashCode()
    {
      return _map.entrySet().hashCode();
    }

    @Override
    public String toString()
    {
      return _map.entrySet().toString();
    }

    private DataMap dataMap()
    {
      return _map;
    }
  }

  protected abstract static class WrappingMapEntry<V> implements Map.Entry<String, V>
  {
    protected final Map.Entry<String, Object> _entry;
    protected V _value = null;

    public WrappingMapEntry(Map.Entry<String, Object> entry)
    {
      _entry = entry;
    }

    @Override
    public String getKey()
    {
      return _entry.getKey();
    }

    @Override
    public abstract V getValue() throws TemplateOutputCastException;

    @Override
    public abstract V setValue(V value);

    @Override
    public boolean equals(Object o)
    {
      if (o == null || o instanceof Map.Entry == false)
      {
        return false;
      }
      Map.Entry other = (Map.Entry) o;
      return getKey().equals(other.getKey()) && getValue().equals(other.getValue());
    }

    @Override
    public int hashCode()
    {
      return getKey().hashCode() ^ getValue().hashCode();
    }

    @Override
    public String toString()
    {
      StringBuilder builder = new StringBuilder();
      builder.append(getKey());
      builder.append('=');
      builder.append(getValue());
      return builder.toString();
    }
  }

  protected static DataMap newDataMapOfSize(int srcMapSize)
  {
    return new DataMap((int) Math.ceil(srcMapSize / 0.75));
  }

  protected DataMap _map;
  protected final MapDataSchema _schema;
  /**
   * Class of values returned by the map.
   */
  protected final Class<V> _valueClass;
  /**
   * Class of values stored in in the underlying {@link DataMap}.
   */
  protected final Class<?> _dataClass;
}

