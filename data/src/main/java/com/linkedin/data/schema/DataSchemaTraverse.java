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

package com.linkedin.data.schema;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;


/**
 * A {@link DataSchemaTraverse} traverses a {@link com.linkedin.data.schema.DataSchema} and its
 * descendants and invokes a callback method for each unique {@link com.linkedin.data.schema.DataSchema}
 * visited.
 * <p>
 * @author slim
 */
public class DataSchemaTraverse
{
  public static enum Order
  {
    PRE_ORDER,
    POST_ORDER
  }

  public static interface Callback
  {
    void callback(List<String> path, DataSchema schema);
  }

  private final IdentityHashMap<DataSchema, Boolean> _seen = new IdentityHashMap<DataSchema, Boolean>();
  private final ArrayList<String> _path = new ArrayList<String>();
  private final Order _order;
  private Callback _callback;

  public DataSchemaTraverse()
  {
    this(Order.PRE_ORDER);
  }

  public DataSchemaTraverse(Order order)
  {
    _order = order;
  }

  public void traverse(DataSchema schema, Callback callback)
  {
    _seen.clear();
    _path.clear();
    _callback = callback;
    _seen.put(schema, Boolean.TRUE);
    traverseRecurse(schema);
    assert(_path.isEmpty());
  }

  private void traverseRecurse(DataSchema schema)
  {
    if (schema instanceof NamedDataSchema)
    {
      _path.add(((NamedDataSchema) schema).getFullName());
    }
    else
    {
      _path.add(schema.getUnionMemberKey());
    }

    if (_order == Order.PRE_ORDER)
    {
      _callback.callback(_path, schema);
    }

    switch (schema.getType())
    {
      case TYPEREF:
        TyperefDataSchema typerefDataSchema = (TyperefDataSchema) schema;
        traverseChild(DataSchemaConstants.REF_KEY, typerefDataSchema.getRef());
        break;
      case MAP:
        MapDataSchema mapDataSchema = (MapDataSchema) schema;
        traverseChild(DataSchemaConstants.KEYS_KEY, mapDataSchema.getKeys());
        traverseChild(DataSchemaConstants.VALUES_KEY, mapDataSchema.getValues());
        break;
      case ARRAY:
        ArrayDataSchema arrayDataSchema = (ArrayDataSchema) schema;
        traverseChild(DataSchemaConstants.ITEMS_KEY, arrayDataSchema.getItems());
        break;
      case RECORD:
        RecordDataSchema recordDataSchema = (RecordDataSchema) schema;
        for (RecordDataSchema.Field field : recordDataSchema.getFields())
        {
          traverseChild(field.getName(), field.getType());
        }
        break;
      case UNION:
        UnionDataSchema unionDataSchema = (UnionDataSchema) schema;
        for (DataSchema memberType : unionDataSchema.getTypes())
        {
          traverseChild(memberType.getUnionMemberKey(), memberType);
        }
        break;
      case FIXED:
        break;
      case ENUM:
        break;
      default:
        assert(schema.isPrimitive());
        break;
    }

    if (_order == Order.POST_ORDER)
    {
      _callback.callback(_path, schema);
    }

    _path.remove(_path.size() - 1);
  }

  private void traverseChild(String childKey, DataSchema childSchema)
  {
    if (! _seen.containsKey(childSchema))
    {
      _seen.put(childSchema, Boolean.TRUE);
      _path.add(childKey);
      traverseRecurse(childSchema);
      _path.remove(_path.size() - 1);
    }
  }
}
