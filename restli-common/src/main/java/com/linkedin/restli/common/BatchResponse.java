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

import java.util.Arrays;
import java.util.Map;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.WrappingMapTemplate;


/**
 * A Batch of records. Used to return a fixed-size, unordered, complete collection of records, keyed on resource ID. Used
 * as a response for a get_batch request.
 */
public class BatchResponse<T extends RecordTemplate> extends RecordTemplate
{
  public static final String RESULTS = "results";
  public static final String ERRORS = "errors";

  private final Class<T> _valueClass;
  private final MapDataSchema _resultsSchema;
  private final RecordDataSchema.Field _resultsField;

  private final MapDataSchema _errorsSchema;
  private final RecordDataSchema.Field _errorsField;

  private final RecordDataSchema _schema;
  private static final Name _BATCH_RESPONSE_NAME = new Name(BatchResponse.class.getSimpleName());

  private static class DynamicRecordMap<R extends RecordTemplate> extends WrappingMapTemplate<R>
  {
    public DynamicRecordMap(DataMap map, MapDataSchema mapSchema, Class<R> valueClass)
    {
      super(map, mapSchema, valueClass);
    }
  }

  /**
   *Initialize a BatchResponse with the given data and valueClass.
   *
   * @param data the class that the BatchResponse contains
   *             (a RecordTemplate, a CreateStatus, etc.)
   * @param valueClass the class that the BatchResponse contains
   */
  public BatchResponse(DataMap data, Class<T> valueClass)
  {
    this(data, valueClass, 0, 0);
  }

  /**
   * Initialize a BatchResponse with the given valueClass and capacities.
   *
   * @param valueClass the class that the BatchResponse contains
   *                   (a RecordTemplate, a CreateStatus, etc.)
   * @param resultsCapacity initial capacity for results
   * @param errorsCapacity initial capacity for errors
   */
  public BatchResponse(Class<T> valueClass, int resultsCapacity, int errorsCapacity)
  {
    this(new DataMap(), valueClass, resultsCapacity, errorsCapacity);
  }


  private BatchResponse(DataMap data, Class<T> valueClass, int resultsCapacity, int errorsCapacity)
  {
    super(data, null);
    _valueClass = valueClass;
    StringBuilder errorMessageBuilder = new StringBuilder(10);
    Name elementSchemaName = new Name(valueClass.getSimpleName());
    _resultsSchema = new MapDataSchema(new RecordDataSchema(elementSchemaName, RecordDataSchema.RecordType.RECORD));
    _resultsField = new RecordDataSchema.Field(_resultsSchema);
    _resultsField.setName(RESULTS, errorMessageBuilder);

    Name errorSchemaName = new Name(ErrorResponse.class.getSimpleName());
    _errorsSchema = new MapDataSchema(new RecordDataSchema(errorSchemaName, RecordDataSchema.RecordType.RECORD));
    _errorsField = new RecordDataSchema.Field(_errorsSchema);
    _errorsField.setName(ERRORS, errorMessageBuilder);
    
    if (data().get(RESULTS) == null)
    {
      data().put(RESULTS, new DataMap(resultsCapacity));
    }

    if (data().get(ERRORS) == null)
    {
      data().put(ERRORS, new DataMap(errorsCapacity));
    }

    _schema = new RecordDataSchema(_BATCH_RESPONSE_NAME, RecordDataSchema.RecordType.RECORD);
    _schema.setFields(Arrays.asList(_resultsField, _errorsField), errorMessageBuilder);

  }

  @Override
  public RecordDataSchema schema()
  {
    return _schema;
  }

  /**
   * @return a Map of String keys to RecordTemplate results
   */
  public Map<String, T> getResults()
  {
    DataMap value = (DataMap) data().get(RESULTS);

    return new DynamicRecordMap<T>(value, _resultsSchema, _valueClass);
  }

  /**
   * @return a Map of String keys to ErrorResponses
   */
  public Map<String, ErrorResponse> getErrors()
  {
    DataMap value = (DataMap) data().get(ERRORS);

    return new DynamicRecordMap<ErrorResponse>(value, _errorsSchema, ErrorResponse.class);
  }
}
