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

package com.linkedin.restli.client.testutils;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.BatchResponse;
import java.util.Map;


/**
 * Factory for creating {@link BatchResponse}s that can be used for tests.
 *
 * @author jflorencio
 * @author kparikh
 */
public class MockBatchResponseFactory
{
  private MockBatchResponseFactory() { }

  /**
   * Creates a {@link com.linkedin.restli.common.BatchResponse}
   *
   * @param entryClass the class of the elements stored in this {@link com.linkedin.restli.common.BatchResponse}
   * @param recordTemplates the elements to be stored in this {@link com.linkedin.restli.common.BatchResponse}
   * @param <T> class of the elements stored in this {@link com.linkedin.restli.common.BatchResponse}
   * @return a {@link com.linkedin.restli.common.BatchResponse} with the above properties
   */
  public static <T extends RecordTemplate> BatchResponse<T> create(Class<T> entryClass, Map<String, T> recordTemplates)
  {
    DataMap batchResponseDataMap = new DataMap();
    DataMap rawBatchData = new DataMap();
    batchResponseDataMap.put(BatchResponse.RESULTS, rawBatchData);
    for (Map.Entry<String, T> entry : recordTemplates.entrySet())
    {
      rawBatchData.put(entry.getKey(), entry.getValue().data());
    }
    return new BatchResponse<>(batchResponseDataMap, entryClass);
  }
}
