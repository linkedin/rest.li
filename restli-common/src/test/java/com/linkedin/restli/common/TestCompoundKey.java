/*
   Copyright (c) 2013 LinkedIn Corp.

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


import com.linkedin.data.DataMap;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestCompoundKey
{
  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testMakeReadOnly()
  {
    CompoundKey compoundKey = new CompoundKey();
    compoundKey.append("foo", "foo-value");
    compoundKey.append("bar", 1);
    compoundKey.append("baz", 7L);

    compoundKey.makeReadOnly();

    compoundKey.append("abc", "def");
  }

  @Test
  public void testToDataMap()
  {
    CompoundKey compoundKey = new CompoundKey();
    compoundKey.append("foo", "foo-value");
    compoundKey.append("bar", 1);
    compoundKey.append("baz", 7L);

    DataMap dataMap = compoundKey.toDataMap();
    Assert.assertEquals(dataMap.get("foo"), compoundKey.getPart("foo"));
    Assert.assertEquals(dataMap.get("bar"), compoundKey.getPart("bar"));
    Assert.assertEquals(dataMap.get("baz"), compoundKey.getPart("baz"));
  }

  @Test
  public void testAppendEnum()
  {
    CompoundKey compoundKey = new CompoundKey().append("foo", ResourceMethod.ACTION,
        new CompoundKey.TypeInfo(ResourceMethod.class, ResourceMethod.class));

    Assert.assertEquals(compoundKey.getPart("foo"), ResourceMethod.ACTION);
    DataMap dataMap = compoundKey.toDataMap();
    Assert.assertEquals(dataMap.get("foo"), ResourceMethod.ACTION.toString());
  }

}
