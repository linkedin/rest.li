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
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestComplexResourceKey
{
  @Test
  public void testEquals() throws CloneNotSupportedException
  {
    DataMap keyMap = new DataMap();
    keyMap.put("keyField1", "keyValue1");
    EmptyRecord key1 = new EmptyRecord(keyMap);
    DataMap paramMap = new DataMap();
    paramMap.put("paramField1", "paramValue1");
    EmptyRecord param1 = new EmptyRecord(paramMap);
    ComplexResourceKey<EmptyRecord, EmptyRecord> complexKey1 =
        new ComplexResourceKey<>(key1, param1);

    EmptyRecord key2 = key1.copy();
    EmptyRecord param2 = param1.copy();
    ComplexResourceKey<EmptyRecord, EmptyRecord> complexKey2 =
        new ComplexResourceKey<>(key2, param2);

    Assert.assertTrue(complexKey1.equals(complexKey2));

    // Different key part
    complexKey2.key.data().put("keyField1", "keyValue2");
    Assert.assertFalse(complexKey1.equals(complexKey2));
    complexKey2.key.data().put("keyField1", "keyValue1");

    // Different param part
    complexKey2.params.data().put("paramField1", "paramValue2");
    Assert.assertFalse(complexKey1.equals(complexKey2));
    complexKey2.params.data().put("paramField1", "paramValue1");

    // One param null, other not
    complexKey1 = new ComplexResourceKey<>(key1, null);
    complexKey2 = new ComplexResourceKey<>(key2, param2);
    Assert.assertFalse(complexKey1.equals(complexKey2));
    Assert.assertFalse(complexKey2.equals(complexKey1));

    // Both param null
    complexKey2 = new ComplexResourceKey<>(key2, null);
    Assert.assertTrue(complexKey1.equals(complexKey2));
  }

  @Test
  public void testMakeReadOnly()
  {
    DataMap keyDataMap = new DataMap();
    keyDataMap.put("key", "key-value");
    EmptyRecord key = new EmptyRecord(keyDataMap);

    DataMap paramsDataMap = new DataMap();
    paramsDataMap.put("params", "params-value");
    EmptyRecord params = new EmptyRecord(paramsDataMap);

    ComplexResourceKey<EmptyRecord, EmptyRecord> complexResourceKey =
        new ComplexResourceKey<>(key, params);

    complexResourceKey.makeReadOnly();

    try
    {
      key.data().put("key", "new key value");
      Assert.fail("Should not be able to update the key after the ComplexResourceKey has been made read only!");
    }
    catch (UnsupportedOperationException e)
    {

    }

    try
    {
      params.data().put("params", "new params value");
      Assert.fail("Should not be able to update the params after the ComplexResourceKey has been made read only!");
    }
    catch (UnsupportedOperationException e)
    {

    }
  }

  @Test
  public void testReadOnlyWithNullParams()
  {
    DataMap keyDataMap = new DataMap();
    keyDataMap.put("key", "key-value");
    EmptyRecord key = new EmptyRecord(keyDataMap);

    ComplexResourceKey<EmptyRecord, EmptyRecord> complexResourceKey =
        new ComplexResourceKey<>(key, null);

    complexResourceKey.makeReadOnly();

    try
    {
      key.data().put("key", "new key value");
      Assert.fail("Should not be able to update the key after the ComplexResourceKey has been made read only!");
    }
    catch (UnsupportedOperationException e)
    {

    }
  }

  @DataProvider
  public Object[][] keySchemaValidation() {
    return new Object[][]
        {
            {11, 11, false, OmniRecord.class},
            {11, 1, true, OmniRecord.class},
            {1, 11, true, OmniRecord.class},
            {1, 1, false, NullSchemaRecord.class},
        };
  }

  @Test(dataProvider = "keySchemaValidation")
  public void testKeySchema(int keyValue, int paramValue, boolean validationFailure, Class<RecordTemplate> schemaClass)
  {
    TypeSpec<RecordTemplate> keyType = new TypeSpec<>(schemaClass);
    TypeSpec<RecordTemplate> paramsType = new TypeSpec<>(schemaClass);
    ComplexKeySpec<RecordTemplate, RecordTemplate> keySpec =
        new ComplexKeySpec<>(keyType, paramsType);
    DataMap paramsData = new DataMap();
    paramsData.put("int", paramValue);
    DataMap data = new DataMap();
    data.put("int", keyValue);
    data.put("$params", paramsData);

    try
    {
      ComplexResourceKey<RecordTemplate, RecordTemplate> key = ComplexResourceKey.buildFromDataMap(data, keySpec);
      key.validate();
      Assert.assertEquals(key.getKey().schema(), keyType.getSchema());
      Assert.assertEquals(key.getParams().schema(), paramsType.getSchema());
      Assert.assertFalse(validationFailure);
    }
    catch (IllegalArgumentException ex)
    {
      Assert.assertTrue(validationFailure, "Unexpected validation failure");
    }
  }

  public static class OmniRecord extends RecordTemplate {
    private static RecordDataSchema SCHEMA =
            (RecordDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"record\", \"name\" : \"omni\", \"fields\" : [ { \"name\" : \"int\", \"type\" : \"int\", \"validate\": { \"regex\": { \"regex\": \"[0-9][0-9]\" } } } ] }");

    public OmniRecord(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  public static class NullSchemaRecord extends RecordTemplate {
    public NullSchemaRecord(DataMap map)
    {
      super(map, null);
    }
  }
}
