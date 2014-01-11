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

package com.linkedin.restli.common;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.WrappingArrayTemplate;

import java.util.Arrays;
import java.util.List;


/**
 * A Collection of records. Used for returning an ordered, variable-length, navigable collection of resources.
 */
public class CollectionResponse<T extends RecordTemplate> extends RecordTemplate
{
  public static final String ELEMENTS = "elements";
  public static final String METADATA = "metadata";
  public static final String PAGING = "paging";
  private static final RecordDataSchema.Field PAGING_FIELD = new RecordDataSchema.Field(CollectionMetadata.SCHEMA);
  static
  {
    PAGING_FIELD.setName(PAGING, new StringBuilder(10));
  }

  private final Class<T> _elementClass;
  private final ArrayDataSchema _arraySchema;
  private final RecordDataSchema.Field _arrayField;
  private final RecordDataSchema _schema;
  private static final Name _COLLECTION_RESPONSE_NAME = new Name(CollectionResponse.class.getSimpleName());


  private static class DynamicRecordArray<R extends RecordTemplate> extends WrappingArrayTemplate<R>
  {
    @SuppressWarnings({"PublicConstructorInNonPublicClass"})
    public DynamicRecordArray(DataList list, ArrayDataSchema arraySchema, Class<R> elementClass)
    {
      super(list, arraySchema, elementClass);
    }
  }


  /**
   * Initialize a CollectionResponse based on the type of elements it returns.
   *
   * @param elementClass the class of the elements returned
   */
  public CollectionResponse(Class<T> elementClass)
  {
    this(new DataMap(), elementClass);
  }

  /**
   * Initialize a CollectionResponse based on the given dataMap and the
   * elements it returns.
   *
   * @param data a DataMap
   * @param elementClass the class of the elements returned
   */
  public CollectionResponse(DataMap data, Class<T> elementClass)
  {
    super(data, null);
    _elementClass = elementClass;
    StringBuilder errorMessageBuilder = new StringBuilder(10);
    Name elementSchemaName = new Name(elementClass.getSimpleName());
    _arraySchema = new ArrayDataSchema(new RecordDataSchema(elementSchemaName, RecordDataSchema.RecordType.RECORD));
    _arrayField = new RecordDataSchema.Field(_arraySchema);
    _arrayField.setName(ELEMENTS, errorMessageBuilder);

    if (data().get(ELEMENTS) == null)
    {
      data().put(ELEMENTS, new DataList());
    }

    _schema = new RecordDataSchema(_COLLECTION_RESPONSE_NAME, RecordDataSchema.RecordType.RECORD);
    _schema.setFields(Arrays.asList(_arrayField, PAGING_FIELD), errorMessageBuilder);

  }

  @Override
  public RecordDataSchema schema()
  {
    return _schema;
  }

  public List<T> getElements()
  {
    DataList value = (DataList) data().get(ELEMENTS);

    return new DynamicRecordArray<T>(value, _arraySchema, _elementClass);
  }

  public boolean hasPaging()
  {
    return contains(PAGING_FIELD);
  }

  public void removePaging()
  {
    remove(PAGING_FIELD);
  }

  public CollectionMetadata getPaging()
  {
    return obtainWrapped(PAGING_FIELD, CollectionMetadata.class, GetMode.STRICT);
  }

  public void setPaging(CollectionMetadata value)
  {
    putWrapped(PAGING_FIELD, CollectionMetadata.class, value);
  }

  public void setMetadataRaw(DataMap metadata)
  {
    if (metadata != null)
    {
      data().put(METADATA, metadata);
    }
  }

  public DataMap getMetadataRaw()
  {
    return (DataMap)data().get(METADATA);
  }

}
