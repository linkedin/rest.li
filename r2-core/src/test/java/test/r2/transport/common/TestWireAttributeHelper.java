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

/* $Id$ */
package test.r2.transport.common;

import com.linkedin.r2.transport.common.WireAttributeHelper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TestWireAttributeHelper
{
  @Test
  public void testReversible()
  {
    final Map<String, String> attrs = new HashMap<String, String>();
    attrs.put("key1", "val1");
    attrs.put("key2", "val2");
    attrs.put("key3", "val3");

    final Map<String, String> copy = new HashMap<String, String>(attrs);
    final Map<String, String> actual =
            WireAttributeHelper.removeWireAttributes(WireAttributeHelper.toWireAttributes(copy));
    Assert.assertEquals(actual, attrs);
  }
}
