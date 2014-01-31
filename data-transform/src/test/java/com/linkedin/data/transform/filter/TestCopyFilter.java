/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.data.transform.filter;


import com.linkedin.data.DataMap;
import com.linkedin.data.transform.DataProcessingException;

import static org.testng.Assert.assertEquals;


/**
 * @author Keren Jin
 */
public class TestCopyFilter extends TestFilterOnData
{
  @Override
  protected void genericFilterTest(DataMap data, DataMap filter, DataMap expected, String description) throws DataProcessingException
  {
    final String dataBefore = data.toString();
    final Object filtered = new CopyFilter().filter(data, filter);
    assertEquals(filtered, expected, "The following test failed: \n" + description  +
        "\nData: " + dataBefore + "\nFilter: " + filter +
        "\nExpected: " + expected + "\nActual result: " + filtered);
  }
}
