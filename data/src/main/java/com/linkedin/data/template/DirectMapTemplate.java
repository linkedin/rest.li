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
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.util.ArgumentUtil;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Abstract class for maps with value types that do not require proxying by a {@link DataTemplate}.
 *
 * @param <V> is the element type of the array.
 */
public abstract class DirectMapTemplate<V> extends AbstractMapTemplate<V>
{
   /**
   * Constructor.
   *
   * This constructor is retained for backwards compatibility.
   *
   * @param map is the underlying {@link DataMap} that will be proxied by this {@link DirectMapTemplate}.
   * @param schema is the {@link DataSchema} of the map.
   * @param valueClass is the class of values returned by this {@link DirectMapTemplate}.
   */
  protected DirectMapTemplate(DataMap map, MapDataSchema schema, Class<V> valueClass)
  {
    this(map, schema, valueClass, valueClass.isEnum() ? String.class : valueClass);
  }

  /**
   * Constructor.
   *
   * @param map is the underlying {@link DataMap} that will be proxied by this {@link DirectMapTemplate}.
   * @param schema is the {@link DataSchema} of the map.
   * @param valueClass is the class of values returned by this {@link DirectMapTemplate}.
   * @param dataClass is the class of values stored in the underlying {@link DataMap}.
   */
  protected DirectMapTemplate(DataMap map, MapDataSchema schema, Class<V> valueClass, Class<?> dataClass)
  {
    super(map, schema, valueClass, dataClass);
    _entrySet = new EntrySet();
  }

  @Override
  public boolean containsValue(Object value)
  {
    if (value != null && _valueClass != _dataClass && value.getClass() == _valueClass)
    {
      try
      {
        @SuppressWarnings("unchecked")
        V v = (V) value;
        value = safeCoerceInput(v);
      }
      catch (ClassCastException exc)
      {
        // do nothing just check map for the input value.
      }
    }
    return _map.containsValue(value);
  }

  @Override
  public Set<Entry<String,V>> entrySet()
  {
    return _entrySet;
  }

  @Override
  public V get(Object key) throws TemplateOutputCastException
  {
    return coerceOutput(_map.get(key));
  }

  @Override
  public V put(String key, V value) throws ClassCastException, TemplateOutputCastException
  {
    return coerceOutput(CheckedUtil.putWithoutChecking(_map, key, safeCoerceInput(value)));
  }

  @Override
  public V remove(Object key) throws TemplateOutputCastException
  {
    return coerceOutput(_map.remove(key));
  }

  protected class EntrySet extends AbstractMapTemplate<V>.AbstractEntrySet
  {
    @SuppressWarnings("unchecked")
    @Override
    public boolean add(Map.Entry<String, V> entry) throws ClassCastException
    {
      safeCoerceInput(entry.getValue());
      return _map.entrySet().add((Map.Entry<String, Object>) entry);
    }

    @Override
    public boolean contains(Object object)
    {
      if (object instanceof Map.Entry == false)
      {
        return false;
      }
      Map.Entry<?,?> entry = (Map.Entry<?, ?>) object;
      Object key = entry.getKey();
      if (key == null || key.getClass() != String.class)
      {
        return false;
      }
      Object value = entry.getValue();
      Class<?> valueClass;
      if (value == null || (valueClass = value.getClass()) != _valueClass)
      {
        return false;
      }
      if (valueClass.isEnum())
      {
        return _map.entrySet().contains(new AbstractMap.SimpleImmutableEntry<String, String>((String) key, value.toString()));
      }
      else
      {
        return _map.entrySet().contains(object);
      }
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

        @SuppressWarnings("unchecked")
        @Override
        public Map.Entry<String, V> next()
        {
          Map.Entry<String, Object> entry = it.next();
          Object value = entry.getValue();
          if (value.getClass() != _valueClass)
          {
            return new WrappingMapEntry<V>(entry)
            {
              @Override
              public V getValue() throws TemplateOutputCastException
              {
                if (_value == null)
                {
                  _value = coerceOutput(_entry.getValue());
                }
                return _value;
              }

              @Override
              public V setValue(V value) throws ClassCastException, TemplateOutputCastException
              {
                V ret = coerceOutput(_entry.setValue(safeCoerceInput(value)));
                _value = null;
                return ret;
              }
            };
          }
          else
          {
            return (Map.Entry<String, V>) entry;
          }
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
      return _map.entrySet().remove(entry);
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
      return _map.entrySet().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
      return _map.entrySet().retainAll(c);
    }
  }

  @SuppressWarnings("unchecked")
  private Object safeCoerceInput(Object object) throws ClassCastException
  {
    //
    // This UGLY hack is needed because we have code that expects some types to be artificially inter-fungible
    // and even tests for it, for example coercing between number types.
    //
    ArgumentUtil.notNull(object, "object");
    if (object.getClass() != _valueClass)
    {
      return DataTemplateUtil.coerceInput((V) object, _valueClass, _dataClass);
    }
    else
    {
      return coerceInput((V) object);
    }
  }

  protected Object coerceInput(V object) throws ClassCastException
  {
    ArgumentUtil.notNull(object, "object");
    return DataTemplateUtil.coerceInput(object, _valueClass, _dataClass);
  }

  protected V coerceOutput(Object object) throws TemplateOutputCastException
  {
    if (object == null)
    {
      return null;
    }
    return DataTemplateUtil.coerceOutput(object, _valueClass);
  }

  protected final EntrySet _entrySet;
}
