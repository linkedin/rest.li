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

package com.linkedin.pegasus.generator.override;

import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.template.*;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static com.linkedin.data.TestUtil.asMap;


/**
 * @author Min Chen
 */
public class TestMap
{
  @Test
  public void testIntegerMap()
  {
    TestDataTemplateUtil.FieldInfo fieldInfo = TestDataTemplateUtil.fieldInfo(new MapTest(), "intMap");
    @SuppressWarnings("unchecked")
    Class<IntegerMap> templateClass = (Class<IntegerMap>) fieldInfo.getFieldClass();
    MapDataSchema schema = (MapDataSchema) fieldInfo.getField().getType();

    Map<String, Integer> input = asMap("one", 1, "three", 3, "five", 5, "seven", 7, "eleven", 11);
    Map<String, Integer> adds = asMap("thirteen", 13, "seventeen", 17, "nineteen", 19);

    TestMapTemplate.testMap(templateClass, schema, input, adds);
  }

  @Test
  public void testStringArrayMap()
  {
    TestDataTemplateUtil.FieldInfo fieldInfo = TestDataTemplateUtil.fieldInfo(new MapTest(), "stringArrayMap");
    @SuppressWarnings("unchecked")
    Class<StringArrayMap> templateClass = (Class<StringArrayMap>) fieldInfo.getFieldClass();
    MapDataSchema schema = (MapDataSchema) fieldInfo.getField().getType();

    Map<String, StringArray> input = new HashMap<String, StringArray>();
    for (int i = 0; i < 5; ++i)
    {
      String key = "input" + i;
      input.put(key, new StringArray());
      input.get(key).add("subinput" + i);
    }
    Map<String, StringArray> adds = new HashMap<String, StringArray>();
    for (int i = 0; i < 5; ++i)
    {
      String key = "add" + i;
      adds.put(key, new StringArray());
      adds.get(key).add("subadd" + i);
    }

    TestMapTemplate.testMap(templateClass, schema, input, adds);
  }

  @Test
  public void testStringMapMap()
  {
    TestDataTemplateUtil.FieldInfo fieldInfo = TestDataTemplateUtil.fieldInfo(new MapTest(), "stringMapMap");
    @SuppressWarnings("unchecked")
    Class<StringMapMap> templateClass = (Class<StringMapMap>) fieldInfo.getFieldClass();
    MapDataSchema schema = (MapDataSchema) fieldInfo.getField().getType();

    Map<String, StringMap> input = new HashMap<String, StringMap>();
    for (int i = 0; i < 5; ++i)
    {
      String key = "input" + i;
      input.put(key, new StringMap());
      input.get(key).put("subinput" + i, "subinputvalue" + i);
    }
    Map<String, StringMap> adds = new HashMap<String, StringMap>();
    for (int i = 0; i < 5; ++i)
    {
      String key = "add" + i;
      adds.put(key, new StringMap());
      adds.get(key).put("subadd" + i, "subaddvalue" + i);
    }

    TestMapTemplate.testMap(templateClass, schema, input, adds);
  }

  @Test
  public void testRecordMap()
  {
    TestDataTemplateUtil.FieldInfo fieldInfo = TestDataTemplateUtil.fieldInfo(new MapTest(), "recordMap");
    @SuppressWarnings("unchecked")
    Class<RecordBarMap> templateClass = (Class<RecordBarMap>) fieldInfo.getFieldClass();
    MapDataSchema schema = (MapDataSchema) fieldInfo.getField().getType();

    Map<String, RecordBar> input = new HashMap<String, RecordBar>();
    for (int i = 0; i < 5; ++i)
    {
      String key = "input" + i;
      input.put(key, new RecordBar());
      input.get(key).setLocation("subinputvalue" + i);
    }
    Map<String, RecordBar> adds = new HashMap<String, RecordBar>();
    for (int i = 0; i < 5; ++i)
    {
      String key = "add" + i;
      adds.put(key, new RecordBar());
      adds.get(key).setLocation("subaddvalue" + i);
    }

    TestMapTemplate.testMap(templateClass, schema, input, adds);
  }
}

