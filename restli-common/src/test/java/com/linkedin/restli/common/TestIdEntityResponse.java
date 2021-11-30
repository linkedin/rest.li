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

package com.linkedin.restli.common;

import com.linkedin.pegasus.generator.test.AnyRecord;
import org.testng.annotations.Test;
import org.testng.Assert;


/**
 * @author Boyang Chen
 */
public class TestIdEntityResponse
{
  @Test
  public void testToString()
  {
    IdEntityResponse<Long, AnyRecord> longIdEntityResponse = new IdEntityResponse<>(6L, new AnyRecord());
    Assert.assertEquals(longIdEntityResponse.toString(), "id: 6, entity: {}");

    IdEntityResponse<Long, AnyRecord> nullIdEntityResponse = new IdEntityResponse<>(null, new AnyRecord());
    Assert.assertEquals(nullIdEntityResponse.toString(), "id: , entity: {}");
  }

  @Test
  public void testEquals()
  {
    IdEntityResponse<Long, AnyRecord> longIdEntityResponse1 = new IdEntityResponse<>(1L, new AnyRecord());
    IdEntityResponse<Long, AnyRecord> longIdEntityResponse2 = new IdEntityResponse<>(1L, new AnyRecord());
    IdEntityResponse<Long, AnyRecord> nullLongResponse = new IdEntityResponse<>(null, new AnyRecord());
    IdEntityResponse<String, AnyRecord> nullStringResponse = new IdEntityResponse<>(null, new AnyRecord());
    IdEntityResponse<String, AnyRecord> stringResponse = new IdEntityResponse<>("hello", new AnyRecord());

    // equals and non-null.
    Assert.assertTrue(longIdEntityResponse1.equals(longIdEntityResponse2));

    // equals and null
    Assert.assertTrue(nullLongResponse.equals(nullStringResponse));
    Assert.assertTrue(nullStringResponse.equals(nullLongResponse));

    // unequal and non-null
    Assert.assertFalse(longIdEntityResponse1.equals(stringResponse));

    // unequal and one null
    Assert.assertFalse(longIdEntityResponse1.equals(nullLongResponse));
    Assert.assertFalse(nullLongResponse.equals(longIdEntityResponse1));
  }

  @Test
  public void testHashCode()
  {
    IdEntityResponse<Long, AnyRecord> longIdEntityResponse1 = new IdEntityResponse<>(1L, new AnyRecord());
    IdEntityResponse<Long, AnyRecord> longIdEntityResponse2 = new IdEntityResponse<>(1L, new AnyRecord());
    IdEntityResponse<Long, AnyRecord> nullLongResponse = new IdEntityResponse<>(null, new AnyRecord());
    IdEntityResponse<String, AnyRecord> nullStringResponse = new IdEntityResponse<>(null, new AnyRecord());

    Assert.assertEquals(longIdEntityResponse1.hashCode(), longIdEntityResponse2.hashCode());
    Assert.assertEquals(nullLongResponse.hashCode(), nullStringResponse.hashCode());
  }
}
