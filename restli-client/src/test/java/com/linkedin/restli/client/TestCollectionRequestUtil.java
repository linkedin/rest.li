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

package com.linkedin.restli.client;


import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.BatchRequest;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.KeyValueRecordFactory;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.client.CollectionRequestUtil;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.common.URLEscaper;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;


/**
 * @author kparikh
 */
public class TestCollectionRequestUtil
{
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "versions")
  public void testPrimitiveKeySingleEntity(ProtocolVersion version)
  {
    KeyValueRecordFactory<Long, TestRecord> factory = new KeyValueRecordFactory<Long, TestRecord>(Long.class,
                                                                                                  null,
                                                                                                  null,
                                                                                                  null,
                                                                                                  TestRecord.class);
    TestRecord testRecord = buildTestRecord(1L, "message");
    KeyValueRecord<Long, TestRecord> kvRecord = factory.create(1L, testRecord);

    @SuppressWarnings("rawtypes")
    CollectionRequest<KeyValueRecord> collectionRequest = new CollectionRequest<KeyValueRecord>(KeyValueRecord.class);
    collectionRequest.getElements().add(kvRecord);

    @SuppressWarnings("unchecked")
    BatchRequest<TestRecord> batchRequest =
        CollectionRequestUtil.convertToBatchRequest(collectionRequest,
                                                    Long.class,
                                                    null,
                                                    null,
                                                    null,
                                                    TestRecord.class,
                                                    version);

    Map<String,TestRecord> entities = batchRequest.getEntities();

    Assert.assertEquals(entities.size(), 1);
    Assert.assertEquals(entities.get("1"), testRecord);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "versions")
  public void testPrimitiveKeyMultipleEntities(ProtocolVersion version)
  {
    @SuppressWarnings("rawtypes")
    KeyValueRecordFactory<Long, TestRecord> factory = new KeyValueRecordFactory<Long, TestRecord>(Long.class,
                                                                                                  null,
                                                                                                  null,
                                                                                                  null,
                                                                                                  TestRecord.class);
    @SuppressWarnings("rawtypes")
    CollectionRequest<KeyValueRecord> collectionRequest = new CollectionRequest<KeyValueRecord>(KeyValueRecord.class);

    Map<Long, TestRecord> inputs = new HashMap<Long, TestRecord>();
    long[] ids = {1L, 2L, 3L};
    for (long id: ids)
    {
      TestRecord testRecord = buildTestRecord(id, id + "");
      inputs.put(id, testRecord);
      collectionRequest.getElements().add(factory.create(id, testRecord));
    }

    @SuppressWarnings("unchecked")
    BatchRequest<TestRecord> batchRequest =
        CollectionRequestUtil.convertToBatchRequest(collectionRequest,
                                                    Long.class,
                                                    null,
                                                    null,
                                                    null,
                                                    TestRecord.class,
                                                    version);

    Map<String,TestRecord> entities = batchRequest.getEntities();

    Assert.assertEquals(entities.size(), ids.length);
    for (long id: ids)
    {
      Assert.assertEquals(entities.get(id + ""), inputs.get(id));
    }
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "versions")
  public void testCompoundKeyMultipleEntities(ProtocolVersion version)
  {
    String key1 = "key1";
    String key2 = "key2";
    CompoundKey c1 = new CompoundKey().append(key1, 1L).append(key2, 2L);
    CompoundKey c2 = new CompoundKey().append(key1, 3L).append(key2, 4L);
    CompoundKey[] keys = {c1, c2};

    Map<String, CompoundKey.TypeInfo> fieldTypes = new HashMap<String, CompoundKey.TypeInfo>();
    fieldTypes.put(key1, new CompoundKey.TypeInfo(Long.class, Long.class));
    fieldTypes.put(key2, new CompoundKey.TypeInfo(Long.class, Long.class));

    @SuppressWarnings("rawtypes")
    KeyValueRecordFactory<CompoundKey, TestRecord> factory =
        new KeyValueRecordFactory<CompoundKey, TestRecord>(CompoundKey.class,
                                                           null,
                                                           null,
                                                           fieldTypes,
                                                           TestRecord.class);
    @SuppressWarnings("rawtypes")
    CollectionRequest<KeyValueRecord> collectionRequest = new CollectionRequest<KeyValueRecord>(KeyValueRecord.class);

    Map<CompoundKey, TestRecord> inputs = new HashMap<CompoundKey, TestRecord>();
    for (CompoundKey key: keys)
    {
      TestRecord testRecord = buildTestRecord(1L, "message" + key.hashCode());
      inputs.put(key, testRecord);
      collectionRequest.getElements().add(factory.create(key, testRecord));
    }

    @SuppressWarnings("unchecked")
    BatchRequest<TestRecord> batchRequest =
        CollectionRequestUtil.convertToBatchRequest(collectionRequest,
                                                    CompoundKey.class,
                                                    null,
                                                    null,
                                                    fieldTypes,
                                                    TestRecord.class,
                                                    version);

    Map<String,TestRecord> entities = batchRequest.getEntities();

    Assert.assertEquals(entities.size(), inputs.size());
    for (CompoundKey key: keys)
    {
      Assert.assertEquals(entities.get(BatchResponse.keyToString(key, version)), inputs.get(key));
    }
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "versions")
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testComplexKeyMultipleEntities(ProtocolVersion version)
  {
    // TestRecord is not keyed by a ComplexResourceKey, but for this test we pretend that it is.
    TestRecord kk1 = buildTestRecord(1, "key key 1");
    TestRecord kp1 = buildTestRecord(2, "key params 1");
    TestRecord kk2 = buildTestRecord(3, "key key 2");
    TestRecord kp2 = buildTestRecord(4, "key params 2");
    ComplexResourceKey<TestRecord, TestRecord> key1 = new ComplexResourceKey<TestRecord, TestRecord>(kk1, kp1);
    ComplexResourceKey<TestRecord, TestRecord> key2 = new ComplexResourceKey<TestRecord, TestRecord>(kk2, kp2);
    ComplexResourceKey keys[] = {key1, key2};

    KeyValueRecordFactory<ComplexResourceKey, TestRecord> factory =
        new KeyValueRecordFactory<ComplexResourceKey, TestRecord>(ComplexResourceKey.class,
                                     TestRecord.class,
                                     TestRecord.class,
                                     null,
                                     TestRecord.class);

    CollectionRequest<KeyValueRecord> collectionRequest = new CollectionRequest<KeyValueRecord>(KeyValueRecord.class);
    Map<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> inputs =
        new HashMap<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>();
    for (ComplexResourceKey key: keys)
    {
      TestRecord testRecord = buildTestRecord(1L, "foo");
      inputs.put(key, testRecord);
      collectionRequest.getElements().add(factory.create(key, testRecord));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    BatchRequest<TestRecord> batchRequest =
        CollectionRequestUtil.convertToBatchRequest(collectionRequest,
                                                    ComplexResourceKey.class,
                                                    TestRecord.class,
                                                    TestRecord.class,
                                                    null,
                                                    TestRecord.class,
                                                    version);

    Map<String,TestRecord> entities = batchRequest.getEntities();

    Assert.assertEquals(entities.size(), inputs.size());
    for (ComplexResourceKey key: keys)
    {
      Assert.assertEquals(entities.get(URIParamUtils.keyToString(key,
                                                                 URLEscaper.Escaping.NO_ESCAPING,
                                                                 null,
                                                                 true,
                                                                 version)),
                          inputs.get(key));
    }
  }

  private TestRecord buildTestRecord(long id, String message)
  {
    return new TestRecord().setId(id).setMessage(message);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "versions")
  public Object[][] versions()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion() },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion() }
    };
  }
}
