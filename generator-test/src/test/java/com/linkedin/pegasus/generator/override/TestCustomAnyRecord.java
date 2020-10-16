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

package com.linkedin.pegasus.generator.override;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * @author Min Chen
 */
public class TestCustomAnyRecord
{


  @Test
  public void testCustomAnyRecordSchema()
  {
    RecordDataSchema schemaFromInstance = (new AnyRecord()).schema();
    DataSchema schemaFromClass = DataTemplateUtil.getSchema(AnyRecord.class);
    assertSame(schemaFromClass, schemaFromInstance);

    CustomAnyRecord custom = new CustomAnyRecord();
    RecordDataSchema customSchemaFromInstance = custom.schema();
    DataSchema customSchemaFromClass = DataTemplateUtil.getSchema(CustomAnyRecord.class);
    assertSame(customSchemaFromClass, customSchemaFromInstance);

    assertEquals(customSchemaFromClass, schemaFromClass);
  }

  @Test
  public void testCustomAnyRecordFields()
  {
    AnyRecord.Fields anyRecordFields = AnyRecord.fields();
    CustomAnyRecord.Fields customAnyRecordFields = CustomAnyRecord.fields();
    assertSame(anyRecordFields, customAnyRecordFields);

    AnyRecord.Fields newFields = new AnyRecord.Fields();
    CustomAnyRecord.Fields newCustomFields = new CustomAnyRecord.Fields();
    assertSame(newFields.getClass(), newCustomFields.getClass());
  }

  @Test
  public void testCustomAnyRecordMethods()
  {
    DataMap map = new DataMap();
    CustomAnyRecord wrapped = DataTemplateUtil.wrap(map, CustomAnyRecord.class);

    assertNull(wrapped.getValue(AnyRecord.class));
    assertNull(wrapped.getValue(CustomAnyRecord.class));

    assertFalse(wrapped.isValueOfClass(AnyRecord.class));
    assertFalse(wrapped.isValueOfClass(CustomAnyRecord.class));

    CustomAnyRecord value = new CustomAnyRecord();
    wrapped.setValue(value);

    assertTrue(map.containsKey(value.schema().getUnionMemberKey()));

    assertSame(wrapped.getValue(AnyRecord.class), value);
    assertSame(wrapped.getValue(CustomAnyRecord.class), value);

    assertTrue(wrapped.isValueOfClass(AnyRecord.class));
    assertTrue(wrapped.isValueOfClass(CustomAnyRecord.class));

    assertFalse(wrapped.isValueOfClass(Date.class));
    assertNull(wrapped.getValue(Date.class));

    Date date = new Date();

    wrapped.setValue(date);

    assertTrue(map.containsKey(date.schema().getUnionMemberKey()));

    assertSame(wrapped.getValue(Date.class), date);
    assertTrue(wrapped.isValueOfClass(Date.class));
    assertNull(wrapped.getValue(AnyRecord.class));
  }

  @Test
  public void testUseCustomAnyRecord()
  {
    AnyRecordClient r = new AnyRecordClient();

    // test required field
    CustomAnyRecord input = new CustomAnyRecord();
    r.setRequired(input);
    assertSame(r.getRequired(), input);

    // test optional field
    assertFalse(r.hasOptional());
    input = new CustomAnyRecord();
    r.setOptional(input);
    assertSame(r.getOptional(), input);
    assertTrue(r.hasOptional());
    r.removeOptional();
    assertFalse(r.hasOptional());

    // test array
    AnyRecordArray array = new AnyRecordArray();
    input = new CustomAnyRecord();
    array.add(input);
    CustomAnyRecord output = array.get(0);
    assertEquals(input, output);

    // test array field
    r.setArray(array);
    assertSame(r.getArray(), array);

    // test map
    AnyRecordMap map = new AnyRecordMap();
    input = new CustomAnyRecord();
    map.put("0", input);
    output = map.get("0");
    assertEquals(input, output);

    // test map field
    r.setMap(map);
    assertSame(r.getMap(), map);
  }
}
