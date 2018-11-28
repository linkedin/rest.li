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
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.internal.common.BatchFinderCriteriaResultDecoder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A Collection of records. Used for returning an ordered, variable-length, navigable collection of resources for BATCH_FINDER.
 * Instead of using the existing {@link CollectionResponse}, this class will provide more flexibility for future feature
 * enhancement specific for BATCH_FINDER.
 */
public class BatchCollectionResponse<T extends RecordTemplate> extends RecordTemplate
{

  private List<BatchFinderCriteriaResult<T>> _collection;

  /**
   * Initialize a BatchCollectionResponse based on the given dataMap.
   *
   * @param data a DataMap
   * @param entityDecoder a decoder that decodes each individual BatchFinderItemStatus response
   */
  public BatchCollectionResponse(DataMap data, BatchFinderCriteriaResultDecoder<T> entityDecoder)
  {
    super(data, generateSchema());
    if (data().get("elements") == null)
    {
      data().put("elements", new DataList());
    }

    if (entityDecoder != null) {
      _collection = createCollectionFromDecoder(entityDecoder);
    }
  }

  private static RecordDataSchema generateSchema()
  {
    StringBuilder errorMessageBuilder = new StringBuilder(10);
    ArrayDataSchema arraySchema = new ArrayDataSchema(new RecordDataSchema(new Name(BatchFinderCriteriaResult.class.getSimpleName()), RecordDataSchema.RecordType.RECORD));
    RecordDataSchema.Field arrayField = new RecordDataSchema.Field(arraySchema);
    arrayField.setName(CollectionResponse.ELEMENTS, errorMessageBuilder);
    RecordDataSchema schema = new RecordDataSchema(new Name(BatchCollectionResponse.class.getSimpleName()), RecordDataSchema.RecordType.RECORD);

    schema.setFields(Arrays.asList(arrayField), errorMessageBuilder);
    return schema;
  }

  private List<BatchFinderCriteriaResult<T>> createCollectionFromDecoder(BatchFinderCriteriaResultDecoder<T> decoder)
  {
    DataList elements = this.data().getDataList(CollectionResponse.ELEMENTS);
    List<BatchFinderCriteriaResult<T>> collection = elements.stream().map(obj -> decoder.makeValue((DataMap) obj)).collect(Collectors.toList());

    return collection;
  }

  public List<BatchFinderCriteriaResult<T>> getResults()
  {
    return _collection;
  }
}
