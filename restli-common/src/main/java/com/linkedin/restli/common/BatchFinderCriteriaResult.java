/*
   Copyright (c) 2018 LinkedIn Corp.

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
import com.linkedin.data.schema.BooleanDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordArray;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;

import java.util.Arrays;
import java.util.List;


/**
 * BatchFinderCriteriaResult keeps track of the result for each batch find criteria.
 * On success, the result contains a list of resource entities and related metadata, paging information.
 * On error, it contains an error response and the list of entities is null.
 * The isError flag is required field to indicate whether the result is successful or not.
 *
 * @author Jiaqi Guan
 */
public class BatchFinderCriteriaResult<T extends RecordTemplate> extends RecordTemplate
{
  public static final String ELEMENTS = "elements";
  public static final String METADATA = "metadata";
  public static final String PAGING = "paging";
  public static final String ERROR = "error";
  public static final String ISERROR = "isError";


  private final Class<T> _elementClass;
  private final ArrayDataSchema _arraySchema;
  private final RecordDataSchema.Field _arrayField;
  private final RecordDataSchema.Field _errorField;
  private final RecordDataSchema.Field _isErrorField;
  private final RecordDataSchema.Field _pagingField;
  private final RecordDataSchema _schema;
  private static final Name _BATCH_FINDER_CRITERIA_RESULT_NAME = new Name(BatchFinderCriteriaResult.class.getSimpleName());


  /**
   * Initialize a BatchFinderCriteriaResult based on the type of elements it returns.
   *
   * @param elementClass the class of the elements returned
   */
  public BatchFinderCriteriaResult(Class<T> elementClass)
  {
    this(new DataMap(), elementClass);
  }

  /**
   * Initialize a BatchFinderCriteriaResult based on the given dataMap and the
   * elements it returns.
   *
   * @param data the underlying DataMap of the BatchFinderCriteriaResult response.
   * @param elementClass the class of items that will be returned when this request is fulfilled
   */
  public BatchFinderCriteriaResult(DataMap data, Class<T> elementClass)
  {
    super(data, null);
    _elementClass = elementClass;
    StringBuilder errorMessageBuilder = new StringBuilder(10);

    //is error flag
    _isErrorField = new RecordDataSchema.Field(new BooleanDataSchema());
    _isErrorField.setDefault(false);
    _isErrorField.setName(ISERROR, errorMessageBuilder);

    // elements
    Name elementSchemaName = new Name(elementClass.getSimpleName());
    _arraySchema = new ArrayDataSchema(new RecordDataSchema(elementSchemaName, RecordDataSchema.RecordType.RECORD));
    _arrayField = new RecordDataSchema.Field(_arraySchema);
    _arrayField.setName(ELEMENTS, errorMessageBuilder);
    _arrayField.setOptional(true);

    //paging
    _pagingField = new RecordDataSchema.Field(DataTemplateUtil.getSchema(CollectionMetadata.class));
    _pagingField.setName(PAGING, errorMessageBuilder);
    _pagingField.setOptional(true);

    // error
    _errorField = new RecordDataSchema.Field(DataTemplateUtil.getSchema(ErrorResponse.class));
    _errorField.setName(ERROR, errorMessageBuilder);
    _errorField.setOptional(true);


    if (data().get(ELEMENTS) == null)
    {
      data().put(ELEMENTS, new DataList());
    }

    _schema = new RecordDataSchema(_BATCH_FINDER_CRITERIA_RESULT_NAME, RecordDataSchema.RecordType.RECORD);
    _schema.setFields(Arrays.asList(_isErrorField, _arrayField, _pagingField, _errorField), errorMessageBuilder);
  }

  @Override
  public RecordDataSchema schema()
  {
    return _schema;
  }

  /**
   * @return the results for an individual criteria in the batch find request in case of success.
   */
  public List<T> getElements()
  {
    DataList value = (DataList) data().get(ELEMENTS);
    return new DynamicRecordArray<T>(value, _arraySchema, _elementClass);
  }

  /**
   * Set up the elements if this result is a success case.
   */
  public void setElements(CollectionResponse<T> collectionResponse) {
    if (collectionResponse != null)
    {
      data().put(ELEMENTS, collectionResponse.data().get(CollectionResponse.ELEMENTS));
    }
  }

  public boolean hasPaging()
  {
    return contains(_pagingField);
  }

  public void removePaging()
  {
    remove(_pagingField);
  }

  public CollectionMetadata getPaging()
  {
    return obtainWrapped(_pagingField, CollectionMetadata.class, GetMode.STRICT);
  }

  public void setPaging(CollectionMetadata value)
  {
    putWrapped(_pagingField, CollectionMetadata.class, value);
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

  /**
   * @return the error returned by the server in case of failure.
   */
  public ErrorResponse getError() {
    return obtainWrapped(_errorField, ErrorResponse.class, GetMode.STRICT);
  }

  /**
   * Set up error response field, if this result is a error case.
   */
  public void setError(ErrorResponse errorResponse) {
    putWrapped(_errorField, ErrorResponse.class, errorResponse);
  }

  /**
   * Determines if the entry is a failure.
   *
   * @return true if the entry contains an exception, false otherwise.
   */
  public boolean isError()
  {
    final Boolean isError = obtainDirect(_isErrorField, Boolean.class, GetMode.STRICT);
    return isError;
  }

  /**
   * Set up a flag to indicate whether the result is a error or success case.
   */
  public void setIsError(boolean isError) {
    putDirect(_isErrorField, Boolean.class, isError);
  }
}
