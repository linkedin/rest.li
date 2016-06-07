/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.common.testutils;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;

import java.util.Arrays;

/**
 * Utilities to construct {@link DataMap}s and {@link DataList}s for tests.
 */
class TestDataBuilders
{

  static DataMap toDataMap(String k1, Object v1)
  {
    DataMap result = new DataMap();
    result.put(k1, v1);
    return result;
  }

  static DataMap toDataMap(String k1, Object v1, String k2, Object v2)
  {
    DataMap result = new DataMap();
    result.put(k1, v1);
    result.put(k2, v2);
    return result;
  }

  static DataMap toDataMap(String k1, Object v1, String k2, Object v2, String k3, Object v3)
  {
    DataMap result = new DataMap();
    result.put(k1, v1);
    result.put(k2, v2);
    return result;
  }

  static DataList toDataList(Object... items)
  {
    DataList data = new DataList();
    data.addAll(Arrays.asList(items));
    return data;
  }
}
