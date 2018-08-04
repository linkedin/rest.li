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

package com.linkedin.data.it;


import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.element.MutableDataElement;
import com.linkedin.data.element.SimpleDataElement;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;


/**
 * Iterates through an acyclic Data object graph in pre-order or post-order.
 * <p>
 *
 * The entire Data object graph reachable from the specified start Data object will be
 * iterated. If the iteration order is pre-order,the start Data object
 * will be the first value returned by the iterator. If the iteration order
 * is post-order, the start Data object will be the last value returned by the
 * iterator.
 * <p>
 *
 * @author slim
 */
public class ObjectIterator implements DataIterator
{
  /**
   * Constructor.
   *
   * @param data is the start Data object that is the starting point for
   *        to traversing the object graph.
   * @param schema provides the {@link DataSchema} of the Data object,
   *        it may be {@code null} if the DataSchema is unknown or not
   *        available. If no {@link DataSchema} is provided, then all
   *        {@link DataElement}'s returned by this {@link DataIterator}
   *        return {@code null} when their {@link DataElement#getSchema}
   *        method is invoked.
   * @param order provides whether the object graph will be traversed
   *        in pre or post-order.
   */
  public ObjectIterator(Object data, DataSchema schema, IterationOrder order)
  {
    this(new SimpleDataElement(data, schema), order);
  }

  /**
   * Constructor.
   *
   * @param element that provides the start Data object and {@link DataSchema}.
   * @param order provides whether the object graph will be traversed
   *        in pre or post-order.
   */
  public ObjectIterator(DataElement element, IterationOrder order)
  {
    _startElement = element;
    _current = null;
    _first = true;
    _preOrder = order == IterationOrder.PRE_ORDER;
  }


  /**
   * Constructor.
   *
   * @param data is the start Data object that is the starting point for
   *        to traversing the object graph.
   * @param schema provides the {@link DataSchema} of the Data object,
   *        it may be {@code null} if the DataSchema is unknown or not
   *        available. If no {@link DataSchema} is provided, then all
   *        {@link DataElement}'s returned by this {@link DataIterator}
   *        return {@code null} when their {@link DataElement#getSchema}
   *        method is invoked.
   */
  private ObjectIterator(Object data, DataSchema schema)
  {
    this(data, schema, IterationOrder.PRE_ORDER);
  }

  @Override
  public DataElement next()
  {
    return (_preOrder ? preOrderNext() : postOrderNext());
  }

  private DataElement postOrderNext()
  {
    if (_first)
    {
      _first = false;
      _current = _startElement;
      while (_current != null && _current.getValue() instanceof DataComplex)
      {
        push();
        _current = _stack.getLast().next();
      }
      if (_current == null)
      {
        _current = _stack.removeLast()._element;
      }
      return _current;
    }

    _current = null;
    while (_stack.isEmpty() == false)
    {
      _current = _stack.getLast().next();
      if (_current == null)
      {
        _current = _stack.removeLast()._element;
        break;
      }
      else if (_current.getValue() instanceof DataComplex)
      {
        push();
      }
      else
      {
        // _current not null and not a DataComplex
        break;
      }
    }
    return _current;
  }

  private DataElement preOrderNext()
  {
    if (_first)
    {
      _first = false;
      _current = _startElement;
      return _current;
    }
    if (_current != null && _current.getValue() instanceof DataComplex)
    {
      push();
    }
    _current = null;
    while (_stack.isEmpty() == false)
    {
      _current = _stack.getLast().next();
      if (_current == null)
      {
        _stack.removeLast();
      }
      else
      {
        // _current not null;
        break;
      }
    }
    return _current;
  }

  @Override
  public void skipToSibling()
  {
    if (_preOrder)
    {
      _current = null;
    }
    else
    {
      // no-op for post-order
    }
  }

  private void push()
  {
    Class<?> clazz = _current.getValue().getClass();
    if (clazz == DataMap.class)
    {
      _stack.addLast(new MapState(_current));
    }
    else if (clazz == DataList.class)
    {
      _stack.addLast(new ListState(_current));
    }
  }

  private abstract class State
  {
    protected State(DataElement element)
    {
      _element = element;
    }

    protected abstract DataElement next();

    protected final DataElement _element;
  }

  private class MapState extends State
  {
    private MapState(DataElement element)
    {
      super(element);
      _map = (DataMap) element.getValue();
      _it = _map.entrySet().iterator();
      _currentEntry = null;
      _childElement = new MutableDataElement(element);
    }

    @Override
    protected DataElement next()
    {
      DataElement element;
      if (_it.hasNext())
      {
        _currentEntry = _it.next();
        _childElement.setValueNameSchema(_currentEntry.getValue(), _currentEntry.getKey(), currentSchema());
        element = _childElement;
      }
      else
      {
        element = null;
      }
      return element;
    }

    private DataSchema currentSchema()
    {
      DataSchema schema;
      DataSchema mapSchema = _element.getSchema();
      if (mapSchema == null)
      {
        schema = null;
      }
      else
      {
        DataSchema dereferencedSchema = mapSchema.getDereferencedDataSchema();
        DataSchema.Type deferencedType = dereferencedSchema.getType();
        switch (deferencedType)
        {
          case RECORD:
            RecordDataSchema.Field field = ((RecordDataSchema) dereferencedSchema).getField(_currentEntry.getKey());
            schema = (field == null ? null : field.getType());
            break;
          case UNION:
            schema = ((UnionDataSchema) dereferencedSchema).getTypeByMemberKey(_currentEntry.getKey());
            break;
          case MAP:
            schema = ((MapDataSchema) dereferencedSchema).getValues();
            break;
          default:
            throw new IllegalStateException("Unknown dereferenced type " + deferencedType + " for DataMap's schema " + mapSchema);
        }
      }
      return schema;
    }

    private final DataMap _map;
    private final Iterator<Map.Entry<String, Object>> _it;
    private final MutableDataElement _childElement;
    private Map.Entry<String, Object> _currentEntry;
  }

  private class ListState extends State
  {
    private ListState(DataElement element)
    {
      super(element);
      _list = (DataList) element.getValue();
      _it = _list.listIterator();
      _currentIndex = -1;
      _childElement = new MutableDataElement(element);
    }

    @Override
    protected DataElement next()
    {
      DataElement element;
      if (_it.hasNext())
      {
        _currentIndex = _it.nextIndex();
        Object value = _it.next();
        _childElement.setValueNameSchema(value, _currentIndex, currentSchema());
        element = _childElement;
      }
      else
      {
        element = null;
      }
      return element;
    }

    private DataSchema currentSchema()
    {
      DataSchema schema = null;

      DataSchema listSchema = _element.getSchema();
      if (listSchema != null)
      {
        DataSchema dereferencedListSchema = listSchema.getDereferencedDataSchema();

        if (dereferencedListSchema.getType() == DataSchema.Type.ARRAY)
        {
          schema = ((ArrayDataSchema) dereferencedListSchema).getItems();
        }
      }

      return schema;
    }

    private final DataList _list;
    private final ListIterator<Object> _it;
    private final MutableDataElement _childElement;
    private int _currentIndex;
  }

  private final DataElement _startElement;
  private final Deque<State> _stack = new ArrayDeque<State>();

  private boolean _first = true;
  private DataElement _current = null;
  private boolean _preOrder = true;
}
