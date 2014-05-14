/*
   Copyright (c) 2014 LinkedIn Corp.

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
import com.linkedin.restli.internal.common.CreateIdStatusDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Response for Batch Create Requests that contain strongly typed keys as first-class citizens.
 *
 * @author Moira Tagle
 */
public class BatchCreateIdResponse<K> extends RecordTemplate
{
  private final CreateIdStatusDecoder<K> _entityDecoder;
  private List<CreateIdStatus<K>> _collection;

  public BatchCreateIdResponse(DataMap map, CreateIdStatusDecoder<K> entityDecoder)
  {
    super(map, generateSchema());
    _entityDecoder = entityDecoder;
  }

  private static RecordDataSchema generateSchema()
  {
    StringBuilder errorMessageBuilder = new StringBuilder(10);
    ArrayDataSchema arraySchema = new ArrayDataSchema(new RecordDataSchema(new Name(CreateStatus.class.getSimpleName()), RecordDataSchema.RecordType.RECORD));
    RecordDataSchema.Field arrayField = new RecordDataSchema.Field(arraySchema);
    arrayField.setName(CollectionResponse.ELEMENTS, errorMessageBuilder);
    RecordDataSchema schema = new RecordDataSchema(new Name(BatchCreateIdResponse.class.getSimpleName()), RecordDataSchema.RecordType.RECORD);
    schema.setFields(Arrays.asList(arrayField), errorMessageBuilder);
    return schema;
  }

  private CreateIdStatus<K> decodeValue(DataMap dataMap)
  {
    return _entityDecoder.makeValue(dataMap);
  }

  private void setCollection()
  {
    DataList elements = this.data().getDataList(CollectionResponse.ELEMENTS);
    List<CreateIdStatus<K>> collection = new ArrayList<CreateIdStatus<K>>(elements.size());
    for (Object obj : elements)
    {
      DataMap dataMap = (DataMap) obj;
      collection.add(decodeValue(dataMap));
    }
    _collection = collection;
  }

  public List<CreateIdStatus<K>> getElements()
  {
    if (_collection == null)
    {
      setCollection();
    }
    return _collection;
  }
}
