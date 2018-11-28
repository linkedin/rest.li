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

package com.linkedin.restli.internal.client;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.BatchCollectionResponse;
import com.linkedin.restli.common.BatchFinderCriteriaResult;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.common.BatchFinderCriteriaResultDecoder;

import java.util.Map;


/**
 * Converts a raw RestResponse into a type-bound {@link BatchCollectionResponse}.
 *
 * @author Jiaqi Guan
 */
public class BatchCollectionResponseDecoder<T extends RecordTemplate>
                  extends RestResponseDecoder<BatchCollectionResponse<T>>
{
  private final Class<T> _elementClass;

  public BatchCollectionResponseDecoder(Class<T> elementClass)
  {
    _elementClass = elementClass;
  }

  @Override
  public Class<?> getEntityClass()
  {
    return BatchFinderCriteriaResult.class;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public BatchCollectionResponse<T> wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
  {
    BatchFinderCriteriaResultDecoder<T> decoder = new BatchFinderCriteriaResultDecoder(_elementClass);
    return dataMap == null ? null : new BatchCollectionResponse(dataMap, decoder);
  }
}