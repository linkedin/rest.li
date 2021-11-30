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

import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.template.*;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Min Chen
 */
public class TestArray
{
  @Test
  public void testIntegerArray()
  {
    TestDataTemplateUtil.FieldInfo fieldInfo = TestDataTemplateUtil.fieldInfo(new ArrayTest(), "intArray");
    @SuppressWarnings("unchecked")
    Class<IntegerArray> templateClass = (Class<IntegerArray>) fieldInfo.getFieldClass();
    ArrayDataSchema schema = (ArrayDataSchema) fieldInfo.getField().getType();

    List<Integer> input = Arrays.asList(1, 3, 5, 7, 9); // must be unique
    List<Integer> adds = Arrays.asList(11, 13);

    TestArrayTemplate.testArray(templateClass, schema, input, adds);
  }

  @Test
  public void testStringMapArray()
  {
    TestDataTemplateUtil.FieldInfo fieldInfo = TestDataTemplateUtil.fieldInfo(new ArrayTest(), "stringMapArray");
    @SuppressWarnings("unchecked")
    Class<StringMapArray> templateClass = (Class<StringMapArray>) fieldInfo.getFieldClass();
    ArrayDataSchema schema = (ArrayDataSchema) fieldInfo.getField().getType();

    List<StringMap> input = new ArrayList<>();
    for (int i = 0; i < 5; ++i)
    {
      input.add(new StringMap());
      input.get(i).put("input key " + i, "value " + i);
    }
    List<StringMap> adds = new ArrayList<>();
    for (int i = 0; i < 5; ++i)
    {
      adds.add(new StringMap());
      input.get(i).put("add key " + i, "value " + i);
    }

    TestArrayTemplate.testArray(templateClass, schema, input, adds);
  }

  @Test
  public void testStringArrayArray()
  {
    TestDataTemplateUtil.FieldInfo fieldInfo = TestDataTemplateUtil.fieldInfo(new ArrayTest(), "stringArrayArray");
    @SuppressWarnings("unchecked")
    Class<StringArrayArray> templateClass = (Class<StringArrayArray>) fieldInfo.getFieldClass();
    ArrayDataSchema schema = (ArrayDataSchema) fieldInfo.getField().getType();

    List<StringArray> input = new ArrayList<>();
    for (int i = 0; i < 5; ++i)
    {
      input.add(new StringArray("input" + i));
    }
    List<StringArray> adds = new ArrayList<>();
    for (int i = 0; i < 5; ++i)
    {
      adds.add(new StringArray("add" + i));
    }

    TestArrayTemplate.testArray(templateClass, schema, input, adds);
  }

  @Test
  public void testEnumFruitsArray()
  {
    TestDataTemplateUtil.FieldInfo fieldInfo = TestDataTemplateUtil.fieldInfo(new ArrayTest(), "enumFruitsArray");
    @SuppressWarnings("unchecked")
    Class<EnumFruitsArray> templateClass = (Class<EnumFruitsArray>) fieldInfo.getFieldClass();
    ArrayDataSchema schema = (ArrayDataSchema) fieldInfo.getField().getType();

    List<EnumFruits> input = Arrays.asList(EnumFruits.APPLE, EnumFruits.ORANGE, EnumFruits.BANANA); // must be unique
    List<EnumFruits> adds = Arrays.asList(EnumFruits.GRAPES, EnumFruits.PINEAPPLE);

    TestArrayTemplate.testArray(templateClass, schema, input, adds);
  }

  @Test
  public void testRecordArray()
  {
    TestDataTemplateUtil.FieldInfo fieldInfo = TestDataTemplateUtil.fieldInfo(new ArrayTest(), "recordArray");
    @SuppressWarnings("unchecked")
    Class<RecordBarArray> templateClass = (Class<RecordBarArray>) fieldInfo.getFieldClass();
    ArrayDataSchema schema = (ArrayDataSchema) fieldInfo.getField().getType();

    List<RecordBar> input = new ArrayList<>();
    for (int i = 0; i < 5; ++i)
    {
      input.add(new RecordBar());
      input.get(i).setLocation("input " + i);
    }
    List<RecordBar> adds = new ArrayList<>();
    for (int i = 0; i < 5; ++i)
    {
      adds.add(new RecordBar());
      input.get(i).setLocation("add " + i);
    }
    TestArrayTemplate.testArray(templateClass, schema, input, adds);
  }
}
