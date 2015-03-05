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

package com.linkedin.restli.internal.common;


import com.linkedin.restli.common.MyCustomString;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.test.MyCustomStringRef;
import com.linkedin.restli.common.test.MyLongRef;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author xma
 */
public class TestResponseUtils
{
  @Test
  public void testConvertTyperefKey()
  {
    Long longKey = (Long) ResponseUtils.convertKey("1",
                                                   TypeSpec.forClassMaybeNull(MyLongRef.class),
                                                   null,
                                                   null,
                                                   AllProtocolVersions.BASELINE_PROTOCOL_VERSION);
    Assert.assertEquals(longKey, new Long(1L));
  }

  public void testConvertCustomTyperefKey()
  {
    MyCustomString customStringKey = (MyCustomString) ResponseUtils.convertKey("foo",
                                                                               TypeSpec.forClassMaybeNull(MyCustomStringRef.class),
                                                                               null,
                                                                               null,
                                                                               AllProtocolVersions.BASELINE_PROTOCOL_VERSION);
    Assert.assertEquals(customStringKey, new MyCustomString("foo"));
  }
}

