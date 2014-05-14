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

package com.linkedin.restli.client;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.DynamicRecordTemplate;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.client.util.PatchRequestRecorder;
import com.linkedin.restli.common.BatchRequest;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.KeyValueRecordFactory;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.client.CollectionRequestUtil;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.common.URLEscaper;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestClientBuilders
{
  public static final String TEST_URI = "test";
  public static final String SUBRESOURCE_URI = "foo/{key1}/bar/{key2}/baz";
  public static final String SUBRESOURCE_SIMPLE_ROOT_URI = "foo/bar/{key1}/baz";
  public static final String SUBRESOURCE_SIMPLE_SUB_URI = "foo/{key1}/bar";
  private static final ResourceSpec _COLL_SPEC      =
                                                        new ResourceSpecImpl(EnumSet.allOf(ResourceMethod.class),
                                                                             Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                             Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                             Long.class,
                                                                             null,
                                                                             null,
                                                                             TestRecord.class,
                                                                             Collections.<String, Class<?>> emptyMap());

  private static Map<String, Object> keyParts = new HashMap<String, Object>();
  static
  {
    keyParts.put("part1", Long.class);
    keyParts.put("part2", String.class);
  }
  private static final ResourceSpec _ASSOC_SPEC     =
                                                        new ResourceSpecImpl(EnumSet.allOf(ResourceMethod.class),
                                                                             Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                             Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                             CompoundKey.class,
                                                                             null,
                                                                             null,
                                                                             TestRecord.class,
                                                                             keyParts);
  private static final ResourceSpec _COMPLEX_KEY_SPEC =
                                                          new ResourceSpecImpl(EnumSet.allOf(ResourceMethod.class),
                                                                               Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                               Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                               ComplexResourceKey.class,
                                                                               TestRecord.class,
                                                                               TestRecord.class,
                                                                               TestRecord.class,
                                                                               Collections.<String, Class<?>> emptyMap());

  private static final ResourceSpec _SIMPLE_RESOURCE_SPEC =
                                                          new ResourceSpecImpl(RestConstants.SIMPLE_RESOURCE_METHODS,
                                                                             Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                             Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                             null,
                                                                             null,
                                                                             null,
                                                                             TestRecord.class,
                                                                             Collections.<String, Class<?>> emptyMap());

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "action")
  public Object[][] action()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/1?action=action" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/1?action=action" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "action")
  public void testActionRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    FieldDef<String> pParam = new FieldDef<String>("p", String.class, DataTemplateUtil.getSchema(String.class));
    Map<String, DynamicRecordMetadata> requestMetadataMap = new HashMap<String, DynamicRecordMetadata>();
    DynamicRecordMetadata requestMetadata = new DynamicRecordMetadata("action", Collections.<FieldDef<?>>singleton(pParam));
    requestMetadataMap.put("action", requestMetadata);
    DynamicRecordMetadata responseMetadata = new DynamicRecordMetadata("action", Collections.<FieldDef<?>>emptyList());
    Map<String, DynamicRecordMetadata> responseMetadataMap = new HashMap<String, DynamicRecordMetadata>();
    responseMetadataMap.put("action", responseMetadata);
    ResourceSpec resourceSpec = new ResourceSpecImpl(Collections.<ResourceMethod>emptySet(), requestMetadataMap, responseMetadataMap);

    ActionRequestBuilder<Long, TestRecord> builder = new ActionRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                 TestRecord.class,
                                                                                 resourceSpec,
                                                                                 RestliRequestOptions.DEFAULT_OPTIONS);

    ActionRequest<TestRecord> request = builder.name("action").setParam(pParam, "42").id(1L).build();

    DataMap d = new DataMap();
    d.put("p", "42");
    @SuppressWarnings("unchecked")
    DynamicRecordTemplate expectedRecordTemplate =
        new DynamicRecordTemplate(d, DynamicRecordMetadata.buildSchema("action",
                                                                       Arrays.asList(new FieldDef<String>("p",
                                                                                                          String.class,
                                                                                                          DataTemplateUtil.getSchema(String.class)))));

    testUriGeneration(request, expectedUri, version);
    Assert.assertEquals(request.getMethod(), ResourceMethod.ACTION);
    Assert.assertEquals(request.getHeaders(), Collections.<String, String>emptyMap());
    testInput(request, expectedRecordTemplate);
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);
    Assert.assertEquals(request.getResponseDecoder().getEntityClass(), Void.class);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchGetWithProjections")
  public Object[][] batchGetWithProjections()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test?fields=message,id&ids=1&ids=2&ids=3" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test?fields=message,id&ids=List(1,2,3)" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchGetWithProjections")
  public void testBatchGetRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    BatchGetRequestBuilder<Long, TestRecord> builder = new BatchGetRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                     TestRecord.class,
                                                                                     _COLL_SPEC,
                                                                                     RestliRequestOptions.DEFAULT_OPTIONS);
    BatchGetRequest<TestRecord> request = builder.ids(1L, 2L, 3L).fields(TestRecord.fields().id(), TestRecord.fields().message()).build();
    testBaseUriGeneration(request);
    testIdsForBatchRequest(request, new HashSet<Long>(Arrays.asList(1L, 2L, 3L)));
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
            TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedUri, ResourceMethod.BATCH_GET,
                      null, Collections.<String, String>emptyMap(), version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchGetWithEncoding")
  public Object[][] batchGetWithEncoding()
  {
    return new Object[][] {
      // Note double encoding for V1 - one comes from CompoundKey.toString, another - from BatchGetRequestBuilder.ids().
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "test?fields=message,id&ids=ampersand%3D%2526%2526%26equals%3D%253D%253D&ids=ampersand%3D%2526%26equals%3D%253D" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "test?fields=message,id&ids=List((ampersand:%26%26,equals:%3D%3D),(ampersand:%26,equals:%3D))" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchGetWithEncoding")
  public void testBatchGetCompoundKeyRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    BatchGetRequestBuilder<CompoundKey, TestRecord> builder = new BatchGetRequestBuilder<CompoundKey, TestRecord>(TEST_URI,
                                                                                                                  TestRecord.class,
                                                                                                                  _ASSOC_SPEC,
                                                                                                                  RestliRequestOptions.DEFAULT_OPTIONS);
    CompoundKey key1 = new CompoundKey();
    key1.append("equals", "=");
    key1.append("ampersand", "&");
    CompoundKey key2 = new CompoundKey();
    key2.append("equals", "==");
    key2.append("ampersand", "&&");

    BatchGetRequest<TestRecord> request = builder.ids(key1,key2).fields(TestRecord.fields().id(), TestRecord.fields().message()).build();

    testBaseUriGeneration(request);

    // Compare key sets. Note that have to convert keys to Strings as the request internally converts them to string
    HashSet<CompoundKey> expectedIds = new HashSet<CompoundKey>(Arrays.asList(key1, key2));
    testIdsForBatchRequest(request, expectedIds);
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
            TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.BATCH_GET,
                      null,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  private CompoundKey buildCompoundKey()
  {
    return new CompoundKey().append("part1", 1L).append("part2", "2");
  }

  private Map<String, CompoundKey.TypeInfo> getCompoundKeyFieldTypes()
  {
    Map<String, CompoundKey.TypeInfo> fieldTypes = new HashMap<String, CompoundKey.TypeInfo>();
    fieldTypes.put("part1", new CompoundKey.TypeInfo(Long.class, Long.class));
    fieldTypes.put("part2", new CompoundKey.TypeInfo(String.class, String.class));
    return fieldTypes;
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "compoundKey")
  public Object[][] compoundKey()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/part1=1&part2=2" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/(part1:1,part2:2)" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "compoundKey")
  public void testGetCompoundKeyRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    GetRequestBuilder<CompoundKey, TestRecord> builder =
        new GetRequestBuilder<CompoundKey, TestRecord>(TEST_URI, TestRecord.class, _ASSOC_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    CompoundKey key = buildCompoundKey();

    GetRequest<TestRecord> request = builder.id(key).build();

    testBaseUriGeneration(request);
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);
    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.GET,
                      null,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public Object[][] noEntity()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testCreateCompoundKeyRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    CreateRequestBuilder<CompoundKey, TestRecord> builder =
        new CreateRequestBuilder<CompoundKey, TestRecord>(TEST_URI, TestRecord.class, _ASSOC_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    TestRecord record = new TestRecord().setMessage("foo");

    CreateRequest<TestRecord> request = builder.input(record).build();

    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);
    testBaseUriGeneration(request);
    checkBasicRequest(request, expectedUri, ResourceMethod.CREATE, record, Collections.<String, String>emptyMap(),
                      version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "compoundKey")
  public void testUpdateCompoundKeyRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    UpdateRequestBuilder<CompoundKey, TestRecord> builder =
        new UpdateRequestBuilder<CompoundKey, TestRecord>(TEST_URI, TestRecord.class, _ASSOC_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    TestRecord record = new TestRecord().setMessage("foo");

    UpdateRequest<TestRecord> request = builder.id(buildCompoundKey()).input(record).build();

    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);
    testBaseUriGeneration(request);
    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.UPDATE,
                      record,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "compoundKey")
  public void testPartialUpdateCompoundKeyRequestBuilder(ProtocolVersion version, String expectedUri)
      throws CloneNotSupportedException
  {
    PartialUpdateRequestBuilder<CompoundKey, TestRecord> builder =
        new PartialUpdateRequestBuilder<CompoundKey, TestRecord>(TEST_URI, TestRecord.class, _ASSOC_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    TestRecord t1 = new TestRecord();
    TestRecord t2 = new TestRecord(t1.data().copy());
    t2.setId(1L);
    t2.setMessage("Foo Bar Baz");
    PatchRequest<TestRecord> patch = PatchGenerator.diff(t1, t2);

    PartialUpdateRequest<TestRecord> request = builder.id(buildCompoundKey()).input(patch).build();

    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);
    testBaseUriGeneration(request);
    checkBasicRequest(request, expectedUri, ResourceMethod.PARTIAL_UPDATE, patch, Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchCompoundKey")
  public Object[][] batchCompoundKey()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "test?ids=part1%3D1%26part2%3D2&ids=part1%3D11%26part2%3D22" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "test?ids=List((part1:1,part2:2),(part1:11,part2:22))" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchCompoundKey")
  public void testBatchUpdateCompoundKeyRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    BatchUpdateRequestBuilder<CompoundKey, TestRecord> builder =
        new BatchUpdateRequestBuilder<CompoundKey, TestRecord>(TEST_URI, TestRecord.class, _ASSOC_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    Map<CompoundKey, TestRecord> inputs = new HashMap<CompoundKey, TestRecord>();
    CompoundKey key1 = new CompoundKey().append("part1", 1L).append("part2", "2");
    CompoundKey key2 = new CompoundKey().append("part1", 11L).append("part2", "22");
    TestRecord t1 = new TestRecord().setId(1L).setMessage("1");
    TestRecord t2 = new TestRecord().setId(2L);
    inputs.put(key1, t1);
    inputs.put(key2, t2);

    BatchRequest<TestRecord> expectedRequest = new BatchRequest<TestRecord>(new DataMap(), TestRecord.class);
    expectedRequest.getEntities().put(toEntityKey(key1, version), t1);
    expectedRequest.getEntities().put(toEntityKey(key2, version), t2);

    BatchUpdateRequest<CompoundKey, TestRecord> request = builder.inputs(inputs).build();

    testBaseUriGeneration(request);
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    KeyValueRecordFactory<CompoundKey, TestRecord> factory =
        new KeyValueRecordFactory<CompoundKey, TestRecord>(CompoundKey.class,
                                                           null,
                                                           null,
                                                           getCompoundKeyFieldTypes(),
                                                           TestRecord.class);
    @SuppressWarnings({"unchecked","rawtypes"})
    CollectionRequest<KeyValueRecord> collectionRequest = buildCollectionRequest(factory,
                                                                                 new CompoundKey[]{key1, key2},
                                                                                 new TestRecord[]{t1, t2});

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.BATCH_UPDATE,
                      collectionRequest,
                      expectedRequest,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchCompoundKey")
  public void testBatchPartialUpdateCompoundKeyRequestBuilder(ProtocolVersion version, String expectedUri)
      throws CloneNotSupportedException
  {
    BatchPartialUpdateRequestBuilder<CompoundKey, TestRecord> builder =
        new BatchPartialUpdateRequestBuilder<CompoundKey, TestRecord>(TEST_URI, TestRecord.class, _ASSOC_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    Map<CompoundKey, PatchRequest<TestRecord>> inputs = new HashMap<CompoundKey, PatchRequest<TestRecord>>();
    CompoundKey key1 = new CompoundKey().append("part1", 1L).append("part2", "2");
    CompoundKey key2 = new CompoundKey().append("part1", 11L).append("part2", "22");
    TestRecord t1 = new TestRecord().setId(1L).setMessage("1");
    TestRecord t2 = new TestRecord().setId(2L);
    TestRecord t3 = new TestRecord().setMessage("3");
    PatchRequest<TestRecord> patch1 = PatchGenerator.diff(t1, t2);
    PatchRequest<TestRecord> patch2 = PatchGenerator.diff(t2, t3);
    inputs.put(key1, patch1);
    inputs.put(key2, patch2);

    BatchPartialUpdateRequest<CompoundKey, TestRecord> request = builder.inputs(inputs).build();

    testBaseUriGeneration(request);
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    @SuppressWarnings({"unchecked","rawtypes"})
    BatchRequest<PatchRequest<TestRecord>> expectedRequest = new BatchRequest(new DataMap(), PatchRequest.class);
    expectedRequest.getEntities().put(toEntityKey(key1, version), patch1);
    expectedRequest.getEntities().put(toEntityKey(key2, version), patch2);

    @SuppressWarnings({"unchecked","rawtypes"})
    KeyValueRecordFactory<CompoundKey, PatchRequest> factory =
        new KeyValueRecordFactory<CompoundKey, PatchRequest>(CompoundKey.class,
                                                             null,
                                                             null,
                                                             getCompoundKeyFieldTypes(),
                                                             PatchRequest.class);
    @SuppressWarnings({"unchecked","rawtypes"})
    CollectionRequest<KeyValueRecord> collectionRequest = buildCollectionRequest(factory,
                                                                                 new CompoundKey[]{key1, key2},
                                                                                 new PatchRequest[]{patch1, patch2});

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.BATCH_PARTIAL_UPDATE,
                      collectionRequest,
                      expectedRequest,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchGetWithProjections")
  public void testBatchGetRequestBuilderCollectionIds(ProtocolVersion version, String expectedUri)
  {
    BatchGetRequestBuilder<Long, TestRecord> builder = new BatchGetRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                                    TestRecord.class,
                                                                                                    _COLL_SPEC,
                                                                                                    RestliRequestOptions.DEFAULT_OPTIONS);
    List<Long> ids = Arrays.asList(1L, 2L, 3L);
    BatchGetRequest<TestRecord> request = builder.ids(ids).fields(TestRecord.fields().id(), TestRecord.fields().message()).build();
    testBaseUriGeneration(request);
    testIdsForBatchRequest(request, new HashSet<Long>(Arrays.asList(1L, 2L, 3L)));
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
            TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedUri, ResourceMethod.BATCH_GET, null, Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batch")
  public Object[][] batch()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test?ids=1&ids=2&ids=3" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test?ids=List(1,2,3)" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batch")
  public void testBatchUpdateRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    BatchUpdateRequestBuilder<Long, TestRecord> builder =
            new BatchUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    Map<Long, TestRecord> updates = new HashMap<Long, TestRecord>();
    updates.put(1L, new TestRecord());
    updates.put(2L, new TestRecord());
    updates.put(3L, new TestRecord());
    BatchUpdateRequest<Long, TestRecord> request = builder.inputs(updates).build();
    testBaseUriGeneration(request);
    testIdsForBatchRequest(request, new HashSet<Long>(Arrays.asList(1L, 2L, 3L)));
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    BatchRequest<TestRecord> expectedRequest = new BatchRequest<TestRecord>(new DataMap(), TestRecord.class);
    expectedRequest.getEntities().put("1", new TestRecord());
    expectedRequest.getEntities().put("2", new TestRecord());
    expectedRequest.getEntities().put("3", new TestRecord());

    @SuppressWarnings({"unchecked","rawtypes"})
    KeyValueRecordFactory<Long, TestRecord> factory =
        new KeyValueRecordFactory<Long, TestRecord>(Long.class,
                                                    null,
                                                    null,
                                                    null,
                                                    TestRecord.class);
    @SuppressWarnings({"unchecked","rawtypes"})
    CollectionRequest<KeyValueRecord> collectionRequest =
        buildCollectionRequest(factory,
                               new Long[]{1L, 2L, 3L},
                               new TestRecord[]{new TestRecord(), new TestRecord(), new TestRecord()});

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.BATCH_UPDATE,
                      collectionRequest,
                      expectedRequest,
                      Collections.<String, String>emptyMap(), version);
  }

  // need suppress on the method because the more specific suppress isn't being obeyed.
  @SuppressWarnings({"unchecked","rawtypes"})
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batch")
  public void testBatchPartialUpdateRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    BatchPartialUpdateRequestBuilder<Long, TestRecord> builder =
            new BatchPartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    builder.input(1L, new PatchRequest<TestRecord>());
    builder.input(2L, new PatchRequest<TestRecord>());
    builder.input(3L, new PatchRequest<TestRecord>());
    BatchPartialUpdateRequest<Long, TestRecord> request = builder.build();
    testBaseUriGeneration(request);
    testIdsForBatchRequest(request, new HashSet<Long>(Arrays.asList(1L, 2L, 3L)));
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    @SuppressWarnings({"unchecked","rawtypes"})
    BatchRequest<PatchRequest<TestRecord>> expectedRequest = new BatchRequest(new DataMap(), PatchRequest.class);
    expectedRequest.getEntities().put("1", new PatchRequest<TestRecord>());
    expectedRequest.getEntities().put("2", new PatchRequest<TestRecord>());
    expectedRequest.getEntities().put("3", new PatchRequest<TestRecord>());

    KeyValueRecordFactory<Long, PatchRequest> factory =
        new KeyValueRecordFactory<Long, PatchRequest>(Long.class,
                                                      null,
                                                      null,
                                                      null,
                                                      PatchRequest.class);
    CollectionRequest<KeyValueRecord> collectionRequest =
        buildCollectionRequest(factory,
                               new Long[]{1L, 2L, 3L},
                               new PatchRequest[]{new PatchRequest(), new PatchRequest(), new PatchRequest()});

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.BATCH_PARTIAL_UPDATE,
                      collectionRequest,
                      expectedRequest,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batch")
  public void testBatchDeleteRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    BatchDeleteRequestBuilder<Long, TestRecord> builder =
            new BatchDeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    BatchDeleteRequest<Long, TestRecord> request = builder.ids(1L, 2L, 3L).build();
    testBaseUriGeneration(request);
    testIdsForBatchRequest(request, new HashSet<Long>(Arrays.asList(1L, 2L, 3L)));
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedUri, ResourceMethod.BATCH_DELETE, null, Collections.<String, String>emptyMap(),
                      version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testBatchCreateRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    BatchCreateRequestBuilder<Long, TestRecord> builder =
            new BatchCreateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    List<TestRecord> newRecords = Arrays.asList(new TestRecord(), new TestRecord(), new TestRecord());
    BatchCreateRequest<TestRecord> request = builder.inputs(newRecords).build();
    testBaseUriGeneration(request);
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    CollectionRequest<TestRecord> expectedRequest = new CollectionRequest<TestRecord>(new DataMap(), TestRecord.class);
    expectedRequest.getElements().addAll(newRecords);
    checkBasicRequest(request, expectedUri, ResourceMethod.BATCH_CREATE,
                      expectedRequest,
                      Collections.<String, String>emptyMap(), version);
  }


  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testCreateRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    CreateRequestBuilder<Long, TestRecord> builder = new CreateRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                                TestRecord.class,
                                                                                                _COLL_SPEC,
                                                                                                RestliRequestOptions.DEFAULT_OPTIONS);
    CreateRequest<TestRecord> request = builder.input(new TestRecord()).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    checkBasicRequest(request, expectedUri, ResourceMethod.CREATE, new TestRecord(), Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "singleEntity")
  public Object[][] singleEntity()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/1" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/1" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "singleEntity")
  public void testDeleteRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    DeleteRequestBuilder<Long, TestRecord> builder = new DeleteRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                                TestRecord.class,
                                                                                                _COLL_SPEC,
                                                                                                RestliRequestOptions.DEFAULT_OPTIONS);
    DeleteRequest<TestRecord> request = builder.id(1L).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedUri, ResourceMethod.DELETE, null, Collections.<String, String>emptyMap(),
                      version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testDeleteRequestBuilderWithKeylessResource(ProtocolVersion version, String expectedUri)
  {
    DeleteRequestBuilder<Long, TestRecord> builder = new DeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class,
                                                                                                _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    DeleteRequest<TestRecord> request = builder.build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedUri, ResourceMethod.DELETE, null, Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "search1")
  public Object[][] search1()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "test/key=a%3Ab?count=4&fields=message,id&p=42&q=search&start=1" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "test/(key:a%3Ab)?count=4&fields=message,id&p=42&q=search&start=1" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "search1")
  public void testFindRequestBuilder1(ProtocolVersion version, String expectedUri)
  {
    FindRequestBuilder<Long, TestRecord> builder = new FindRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                            TestRecord.class,
                                                                                            _COLL_SPEC,
                                                                                            RestliRequestOptions.DEFAULT_OPTIONS);
    FindRequest<TestRecord> request =
        builder.name("search")
               .assocKey("key", "a:b")
               .paginate(1, 4)
               .fields(TestRecord.fields().id(), TestRecord.fields().message())
               .setParam("p", 42)
               .build();
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.FINDER,
                      null,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "search2")
  public Object[][] search2()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/key=a%3Ab?p=42&q=search&start=1" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/(key:a%3Ab)?p=42&q=search&start=1" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "search2")
  public void testFindRequestBuilder2(ProtocolVersion version, String expectedUri)
  {
    FindRequestBuilder<Long, TestRecord> builder = new FindRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                            TestRecord.class,
                                                                                            _COLL_SPEC,
                                                                                            RestliRequestOptions.DEFAULT_OPTIONS);
    FindRequest<TestRecord> request = builder.name("search")
      .assocKey("key", "a:b")
      .paginateStart(1)
      .setParam("p", 42)
      .build();
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.FINDER,
                      null,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "search3")
  public Object[][] search3()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/key=a%3Ab?count=4&p=42&q=search" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/(key:a%3Ab)?count=4&p=42&q=search" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "search3")
  public void testFindRequestBuilder3(ProtocolVersion version, String expectedUri)
  {
    FindRequestBuilder<Long, TestRecord> builder = new FindRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                            TestRecord.class,
                                                                                            _COLL_SPEC,
                                                                                            RestliRequestOptions.DEFAULT_OPTIONS);
    FindRequest<TestRecord> request = builder.name("search")
      .assocKey("key", "a:b")
      .paginateCount(4)
      .setParam("p", 42)
      .build();
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.FINDER,
                      null,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getAll1")
  public Object[][] getAll1()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test?count=4&fields=message,id&start=1" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test?count=4&fields=message,id&start=1" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getAll1")
  public void testGetAllRequestBuilder1(ProtocolVersion version, String expectedUri)
  {
    GetAllRequestBuilder<Long, TestRecord> builder =
        new GetAllRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    GetAllRequest<TestRecord> request =
        builder.paginate(1, 4)
               .fields(TestRecord.fields().id(), TestRecord.fields().message())
               .build();
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);
    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.GET_ALL,
                      null,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getAll2")
  public Object[][] getAll2()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test?start=1" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test?start=1" }
    };
  }

  @Test(dataProvider =  TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getAll2")
  public void testGetAllRequestBuilder2(ProtocolVersion version, String expectedUri)
  {
    GetAllRequestBuilder<Long, TestRecord> builder =
        new GetAllRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    GetAllRequest<TestRecord> request = builder.paginateStart(1).build();
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);
    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.GET_ALL,
                      null,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getAll3")
  public Object[][] getAll3()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test?count=4" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test?count=4" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getAll3")
  public void testGetAllRequestBuilder3(ProtocolVersion version, String expectedUri)
  {
    GetAllRequestBuilder<Long, TestRecord> builder =
        new GetAllRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    GetAllRequest<TestRecord> request = builder.paginateCount(4).build();
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);
    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.GET_ALL,
                      null,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getWithProjection")
  public Object[][] getWithProjection()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/1?fields=message,id" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/1?fields=message,id" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getWithProjection")
  public void testGetRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    GetRequestBuilder<Long, TestRecord> builder = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    GetRequest<TestRecord> request = builder.id(1L).fields(TestRecord.fields().id(), TestRecord.fields().message()).build();
    testBaseUriGeneration(request);
    testIdForGetRequest(request, new Long(1L));
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
            TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedUri, ResourceMethod.GET, null, Collections.<String, String>emptyMap(), version);
  }

  @Test
  public void testRestliRequestOptionsDefault()
  {
    GetRequestBuilder<Long, TestRecord> builder = new GetRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                          TestRecord.class,
                                                                                          _COLL_SPEC,
                                                                                          RestliRequestOptions.DEFAULT_OPTIONS);
    Assert.assertEquals(builder.id(1L).build().getRequestOptions(), RestliRequestOptions.DEFAULT_OPTIONS);
  }

  @Test
  public void testRestliRequestOptionsOverride()
  {
    GetRequestBuilder<Long, TestRecord> builder = new GetRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                          TestRecord.class,
                                                                                          _COLL_SPEC,
                                                                                          RestliRequestOptions.DEFAULT_OPTIONS);
    RestliRequestOptions overrideOptions =
        new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.FORCE_USE_NEXT).build();
    Assert.assertEquals(builder.id(1L).setRequestOptions(overrideOptions).build().getRequestOptions(), overrideOptions);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getOnKeyless")
  public Object[][] getOnKeyless()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test?fields=message,id" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test?fields=message,id" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getOnKeyless")
  public void testGetRequestBuilderWithKeylessResource(ProtocolVersion version, String expectedUri)
  {
    GetRequestBuilder<Void, TestRecord> builder = new GetRequestBuilder<Void, TestRecord>(TEST_URI,
                                                                                          TestRecord.class,
                                                                                          _SIMPLE_RESOURCE_SPEC,
                                                                                          RestliRequestOptions.DEFAULT_OPTIONS);
    GetRequest<TestRecord> request = builder.fields(TestRecord.fields().id(), TestRecord.fields().message()).build();
    testBaseUriGeneration(request);
    testIdForGetRequest(request, null);
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
        TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedUri, ResourceMethod.GET, null, Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "builderParam")
  public Object[][] builderParam()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "test/1?arrayKey1=3&arrayKey1=4&arrayKey1=5&arrayKey2=3&arrayKey2=4&arrayKey2=5&simpleKey=2" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "test/1?arrayKey1=List(3,4,5)&arrayKey2=List(3,4,5)&simpleKey=2" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "builderParam")
  public void testBuilderParam(ProtocolVersion version, String expectedUri)
  {
    final GetRequestBuilder<Long, TestRecord> builder = new GetRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                                TestRecord.class,
                                                                                                _COLL_SPEC,
                                                                                                RestliRequestOptions.DEFAULT_OPTIONS);
    final Collection<Integer> coll = Arrays.asList(3, 4, 5);
    final IntegerArray array = new IntegerArray(coll);
    final GetRequest<TestRecord> request = builder
                                          .id(1L)
                                          .setReqParam("simpleKey", 2)
                                          .setParam("arrayKey1", coll)
                                          .setParam("arrayKey2", array)
                                          .build();
    testUriGeneration(request, expectedUri, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "singleEntity")
  public void testPartialUpdateRequestBuilder(ProtocolVersion version, String expectedUri) throws Exception
  {
    PartialUpdateRequestBuilder<Long, TestRecord> builder = new PartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                                              TestRecord.class,
                                                                                                              _COLL_SPEC,
                                                                                                              RestliRequestOptions.DEFAULT_OPTIONS);
    TestRecord t1 = new TestRecord();
    TestRecord t2 = new TestRecord(t1.data().copy());
    t2.setId(1L);
    t2.setMessage("Foo Bar Baz");
    PatchRequest<TestRecord> patch = PatchGenerator.diff(t1, t2);
    PartialUpdateRequest<TestRecord> request = builder.id(1L).input(patch).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.PARTIAL_UPDATE,
                      patch,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testPartialUpdateRequestBuilderWithKeylessResource(ProtocolVersion version, String expectedUri) throws Exception
  {
    PartialUpdateRequestBuilder<Long, TestRecord> builder = new PartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                                              TestRecord.class,
                                                                                                              _SIMPLE_RESOURCE_SPEC,
                                                                                                              RestliRequestOptions.DEFAULT_OPTIONS);
    TestRecord t1 = new TestRecord();
    TestRecord t2 = new TestRecord(t1.data().copy());
    t2.setId(1L);
    t2.setMessage("Foo Bar Baz");
    PatchRequest<TestRecord> patch = PatchGenerator.diff(t1, t2);
    PartialUpdateRequest<TestRecord> request = builder.input(patch).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.PARTIAL_UPDATE,
                      patch,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @Test
  public void testPatchGenerateAndPatchRequestRecorderGenerateIdenticalPatches()
      throws CloneNotSupportedException
  {
    TestRecord t1 = new TestRecord();
    TestRecord t2 = new TestRecord(t1.data().copy());
    t2.setId(1L);
    t2.setMessage("Foo Bar Baz");
    PatchRequest<TestRecord> patchFromGenerator = PatchGenerator.diff(t1, t2);

    PatchRequestRecorder<TestRecord> patchRecorder = new PatchRequestRecorder<TestRecord>(TestRecord.class);
    patchRecorder.getRecordingProxy().setId(1L).setMessage("Foo Bar Baz");
    PatchRequest<TestRecord> patchFromRecorder = patchRecorder.generatePatchRequest();

    Assert.assertEquals(patchFromRecorder.getPatchDocument(), patchFromGenerator.getPatchDocument());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "singleEntity")
  public void testUpdateRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    UpdateRequestBuilder<Long, TestRecord> builder = new UpdateRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                                TestRecord.class,
                                                                                                _COLL_SPEC,
                                                                                                RestliRequestOptions.DEFAULT_OPTIONS);
    UpdateRequest<TestRecord> request = builder.id(1L).input(new TestRecord()).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.UPDATE,
                      new TestRecord(),
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testUpdateRequestBuilderWithKeylessResource(ProtocolVersion version, String expectedUri)
  {
    UpdateRequestBuilder<Void, TestRecord> builder = new UpdateRequestBuilder<Void, TestRecord>(TEST_URI,
                                                                                                TestRecord.class,
                                                                                                _SIMPLE_RESOURCE_SPEC,
                                                                                                RestliRequestOptions.DEFAULT_OPTIONS);
    UpdateRequest<TestRecord> request = builder.input(new TestRecord()).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.UPDATE,
                      new TestRecord(),
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "complexKeyAndParam")
  public Object[][] complexKeyAndParam()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/$params.id=10&$params.message=ParamMessage&id=1&message=KeyMessage?testParam.id=123&testParam.message=ParamMessage" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/($params:(id:10,message:ParamMessage),id:1,message:KeyMessage)?testParam=(id:123,message:ParamMessage)" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "complexKeyAndParam")
  public void testComplexKeyGetRequestBuilder(ProtocolVersion version, String expectedUri) throws Exception
  {
    GetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new GetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                      TestRecord.class,
                                                                                      _COMPLEX_KEY_SPEC,
                                                                                      RestliRequestOptions.DEFAULT_OPTIONS);
    ComplexResourceKey<TestRecord, TestRecord> id =
        buildComplexKey(1L, "KeyMessage", 10L, "ParamMessage");
    RecordTemplate param1 = buildComplexParam(123, "ParamMessage");

    GetRequest<TestRecord> request = builder.id(id).setParam("testParam", param1).build();
    Assert.assertTrue(request.isSafe());
    Assert.assertTrue(request.isIdempotent());
    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.GET,
                      null,
                      Collections.<String, String> emptyMap(),
                      version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "complexKeyAndParam")
  public void testComplexKeyDeleteRequestBuilder(ProtocolVersion version, String expectedUri) throws Exception
  {
    DeleteRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new DeleteRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                         TestRecord.class,
                                                                                         _COMPLEX_KEY_SPEC,
                                                                                         RestliRequestOptions.DEFAULT_OPTIONS);
    ComplexResourceKey<TestRecord, TestRecord> id =
        buildComplexKey(1L, "KeyMessage", 10L, "ParamMessage");
    RecordTemplate param = buildComplexParam(123, "ParamMessage");

    DeleteRequest<TestRecord> request = builder.id(id).setParam("testParam", param).build();
    Assert.assertFalse(request.isSafe());
    Assert.assertTrue(request.isIdempotent());
    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.DELETE,
                      null,
                      Collections.<String, String> emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchComplexKeyAndParam")
  public Object[][] batchComplexKeyAndParam()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "test?ids%5B0%5D.$params.id=20&ids%5B0%5D.$params.message=ParamMessage2&ids%5B0%5D.id=2&ids%5B0%5D.message=KeyMessage2&ids%5B1%5D.$params.id=10&ids%5B1%5D.$params.message=ParamMessage1&ids%5B1%5D.id=1&ids%5B1%5D.message=KeyMessage1&testParam.id=123&testParam.message=ParamMessage" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "test?ids=List(($params:(id:20,message:ParamMessage2),id:2,message:KeyMessage2),($params:(id:10,message:ParamMessage1),id:1,message:KeyMessage1))&testParam=(id:123,message:ParamMessage)" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchComplexKeyAndParam")
  public void testComplexKeyBatchGetRequestBuilder(ProtocolVersion version, String expectedUri) throws Exception
  {
    BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                           TestRecord.class,
                                                                                           _COMPLEX_KEY_SPEC,
                                                                                           RestliRequestOptions.DEFAULT_OPTIONS);
    ComplexResourceKey<TestRecord, TestRecord> id1 =
        buildComplexKey(1L, "KeyMessage1", 10L, "ParamMessage1");
    ComplexResourceKey<TestRecord, TestRecord> id2 =
        buildComplexKey(2L, "KeyMessage2", 20L, "ParamMessage2");
    RecordTemplate param = buildComplexParam(123, "ParamMessage");

    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> request = builder.ids(id1, id2).setParam("testParam", param).build();
    Assert.assertTrue(request.isIdempotent());
    Assert.assertTrue(request.isSafe());
    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.BATCH_GET,
                      null,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @SuppressWarnings("deprecation")
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchComplexKeyAndParam")
  public void testComplexKeyBatchUpdateRequestBuilder(ProtocolVersion version, String expectedUri) throws Exception
  {
    BatchUpdateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new BatchUpdateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                              TestRecord.class,
                                                                                              _COMPLEX_KEY_SPEC,
                                                                                              RestliRequestOptions.DEFAULT_OPTIONS);
    ComplexResourceKey<TestRecord, TestRecord> id1 =
        buildComplexKey(1L, "KeyMessage1", 10L, "ParamMessage1");
    ComplexResourceKey<TestRecord, TestRecord> id2 =
        buildComplexKey(2L, "KeyMessage2", 20L, "ParamMessage2");
    RecordTemplate param = buildComplexParam(123, "ParamMessage");
    TestRecord t1 = new TestRecord().setMessage("foo");
    TestRecord t2 = new TestRecord().setMessage("bar");

    BatchUpdateRequest<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> request =
        builder.input(id1, t1).input(id2, t2).setParam("testParam", param).build();

    // using toStringFull (which is deprecated) because this is only used to check v1
    BatchRequest<TestRecord> expectedRequest = new BatchRequest<TestRecord>(new DataMap(), TestRecord.class);
    expectedRequest.getEntities().put(toEntityKey(id1, version), t1);
    expectedRequest.getEntities().put(toEntityKey(id2, version), t2);

    testBaseUriGeneration(request);
    Assert.assertTrue(request.isIdempotent());
    Assert.assertFalse(request.isSafe());

    @SuppressWarnings({"unchecked","rawtypes"})
    KeyValueRecordFactory<ComplexResourceKey, TestRecord> factory =
        new KeyValueRecordFactory<ComplexResourceKey, TestRecord>(ComplexResourceKey.class,
                                                                  TestRecord.class,
                                                                  TestRecord.class,
                                                                  null,
                                                                  TestRecord.class);
    @SuppressWarnings({"unchecked","rawtypes"})
    CollectionRequest<KeyValueRecord> collectionRequest = buildCollectionRequest(factory,
                                                                                 new ComplexResourceKey[]{id1, id2},
                                                                                 new TestRecord[]{t1, t2});

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.BATCH_UPDATE,
                      collectionRequest,
                      expectedRequest,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "complexKey")
  public Object[][] complexKey()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/$params.id=2&$params.message=paramMessage&id=1&message=keyMessage" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/($params:(id:2,message:paramMessage),id:1,message:keyMessage)" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "complexKey")
  public void testComplexKeyUpdateRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    UpdateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new UpdateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                         TestRecord.class,
                                                                                         _COMPLEX_KEY_SPEC,
                                                                                         RestliRequestOptions.DEFAULT_OPTIONS);
    ComplexResourceKey<TestRecord, TestRecord> key = buildComplexKey(1L, "keyMessage", 2L, "paramMessage");

    UpdateRequest<TestRecord> request = builder.id(key).input(new TestRecord()).build();

    testBaseUriGeneration(request);
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);
    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.UPDATE,
                      new TestRecord(),
                      Collections.<String, String> emptyMap(),
                      version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testComplexKeyCreateRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    CreateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new CreateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                         TestRecord.class,
                                                                                         _COMPLEX_KEY_SPEC,
                                                                                         RestliRequestOptions.DEFAULT_OPTIONS);
    CreateRequest<TestRecord> request = builder.input(new TestRecord()).build();

    testBaseUriGeneration(request);
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);
    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.CREATE,
                      new TestRecord(),
                      Collections.<String, String> emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchComplexKey")
  public Object[][] batchComplexKey()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "test?ids%5B0%5D.$params.id=2&ids%5B0%5D.$params.message=paramMessage1&ids%5B0%5D.id=1&ids%5B0%5D.message=keyMessage1&ids%5B1%5D.$params.id=4&ids%5B1%5D.$params.message=paramMessage2&ids%5B1%5D.id=3&ids%5B1%5D.message=keyMessage2" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "test?ids=List(($params:(id:2,message:paramMessage1),id:1,message:keyMessage1),($params:(id:4,message:paramMessage2),id:3,message:keyMessage2))" }
    };
  }

  @SuppressWarnings("deprecation")
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchComplexKey")
  public void testComplexKeyBatchPartialUpdateRequestBuilder(ProtocolVersion version, String expectedUri)
  {
    BatchPartialUpdateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new BatchPartialUpdateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                                     TestRecord.class,
                                                                                                     _COMPLEX_KEY_SPEC,
                                                                                                     RestliRequestOptions.DEFAULT_OPTIONS);
    Map<ComplexResourceKey<TestRecord, TestRecord>, PatchRequest<TestRecord>> inputs =
        new HashMap<ComplexResourceKey<TestRecord, TestRecord>, PatchRequest<TestRecord>>();
    ComplexResourceKey<TestRecord, TestRecord> key1 = buildComplexKey(1L, "keyMessage1", 2L, "paramMessage1");
    ComplexResourceKey<TestRecord, TestRecord> key2 = buildComplexKey(3L, "keyMessage2", 4L, "paramMessage2");
    TestRecord t1 = new TestRecord().setId(1L);
    TestRecord t2 = new TestRecord().setId(2L).setMessage("foo");
    TestRecord t3 = new TestRecord().setMessage("bar");
    PatchRequest<TestRecord> patch1 = PatchGenerator.diff(t1, t2);
    PatchRequest<TestRecord> patch2 = PatchGenerator.diff(t2, t3);
    inputs.put(key1, patch1);
    inputs.put(key2, patch2);

    BatchPartialUpdateRequest<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> request =
        builder.inputs(inputs).build();

    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);
    testBaseUriGeneration(request);

    // using .toStringFull (which is deprecated) because this is only used for checking v1
    @SuppressWarnings({"unchecked","rawtypes"})
    BatchRequest<PatchRequest<TestRecord>> batchRequest = new BatchRequest(new DataMap(), PatchRequest.class);
    batchRequest.getEntities().put(toEntityKey(key1, version), patch1);
    batchRequest.getEntities().put(toEntityKey(key2, version), patch2);

    @SuppressWarnings({"unchecked","rawtypes"})
    KeyValueRecordFactory<ComplexResourceKey, PatchRequest> factory =
        new KeyValueRecordFactory<ComplexResourceKey, PatchRequest>(ComplexResourceKey.class,
                                                                    TestRecord.class,
                                                                    TestRecord.class,
                                                                    null,
                                                                    PatchRequest.class);
    @SuppressWarnings({"unchecked","rawtypes"})
    CollectionRequest<KeyValueRecord> collectionRequest = buildCollectionRequest(factory,
                                                                                 new ComplexResourceKey[]{key1, key2},
                                                                                 new PatchRequest[]{patch1, patch2});

    checkBasicRequest(request,
                      expectedUri,
                      ResourceMethod.BATCH_PARTIAL_UPDATE,
                      collectionRequest,
                      batchRequest,
                      Collections.<String, String>emptyMap(),
                      version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceAction1")
  public Object[][] subSubResourceAction1()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/1/bar/2/baz?action=action" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/1/bar/2/baz?action=action" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceAction1")
  public void testBuilderPathKeys1(ProtocolVersion version, String expectedUri)
  {
    List<FieldDef<?>> fieldDefs = new ArrayList<FieldDef<?>>();
    fieldDefs.add(new FieldDef<Integer>("key1", Integer.class, DataTemplateUtil.getSchema(Integer.class)));
    fieldDefs.add(new FieldDef<Integer>("key2", Integer.class, DataTemplateUtil.getSchema(Integer.class)));
    DynamicRecordMetadata requestMetadata = new DynamicRecordMetadata("action", fieldDefs);
    Map<String, DynamicRecordMetadata> requestMetadataMap = new HashMap<String, DynamicRecordMetadata>();
    requestMetadataMap.put("action", requestMetadata);
    DynamicRecordMetadata responseMetadata = new DynamicRecordMetadata("action", Collections.<FieldDef<?>>emptyList());
    Map<String, DynamicRecordMetadata> responseMetadataMap = new HashMap<String, DynamicRecordMetadata>();
    responseMetadataMap.put("action", responseMetadata);
    ResourceSpec resourceSpec = getResourceSpecForBuilderPathKeys();
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};

    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<TestRecord> request = new ActionRequestBuilder<Void, TestRecord>(SUBRESOURCE_URI, TestRecord.class, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS)
      .name("action").pathKey("key1", 1).pathKey("key2", 2).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_URI, expectedPathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceAction2")
  public Object[][] subSubResourceAction2()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "foo/http%3A%2F%2Fexample.com%2Fimages%2F1.png/bar/http%3A%2F%2Fexample.com%2Fimages%2F2.png/baz?action=action" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "foo/http%3A%2F%2Fexample.com%2Fimages%2F1.png/bar/http%3A%2F%2Fexample.com%2Fimages%2F2.png/baz?action=action" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceAction2")
  public void testBuilderPathKeys2(ProtocolVersion version, String expectedUri)
  {
    ResourceSpec resourceSpec = getResourceSpecForBuilderPathKeys();
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};

    // test with keys containing URL escaped chars
    Request<TestRecord> request = new ActionRequestBuilder<Void, TestRecord>(SUBRESOURCE_URI, TestRecord.class, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS)
      .name("action").pathKey("key1", "http://example.com/images/1.png").pathKey("key2", "http://example.com/images/2.png").build();
    testUriGeneration(request,
                      expectedUri,
                      version);
    Map<String, String> pathKeys1 = new HashMap<String, String>();
    pathKeys1.put("key1", "http://example.com/images/1.png");
    pathKeys1.put("key2", "http://example.com/images/2.png");
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_URI, pathKeys1);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceBatch")
  public Object[][] subSubResourceBatch()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/1/bar/2/baz?ids=1&ids=2" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/1/bar/2/baz?ids=List(1,2)" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceBatch")
  public void testBuilderPathKeys3(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<BatchResponse<TestRecord>> request = new BatchGetRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .ids(1L, 2L).pathKey("key1", 1).pathKey("key2", 2).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_URI, expectedPathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceNoEntity")
  public Object[][] subSubResourceNoEntity()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/1/bar/2/baz" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/1/bar/2/baz" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceNoEntity")
  public void testBuilderPathKeys4(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<EmptyRecord> request = new CreateRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .pathKey("key1", 1).pathKey("key2", 2).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_URI, expectedPathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceSingleEntity")
  public Object[][] subSubResourceSingleEntity()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/1/bar/2/baz/3" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/1/bar/2/baz/3" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceSingleEntity")
  public void testBuilderPathKeys5(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<EmptyRecord> request = new DeleteRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(3L).pathKey("key1", 1).pathKey("key2", 2).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_URI, expectedPathKeys);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceNoEntity")
  public void testBuilderPathKeys6(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<CollectionResponse<TestRecord>> request = new FindRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .pathKey("key1", 1).pathKey("key2", 2).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_URI, expectedPathKeys);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceSingleEntity")
  public void testBuilderPathKeys7(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(3L).pathKey("key1", 1).pathKey("key2", 2).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_URI, expectedPathKeys);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceSingleEntity")
  public void testBuilderPathKeys8(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<EmptyRecord> request = new PartialUpdateRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(3L).pathKey("key1", 1).pathKey("key2", 2).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_URI, expectedPathKeys);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceSingleEntity")
  public void testBuilderPathKeys9(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<EmptyRecord> request = new UpdateRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(3L).pathKey("key1", 1).pathKey("key2", 2).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_URI, expectedPathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceAction1")
  public Object[][] subResourceAction1()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/bar/1/baz?action=action" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/bar/1/baz?action=action" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceAction1")
  public void testBuilderPathKeys10(ProtocolVersion version, String expectedUri)
  {
    ResourceSpec resourceSpec = getResourceSpecForBuilderPathKeys();
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};

    // simple resource & sub resources tests
    Request<TestRecord> request = new ActionRequestBuilder<Void, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS)
      .name("action").pathKey("key1", 1).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceAction2")
  public Object[][] subResourceAction2()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/bar/http%3A%2F%2Fexample.com%2Fimages%2F1.png/baz?action=action" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/bar/http%3A%2F%2Fexample.com%2Fimages%2F1.png/baz?action=action" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceAction2")
  public void testBuilderPathKeys11(ProtocolVersion version, String expectedUri)
  {
    ResourceSpec resourceSpec = getResourceSpecForBuilderPathKeys();
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};

    Request<TestRecord> request = new ActionRequestBuilder<Void, TestRecord>(
      SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS).name("action")
      .pathKey("key1", "http://example.com/images/1.png")
      .build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request,
                 expectedResourcePath,
                 SUBRESOURCE_SIMPLE_ROOT_URI,
                 Collections.singletonMap("key1", "http://example.com/images/1.png"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceBatch")
  public Object[][] subResourceBatch()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/bar/1/baz?ids=1&ids=2" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/bar/1/baz?ids=List(1,2)" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceBatch")
  public void testBuilderPathKeys12(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};

    Request<BatchResponse<TestRecord>> request = new BatchGetRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .ids(1L, 2L).pathKey("key1", 1).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceNoEntity")
  public Object[][] subResourceNoEntity()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/bar/1/baz" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/bar/1/baz" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceNoEntity")
  public void testBuilderPathKeys13(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};

    Request<EmptyRecord> request = new CreateRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .pathKey("key1", 1).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceSingleEntity")
  public Object[][] subResourceSingleEntity()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/bar/1/baz/2" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/bar/1/baz/2" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceSingleEntity")
  public void testBuilderPathKeys14(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};

    Request<EmptyRecord> request = new DeleteRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(2L).pathKey("key1", 1).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceNoEntity")
  public void testBuilderPathKeys15(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};

    Request<CollectionResponse<TestRecord>> request = new FindRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .pathKey("key1", 1).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceSingleEntity")
  public void testBuilderPathKeys16(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};

    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(2L).pathKey("key1", 1).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceSingleEntity")
  public void testBuilderPathKeys17(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};

    Request<EmptyRecord> request = new PartialUpdateRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(2L).pathKey("key1", 1).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceSingleEntity")
  public void testBuilderPathKeys18(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar", "baz"};

    Request<EmptyRecord> request = new UpdateRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(2L).pathKey("key1", 1).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceAction1")
  public Object[][] simpleSubResourceAction1()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/1/bar?action=action" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/1/bar?action=action" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceAction1")
  public void testBuilderPathKeys19(ProtocolVersion version, String expectedUri)
  {
    ResourceSpec resourceSpec = getResourceSpecForBuilderPathKeys();
    String[] expectedResourcePath = new String[] {"foo", "bar"};

    Request<TestRecord> request = new ActionRequestBuilder<Void, TestRecord>(SUBRESOURCE_SIMPLE_SUB_URI, TestRecord.class, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS)
      .name("action").pathKey("key1", 1).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_SIMPLE_SUB_URI, Collections.singletonMap("key1", 1));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceAction2")
  public Object[][] simpleSubResourceAction2()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/http%3A%2F%2Fexample.com%2Fimages%2F1.png/bar?action=action" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/http%3A%2F%2Fexample.com%2Fimages%2F1.png/bar?action=action" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceAction2")
  public void testBuilderPathKeys20(ProtocolVersion version, String expectedUri)
  {
    ResourceSpec resourceSpec = getResourceSpecForBuilderPathKeys();
    String[] expectedResourcePath = new String[] {"foo", "bar"};

    Request<TestRecord> request = new ActionRequestBuilder<Void, TestRecord>(
      SUBRESOURCE_SIMPLE_SUB_URI, TestRecord.class, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS).name("action")
      .pathKey("key1", "http://example.com/images/1.png")
      .build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request,
                 expectedResourcePath,
                 SUBRESOURCE_SIMPLE_SUB_URI,
                 Collections.singletonMap("key1", "http://example.com/images/1.png"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceNoEntity")
  public Object[][] simpleSubResourceNoEntity()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/1/bar" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/1/bar" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceNoEntity")
  public void testBuilderPathKeys21(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar"};

    Request<EmptyRecord> request = new DeleteRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_SUB_URI, TestRecord.class,
                                                         _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS).pathKey("key1", 1).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_SIMPLE_SUB_URI, Collections.singletonMap("key1", 1));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceNoEntity")
  public void testBuilderPathKeys22(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar"};

    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_SUB_URI, TestRecord.class, _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .pathKey("key1", 1).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_SIMPLE_SUB_URI, Collections.singletonMap("key1", 1));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceNoEntity")
  public void testBuilderPathKeys23(ProtocolVersion version, String expectedUri)
  {
    String[] expectedResourcePath = new String[] {"foo", "bar"};

    Request<EmptyRecord> request = new UpdateRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_SUB_URI, TestRecord.class,
                                                         _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS).pathKey("key1", 1).build();
    testUriGeneration(request, expectedUri, version);
    testPathKeys(request, expectedResourcePath, SUBRESOURCE_SIMPLE_SUB_URI, Collections.singletonMap("key1", 1));
  }

  private ResourceSpec getResourceSpecForBuilderPathKeys()
  {
    List<FieldDef<?>> fieldDefs = new ArrayList<FieldDef<?>>();
    fieldDefs.add(new FieldDef<Integer>("key1", Integer.class, DataTemplateUtil.getSchema(
      Integer.class)));
    fieldDefs.add(new FieldDef<Integer>("key2", Integer.class, DataTemplateUtil.getSchema(Integer.class)));
    DynamicRecordMetadata requestMetadata = new DynamicRecordMetadata("action", fieldDefs);
    Map<String, DynamicRecordMetadata> requestMetadataMap = new HashMap<String, DynamicRecordMetadata>();
    requestMetadataMap.put("action", requestMetadata);
    DynamicRecordMetadata responseMetadata = new DynamicRecordMetadata("action", Collections.<FieldDef<?>>emptyList());
    Map<String, DynamicRecordMetadata> responseMetadataMap = new HashMap<String, DynamicRecordMetadata>();
    responseMetadataMap.put("action", responseMetadata);
    return new ResourceSpecImpl(Collections.<ResourceMethod>emptySet(), requestMetadataMap, responseMetadataMap);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public Object[][] noEntityWithParam()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test?foo=bar" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test?foo=bar" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public void testCrudBuilderParams1(ProtocolVersion version, String expectedUri)
  {
    Request<EmptyRecord> request = new CreateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "entityWithParam")
  public Object[][] entityWithParam()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/3?foo=bar" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/3?foo=bar" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "entityWithParam")
  public void testCrudBuilderParams2(ProtocolVersion version, String expectedUri)
  {
    Request<EmptyRecord> request = new DeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public void testCrudBuilderParams3(ProtocolVersion version, String expectedUri)
  {
    Request<CollectionResponse<TestRecord>> request = new FindRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "entityWithParam")
  public void testCrudBuilderParams4(ProtocolVersion version, String expectedUri)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "entityWithParam")
  public void testCrudBuilderParams5(ProtocolVersion version, String expectedUri)
  {
    Request<EmptyRecord> request = new PartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "entityWithParam")
  public void testCrudBuilderParams6(ProtocolVersion version, String expectedUri)
  {
    Request<EmptyRecord> request = new UpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchWithParam")
  public Object[][] batchWithParam()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test?foo=bar&ids=1&ids=2" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test?foo=bar&ids=List(1,2)" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchWithParam")
  public void testCrudBuilderParams7(ProtocolVersion version, String expectedUri)
  {
    Request<BatchResponse<TestRecord>> request = new BatchGetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .ids(1L, 2L).setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public void testCrudBuilderParams8(ProtocolVersion version, String expectedUri)
  {
    Request<CollectionResponse<CreateStatus>> request = new BatchCreateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .input(new TestRecord()).setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchWithParam")
  public void testCrudBuilderParams9(ProtocolVersion version, String expectedUri)
  {
    Request<BatchKVResponse<Long, UpdateStatus>>  request = new BatchDeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .ids(1L, 2L).setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchSingleWithParam")
  public Object[][] batchSingleWithParam()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test?foo=bar&ids=1" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test?foo=bar&ids=List(1)" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchSingleWithParam")
  public void testCrudBuilderParams10(ProtocolVersion version, String expectedUri)
  {
    Request<BatchKVResponse<Long, UpdateStatus>> request = new BatchUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .input(1L, new TestRecord()).setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchSingleWithParam")
  public void testCrudBuilderParams11(ProtocolVersion version, String expectedUri)
  {
    Request<BatchKVResponse<Long, UpdateStatus>> request = new BatchPartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .input(1L, new PatchRequest<TestRecord>()).setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public void testCrudBuilderParams12(ProtocolVersion version, String expectedUri)
  {
    //Simple resource tests
    Request<EmptyRecord> request = new DeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public void testCrudBuilderParams13(ProtocolVersion version, String expectedUri)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public void testCrudBuilderParams14(ProtocolVersion version, String expectedUri)
  {
    Request<EmptyRecord> request = new UpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .setParam("foo", "bar").build();
    testUriGeneration(request, expectedUri, version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingEqualsAndValue")
  public Object[][] encodingEqualsAnd1()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/3?foo=bar%26baz%3Dqux" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/3?foo=bar%26baz%3Dqux" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingEqualsAndValue")
  public void testParamEncodingEqualsAndValue(ProtocolVersion version, String expectedUri)
  {
    GetRequest<?> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo", "bar&baz=qux").build();
    testUriGeneration(request, expectedUri, version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingEqualsAndKey")
  public Object[][] encodingEqualsAnd2()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/3?foo%26bar%3Dbaz=qux" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/3?foo%26bar%3Dbaz=qux" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingEqualsAndKey")
  public void testParamEncodingEqualsAndKey(ProtocolVersion version, String expectedUri)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo&bar=baz", "qux").build();
    testUriGeneration(request, expectedUri, version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingSlash")
  public Object[][] encodingSlash()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/3?foo/bar=baz/qux" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/3?foo/bar=baz/qux" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingSlash")
  public void testParamEncodingSlash(ProtocolVersion version, String expectedUri)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo/bar", "baz/qux").build();
    testUriGeneration(request, expectedUri, version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingColon")
  public Object[][] encodingColon()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/3?foo:bar=baz:qux" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/3?foo%3Abar=baz%3Aqux" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingColon")
  public void testParamEncodingColon(ProtocolVersion version, String expectedUri)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo:bar", "baz:qux").build();
    testUriGeneration(request, expectedUri, version);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingQuestionMark")
  public Object[][] encodingQuestionMark()
  {
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/3?foo?bar=baz?qux" },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/3?foo?bar=baz?qux" }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingQuestionMark")
  public void testParamEncodingQuestionMark(ProtocolVersion version, String expectedUri)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo?bar", "baz?qux").build();
    testUriGeneration(request, expectedUri, version);
  }

  @Test
  public void testBuilderExceptions()
  {
    try
    {
      new DeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
          .build();
      Assert.fail("Building a delete request w/o an id on a collection should fail.");
    }
    catch(IllegalArgumentException e)
    {
      Assert.assertTrue(e.getMessage().contains("id required"));
    }

    try
    {
      new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS).build();
      Assert.fail("Building a get request w/o an id on a collection should fail.");
    }
    catch(IllegalArgumentException e)
    {
      Assert.assertTrue(e.getMessage().contains("id required"));
    }

    try
    {
      new UpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS).build();
      Assert.fail("Building an update request w/o an id on a collection should fail.");
    }
    catch(IllegalArgumentException e)
    {
      Assert.assertTrue(e.getMessage().contains("id required"));
    }

    try
    {
      new DeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
          .id(1L).build();
      Assert.fail("Building a delete request with an id on a simple resource should fail.");
    }
    catch(IllegalArgumentException e)
    {
      Assert.assertTrue(e.getMessage().contains("id is not allowed"));
    }

    try
    {
      new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
          .id(1L).build();
      Assert.fail("Building a get request with an id on a simple resource should fail.");
    }
    catch(IllegalArgumentException e)
    {
      Assert.assertTrue(e.getMessage().contains("id is not allowed"));
    }

    try
    {
      new UpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
          .id(1L).build();
      Assert.fail("Building an update request with an id on a simple resource should fail.");
    }
    catch(IllegalArgumentException e)
    {
      Assert.assertTrue(e.getMessage().contains("id is not allowed"));
    }
  }

  /**
   * Helper method to build complex key instance
   */
  private ComplexResourceKey<TestRecord, TestRecord> buildComplexKey(long keyId, String keyMessage, long paramId, String paramMessage)
  {
    ComplexResourceKey<TestRecord, TestRecord> id =
      new ComplexResourceKey<TestRecord, TestRecord>(new TestRecord(),
                                                     new TestRecord());
    id.getKey().setId(keyId);
    id.getKey().setMessage(keyMessage);
    id.getParams().setId(paramId);
    id.getParams().setMessage(paramMessage);
    return id;
  }

  /**
   * Helper method to build complex param instance
   */
  private RecordTemplate buildComplexParam(int id, String message)
  {
    TestRecord result = new TestRecord();
    result.setId(id);
    result.setMessage(message);
    return result;
  }

  /**
   * Tests the deprecated getResourcePath as well as the new (and equivalent) equality of path keys and base uri
   * template.
   */
  @SuppressWarnings("deprecation")
  private void testPathKeys(Request<?> request,
                            String[] expectedResourcePath,
                            String expectedBaseUriTemplate,
                            Map<String, ?> expectedPathKeys)
  {
    Assert.assertEquals(request.getResourcePath().toArray(), expectedResourcePath);
    Assert.assertEquals(request.getBaseUriTemplate(), expectedBaseUriTemplate);
    Assert.assertEquals(request.getPathKeys(), expectedPathKeys);
  }

  /**
   * Tests the deprecated API for getting the URI of a request, as well as the new way of constructing the URI using
   * a builder.
   * @param request
   * @param expectedUri
   * @param version
   */
  @SuppressWarnings("deprecation")
  private void testUriGeneration(Request<?> request, String expectedUri, ProtocolVersion version)
  {
    Assert.assertEquals(RestliUriBuilderUtil.createUriBuilder(request, version).build(), URI.create(expectedUri));
    if (version.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()) <= 0)
    {
      // getUri is deprecated and will only return a 1.0 version of the URI format.
      assertEquals(request.getUri().toString(), expectedUri);
    }
  }

  @SuppressWarnings("deprecation")
  private void testBaseUriGeneration(Request<?> request)
  {
    URI expectedBaseUri = URI.create(TEST_URI);
    Assert.assertEquals(RestliUriBuilderUtil.createUriBuilder(request).buildBaseUri(), expectedBaseUri);
    if (request instanceof BatchUpdateRequest)
    {
      Assert.assertEquals(((BatchUpdateRequest) request).getBaseURI(), expectedBaseUri);
    }
    else if (request instanceof BatchCreateRequest)
    {
      Assert.assertEquals(((BatchCreateRequest) request).getBaseURI(), expectedBaseUri);
    }
    else if (request instanceof BatchDeleteRequest)
    {
      Assert.assertEquals(((BatchDeleteRequest) request).getBaseUri(), expectedBaseUri);
    }
    else if (request instanceof BatchGetRequest)
    {
      Assert.assertEquals(((BatchGetRequest) request).getBaseURI(), expectedBaseUri);
    }
    else if (request instanceof BatchPartialUpdateRequest)
    {
      Assert.assertEquals(((BatchPartialUpdateRequest) request).getBaseURI(), expectedBaseUri);
    }
    else if (request instanceof GetRequest)
    {
      Assert.assertEquals(((GetRequest) request).getBaseURI(), expectedBaseUri);
    }
    else
    {
      // no-op as not all requests have a getBaseUri() or getBaseURI() method
    }
  }

  /**
   *
   * @param request
   * @param expectedUri
   * @param expectedMethod
   * @param expectedInput
   * @param expectedBatchInput
   * @param expectedHeaders
   * @param version
   */
  @SuppressWarnings("deprecation")
  private void checkBasicRequest(Request<?> request,
                                 String expectedUri,
                                 ResourceMethod expectedMethod,
                                 CollectionRequest<?> expectedInput,
                                 BatchRequest<?> expectedBatchInput,
                                 Map<String, String> expectedHeaders,
                                 ProtocolVersion version)
  {
    checkBasicRequest(request, expectedUri, expectedMethod, expectedInput, expectedHeaders,
                      version);
    if (request.getMethod() == ResourceMethod.BATCH_UPDATE ||
        request.getMethod() == ResourceMethod.BATCH_PARTIAL_UPDATE)
    {
      if (version.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()) <= 0)
      {
        // check the deprecated API
        Assert.assertEquals(request.getInput(), expectedBatchInput);
      }
      // check the conversion
      checkInputForBatchUpdateAndPatch(request, expectedBatchInput, version);
    }
  }

  private void checkBasicRequest(Request<?> request,
                                 String expectedUri,
                                 ResourceMethod expectedMethod,
                                 RecordTemplate expectedInput,
                                 Map<String, String> expectedHeaders,
                                 ProtocolVersion version)
  {
    testUriGeneration(request, expectedUri, version);
    Assert.assertEquals(request.getMethod(), expectedMethod);
    Assert.assertEquals(request.getHeaders(), expectedHeaders);
    Assert.assertEquals(request.getInputRecord(), expectedInput);
  }

  /**
   * Converts the new request body encoding into the old format and then checks that the conversion matches the expected
   * old format input.
   *
   * This method can be removed once we stop conversion of CollectionRequest to BatchRequest for BatchUpdates and
   * BatchPartialUpdates.
   *
   * @param request
   * @param expectedInput
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void checkInputForBatchUpdateAndPatch(Request<?> request, RecordTemplate expectedInput, ProtocolVersion version)
  {
    Assert.assertEquals(CollectionRequestUtil.convertToBatchRequest((CollectionRequest<KeyValueRecord>) request.getInputRecord(),
                                                                    request.getResourceSpec().getKeyClass(),
                                                                    request.getResourceSpec().getKeyKeyClass(),
                                                                    request.getResourceSpec().getKeyParamsClass(),
                                                                    request.getResourceSpec().getKeyParts(),
                                                                    request.getResourceSpec().getValueClass(),
                                                                    version),
                        expectedInput);
  }

  /**
   * Tests the new and the old, deprecated API of getting the object IDs in a batch request
   * @param batchRequest the batch request we are testing
   * @param objectIds the ids as objects
   */
  @SuppressWarnings("deprecation")
  private void testIdsForBatchRequest(com.linkedin.restli.client.BatchRequest<?> batchRequest, Set<?> objectIds)
  {
    Assert.assertEquals(batchRequest.getObjectIds(), objectIds);

    // test the old deprecated API which returns string versions of all primitive and compound key IDs
    Set<String> stringIds = new HashSet<String>(objectIds.size());
    for (Object o: objectIds)
    {
      stringIds.add(o.toString());
    }
    Assert.assertEquals(batchRequest.getIdObjects(), stringIds);
  }

  /**
   * Tests the new and the old, deprecated API of getting the object ID in a {@link GetRequest}
   * @param request
   * @param expectedId
   */
  @SuppressWarnings("deprecation")
  private void testIdForGetRequest(GetRequest<?> request, Object expectedId)
  {
    Assert.assertEquals(request.getObjectId(), expectedId);
    if (expectedId == null)
    {
      Assert.assertEquals(request.getIdObject(), expectedId);
    }
    else
    {
      Assert.assertEquals(request.getIdObject(), expectedId.toString());
    }
  }

  /**
   * Tests the new and the old, deprecated API of getting the input in a {@link Request}
   * @param request
   * @param expectedInput
   */
  @SuppressWarnings("deprecation")
  private void testInput(Request<?> request, RecordTemplate expectedInput)
  {
    Assert.assertEquals(request.getInputRecord(), expectedInput);
    Assert.assertEquals(request.getInput(), expectedInput);
  }

  /**
   * Builds a {@link CollectionRequest} consisting of {@link KeyValueRecord}s
   * @param factory
   * @param keys
   * @param values
   * @param <K>
   * @param <V>
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private <K, V extends RecordTemplate> CollectionRequest<KeyValueRecord> buildCollectionRequest(KeyValueRecordFactory<K, V> factory,
                                                                                                 K[] keys,
                                                                                                 V[] values)
  {
    CollectionRequest<KeyValueRecord> collectionRequest =
        new CollectionRequest<KeyValueRecord>(new DataMap(), KeyValueRecord.class);
    for (int i = 0; i < keys.length; i++)
    {
      collectionRequest.getElements().add(factory.create(keys[i], values[i]));
    }
    return collectionRequest;
  }

  private static String toEntityKey(Object key, ProtocolVersion version)
  {
    return URIParamUtils.keyToString(key, URLEscaper.Escaping.NO_ESCAPING, null, true, version);
  }
}
