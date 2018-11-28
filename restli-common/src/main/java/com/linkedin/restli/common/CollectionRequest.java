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

/**
 * $Id: $
 */

package com.linkedin.restli.common;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DynamicRecordArray;
import com.linkedin.data.template.RecordTemplate;

import java.util.Arrays;
import java.util.List;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class CollectionRequest<T extends RecordTemplate> extends RecordTemplate
{
  public static final String ELEMENTS = "elements";

  private final Class<T> _elementClass;
  private final ArrayDataSchema _arraySchema;
  private final RecordDataSchema.Field _arrayField;
  private final RecordDataSchema _schema;
  private static final Name _COLLECTION_REQUEST_NAME = new Name(CollectionRequest.class.getSimpleName());
  private DynamicRecordArray<T> _templatedCollection;


  /**
   * Initialize a CollectionRequest based on the given elementClass.
   *
   * @param elementClass the class of items that will be returned when this
   *                     request is fulfilled
   */
  public CollectionRequest(Class<T> elementClass)
  {
    this(new DataMap(), elementClass);
  }

  /**
   * Initialize a CollectionRequest based on the given data and elementClass.
   *
   * @param data a DataMap
   * @param elementClass the class of items that will be returned when this
   *                     request is fulfilled
   */
  public CollectionRequest(DataMap data, Class<T> elementClass)
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

    _schema = new RecordDataSchema(_COLLECTION_REQUEST_NAME, RecordDataSchema.RecordType.RECORD);
    _schema.setFields(Arrays.asList(_arrayField), errorMessageBuilder);

  }

  @Override
  public RecordDataSchema schema()
  {
    return _schema;
  }

  /**
   * Return a mutable list of elements that can be added to as the collection.
   * request is fulfilled.
   *
   * @return mutable List of elements
   */
  public List<T> getElements()
  {
    if (_templatedCollection == null)
    {
      DataList value = (DataList) data().get(ELEMENTS);
      _templatedCollection = new DynamicRecordArray<T>(value, _arraySchema, _elementClass);
    }

    return _templatedCollection;
  }
}
