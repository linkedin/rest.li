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

package com.linkedin.restli.client.testutils;


import java.util.List;

import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.CreateIdStatus;


/**
 * Factory for creating {@link BatchCreateIdResponse} that can be used in tests.
 *
 * @author xma
 */
public class MockBatchCreateIdResponseFactory
{
  private MockBatchCreateIdResponseFactory() { }

  /**
   * Creates a {@link BatchCreateIdResponse} using the given elements.
   *
   * @param elements The {@link CreateIdStatus} objects in the batch.
   */
  public static <K> BatchCreateIdResponse<K> create(List<CreateIdStatus<K>> elements)
  {
    return new BatchCreateIdResponse<K>(elements);
  }
}
