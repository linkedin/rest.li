/*
   Copyright (c) 2015 LinkedIn Corp.

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
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.internal.common.CreateIdEntityStatusDecoder;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Response for Batch Create id entity Requests that contain strongly typed keys and values.
 *
 * @author Boyang Chen
 */
public class BatchCreateIdEntityResponse<K, V extends RecordTemplate> extends RecordTemplate
{
  private final List<CreateIdEntityStatus<K, V>> _collection;

  public BatchCreateIdEntityResponse(DataMap map, CreateIdEntityStatusDecoder<K, V> idEntityDecoder)
      throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
  {
    super(map, generateSchema());
    _collection = createCollectionFromDecoder(idEntityDecoder);
  }

  public BatchCreateIdEntityResponse(List<CreateIdEntityStatus<K, V>> elements)
  {
    super(generateDataMap(elements), generateSchema());
    _collection = elements;

  }

  private static DataMap generateDataMap(List<? extends RecordTemplate> elements)
  {
    DataMap dataMap = new DataMap();
    DataList listElements = new DataList();
    for (RecordTemplate recordTemplate : elements)
    {
      CreateIdEntityStatus<?, ?> status = (CreateIdEntityStatus) recordTemplate;
      CheckedUtil.addWithoutChecking(listElements, status.data());
    }
    dataMap.put(CollectionResponse.ELEMENTS, listElements);

    return dataMap;
  }

  private static RecordDataSchema generateSchema()
  {
    StringBuilder errorMessageBuilder = new StringBuilder(10);
    ArrayDataSchema arraySchema = new ArrayDataSchema(new RecordDataSchema(new Name(CreateStatus.class.getSimpleName()), RecordDataSchema.RecordType.RECORD));
    RecordDataSchema.Field arrayField = new RecordDataSchema.Field(arraySchema);
    arrayField.setName(CollectionResponse.ELEMENTS, errorMessageBuilder);
    RecordDataSchema schema = new RecordDataSchema(new Name(BatchCreateIdEntityResponse.class.getSimpleName()), RecordDataSchema.RecordType.RECORD);
    schema.setFields(Arrays.asList(arrayField), errorMessageBuilder);
    return schema;
  }

  private List<CreateIdEntityStatus<K, V>> createCollectionFromDecoder(CreateIdEntityStatusDecoder<K, V> decoder)
      throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
  {
    DataList elements = this.data().getDataList(CollectionResponse.ELEMENTS);
    List<CreateIdEntityStatus<K, V>> collection = new ArrayList<CreateIdEntityStatus<K, V>>(elements.size());
    for (Object obj : elements)
    {
      DataMap dataMap = (DataMap) obj;
      CreateIdEntityStatus<K, V> status = decodeValue(dataMap, decoder);
      collection.add(status);
    }
    return collection;
  }

  private CreateIdEntityStatus<K, V> decodeValue(DataMap dataMap, CreateIdEntityStatusDecoder<K, V> decoder)
      throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
  {
    return decoder.makeValue(dataMap);
  }

  public List<CreateIdEntityStatus<K, V>> getElements()
  {
    return _collection;
  }
}