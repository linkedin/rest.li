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
  public void testNewInstanceCreations()
  {
    final Map<String, String> attrs = new HashMap<>();
    final Map<String, String> toAttrs = WireAttributeHelper.toWireAttributes(attrs);
    final Map<String, String> removeAttrs = WireAttributeHelper.removeWireAttributes(attrs);

    Assert.assertNotSame(attrs, toAttrs);
    Assert.assertNotSame(attrs, removeAttrs);
    Assert.assertNotSame(toAttrs, removeAttrs);
  }

  @Test
  public void testCaseInsensitivity()
  {
    Map<String, String> attrs = new HashMap<>();
    attrs.put("key1", "val1");
    attrs.put("key2", "val2");
    attrs.put("key3", "val3");

    attrs = WireAttributeHelper.toWireAttributes(attrs);
    Assert.assertTrue(attrs.containsKey("X-LI-R2-W-KEY1"));
    Assert.assertTrue(attrs.containsKey("x-li-r2-w-key2"));
    Assert.assertTrue(attrs.containsKey("X-LI-R2-W-Key3"));

    attrs = WireAttributeHelper.removeWireAttributes(attrs);

    Assert.assertTrue(attrs.containsKey("KeY1"));
    Assert.assertTrue(attrs.containsKey("KEY2"));
    Assert.assertTrue(attrs.containsKey("KEY3"));
  }

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

  @Test
  public void testRemoveWireAttributes()
  {
    final Map<String, String> headers = new HashMap<String, String>();
    headers.put("key1", "val1");
    headers.put("X-LI-R2-W-key2", "val2");

    final Map<String, String> attributes = WireAttributeHelper.removeWireAttributes(headers);

    // verifies headers with wire attribute prefix are removed
    Assert.assertNotNull(headers, "Original header map should not be null after invocation.");
    Assert.assertEquals(headers.size(), 1, "Incorrect number of headers returned.");
    Assert.assertEquals(headers.get("key1"), "val1", "Headers contain wrong contents.");

    // verifies wire attributes have prefix removed and put into a case insensitive map
    Assert.assertNotNull(attributes, "Parsed wire attribute map should not be null.");
    Assert.assertEquals(attributes.size(), 1, "Incorrect number of wire attributes returned.");
    Assert.assertEquals(attributes.get("key2"), "val2", "Wire attribute key key2 is incorrect.");
  }

  @Test
  public void testRemoveWireAttributesCaseInsensitive()
  {
    final Map<String, String> headers = new HashMap<String, String>();
    headers.put("X-LI-R2-W-key2", "val2");
    headers.put("x-li-r2-w-key3", "val3");
    headers.put("x-li-r2-w-kEY4", "val4");

    final Map<String, String> attributes = WireAttributeHelper.removeWireAttributes(headers);

    // verifies wire attributes have prefix removed and put into a case insensitive map
    Assert.assertNotNull(attributes, "Parsed wire attribute map should not be null.");
    Assert.assertEquals(attributes.size(), 3, "Incorrect number of wire attributes returned.");
    Assert.assertEquals(attributes.get("key2"), "val2", "Wire attribute key key2 is incorrect.");
    Assert.assertEquals(attributes.get("KEY2"), "val2", "Wire attribute key KEY2 is incorrect.");
    Assert.assertEquals(attributes.get("key3"), "val3", "Wire attribute key key3 is incorrect.");
    Assert.assertEquals(attributes.get("KEY3"), "val3", "Wire attribute key KEY3 is incorrect.");
    Assert.assertEquals(attributes.get("key4"), "val4", "Wire attribute key key4 is incorrect.");
    Assert.assertEquals(attributes.get("KEY4"), "val4", "Wire attribute key KEY4 is incorrect.");
  }

  @Test
  public void testToWireAttributes()
  {
    final Map<String, String> headers = new HashMap<String, String>();
    headers.put("key1", "val1");
    headers.put("key2", "val2");

    final Map<String, String> attributes = WireAttributeHelper.toWireAttributes(headers);

    // verifies wire attributes have prefix added and put into a case insensitive map
    Assert.assertNotNull(attributes, "Parsed wire attributes should not be null.");
    Assert.assertEquals(attributes.size(), 2, "Incorrect number of wire attributes returned.");
    Assert.assertEquals(attributes.get("X-LI-R2-W-key1"), "val1", "Wire attribute X-LI-R2-W-KEY1 is incorrect.");
    Assert.assertEquals(attributes.get("X-LI-R2-W-key2"), "val2", "Wire attribute X-LI-R2-W-KEY2 is incorrect.");
  }

  @Test
  public void testToWireAttributesCaseInsensitive()
  {
    final Map<String, String> headers = new HashMap<String, String>();
    headers.put("key1", "val1");
    headers.put("key2", "val2");

    final Map<String, String> attributes = WireAttributeHelper.toWireAttributes(headers);

    // verifies wire attributes have prefix added and put into a case insensitive map
    Assert.assertNotNull(attributes, "Parsed wire attributes should not be null.");
    Assert.assertEquals(attributes.size(), 2, "Incorrect number of wire attributes returned.");
    Assert.assertEquals(attributes.get("x-li-r2-w-key1"), "val1", "Wire attribute X-LI-R2-W-KEY1 is incorrect.");
    Assert.assertEquals(attributes.get("x-li-r2-w-key2"), "val2", "Wire attribute X-LI-R2-W-KEY2 is incorrect.");
  }
}
