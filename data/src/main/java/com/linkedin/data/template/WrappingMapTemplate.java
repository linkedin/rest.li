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
import com.linkedin.data.DataMapBuilder;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.template.DataObjectToObjectCache;
import com.linkedin.util.ArgumentUtil;
import java.lang.reflect.Constructor;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class for maps with value types that require proxying by a {@link DataTemplate}.
 *
 * @param <V> is the element type of the array.
 */
public abstract class WrappingMapTemplate<V extends DataTemplate<?>> extends AbstractMapTemplate<V>
{
  /**
   * Constructor.
   *
   * @param map is the underlying {@link DataMap} that will be proxied by this {@link WrappingMapTemplate}.
   * @param schema is the {@link DataSchema} of the map.
   * @param valueClass is the class of elements returned by this {@link WrappingMapTemplate}.
   */
  protected WrappingMapTemplate(DataMap map, MapDataSchema schema, Class<V> valueClass)
      throws TemplateOutputCastException
  {
    super(map, schema, valueClass, DataTemplateUtil.getDataClass(schema.getValues()));
    _cache = new DataObjectToObjectCache<>(DataMapBuilder.getOptimumHashMapCapacityFromSize(map.size()));
    _entrySet = new EntrySet();
  }

  /**
   * Whether this map contains the provided value.
   *
   * @param value provides the value to check.
   * @return true if the provided value is a {@link DataTemplate} and
   *              the underlying {@link DataMap} contains the underlying
   *              Data object of the value {@link DataTemplate}.
   */
  @Override
  public boolean containsValue(Object value)
  {
    if (value == null || value instanceof DataTemplate == false)
    {
      return false;
    }
    DataTemplate<?> template = (DataTemplate<?>) value;
    return _map.containsValue(template.data());
  }

  @Override
  public Set<Entry<String,V>> entrySet()
  {
    return _entrySet;
  }

  @Override
  public V get(Object key) throws TemplateOutputCastException
  {
    if (key != null && key.getClass() == String.class)
    {
      return cacheLookup(_map.get(key), (String) key);
    }
    else
    {
      return null;
    }
  }

  @Override
  public V put(String key, V value) throws ClassCastException, TemplateOutputCastException
  {
    Object found = CheckedUtil.putWithoutChecking(_map, key, unwrap(value));
    return cacheLookup(found, null);
  }

  @Override
  public V remove(Object key) throws TemplateOutputCastException
  {
    return cacheLookup(_map.remove(key), null);
  }

  @Override
  public WrappingMapTemplate<V> clone() throws CloneNotSupportedException
  {
    WrappingMapTemplate<V> clone = (WrappingMapTemplate<V>) super.clone();
    clone.initializeClone();
    return clone;
  }

  private void initializeClone() throws CloneNotSupportedException
  {
    _cache = _cache.clone();
    _entrySet = new EntrySet();
  }

  @Override
  public WrappingMapTemplate<V> copy() throws CloneNotSupportedException
  {
    WrappingMapTemplate<V> copy = (WrappingMapTemplate<V>) super.copy();
    copy.initializeCopy();
    return copy;
  }

  private void initializeCopy()
  {
    _cache = new DataObjectToObjectCache<V>(data().size());
    _entrySet = new EntrySet();
  }

  @SuppressWarnings("unchecked")
  protected Object unwrap(Object value) throws ClassCastException
  {
    ArgumentUtil.notNull(value, "value");
    if (value.getClass() == _valueClass)
    {
      return ((V) value).data();
    }
    else
    {
      throw new ClassCastException("Input " + value + " should be a " + _valueClass.getName());
    }
  }

  /**
   * Lookup the {@link DataTemplate} for a Data object, if not cached,
   * create a {@link DataTemplate} for the Data object and add it to the cache.
   *
   * @param value is the Data object.
   * @param key of the Data object in the underlying {@link DataMap},
   *        if key is null, then the Data object is being removed
   *        from the underlying {@link DataMap}.
   * @return the {@link DataTemplate} that proxies the Data object.
   * @throws TemplateOutputCastException if the value cannot be wrapped.
   */
  protected V cacheLookup(Object value, String key) throws TemplateOutputCastException
  {
    V wrapped;
    if (value == null)
    {
      wrapped = null;
    }
    else if ((wrapped = _cache.get(value)) == null || wrapped.data() != value)
    {
      wrapped = coerceOutput(value);
      if (key != null)
      {
        _cache.put(value, wrapped);
      }
    }
    return wrapped;
  }

