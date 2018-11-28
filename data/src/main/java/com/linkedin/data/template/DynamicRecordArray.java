package com.linkedin.data.template;

import com.linkedin.data.DataList;
import com.linkedin.data.schema.ArrayDataSchema;

/**
 * Class for array of value types that require proxying by a {@link RecordTemplate}.
 *
 * @param <E> is the element type of the array.
 */
public class DynamicRecordArray<E extends RecordTemplate> extends WrappingArrayTemplate<E>
{
  public DynamicRecordArray(DataList list, ArrayDataSchema arraySchema, Class<E> elementClass)
  {
    super(list, arraySchema, elementClass);
  }
}