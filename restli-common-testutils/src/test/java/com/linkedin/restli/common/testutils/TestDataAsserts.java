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

import com.linkedin.restli.test.RecordTemplateWithDefaultValue;
import org.testng.annotations.Test;

import static com.linkedin.restli.common.testutils.DataAsserts.*;
import static com.linkedin.restli.common.testutils.TestDataBuilders.*;

public class TestDataAsserts
{

  @Test(
      expectedExceptions = AssertionError.class,
      expectedExceptionsMessageRegExp = ".*field(.|\n)*Expected: bar(.|\n)*got: foo.*"
  )
  public void testAssertEqualsDataMaps()
  {
    assertEquals(toDataMap("field", "foo"), toDataMap("field", "bar"));
  }

  @Test(
      expectedExceptions = AssertionError.class,
      expectedExceptionsMessageRegExp = ".*\\[0\\](.|\n)*Expected: bar(.|\n)*got: foo.*"
  )
  public void testAssertEqualsDataLists()
  {
    assertEquals(toDataList("foo"), toDataList("bar"));
  }

  @Test(
      expectedExceptions = AssertionError.class,
      expectedExceptionsMessageRegExp = ".*id(.|\n)*Expected: 1(.|\n)*got: 2.*"
  )
  public void testAssertEqualsRecordTemplates()
  {
    RecordTemplateWithDefaultValue expected = new RecordTemplateWithDefaultValue().setId(1);
    RecordTemplateWithDefaultValue actual = new RecordTemplateWithDefaultValue().setId(2);

    assertEquals(actual, expected);
  }
}
