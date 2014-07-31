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


import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.common.test.MyCustomStringRef;
import com.linkedin.restli.common.test.MyLongRef;
import com.linkedin.restli.common.test.RecordTemplateWithPrimitiveKey;
import com.linkedin.restli.common.test.SimpleEnum;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestKeyValueRecord
{
  @Test
  public void testPrimitive()
  {
    KeyValueRecordFactory<Long, RecordTemplateWithPrimitiveKey> factory =
        new KeyValueRecordFactory<Long, RecordTemplateWithPrimitiveKey>(Long.class,
                                                            null,
                                                            null,
                                                            null,
                                                            RecordTemplateWithPrimitiveKey.class);

    Long id = 1L;
    RecordTemplateWithPrimitiveKey mockRecordTemplate = new RecordTemplateWithPrimitiveKey();
    mockRecordTemplate.setId(id).setBody("foo");

    KeyValueRecord<Long, RecordTemplateWithPrimitiveKey> keyValueRecord = factory.create(id, mockRecordTemplate);

    Assert.assertEquals(keyValueRecord.getPrimitiveKey(Long.class), id);
    Assert.assertEquals(keyValueRecord.getValue(RecordTemplateWithPrimitiveKey.class), mockRecordTemplate);
  }

  @Test
  public void testEnum()
  {
    KeyValueRecordFactory<SimpleEnum, RecordTemplateWithPrimitiveKey> factory =
        new KeyValueRecordFactory<SimpleEnum, RecordTemplateWithPrimitiveKey>(SimpleEnum.class,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         RecordTemplateWithPrimitiveKey.class);
    SimpleEnum id = SimpleEnum.A;
    RecordTemplateWithPrimitiveKey mockRecordTemplate = new RecordTemplateWithPrimitiveKey();
    mockRecordTemplate.setId(1L).setBody("foo");

    KeyValueRecord<SimpleEnum, RecordTemplateWithPrimitiveKey> keyValueRecord = factory.create(id, mockRecordTemplate);

    Assert.assertEquals(keyValueRecord.getPrimitiveKey(SimpleEnum.class), id);
    Assert.assertEquals(keyValueRecord.getValue(RecordTemplateWithPrimitiveKey.class), mockRecordTemplate);
  }

  @Test
  public void testCompoundKeyWithEnum()
  {
    CompoundKey compoundKey = new CompoundKey();

    Long longKey = 1L;
    compoundKey.append("longKey", longKey);

    SimpleEnum simpleEnum = SimpleEnum.A;
    compoundKey.append("enumKey", simpleEnum);

    Map<String, CompoundKey.TypeInfo> fieldTypes = new HashMap<String, CompoundKey.TypeInfo>();
    fieldTypes.put("longKey", new CompoundKey.TypeInfo(Long.class, Long.class));
    fieldTypes.put("enumKey", new CompoundKey.TypeInfo(SimpleEnum.class, String.class));

    testCompoundKey(compoundKey, fieldTypes);
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void testComplex()
  {
    KeyValueRecordFactory<ComplexResourceKey, RecordTemplateWithPrimitiveKey> factory =
        new KeyValueRecordFactory<ComplexResourceKey, RecordTemplateWithPrimitiveKey>(ComplexResourceKey.class,
                                                                          MyComplexKey.class,
                                                                          MyComplexKey.class,
                                                                          null,
                                                                          RecordTemplateWithPrimitiveKey.class);

    MyComplexKey key = new MyComplexKey().setA("key").setB(1L);
    MyComplexKey params = new MyComplexKey().setA("params").setB(2L);
    ComplexResourceKey<MyComplexKey, MyComplexKey> complexKey =
        new ComplexResourceKey<MyComplexKey, MyComplexKey>(key, params);

    RecordTemplateWithPrimitiveKey mockRecord = new RecordTemplateWithPrimitiveKey().setId(1L).setBody("foo");

    KeyValueRecord<ComplexResourceKey, RecordTemplateWithPrimitiveKey> keyValueRecord =
        factory.create(complexKey, mockRecord);

    Assert.assertEquals(keyValueRecord.getComplexKey(MyComplexKey.class, MyComplexKey.class), complexKey);
    Assert.assertEquals(keyValueRecord.getValue(RecordTemplateWithPrimitiveKey.class), mockRecord);
  }

  @Test
  public void testCompoundKeyWithPrimitiveKeys()
  {
    CompoundKey compoundKey = new CompoundKey();

    Long longKey = 1L;
    compoundKey.append("longKey", longKey);

    String stringKey = "1";
    compoundKey.append("stringKey", stringKey);

    Map<String, CompoundKey.TypeInfo> fieldTypes = new HashMap<String, CompoundKey.TypeInfo>();
    fieldTypes.put("longKey", new CompoundKey.TypeInfo(Long.class, Long.class));
    fieldTypes.put("stringKey", new CompoundKey.TypeInfo(String.class, String.class));

    testCompoundKey(compoundKey, fieldTypes);
  }

  @Test
  public void testCompoundKeyWithPrimitiveTyperef()
  {
    CompoundKey compoundKey = new CompoundKey();

    String stringKey = "11";
    compoundKey.append("stringKey", stringKey);

    Long longKey = 1L;
    compoundKey.append("longKey", longKey);

    Map<String, CompoundKey.TypeInfo> fieldTypes = new HashMap<String, CompoundKey.TypeInfo>();
    fieldTypes.put("stringKey", new CompoundKey.TypeInfo(String.class, String.class));
    fieldTypes.put("longKey", new CompoundKey.TypeInfo(Long.class, MyLongRef.class));

    testCompoundKey(compoundKey, fieldTypes);
  }

  @Test
  public void testCompoundKeysWithCustomTyperefs()
  {
    CompoundKey compoundKey = new CompoundKey();

    MyCustomString myCustomString = new MyCustomString("myCustomString");
    compoundKey.append("myCustomString", myCustomString);

    Map<String, CompoundKey.TypeInfo> fieldTypes = new HashMap<String, CompoundKey.TypeInfo>();
    fieldTypes.put("myCustomString", new CompoundKey.TypeInfo(MyCustomString.class, MyCustomStringRef.class));

    testCompoundKey(compoundKey, fieldTypes);
  }

  private void testCompoundKey(CompoundKey compoundKey, Map<String, CompoundKey.TypeInfo> fieldTypes)
  {
    RecordTemplateWithPrimitiveKey mockRecord = new RecordTemplateWithPrimitiveKey().setId(7L).setBody("foo");

    KeyValueRecordFactory<CompoundKey, RecordTemplateWithPrimitiveKey> factory =
        new KeyValueRecordFactory<CompoundKey, RecordTemplateWithPrimitiveKey>(CompoundKey.class,
                                                                   null,
                                                                   null,
                                                                   fieldTypes,
                                                                   RecordTemplateWithPrimitiveKey.class);

    KeyValueRecord<CompoundKey, RecordTemplateWithPrimitiveKey> keyValueRecord = factory.create(compoundKey, mockRecord);

    Assert.assertEquals(keyValueRecord.getCompoundKey(fieldTypes), compoundKey);
    Assert.assertEquals(keyValueRecord.getValue(RecordTemplateWithPrimitiveKey.class), mockRecord);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testTyperefToCustomType()
  {
    KeyValueRecordFactory factory = new KeyValueRecordFactory(MyCustomStringRef.class,
                                                              null,
                                                              null,
                                                              null,
                                                              RecordTemplateWithPrimitiveKey.class);

    MyCustomString id = new MyCustomString("1");
    RecordTemplateWithPrimitiveKey mockRecord = new RecordTemplateWithPrimitiveKey().setId(1L).setBody("foo");

    KeyValueRecord keyValueRecord = factory.create(id, mockRecord);

    Assert.assertEquals(keyValueRecord.getPrimitiveKey(MyCustomStringRef.class), id);
    Assert.assertEquals(keyValueRecord.getValue(RecordTemplateWithPrimitiveKey.class), mockRecord);
  }
}
