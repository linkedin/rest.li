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
import com.linkedin.data.template.RecordTemplate;


/**
 * Assertions for {@link DataMap} and {@link DataList} objects.
 *
 * @author Anirudh Padmarao
 */
public final class DataAsserts
{
  private DataAsserts() { }

  /**
   * Assert that the actual data map equals the expected data map using {@link DataCompare}
   *
   * @param actual   the actual data object
   * @param expected the expected data object
   */
  public static void assertEquals(DataMap actual, DataMap expected)
  {
    assertDataEquals(actual, expected);
  }

  /**
   * Assert that the actual data list equals the expected data list using {@link DataCompare}
   *
   * @param actual   the actual data object
   * @param expected the expected data object
   */
  public static void assertEquals(DataList actual, DataList expected)
  {
    assertDataEquals(actual, expected);
  }

  /**
   * Asserts that actual record template data equals the expected record template data using {@link DataCompare}
   *
   * @param actual   the actual data object
   * @param expected the expected object
   */
  public static void assertEquals(RecordTemplate actual, RecordTemplate expected)
  {
    assertDataEquals(actual.data(), expected.data());
  }

  private static void assertDataEquals(Object actual, Object expected)
  {
    DataCompare.Result compareResult = DataCompare.compare(expected, actual);
    if (compareResult.hasError())
    {
      throw new AssertionError(compareResult.toString());
    }
  }
}