  protected V coerceOutput(Object value) throws TemplateOutputCastException
  {
    if (_constructor == null)
    {
      _constructor = DataTemplateUtil.templateConstructor(valueClass(), schema().getValues());
    }

    return DataTemplateUtil.wrap(value, _constructor);
  }

  protected class EntrySet extends AbstractMapTemplate<V>.AbstractEntrySet
  {
    protected EntrySet()
    {
      super();
    }

    @Override
    public boolean add(Map.Entry<String, V> entry) throws ClassCastException
    {
      V value = entry.getValue();
      Object unwrapped = unwrap(value);
      return _map.entrySet().add(new AbstractMap.SimpleEntry<>(entry.getKey(), unwrapped));
    }

    @Override
    public boolean contains(Object entry)
    {
      Map.Entry<String, Object> unwrappedEntry = unwrapEntry(entry);
      if (unwrappedEntry != null)
      {
        return _map.entrySet().contains(unwrappedEntry);
      }
      return false;
    }

    @Override
    public Iterator<Map.Entry<String, V>> iterator()
    {
      final Iterator<Map.Entry<String, Object>> it = _map.entrySet().iterator();

      return new Iterator<Map.Entry<String, V>>()
      {
        @Override
        public boolean hasNext()
        {
          return it.hasNext();
        }

        @Override
        public Map.Entry<String, V> next() throws TemplateOutputCastException
        {
          Map.Entry<String, Object> next = it.next();
          return wrapEntry(next);
        }

        @Override
        public void remove()
        {
          it.remove();
        }
      };
    }

    @Override
    public boolean remove(Object entry)
    {
      Map.Entry<String, Object> unwrappedEntry = unwrapEntry(entry);
      return _map.entrySet().remove(unwrappedEntry);
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
      return _map.entrySet().removeAll(unwrapCollection(c));
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
      return _map.entrySet().retainAll(unwrapCollection(c));
    }

    protected Map.Entry<String, Object> unwrapEntry(Object entry)
    {
      if (entry instanceof Map.Entry)
      {
        Map.Entry<?, ?> mapEntry = (Map.Entry<?, ?>) entry;
        Object key = mapEntry.getKey();
        if (key != null && key.getClass() == String.class)
        {
          Object value = mapEntry.getValue();
          if (value == null || value.getClass() != _valueClass)
          {
            return null;
          }
          Object unwrapped = (_valueClass.cast(value)).data();
          assert(unwrapped != value);
          return new AbstractMap.SimpleImmutableEntry<String, Object>((String) key, unwrapped);
        }
      }
      return null;
    }

    protected Collection<Map.Entry<String, Object>> unwrapCollection(Collection<?> c)
    {
      ArrayList<Map.Entry<String, Object>> unwrappedList = new ArrayList<Map.Entry<String, Object>>(c.size());
      for (Object entry : c)
      {
        Map.Entry<String, Object> unwrappedEntry = unwrapEntry(entry);
        if (unwrappedEntry != null)
        {
          unwrappedList.add(unwrappedEntry);
        }
      }
      return unwrappedList;
    }

    protected Map.Entry<String, V> wrapEntry(final Map.Entry<String, Object> entry) throws TemplateOutputCastException
    {
      return new WrappingMapEntry<V>(entry)
      {
        @Override
        public V getValue() throws TemplateOutputCastException
        {
          if (_value == null)
          {
            _value = cacheLookup(entry.getValue(), getKey());
          }
          return _value;
        }

        @Override
        public V setValue(V value) throws ClassCastException, TemplateOutputCastException
        {
          Object ret =_entry.setValue(unwrap(value));
          _value = null;
          return cacheLookup(ret, null);
        }
      };
    }

  }

  private Constructor<V> _constructor;
  protected EntrySet _entrySet;
  protected DataObjectToObjectCache<V> _cache;
}
