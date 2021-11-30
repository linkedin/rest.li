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


import org.testng.annotations.Test;
import org.testng.Assert;


/**
 * @author Moira Tagle
 */
public class TestIdResponse
{
  // just testing to make sure that no errors occur.
  @Test
  public void testToString()
  {
    IdResponse<Long> longIdResponse = new IdResponse<>(6L);
    longIdResponse.toString();

    IdResponse<Long> nullIdResponse = new IdResponse<>(null);
    nullIdResponse.toString();
  }

  @Test
  public void testEquals()
  {
    IdResponse<Long> longResponse1 = new IdResponse<>(1L);
    IdResponse<Long> longResponse2 = new IdResponse<>(1L);
    IdResponse<Long> nullLongResponse = new IdResponse<>(null);
    IdResponse<String> stringResponse = new IdResponse<>("hello");
    IdResponse<String> nullStringResponse = new IdResponse<>(null);

    // equals and non-null.
    Assert.assertTrue(longResponse1.equals(longResponse2));

    // equals and null
    Assert.assertTrue(nullLongResponse.equals(nullStringResponse));
    Assert.assertTrue(nullStringResponse.equals(nullLongResponse));

    // unequal and non-null
    Assert.assertFalse(longResponse1.equals(stringResponse));

    // unequal and one null
    Assert.assertFalse(longResponse1.equals(nullLongResponse));
    Assert.assertFalse(nullLongResponse.equals(longResponse1));
  }

  @Test
  public void testHashCode()
  {
    IdResponse<Long> longResponse1 = new IdResponse<>(1L);
    IdResponse<Long> longResponse2 = new IdResponse<>(1L);
    IdResponse<Long> nullLongResponse = new IdResponse<>(null);
    IdResponse<String> nullStringResponse = new IdResponse<>(null);

    Assert.assertEquals(longResponse1.hashCode(), longResponse2.hashCode());
    Assert.assertEquals(nullLongResponse.hashCode(), nullStringResponse.hashCode());
  }
}
