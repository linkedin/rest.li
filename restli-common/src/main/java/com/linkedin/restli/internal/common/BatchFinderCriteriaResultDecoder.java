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

package com.linkedin.restli.internal.common;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.BatchFinderCriteriaResult;


/**
 * Decode an individual {@link BatchFinderCriteriaResult} which is a result from individual batch finder criteria.
 *
 * @author Jiaqi Guan
 * @param <T> entity template class
 */
public class BatchFinderCriteriaResultDecoder<T extends RecordTemplate>
{
  private final Class<T> _elementClass;

  public BatchFinderCriteriaResultDecoder(Class<T> elementClass) {
    _elementClass = elementClass;
  }

  @SuppressWarnings("unchecked")
  public BatchFinderCriteriaResult<T> makeValue(DataMap dataMap)
  {
    return new BatchFinderCriteriaResult<>(dataMap, _elementClass);
  }
}
