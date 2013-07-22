package com.linkedin.restli.common;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.data.DataMap;

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
        new ComplexResourceKey<EmptyRecord, EmptyRecord>(key1, param1);

    EmptyRecord key2 = key1.copy();
    EmptyRecord param2 = param1.copy();
    ComplexResourceKey<EmptyRecord, EmptyRecord> complexKey2 =
        new ComplexResourceKey<EmptyRecord, EmptyRecord>(key2, param2);

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
    complexKey1 = new ComplexResourceKey<EmptyRecord, EmptyRecord>(key1, null);
    complexKey2 = new ComplexResourceKey<EmptyRecord, EmptyRecord>(key2, param2);
    Assert.assertFalse(complexKey1.equals(complexKey2));
    Assert.assertFalse(complexKey2.equals(complexKey1));

    // Both param null
    complexKey2 = new ComplexResourceKey<EmptyRecord, EmptyRecord>(key2, null);
    Assert.assertTrue(complexKey1.equals(complexKey2));
  }
}
