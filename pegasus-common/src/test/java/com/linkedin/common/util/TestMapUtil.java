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

package com.linkedin.common.util;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author Keren Jin
 */
public class TestMapUtil
{
  @BeforeTest
  private void prepareMap()
  {
    _subjectMap = new HashMap<>();
    _probeMap = new HashMap<>();

    _subjectMap.put("boolean", true);
    _subjectMap.put("integer", 1);
    _subjectMap.put("long", 2L);
    _subjectMap.put("float", 3F);
    _subjectMap.put("double", 4D);
    _subjectMap.put("string", "Foo");
    _subjectMap.put("subMap", new LinkedHashMap<String,Object>());
    _subjectMap.put("siblingMap", new Hashtable<String,Object>());
  }

  @Test
  public void testGetWithDefault()
  {
    Assert.assertTrue(MapUtil.getWithDefault(_subjectMap, "boolean", false));
    Assert.assertFalse(MapUtil.getWithDefault(_subjectMap, "boolean_default", false));

    Assert.assertEquals(MapUtil.getWithDefault(_subjectMap, "integer", 11), (Integer) 1);
    Assert.assertEquals(MapUtil.getWithDefault(_subjectMap, "integer_default", 11),
                        (Integer) 11);

    Assert.assertEquals(MapUtil.getWithDefault(_subjectMap, "long", 12L), (Long) 2L);
    Assert.assertEquals(MapUtil.getWithDefault(_subjectMap, "long_default", 12L), (Long) 12L);

    Assert.assertEquals(MapUtil.getWithDefault(_subjectMap, "float", 13F), 3F);
    Assert.assertEquals(MapUtil.getWithDefault(_subjectMap, "float_default", 13F), 13F);

    Assert.assertEquals(MapUtil.getWithDefault(_subjectMap, "double", 14D), 4D);
    Assert.assertEquals(MapUtil.getWithDefault(_subjectMap, "double_default", 14D), 14D);

    Assert.assertEquals(MapUtil.getWithDefault(_subjectMap, "string", "Bar"), "Foo");
    Assert.assertEquals(MapUtil.getWithDefault(_subjectMap, "string_default", "Bar"), "Bar");

    Assert.assertSame(MapUtil.getWithDefault(_subjectMap, "subMap", null, LinkedHashMap.class).getClass(), LinkedHashMap.class);
    Assert.assertSame(MapUtil.getWithDefault(_subjectMap, "subMap", _probeMap).getClass(), LinkedHashMap.class);
    Assert.assertSame(MapUtil.getWithDefault(_subjectMap, "subMap_default", _probeMap).getClass(), HashMap.class);
    Assert.assertNull(MapUtil.getWithDefault(_subjectMap, "subMap_default", null, Object.class));

    Assert.assertSame(MapUtil.getWithDefault(_subjectMap, "siblingMap", null, Hashtable.class).getClass(), Hashtable.class);
    Assert.assertSame(MapUtil.getWithDefault(_subjectMap, "siblingMap", _probeMap, Map.class).getClass(), Hashtable.class);
    Assert.assertSame(MapUtil.getWithDefault(_subjectMap, "siblingMap_default", _probeMap, Map.class).getClass(), HashMap.class);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullArgument()
  {
    MapUtil.getWithDefault(_subjectMap, "subMap", null);
    MapUtil.getWithDefault(_subjectMap, "subMap_default", null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSiblingCastException()
  {
    MapUtil.getWithDefault(_subjectMap, "siblingMap", _probeMap);
  }

  private Map<String, Object> _subjectMap;
  private Map<String, Object> _probeMap;
}
