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
import com.linkedin.restli.internal.common.CreateIdStatusDecoder;

import java.util.ArrayList;
import java.util.List;


/**
 * Response for Batch Create Requests.  Effectively returns a collection of {@link com.linkedin.restli.common.CreateIdStatus},
 * but individual {@link com.linkedin.restli.common.CreateStatus} must be cast to CreateIdStatus to get access to the
 * strongly-typed keys.
 *
 * @author Moira Tagle
 */
public class BatchCreateResponse<K> extends CollectionResponse<CreateStatus>
{
  private final CreateIdStatusDecoder<K> _entityDecoder;
  private List<CreateStatus> _collection;

  public BatchCreateResponse()
  {
    this(null, null);
  }

  public BatchCreateResponse(DataMap data, CreateIdStatusDecoder<K> entityDecoder)
  {
    super(data, CreateStatus.class);
    _entityDecoder = entityDecoder;
  }

  private CreateStatus decodeValue(DataMap dataMap)
  {
    return _entityDecoder.makeValue(dataMap);
  }

  private void setCollection()
  {
    DataList elements = this.data().getDataList(CollectionResponse.ELEMENTS);
    List<CreateStatus> collection = new ArrayList<CreateStatus>(elements.size());
    for (Object obj : elements)
    {
      DataMap dataMap = (DataMap) obj;
      collection.add(decodeValue(dataMap));
    }
    _collection = collection;
  }

  /**
   * @return actually returns a list of {@link com.linkedin.restli.common.CreateIdStatus}
   */
  public List<CreateStatus> getElements()
  {
    if (_collection == null)
    {
      setCollection();
    }
    return _collection;
  }
}
