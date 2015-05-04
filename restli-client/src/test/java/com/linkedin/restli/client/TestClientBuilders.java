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


import com.linkedin.data.ByteString;
import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.Null;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.DynamicRecordTemplate;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.filter.CompressionOption;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.client.util.PatchGenerator;
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
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.client.CollectionRequestUtil;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.common.URLEscaper;
import com.linkedin.restli.internal.testutils.URIDetails;

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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


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
    //Sample valid URIs:
    //"test/1?action=action"
    //"test/1?action=action"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("action", "action");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/1",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/1",
        null, queryParamsMap, null);

    return new Object[][] {
        {uriDetails1},
        {uriDetails2}
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "action")
  public void testActionRequestBuilder(URIDetails expectedURIDetails)
  {
    FieldDef<String> pParam = new FieldDef<String>("p", String.class, DataTemplateUtil.getSchema(String.class));
    Map<String, DynamicRecordMetadata> requestMetadataMap = new HashMap<String, DynamicRecordMetadata>();
    DynamicRecordMetadata requestMetadata = new DynamicRecordMetadata("action", Collections.<FieldDef<?>>singleton(pParam));
    requestMetadataMap.put("action", requestMetadata);
    DynamicRecordMetadata responseMetadata = new DynamicRecordMetadata("action", Collections.<FieldDef<?>>emptyList());
    Map<String, DynamicRecordMetadata> responseMetadataMap = new HashMap<String, DynamicRecordMetadata>();
    responseMetadataMap.put("action", responseMetadata);
    ResourceSpec resourceSpec = new ResourceSpecImpl(Collections.<ResourceMethod>emptySet(),
                                                     requestMetadataMap,
                                                     responseMetadataMap,
                                                     Long.class,
                                                     TestRecord.class,
                                                     Collections.<String, CompoundKey.TypeInfo> emptyMap());

    ActionRequestBuilder<Long, TestRecord> builder = new ActionRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                 TestRecord.class,
                                                                                 resourceSpec,
                                                                                 RestliRequestOptions.DEFAULT_OPTIONS);

    ActionRequest<TestRecord> request = builder.name("action").setParam(pParam, "42").id(1L).build();

    DataMap d = new DataMap();
    d.put("p", "42");
    @SuppressWarnings("unchecked")
    DynamicRecordTemplate expectedRecordTemplate =
        new DynamicRecordTemplate(
            d,
            DynamicRecordMetadata.buildSchema("action",
                                              Arrays.asList(
                                                 new FieldDef<String>("p",
                                                                      String.class,
                                                                      DataTemplateUtil.getSchema(String.class)))));

    URIDetails.testUriGeneration(request, expectedURIDetails);
    Assert.assertEquals(request.getMethod(), ResourceMethod.ACTION);
    Assert.assertEquals(request.getHeaders(), Collections.<String, String>emptyMap());
    Assert.assertEquals(request.getInputRecord(), expectedRecordTemplate);
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);
    Assert.assertEquals(request.getResponseDecoder().getEntityClass(), Void.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testActionRequestInputIsReadOnly()
  {
    FieldDef<TestRecord> pParam = new FieldDef<TestRecord>("p",
                                                           TestRecord.class,
                                                           DataTemplateUtil.getSchema(TestRecord.class));
    Map<String, DynamicRecordMetadata> requestMetadataMap = new HashMap<String, DynamicRecordMetadata>();

    DynamicRecordMetadata requestMetadata =
        new DynamicRecordMetadata("action", Collections.<FieldDef<?>>singleton(pParam));
    requestMetadataMap.put("action", requestMetadata);

    DynamicRecordMetadata responseMetadata =
        new DynamicRecordMetadata("action", Collections.<FieldDef<?>>emptyList());
    Map<String, DynamicRecordMetadata> responseMetadataMap = new HashMap<String, DynamicRecordMetadata>();
    responseMetadataMap.put("action", responseMetadata);

    ResourceSpec resourceSpec = new ResourceSpecImpl(Collections.<ResourceMethod>emptySet(),
                                                     requestMetadataMap,
                                                     responseMetadataMap,
                                                     ComplexResourceKey.class,
                                                     TestRecord.class,
                                                     TestRecord.class,
                                                     TestRecord.class,
                                                     Collections.<String, CompoundKey.TypeInfo> emptyMap());

    ActionRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new ActionRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(
            TEST_URI,
            TestRecord.class,
            resourceSpec,
            RestliRequestOptions.DEFAULT_OPTIONS);
    TestRecord testRecord1 = new TestRecord();
    TestRecord testRecord2 = new TestRecord();
    ComplexResourceKey<TestRecord, TestRecord> key =
        new ComplexResourceKey<TestRecord, TestRecord>(testRecord1, testRecord2);

    ActionRequest<TestRecord> request = builder.name("action").setParam(pParam, testRecord1).id(key).build();

    DynamicRecordTemplate inputParams = (DynamicRecordTemplate) request.getInputRecord();
    Assert.assertNotSame(inputParams.getValue(pParam).data(), testRecord1.data());
    Assert.assertTrue(inputParams.data().isReadOnly());
    Assert.assertTrue(inputParams.getValue(pParam).data().isMadeReadOnly());
    Assert.assertNotSame(request.getId(), key);
    Assert.assertTrue(((ComplexResourceKey<TestRecord, TestRecord>) request.getId()).isReadOnly());

    testRecord1.data().makeReadOnly();
    testRecord2.data().makeReadOnly();

    request = builder.build();

    inputParams = (DynamicRecordTemplate) request.getInputRecord();
    Assert.assertSame(inputParams.getValue(pParam).data(), testRecord1.data());
    Assert.assertTrue(inputParams.data().isReadOnly());
    Assert.assertSame(request.getId(), key);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchGetWithProjections")
  public Object[][] batchGetWithProjections()
  {
    //Sample valid URIs:
    //"test?fields=message,id&ids=1&ids=2&ids=3"
    //"test?fields=message,id&ids=List(1,2,3)"

    final Set<String> fieldSet = new HashSet<String>();
    fieldSet.add("message");
    fieldSet.add("id");

    final Set<String> idSet = new HashSet<String>();
    idSet.add("1");
    idSet.add("2");
    idSet.add("3");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        idSet, null, fieldSet);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        idSet, null, fieldSet);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchGetWithProjections")
  public void testBatchGetRequestBuilder(URIDetails expectedURIDetails)
  {
    BatchGetRequestBuilder<Long, TestRecord> builder = new BatchGetRequestBuilder<Long, TestRecord>(
        TEST_URI,
        TestRecord.class,
        _COLL_SPEC,
        RestliRequestOptions.DEFAULT_OPTIONS);
    BatchGetRequest<TestRecord> request =
        builder.ids(1L, 2L, 3L).fields(TestRecord.fields().id(), TestRecord.fields().message()).build();
    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    Assert.assertEquals(request.getObjectIds(), new HashSet<Long>(Arrays.asList(1L, 2L, 3L)));
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
        TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedURIDetails, ResourceMethod.BATCH_GET, null, Collections.<String, String>emptyMap());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testBatchGetKVInputIsReadOnly()
  {
    BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(
            TEST_URI,
            TestRecord.class,
            _COMPLEX_KEY_SPEC,
            RestliRequestOptions.DEFAULT_OPTIONS);

    TestRecord testRecord1 = new TestRecord();
    TestRecord testRecord2 = new TestRecord();
    ComplexResourceKey<TestRecord, TestRecord> key =
        new ComplexResourceKey<TestRecord, TestRecord>(testRecord1, testRecord2);

    BatchGetKVRequest<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> request = builder.ids(key).buildKV();

    ComplexResourceKey<TestRecord, TestRecord> requestKey =
        (ComplexResourceKey<TestRecord, TestRecord>) request.getObjectIds().iterator().next();

    Assert.assertNotSame(requestKey, key);
    Assert.assertTrue(requestKey.isReadOnly());

    key.makeReadOnly();

    request = builder.buildKV();

    requestKey = (ComplexResourceKey<TestRecord, TestRecord>) request.getObjectIds().iterator().next();

    Assert.assertSame(requestKey, key);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testBatchGetEntityInputIsReadOnly()
  {
    BatchGetEntityRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new BatchGetEntityRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(
            TEST_URI,
            _COMPLEX_KEY_SPEC,
            RestliRequestOptions.DEFAULT_OPTIONS);

    TestRecord testRecord1 = new TestRecord();
    TestRecord testRecord2 = new TestRecord();
    ComplexResourceKey<TestRecord, TestRecord> key =
        new ComplexResourceKey<TestRecord, TestRecord>(testRecord1, testRecord2);

    BatchGetEntityRequest<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> request = builder.ids(key).build();

    ComplexResourceKey<TestRecord, TestRecord> requestKey =
        (ComplexResourceKey<TestRecord, TestRecord>) request.getObjectIds().iterator().next();

    Assert.assertNotSame(requestKey, key);
    Assert.assertTrue(requestKey.isReadOnly());

    key.makeReadOnly();

    request = builder.build();

    requestKey = (ComplexResourceKey<TestRecord, TestRecord>) request.getObjectIds().iterator().next();

    Assert.assertSame(requestKey, key);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchGetWithEncoding")
  public Object[][] batchGetWithEncoding()
  {
    //Sample URIs:
    //"test?fields=message,id&ids=ampersand%3D%2526%2526%26equals%3D%253D%253D&ids=ampersand%3D%2526%26equals%3D%253D"
    //"test?fields=message,id&ids=List((ampersand:%26%26,equals:%3D%3D),(ampersand:%26,equals:%3D))"

    final Set<String> fieldSet = new HashSet<String>();
    fieldSet.add("message");
    fieldSet.add("id");

    //Note that we need two different ID sets, one for V1 and one for V2 since batch operations on compound keys
    //are unique.
    final Set<String> idSetV1 = new HashSet<String>();
    idSetV1.add("ampersand=%26%26&equals=%3D%3D");
    idSetV1.add("ampersand=%26&equals=%3D");

    final Set<DataMap> idSetV2 = new HashSet<DataMap>();
    final DataMap map1 = new DataMap();
    map1.put("ampersand", "&&");
    map1.put("equals", "==");
    final DataMap map2 = new DataMap();
    map2.put("ampersand", "&");
    map2.put("equals", "=");
    idSetV2.add(map1);
    idSetV2.add(map2);

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        idSetV1, null, fieldSet);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        idSetV2, null, fieldSet);

    return new Object[][] {
      // Note double encoding for V1 - one comes from CompoundKey.toString, another - from BatchGetRequestBuilder.ids().
      { uriDetails1 },
      { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchGetWithEncoding")
  public void testBatchGetCompoundKeyRequestBuilder(URIDetails expectedURIDetails)
  {
    BatchGetRequestBuilder<CompoundKey, TestRecord> builder =
        new BatchGetRequestBuilder<CompoundKey, TestRecord>(TEST_URI, TestRecord.class, _ASSOC_SPEC,
            RestliRequestOptions.DEFAULT_OPTIONS);

    CompoundKey key1 = new CompoundKey();
    key1.append("equals", "=");
    key1.append("ampersand", "&");
    CompoundKey key2 = new CompoundKey();
    key2.append("equals", "==");
    key2.append("ampersand", "&&");

    BatchGetKVRequest<CompoundKey, TestRecord> request =
        builder.ids(key1,key2).fields(TestRecord.fields().id(), TestRecord.fields().message()).buildKV();

    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());

    // Compare key sets. Note that have to convert keys to Strings as the request internally converts them to string
    HashSet<CompoundKey> expectedIds = new HashSet<CompoundKey>(Arrays.asList(key1, key2));
    Assert.assertEquals(request.getObjectIds(), expectedIds);
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
            TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request,
                      expectedURIDetails,
                      ResourceMethod.BATCH_GET,
                      null,
                      Collections.<String, String>emptyMap());
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
    //Sample URIs:
    //"test/part1=1&part2=2"
    //"test/(part1:1,part2:2)"

    //We can compare the serialized compound key as a part of the path since its always serialized into a sorted order
    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "test/part1=1&part2=2", null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "test/(part1:1,part2:2)", null, null, null);

    return new Object[][] {
      { uriDetails1 },
      { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "compoundKey")
  public void testGetCompoundKeyRequestBuilder(URIDetails expectedURIDetails)
  {
    GetRequestBuilder<CompoundKey, TestRecord> builder =
        new GetRequestBuilder<CompoundKey, TestRecord>(TEST_URI, TestRecord.class, _ASSOC_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    CompoundKey key = buildCompoundKey();

    GetRequest<TestRecord> request = builder.id(key).build();

    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);
    checkBasicRequest(request,
                      expectedURIDetails,
                      ResourceMethod.GET,
                      null,
                      Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public Object[][] noEntity()
  {
    //Sample URIs:
    //"test"
    //"test"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "test", null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "test", null, null, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testCreateCompoundKeyRequestBuilder(URIDetails expectedURIDetails)
  {
    CreateRequestBuilder<CompoundKey, TestRecord> builder =
        new CreateRequestBuilder<CompoundKey, TestRecord>(TEST_URI, TestRecord.class, _ASSOC_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    TestRecord record = new TestRecord().setMessage("foo");

    CreateRequest<TestRecord> request = builder.input(record).build();

    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    checkBasicRequest(request, expectedURIDetails, ResourceMethod.CREATE, record, Collections.<String, String>emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "compoundKey")
  public void testUpdateCompoundKeyRequestBuilder(URIDetails expectedURIDetails)
  {
    UpdateRequestBuilder<CompoundKey, TestRecord> builder =
        new UpdateRequestBuilder<CompoundKey, TestRecord>(TEST_URI, TestRecord.class, _ASSOC_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    TestRecord record = new TestRecord().setMessage("foo");

    UpdateRequest<TestRecord> request = builder.id(buildCompoundKey()).input(record).build();

    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);
    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    checkBasicRequest(request,
                      expectedURIDetails,
                      ResourceMethod.UPDATE,
                      record,
                      Collections.<String, String>emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "compoundKey")
  public void testPartialUpdateCompoundKeyRequestBuilder(URIDetails expectedURIDetails)
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

    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    checkBasicRequest(request, expectedURIDetails, ResourceMethod.PARTIAL_UPDATE, patch, Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchCompoundKey")
  public Object[][] batchCompoundKey()
  {
    //Sample URIs:
    //"test?ids=part1%3D1%26part2%3D2&ids=part1%3D11%26part2%3D22",
    //"test?ids=List((part1:1,part2:2),(part1:11,part2:22))");

    //Note that we need two different ID sets, one for V1 and one for V2 since batch operations on compound keys
    //are unique.
    final Set<String> idSetV1 = new HashSet<String>();
    idSetV1.add("part1=1&part2=2");
    idSetV1.add("part1=11&part2=22");

    final Set<DataMap> idSetV2 = new HashSet<DataMap>();
    final DataMap id1 = new DataMap();
    id1.put("part1", "1");
    id1.put("part2", "2");
    final DataMap id2 = new DataMap();
    id2.put("part1", "11");
    id2.put("part2", "22");
    idSetV2.add(id1);
    idSetV2.add(id2);

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        idSetV1, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        idSetV2, null, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchCompoundKey")
  public void testBatchUpdateCompoundKeyRequestBuilder(URIDetails expectedURIDetails)
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
    expectedRequest.getEntities().put(toEntityKey(key1, expectedURIDetails.getProtocolVersion()), t1);
    expectedRequest.getEntities().put(toEntityKey(key2, expectedURIDetails.getProtocolVersion()), t2);

    BatchUpdateRequest<CompoundKey, TestRecord> request = builder.inputs(inputs).build();

    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
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
                      expectedURIDetails,
                      ResourceMethod.BATCH_UPDATE,
                      collectionRequest,
                      expectedRequest,
                      Collections.<String, String>emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchCompoundKey")
  public void testBatchPartialUpdateCompoundKeyRequestBuilder(URIDetails expectedURIDetails)
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

    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    @SuppressWarnings({"unchecked","rawtypes"})
    BatchRequest<PatchRequest<TestRecord>> expectedRequest = new BatchRequest(new DataMap(), PatchRequest.class);
    expectedRequest.getEntities().put(toEntityKey(key1, expectedURIDetails.getProtocolVersion()), patch1);
    expectedRequest.getEntities().put(toEntityKey(key2, expectedURIDetails.getProtocolVersion()), patch2);

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
                      expectedURIDetails,
                      ResourceMethod.BATCH_PARTIAL_UPDATE,
                      collectionRequest,
                      expectedRequest,
                      Collections.<String, String>emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchGetWithProjections")
  public void testBatchGetRequestBuilderCollectionIds(URIDetails expectedURIDetails)
  {
    BatchGetRequestBuilder<Long, TestRecord> builder = new BatchGetRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                                    TestRecord.class,
                                                                                                    _COLL_SPEC,
                                                                                                    RestliRequestOptions.DEFAULT_OPTIONS);
    List<Long> ids = Arrays.asList(1L, 2L, 3L);
    BatchGetRequest<TestRecord> request = builder.ids(ids).fields(TestRecord.fields().id(), TestRecord.fields().message()).build();
    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    Assert.assertEquals(request.getObjectIds(), new HashSet<Long>(Arrays.asList(1L, 2L, 3L)));
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
            TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedURIDetails, ResourceMethod.BATCH_GET, null, Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batch")
  public Object[][] batch()
  {
    //Sample URIs:
    //"test?ids=1&ids=2&ids=3"
    //"test?ids=List(1,2,3)"

    final Set<String> idSet = new HashSet<String>();
    idSet.add("1");
    idSet.add("2");
    idSet.add("3");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        idSet, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        idSet, null, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batch")
  public void testBatchUpdateRequestBuilder(URIDetails expectedURIDetails)
  {
    BatchUpdateRequestBuilder<Long, TestRecord> builder =
            new BatchUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    Map<Long, TestRecord> updates = new HashMap<Long, TestRecord>();
    updates.put(1L, new TestRecord());
    updates.put(2L, new TestRecord());
    updates.put(3L, new TestRecord());
    BatchUpdateRequest<Long, TestRecord> request = builder.inputs(updates).build();
    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    Assert.assertEquals(request.getObjectIds(), new HashSet<Long>(Arrays.asList(1L, 2L, 3L)));
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
                      expectedURIDetails,
                      ResourceMethod.BATCH_UPDATE,
                      collectionRequest,
                      expectedRequest,
                      Collections.<String, String>emptyMap());
  }

  // need suppress on the method because the more specific suppress isn't being obeyed.
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batch")
  public void testBatchPartialUpdateRequestBuilder(URIDetails expectedURIDetails)
  {
    BatchPartialUpdateRequestBuilder<Long, TestRecord> builder =
            new BatchPartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    builder.input(1L, new PatchRequest<TestRecord>());
    builder.input(2L, new PatchRequest<TestRecord>());
    builder.input(3L, new PatchRequest<TestRecord>());
    BatchPartialUpdateRequest<Long, TestRecord> request = builder.build();
    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    Assert.assertEquals(request.getObjectIds(), new HashSet<Long>(Arrays.asList(1L, 2L, 3L)));
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
                      expectedURIDetails,
                      ResourceMethod.BATCH_PARTIAL_UPDATE,
                      collectionRequest,
                      expectedRequest,
                      Collections.<String, String>emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batch")
  public void testBatchDeleteRequestBuilder(URIDetails expectedURIDetails)
  {
    BatchDeleteRequestBuilder<Long, TestRecord> builder =
            new BatchDeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    BatchDeleteRequest<Long, TestRecord> request = builder.ids(1L, 2L, 3L).build();
    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    Assert.assertEquals(request.getObjectIds(), new HashSet<Long>(Arrays.asList(1L, 2L, 3L)));
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedURIDetails, ResourceMethod.BATCH_DELETE, null, Collections.<String, String>emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testBatchCreateRequestBuilder(URIDetails expectedURIDetails)
  {
    BatchCreateRequestBuilder<Long, TestRecord> builder =
            new BatchCreateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    List<TestRecord> newRecords = Arrays.asList(new TestRecord(), new TestRecord(), new TestRecord());
    BatchCreateRequest<TestRecord> request = builder.inputs(newRecords).build();
    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    CollectionRequest<TestRecord> expectedRequest = new CollectionRequest<TestRecord>(new DataMap(), TestRecord.class);
    expectedRequest.getElements().addAll(newRecords);
    checkBasicRequest(request, expectedURIDetails, ResourceMethod.BATCH_CREATE,
                      expectedRequest,
                      Collections.<String, String>emptyMap());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testBatchCreateRequestInputIsReadOnly()
  {
    BatchCreateRequestBuilder<Long, TestRecord> builder =
        new BatchCreateRequestBuilder<Long, TestRecord>(TEST_URI,
                                                        TestRecord.class,
                                                        _COLL_SPEC,
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    TestRecord testRecord = new TestRecord();
    List<TestRecord> newRecords = Arrays.asList(testRecord);
    BatchCreateRequest<TestRecord> request = builder.inputs(newRecords).build();
    CollectionRequest<TestRecord> createInput = (CollectionRequest<TestRecord>) request.getInputRecord();

    Assert.assertNotSame(createInput.getElements().get(0), testRecord);
    Assert.assertTrue(createInput.getElements().get(0).data().isMadeReadOnly());

    testRecord.data().makeReadOnly();
    request = builder.build();
    createInput = (CollectionRequest<TestRecord>) request.getInputRecord();
    Assert.assertSame(createInput.getElements().get(0), testRecord);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testBatchCreateIdRequestInputIsReadOnly()
  {
    BatchCreateIdRequestBuilder<Long, TestRecord> builder =
        new BatchCreateIdRequestBuilder<Long, TestRecord>(TEST_URI,
                                                          TestRecord.class,
                                                          _COLL_SPEC,
                                                          RestliRequestOptions.DEFAULT_OPTIONS);
    TestRecord testRecord = new TestRecord();
    List<TestRecord> newRecords = Arrays.asList(testRecord);
    BatchCreateIdRequest<Long, TestRecord> request = builder.inputs(newRecords).build();
    CollectionRequest<TestRecord> createInput = (CollectionRequest<TestRecord>) request.getInputRecord();

    Assert.assertNotSame(createInput.getElements().get(0), testRecord);
    Assert.assertTrue(createInput.getElements().get(0).data().isMadeReadOnly());

    testRecord.data().makeReadOnly();
    request = builder.build();
    createInput = (CollectionRequest<TestRecord>) request.getInputRecord();
    Assert.assertSame(createInput.getElements().get(0), testRecord);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testCreateRequestBuilder(URIDetails expectedURIDetails)
  {
    CreateRequestBuilder<Long, TestRecord> builder = new CreateRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                                TestRecord.class,
                                                                                                _COLL_SPEC,
                                                                                                RestliRequestOptions.DEFAULT_OPTIONS);
    CreateRequest<TestRecord> request = builder.input(new TestRecord()).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    checkBasicRequest(request, expectedURIDetails, ResourceMethod.CREATE, new TestRecord(), Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "singleEntity")
  public Object[][] singleEntity()
  {
    //Sample URIs:
    //"test/1"
    //"test/1"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "test/1", null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "test/1", null, null, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "singleEntity")
  public void testDeleteRequestBuilder(URIDetails expectedURIDetails)
  {
    DeleteRequestBuilder<Long, TestRecord> builder = new DeleteRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                                TestRecord.class,
                                                                                                _COLL_SPEC,
                                                                                                RestliRequestOptions.DEFAULT_OPTIONS);
    DeleteRequest<TestRecord> request = builder.id(1L).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedURIDetails, ResourceMethod.DELETE, null, Collections.<String, String>emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testDeleteRequestBuilderWithKeylessResource(URIDetails expectedURIDetails)
  {
    DeleteRequestBuilder<Long, TestRecord> builder = new DeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class,
                                                                                                _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    DeleteRequest<TestRecord> request = builder.build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedURIDetails, ResourceMethod.DELETE, null, Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "search1")
  public Object[][] search1()
  {
    //Sample URIs:
    //"test/key=a%3Ab?count=4&fields=message,id&p=42&q=search&start=1"
    //"test/(key:a%3Ab)?count=4&fields=message,id&p=42&q=search&start=1"

    final Set<String> fieldSet = new HashSet<String>();
    fieldSet.add("message");
    fieldSet.add("id");

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("p", "42");
    queryParamsMap.put("q", "search");
    queryParamsMap.put("start", "1");
    queryParamsMap.put("count", "4");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/key=a%3Ab",
        null, queryParamsMap, fieldSet);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/(key:a%3Ab)",
        null, queryParamsMap, fieldSet);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "search1")
  public void testFindRequestBuilder1(URIDetails expectedURIDetails)
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
                      expectedURIDetails,
                      ResourceMethod.FINDER,
                      null,
                      Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "search2")
  public Object[][] search2()
  {
    //Sample URIs:
    //"test/key=a%3Ab?p=42&q=search&start=1"
    //"test/(key:a%3Ab)?p=42&q=search&start=1"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("p", "42");
    queryParamsMap.put("q", "search");
    queryParamsMap.put("start", "1");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/key=a%3Ab",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/(key:a%3Ab)",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "search2")
  public void testFindRequestBuilder2(URIDetails expectedURIDetails)
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
                      expectedURIDetails,
                      ResourceMethod.FINDER,
                      null,
                      Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "search3")
  public Object[][] search3()
  {
    //Sample URIs:
    //"test/key=a%3Ab?count=4&p=42&q=search"
    //"test/(key:a%3Ab)?count=4&p=42&q=search"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("p", "42");
    queryParamsMap.put("q", "search");
    queryParamsMap.put("count", "4");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/key=a%3Ab",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/(key:a%3Ab)",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "search3")
  public void testFindRequestBuilder3(URIDetails expectedURIDetails)
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
                      expectedURIDetails,
                      ResourceMethod.FINDER,
                      null,
                      Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getAll1")
  public Object[][] getAll1()
  {
    //Sample URIs:
    //"test?count=4&fields=message,id&start=1"
    //"test?count=4&fields=message,id&start=1"

    final Set<String> fieldSet = new HashSet<String>();
    fieldSet.add("message");
    fieldSet.add("id");

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("count", "4");
    queryParamsMap.put("start", "1");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        null, queryParamsMap, fieldSet);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        null, queryParamsMap, fieldSet);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getAll1")
  public void testGetAllRequestBuilder1(URIDetails expectedURIDetails)
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
                      expectedURIDetails,
                      ResourceMethod.GET_ALL,
                      null,
                      Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getAll2")
  public Object[][] getAll2()
  {
    //Sample URIs:
    //"test?start=1"
    //"test?start=1"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("start", "1");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider =  TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getAll2")
  public void testGetAllRequestBuilder2(URIDetails expectedURIDetails)
  {
    GetAllRequestBuilder<Long, TestRecord> builder =
        new GetAllRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    GetAllRequest<TestRecord> request = builder.paginateStart(1).build();
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);
    checkBasicRequest(request,
                      expectedURIDetails,
                      ResourceMethod.GET_ALL,
                      null,
                      Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getAll3")
  public Object[][] getAll3()
  {
    //Sample URIs:
    //"test?count=4"
    //"test?count=4"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("count", "4");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getAll3")
  public void testGetAllRequestBuilder3(URIDetails expectedURIDetails)
  {
    GetAllRequestBuilder<Long, TestRecord> builder =
        new GetAllRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);

    GetAllRequest<TestRecord> request = builder.paginateCount(4).build();
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);
    checkBasicRequest(request,
                      expectedURIDetails,
                      ResourceMethod.GET_ALL,
                      null,
                      Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getWithProjection")
  public Object[][] getWithProjection()
  {
    //Sample URIs:
    //"test/1?fields=message,id"
    //"test/1?fields=message,id"
    final Set<String> fieldSet = new HashSet<String>();
    fieldSet.add("message");
    fieldSet.add("id");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/1",
        null, null, fieldSet);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/1",
        null, null, fieldSet);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getWithProjection")
  public void testGetRequestBuilder(URIDetails expectedURIDetails)
  {
    GetRequestBuilder<Long, TestRecord> builder = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    GetRequest<TestRecord> request = builder.id(1L).fields(TestRecord.fields().id(), TestRecord.fields().message()).build();
    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    Assert.assertEquals(request.getObjectId(), new Long(1L));
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
            TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedURIDetails, ResourceMethod.GET, null, Collections.<String, String>emptyMap());
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

    overrideOptions = new RestliRequestOptionsBuilder().setRequestCompressionOverride(CompressionOption.FORCE_OFF).build();
    Assert.assertEquals(builder.id(1L).setRequestOptions(overrideOptions).build().getRequestOptions(), overrideOptions);

    overrideOptions = new RestliRequestOptionsBuilder().setContentType(RestClient.ContentType.PSON).build();
    Assert.assertEquals(builder.id(1L).setRequestOptions(overrideOptions).build().getRequestOptions(), overrideOptions);

    overrideOptions = new RestliRequestOptionsBuilder().setAcceptTypes(Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON)).build();
    Assert.assertEquals(builder.id(1L).setRequestOptions(overrideOptions).build().getRequestOptions(), overrideOptions);

    overrideOptions = new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.FORCE_USE_NEXT)
        .setRequestCompressionOverride(CompressionOption.FORCE_OFF).setContentType(RestClient.ContentType.PSON)
        .setAcceptTypes(Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON)).build();
    Assert.assertEquals(builder.id(1L).setRequestOptions(overrideOptions).build().getRequestOptions(), overrideOptions);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getOnKeyless")
  public Object[][] getOnKeyless()
  {
    //Sample URIs:
    //"test?fields=message,id"
    //"test?fields=message,id"

    final Set<String> fieldSet = new HashSet<String>();
    fieldSet.add("message");
    fieldSet.add("id");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        null, null, fieldSet);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        null, null, fieldSet);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "getOnKeyless")
  public void testGetRequestBuilderWithKeylessResource(URIDetails expectedURIDetails)
  {
    GetRequestBuilder<Void, TestRecord> builder = new GetRequestBuilder<Void, TestRecord>(TEST_URI,
                                                                                          TestRecord.class,
                                                                                          _SIMPLE_RESOURCE_SPEC,
                                                                                          RestliRequestOptions.DEFAULT_OPTIONS);
    GetRequest<TestRecord> request = builder.fields(TestRecord.fields().id(), TestRecord.fields().message()).build();
    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    Assert.assertEquals(request.getObjectId(), null);
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
        TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, expectedURIDetails, ResourceMethod.GET, null, Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "builderParam")
  public Object[][] builderParam()
  {
    //Sample URIs:
    //"test/1?arrayKey1=3&arrayKey1=4&arrayKey1=5&arrayKey2=3&arrayKey2=4&arrayKey2=5&simpleKey=2"
    //"test/1?arrayKey1=List(3,4,5)&arrayKey2=List(3,4,5)&simpleKey=2"

    final Map<String, Object> queryParamsMap = new HashMap<String, Object>();
    queryParamsMap.put("simpleKey", "2");
    final DataList arrayKey1List = new DataList();
    arrayKey1List.add("3");
    arrayKey1List.add("4");
    arrayKey1List.add("5");
    final DataList arrayKey2List = new DataList();
    arrayKey2List.add("3");
    arrayKey2List.add("4");
    arrayKey2List.add("5");
    queryParamsMap.put("arrayKey1", arrayKey1List);
    queryParamsMap.put("arrayKey2", arrayKey2List);

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/1",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/1",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "builderParam")
  public void testBuilderParam(URIDetails expectedURIDetails)
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
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "singleEntity")
  public void testPartialUpdateRequestBuilder(URIDetails expectedURIDetails) throws Exception
  {
    PartialUpdateRequestBuilder<Long, TestRecord> builder =
        new PartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI,
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
                      expectedURIDetails,
                      ResourceMethod.PARTIAL_UPDATE,
                      patch,
                      Collections.<String, String>emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testPartialUpdateRequestBuilderWithKeylessResource(URIDetails expectedURIDetails) throws Exception
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
                      expectedURIDetails,
                      ResourceMethod.PARTIAL_UPDATE,
                      patch,
                      Collections.<String, String>emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "singleEntity")
  public void testUpdateRequestBuilder(URIDetails expectedURIDetails)
  {
    UpdateRequestBuilder<Long, TestRecord> builder = new UpdateRequestBuilder<Long, TestRecord>(TEST_URI,
                                                                                                TestRecord.class,
                                                                                                _COLL_SPEC,
                                                                                                RestliRequestOptions.DEFAULT_OPTIONS);
    UpdateRequest<TestRecord> request = builder.id(1L).input(new TestRecord()).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request,
                      expectedURIDetails,
                      ResourceMethod.UPDATE,
                      new TestRecord(),
                      Collections.<String, String>emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testUpdateRequestBuilderWithKeylessResource(URIDetails expectedURIDetails)
  {
    UpdateRequestBuilder<Void, TestRecord> builder = new UpdateRequestBuilder<Void, TestRecord>(TEST_URI,
                                                                                                TestRecord.class,
                                                                                                _SIMPLE_RESOURCE_SPEC,
                                                                                                RestliRequestOptions.DEFAULT_OPTIONS);
    UpdateRequest<TestRecord> request = builder.input(new TestRecord()).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request,
                      expectedURIDetails,
                      ResourceMethod.UPDATE,
                      new TestRecord(),
                      Collections.<String, String>emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "complexKeyAndParam")
  public Object[][] complexKeyAndParam()
  {
    //Sample URIs:
    //"test/$params.id=10&$params.message=ParamMessage&id=1&message=KeyMessage?testParam.id=123&testParam.message=ParamMessage"
    //"test/($params:(id:10,message:ParamMessage),id:1,message:KeyMessage)?testParam=(id:123,message:ParamMessage)"

    final DataMap idMessageMap = new DataMap();
    idMessageMap.put("id", "123");
    idMessageMap.put("message", "ParamMessage");
    final Map<String, DataComplex> queryParamsMap = new HashMap<String, DataComplex>();
    queryParamsMap.put("testParam", idMessageMap);

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "test/$params.id=10&$params.message=ParamMessage&id=1&message=KeyMessage", null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "test/($params:(id:10,message:ParamMessage),id:1,message:KeyMessage)", null, queryParamsMap, null);

    return new Object[][] {
        {uriDetails1},
        {uriDetails2}
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "complexKeyAndParam")
  public void testComplexKeyGetRequestBuilder(URIDetails expectedURIDetails) throws Exception
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
                      expectedURIDetails,
                      ResourceMethod.GET,
                      null,
                      Collections.<String, String> emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "complexKeyAndParam")
  public void testComplexKeyDeleteRequestBuilder(URIDetails expectedURIDetails) throws Exception
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
                      expectedURIDetails,
                      ResourceMethod.DELETE,
                      null,
                      Collections.<String, String> emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchComplexKeyAndParam")
  public Object[][] batchComplexKeyAndParam()
  {
    //Sample valid URIs:
    //"test?ids%5B0%5D.$params.id=20&ids%5B0%5D.$params.message=ParamMessage2&ids%5B0%5D.id=2&ids%5B0%5D.message=KeyMessage2&ids%5B1%5D.$params.id=10&ids%5B1%5D.$params.message=ParamMessage1&ids%5B1%5D.id=1&ids%5B1%5D.message=KeyMessage1&testParam.id=123&testParam.message=ParamMessage"
    //"test?ids=List(($params:(id:20,message:ParamMessage2),id:2,message:KeyMessage2),($params:(id:10,message:ParamMessage1),id:1,message:KeyMessage1))&testParam=(id:123,message:ParamMessage)"

    final DataMap idMessageMap = new DataMap();
    idMessageMap.put("id", "123");
    idMessageMap.put("message", "ParamMessage");
    final Map<String, DataComplex> queryParamsMap = new HashMap<String, DataComplex>();
    queryParamsMap.put("testParam", idMessageMap);

    final Set<DataMap> idList = new HashSet<DataMap>();
    final DataMap idMapOne = new DataMap();
    idMapOne.put("id", "1");
    idMapOne.put("message", "KeyMessage1");
    final DataMap paramMapOne = new DataMap();
    paramMapOne.put("id", "10");
    paramMapOne.put("message", "ParamMessage1");
    idMapOne.put("$params", paramMapOne);
    final DataMap idMapTwo = new DataMap();
    idMapTwo.put("id", "2");
    idMapTwo.put("message", "KeyMessage2");
    final DataMap paramMapTwo = new DataMap();
    paramMapTwo.put("id", "20");
    paramMapTwo.put("message", "ParamMessage2");
    idMapTwo.put("$params", paramMapTwo);
    idList.add(idMapOne);
    idList.add(idMapTwo);

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        idList, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        idList, queryParamsMap, null);

    return new Object[][] {
        {uriDetails1},
        {uriDetails2}
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchComplexKeyAndParam")
  public void testComplexKeyBatchGetRequestBuilder(URIDetails expectedURIDetails) throws Exception
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
    BatchGetKVRequest<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> request =
        builder.ids(id1, id2).setParam("testParam", param).buildKV();
    Assert.assertTrue(request.isIdempotent());
    Assert.assertTrue(request.isSafe());
    checkBasicRequest(request,
                      expectedURIDetails,
                      ResourceMethod.BATCH_GET,
                      null,
                      Collections.<String, String>emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchComplexKeyAndParam")
  public void testComplexKeyBatchUpdateRequestBuilder(URIDetails expectedURIDetails) throws Exception
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
    expectedRequest.getEntities().put(toEntityKey(id1, expectedURIDetails.getProtocolVersion()), t1);
    expectedRequest.getEntities().put(toEntityKey(id2, expectedURIDetails.getProtocolVersion()), t2);

    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
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
                      expectedURIDetails,
                      ResourceMethod.BATCH_UPDATE,
                      collectionRequest,
                      expectedRequest,
                      Collections.<String, String>emptyMap());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testBatchUpdateRequestInputIsReadOnly()
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
    TestRecord t1 = new TestRecord().setMessage("foo");
    TestRecord t2 = new TestRecord().setMessage("bar");

    BatchUpdateRequest<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> request =
        builder.input(id1, t1).input(id2, t2).build();

    checkKeyValueRecordCollectionIsReadOnly(id1,
                                              id2,
                                              t1,
                                              t2,
                                              TestRecord.class,
                                              (CollectionRequest<KeyValueRecord<?, TestRecord>>) request.getInputRecord());

    checkKeyValueMapIsReadOnly(id1, id2, t1, t2, TestRecord.class, request.getUpdateInputMap());

    id1.makeReadOnly();
    t2.data().makeReadOnly();

    request = builder.input(id1, t1).input(id2, t2).build();

    checkKeyValueRecordCollectionIsReadOnly(id1,
                                              id2,
                                              t1,
                                              t2,
                                              TestRecord.class,
                                              (CollectionRequest<KeyValueRecord<?, TestRecord>>) request.getInputRecord());

    checkKeyValueMapIsReadOnly(id1, id2, t1, t2, TestRecord.class, request.getUpdateInputMap());
  }

  private void checkKeyValueMapIsReadOnly(ComplexResourceKey<TestRecord, TestRecord> id1,
                                            ComplexResourceKey<TestRecord, TestRecord> id2,
                                            RecordTemplate t1,
                                            RecordTemplate t2,
                                            Class<? extends RecordTemplate> valueClass,
                                            Map<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> entries)
  {
    for (Map.Entry<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> entry : entries.entrySet())
    {
      ComplexResourceKey<TestRecord, TestRecord> generatedKey = entry.getKey();

      if (generatedKey.equals(id1))
      {
        checkComplexKeyIsReadOnly(id1, generatedKey);
      }
      else
      {
        checkComplexKeyIsReadOnly(id2, generatedKey);
      }

      Assert.assertTrue(generatedKey.isReadOnly());

      RecordTemplate value = entry.getValue();
      if (value.equals(t1))
      {
        if (t1.data().isMadeReadOnly())
        {
          Assert.assertSame(t1, value);
        }
        else
        {
          Assert.assertNotSame(t1, value);
        }
      }
      else
      {
        if (t2.data().isReadOnly())
        {
          Assert.assertSame(t2, value);
        }
        else
        {
          Assert.assertNotSame(t2, value);
        }
      }

      Assert.assertTrue(value.data().isMadeReadOnly());
    }
  }

  @SuppressWarnings("unchecked")
  private <V extends RecordTemplate> void checkKeyValueRecordCollectionIsReadOnly(
                                                         ComplexResourceKey<TestRecord, TestRecord> id1,
                                                         ComplexResourceKey<TestRecord, TestRecord> id2,
                                                         RecordTemplate t1,
                                                         RecordTemplate t2,
                                                         Class<V> valueClass,
                                                         CollectionRequest<KeyValueRecord<?, V>> entries)
  {
    for (KeyValueRecord<?, V> entry : entries.getElements())
    {
      ComplexResourceKey<TestRecord, TestRecord> generatedKey =
          entry.getComplexKey(TestRecord.class, TestRecord.class);

      if (generatedKey.equals(id1))
      {
        checkComplexKeyIsReadOnly(id1, generatedKey);
      }
      else
      {
        checkComplexKeyIsReadOnly(id2, generatedKey);
      }

      Assert.assertTrue(generatedKey.isReadOnly());

      RecordTemplate value = entry.getValue(valueClass);
      if (value.equals(t1))
      {
        if (t1.data().isMadeReadOnly())
        {
          Assert.assertSame(t1.data(), value.data());
        }
        else
        {
          Assert.assertNotSame(t1.data(), value.data());
        }
      }
      else
      {
        if (t2.data().isReadOnly())
        {
          Assert.assertSame(t2.data(), value.data());
        }
        else
        {
          Assert.assertNotSame(t2.data(), value.data());
        }
      }

      Assert.assertTrue(value.data().isMadeReadOnly());
    }
  }

  private void checkComplexKeyIsReadOnly(ComplexResourceKey<TestRecord, TestRecord> originalKey,
                                           ComplexResourceKey<TestRecord, TestRecord> generatedKey)
  {
    if (originalKey.isReadOnly())
    {
      Assert.assertSame(originalKey.getKey().data(), generatedKey.getKey().data());
      Assert.assertSame(originalKey.getParams().data(), generatedKey.getParams().data());
    }
    else
    {
      Assert.assertNotSame(originalKey.getKey().data(), generatedKey.getKey().data());
      Assert.assertNotSame(originalKey.getParams().data(), generatedKey.getParams().data());
    }
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "complexKey")
  public Object[][] complexKey()
  {
    //Sample URIs:
    //"test/$params.id=2&$params.message=paramMessage&id=1&message=keyMessage"
    //"test/($params:(id:2,message:paramMessage),id:1,message:keyMessage)"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "test/$params.id=2&$params.message=paramMessage&id=1&message=keyMessage", null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "test/($params:(id:2,message:paramMessage),id:1,message:keyMessage)", null, null, null);

    return new Object[][] {
        {uriDetails1},
        {uriDetails2}
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "complexKey")
  public void testComplexKeyUpdateRequestBuilder(URIDetails expectedURIDetails)
  {
    UpdateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new UpdateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                         TestRecord.class,
                                                                                         _COMPLEX_KEY_SPEC,
                                                                                         RestliRequestOptions.DEFAULT_OPTIONS);
    ComplexResourceKey<TestRecord, TestRecord> key = buildComplexKey(1L, "keyMessage", 2L, "paramMessage");

    UpdateRequest<TestRecord> request = builder.id(key).input(new TestRecord()).build();

    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);
    checkBasicRequest(request,
                      expectedURIDetails,
                      ResourceMethod.UPDATE,
                      new TestRecord(),
                      Collections.<String, String> emptyMap());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntity")
  public void testComplexKeyCreateRequestBuilder(URIDetails expectedURIDetails)
  {
    CreateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new CreateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                         TestRecord.class,
                                                                                         _COMPLEX_KEY_SPEC,
                                                                                         RestliRequestOptions.DEFAULT_OPTIONS);
    CreateRequest<TestRecord> request = builder.input(new TestRecord()).build();

    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);
    checkBasicRequest(request,
                      expectedURIDetails,
                      ResourceMethod.CREATE,
                      new TestRecord(),
                      Collections.<String, String> emptyMap());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchComplexKey")
  public Object[][] batchComplexKey()
  {
    //Sample URIs:
    //"test?ids%5B0%5D.$params.id=2&ids%5B0%5D.$params.message=paramMessage1&ids%5B0%5D.id=1&ids%5B0%5D.message=keyMessage1&ids%5B1%5D.$params.id=4&ids%5B1%5D.$params.message=paramMessage2&ids%5B1%5D.id=3&ids%5B1%5D.message=keyMessage2"
    //"test?ids=List(($params:(id:2,message:paramMessage1),id:1,message:keyMessage1),($params:(id:4,message:paramMessage2),id:3,message:keyMessage2))"

    final Set<DataMap> idList = new HashSet<DataMap>();
    final DataMap idMapOne = new DataMap();
    idMapOne.put("id", "1");
    idMapOne.put("message", "keyMessage1");
    final DataMap paramMapOne = new DataMap();
    paramMapOne.put("id", "2");
    paramMapOne.put("message", "paramMessage1");
    idMapOne.put("$params", paramMapOne);
    idList.add(idMapOne);

    final DataMap idMapTwo = new DataMap();
    idMapTwo.put("id", "3");
    idMapTwo.put("message", "keyMessage2");
    final DataMap paramMapTwo = new DataMap();
    paramMapTwo.put("id", "4");
    paramMapTwo.put("message", "paramMessage2");
    idMapTwo.put("$params", paramMapTwo);
    idList.add(idMapTwo);

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        idList, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        idList, null, null);

    return new Object[][] {
        {uriDetails1},
        {uriDetails2}
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchComplexKey")
  public void testComplexKeyBatchPartialUpdateRequestBuilder(URIDetails expectedURIDetails)
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
    testBaseUriGeneration(request, expectedURIDetails.getProtocolVersion());

    // using .toStringFull (which is deprecated) because this is only used for checking v1
    @SuppressWarnings({"unchecked","rawtypes"})
    BatchRequest<PatchRequest<TestRecord>> batchRequest = new BatchRequest(new DataMap(), PatchRequest.class);
    batchRequest.getEntities().put(toEntityKey(key1, expectedURIDetails.getProtocolVersion()), patch1);
    batchRequest.getEntities().put(toEntityKey(key2, expectedURIDetails.getProtocolVersion()), patch2);

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

    checkBasicRequest(request, expectedURIDetails, ResourceMethod.BATCH_PARTIAL_UPDATE, collectionRequest, batchRequest,
        Collections.<String, String>emptyMap());
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testBatchPartialUpdateRequestInputIsReadOnly()
  {
    BatchPartialUpdateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new BatchPartialUpdateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(
            TEST_URI,
            TestRecord.class,
            _COMPLEX_KEY_SPEC,
            RestliRequestOptions.DEFAULT_OPTIONS);
    ComplexResourceKey<TestRecord, TestRecord> id1 =
        buildComplexKey(1L, "KeyMessage1", 10L, "ParamMessage1");
    ComplexResourceKey<TestRecord, TestRecord> id2 =
        buildComplexKey(2L, "KeyMessage2", 20L, "ParamMessage2");
    PatchRequest<TestRecord> t1 = PatchGenerator.diffEmpty(new TestRecord().setMessage("foo"));
    PatchRequest<TestRecord> t2 = PatchGenerator.diffEmpty(new TestRecord().setMessage("bar"));

    BatchPartialUpdateRequest<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> request =
        builder.input(id1, t1).input(id2, t2).build();

    checkKeyValueRecordCollectionIsReadOnly(id1,
                                              id2,
                                              t1,
                                              t2,
                                              PatchRequest.class,
                                              (CollectionRequest<KeyValueRecord<?, PatchRequest>>) request.getInputRecord());

    id1.makeReadOnly();
    t2.data().makeReadOnly();

    request = builder.input(id1, t1).input(id2, t2).build();

    checkKeyValueRecordCollectionIsReadOnly(id1,
                                              id2,
                                              t1,
                                              t2,
                                              PatchRequest.class,
                                              (CollectionRequest<KeyValueRecord<?, PatchRequest>>) request.getInputRecord());
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceAction1")
  public Object[][] subSubResourceAction1()
  {
    //Sample URIs:
    //"foo/1/bar/2/baz?action=action"
    //"foo/1/bar/2/baz?action=action"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("action", "action");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "foo/1/bar/2/baz", null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "foo/1/bar/2/baz", null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceAction1")
  public void testBuilderPathKeys1(URIDetails expectedURIDetails)
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

    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<TestRecord> request = new ActionRequestBuilder<Void, TestRecord>(SUBRESOURCE_URI, TestRecord.class, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS)
      .name("action").pathKey("key1", 1).pathKey("key2", 2).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_URI, expectedPathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceAction2")
  public Object[][] subSubResourceAction2()
  {
    //Sample URIs:
    //"foo/http%3A%2F%2Fexample.com%2Fimages%2F1.png/bar/http%3A%2F%2Fexample.com%2Fimages%2F2.png/baz?action=action"
    //"foo/http%3A%2F%2Fexample.com%2Fimages%2F1.png/bar/http%3A%2F%2Fexample.com%2Fimages%2F2.png/baz?action=action"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("action", "action");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "foo/http%3A%2F%2Fexample.com%2Fimages%2F1.png/bar/http%3A%2F%2Fexample.com%2Fimages%2F2.png/baz", null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "foo/http%3A%2F%2Fexample.com%2Fimages%2F1.png/bar/http%3A%2F%2Fexample.com%2Fimages%2F2.png/baz", null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceAction2")
  public void testBuilderPathKeys2(URIDetails expectedURIDetails)
  {
    ResourceSpec resourceSpec = getResourceSpecForBuilderPathKeys();

    // test with keys containing URL escaped chars
    Request<TestRecord> request = new ActionRequestBuilder<Void, TestRecord>(SUBRESOURCE_URI, TestRecord.class, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS)
      .name("action").pathKey("key1", "http://example.com/images/1.png").pathKey("key2", "http://example.com/images/2.png").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);

    Map<String, String> pathKeys1 = new HashMap<String, String>();
    pathKeys1.put("key1", "http://example.com/images/1.png");
    pathKeys1.put("key2", "http://example.com/images/2.png");
    testPathKeys(request, SUBRESOURCE_URI, pathKeys1);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceBatch")
  public Object[][] subSubResourceBatch()
  {
    //Sample URIs:
    //"foo/1/bar/2/baz?ids=1&ids=2"
    //"foo/1/bar/2/baz?ids=List(1,2)"

    final Set<String> idSet = new HashSet<String>();
    idSet.add("1");
    idSet.add("2");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/1/bar/2/baz",
        idSet, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/1/bar/2/baz",
        idSet, null, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceBatch")
  public void testBuilderPathKeys3(URIDetails expectedURIDetails)
  {
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<BatchResponse<TestRecord>> request = new BatchGetRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .ids(1L, 2L).pathKey("key1", 1).pathKey("key2", 2).build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_URI, expectedPathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceNoEntity")
  public Object[][] subSubResourceNoEntity()
  {
    //Sample URIs:
    //"foo/1/bar/2/baz"
    //"foo/1/bar/2/baz"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/1/bar/2/baz",
        null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/1/bar/2/baz",
        null, null, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceNoEntity")
  public void testBuilderPathKeys4(URIDetails expectedURIDetails)
  {
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<EmptyRecord> request = new CreateRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .pathKey("key1", 1).pathKey("key2", 2).build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_URI, expectedPathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceSingleEntity")
  public Object[][] subSubResourceSingleEntity()
  {
    //"foo/1/bar/2/baz/3"
    //"foo/1/bar/2/baz/3"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/1/bar/2/baz/3",
        null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/1/bar/2/baz/3",
        null, null, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceSingleEntity")
  public void testBuilderPathKeys5(URIDetails expectedURIDetails)
  {
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<EmptyRecord> request = new DeleteRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(3L).pathKey("key1", 1).pathKey("key2", 2).build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_URI, expectedPathKeys);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceNoEntity")
  public void testBuilderPathKeys6(URIDetails expectedURIDetails)
  {
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<CollectionResponse<TestRecord>> request = new FindRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .pathKey("key1", 1).pathKey("key2", 2).build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_URI, expectedPathKeys);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceSingleEntity")
  public void testBuilderPathKeys7(URIDetails expectedURIDetails)
  {
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(3L).pathKey("key1", 1).pathKey("key2", 2).build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_URI, expectedPathKeys);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceSingleEntity")
  public void testBuilderPathKeys8(URIDetails expectedURIDetails)
  {
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<EmptyRecord> request = new PartialUpdateRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(3L).pathKey("key1", 1).pathKey("key2", 2).build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_URI, expectedPathKeys);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subSubResourceSingleEntity")
  public void testBuilderPathKeys9(URIDetails expectedURIDetails)
  {
    Map<String, Integer> expectedPathKeys = new HashMap<String, Integer>();
    expectedPathKeys.put("key1", 1);
    expectedPathKeys.put("key2", 2);

    Request<EmptyRecord> request = new UpdateRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(3L).pathKey("key1", 1).pathKey("key2", 2).build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_URI, expectedPathKeys);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceAction1")
  public Object[][] subResourceAction1()
  {
    //Sample URIs:
    //"foo/bar/1/baz?action=action"
    //"foo/bar/1/baz?action=action"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("action", "action");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/bar/1/baz",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/bar/1/baz",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceAction1")
  public void testBuilderPathKeys10(URIDetails expectedURIDetails)
  {
    ResourceSpec resourceSpec = getResourceSpecForBuilderPathKeys();

    // simple resource & sub resources tests
    Request<TestRecord> request = new ActionRequestBuilder<Void, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS)
      .name("action").pathKey("key1", 1).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceAction2")
  public Object[][] subResourceAction2()
  {
    //Sample URIs:
    //"foo/bar/http%3A%2F%2Fexample.com%2Fimages%2F1.png/baz?action=action"
    //"foo/bar/http%3A%2F%2Fexample.com%2Fimages%2F1.png/baz?action=action"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("action", "action");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "foo/bar/http%3A%2F%2Fexample.com%2Fimages%2F1.png/baz", null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "foo/bar/http%3A%2F%2Fexample.com%2Fimages%2F1.png/baz", null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceAction2")
  public void testBuilderPathKeys11(URIDetails expectedURIDetails)
  {
    ResourceSpec resourceSpec = getResourceSpecForBuilderPathKeys();

    Request<TestRecord> request = new ActionRequestBuilder<Void, TestRecord>(
      SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS).name("action")
      .pathKey("key1", "http://example.com/images/1.png")
      .build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request,
                 SUBRESOURCE_SIMPLE_ROOT_URI,
                 Collections.singletonMap("key1", "http://example.com/images/1.png"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceBatch")
  public Object[][] subResourceBatch()
  {
    //Sample URIs:
    //"foo/bar/1/baz?ids=1&ids=2"
    //"foo/bar/1/baz?ids=List(1,2)"

    final Set<String> idSet = new HashSet<String>();
    idSet.add("1");
    idSet.add("2");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/bar/1/baz",
        idSet, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/bar/1/baz",
        idSet, null, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceBatch")
  public void testBuilderPathKeys12(URIDetails expectedURIDetails)
  {
    Request<BatchResponse<TestRecord>> request = new BatchGetRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .ids(1L, 2L).pathKey("key1", 1).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceNoEntity")
  public Object[][] subResourceNoEntity()
  {
    //Sample URIs:
    //"foo/bar/1/baz"
    //"foo/bar/1/baz"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/bar/1/baz",
        null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/bar/1/baz",
        null, null, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceNoEntity")
  public void testBuilderPathKeys13(URIDetails expectedURIDetails)
  {
    Request<EmptyRecord> request = new CreateRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .pathKey("key1", 1).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceSingleEntity")
  public Object[][] subResourceSingleEntity()
  {
    //Sample URIs:
    //"foo/bar/1/baz/2"
    //"foo/bar/1/baz/2"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/bar/1/baz/2",
        null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/bar/1/baz/2",
        null, null, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceSingleEntity")
  public void testBuilderPathKeys14(URIDetails expectedURIDetails)
  {
    Request<EmptyRecord> request = new DeleteRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(2L).pathKey("key1", 1).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceNoEntity")
  public void testBuilderPathKeys15(URIDetails expectedURIDetails)
  {
    Request<CollectionResponse<TestRecord>> request = new FindRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .pathKey("key1", 1).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceSingleEntity")
  public void testBuilderPathKeys16(URIDetails expectedURIDetails)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(2L).pathKey("key1", 1).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceSingleEntity")
  public void testBuilderPathKeys17(URIDetails expectedURIDetails)
  {
    Request<EmptyRecord> request = new PartialUpdateRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(2L).pathKey("key1", 1).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "subResourceSingleEntity")
  public void testBuilderPathKeys18(URIDetails expectedURIDetails)
  {
    Request<EmptyRecord> request = new UpdateRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_ROOT_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .id(2L).pathKey("key1", 1).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_SIMPLE_ROOT_URI, Collections.singletonMap("key1", 1));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceAction1")
  public Object[][] simpleSubResourceAction1()
  {
    //"foo/1/bar?action=action"
    //"foo/1/bar?action=action"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("action", "action");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/1/bar",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/1/bar",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceAction1")
  public void testBuilderPathKeys19(URIDetails expectedURIDetails)
  {
    ResourceSpec resourceSpec = getResourceSpecForBuilderPathKeys();

    Request<TestRecord> request = new ActionRequestBuilder<Void, TestRecord>(SUBRESOURCE_SIMPLE_SUB_URI, TestRecord.class, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS)
      .name("action").pathKey("key1", 1).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_SIMPLE_SUB_URI, Collections.singletonMap("key1", 1));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceAction2")
  public Object[][] simpleSubResourceAction2()
  {
    //Sample URIs:
    //"foo/http%3A%2F%2Fexample.com%2Fimages%2F1.png/bar?action=action"
    //"foo/http%3A%2F%2Fexample.com%2Fimages%2F1.png/bar?action=action"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("action", "action");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "foo/http%3A%2F%2Fexample.com%2Fimages%2F1.png/bar", null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "foo/http%3A%2F%2Fexample.com%2Fimages%2F1.png/bar", null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceAction2")
  public void testBuilderPathKeys20(URIDetails expectedURIDetails)
  {
    ResourceSpec resourceSpec = getResourceSpecForBuilderPathKeys();

    Request<TestRecord> request = new ActionRequestBuilder<Void, TestRecord>(
      SUBRESOURCE_SIMPLE_SUB_URI, TestRecord.class, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS).name("action")
      .pathKey("key1", "http://example.com/images/1.png")
      .build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request,
                 SUBRESOURCE_SIMPLE_SUB_URI,
                 Collections.singletonMap("key1", "http://example.com/images/1.png"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceNoEntity")
  public Object[][] simpleSubResourceNoEntity()
  {
    //Sample URIs:
    //"foo/1/bar"
    //"foo/1/bar"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "foo/1/bar",
        null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "foo/1/bar",
        null, null, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceNoEntity")
  public void testBuilderPathKeys21(URIDetails expectedURIDetails)
  {
    Request<EmptyRecord> request = new DeleteRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_SUB_URI, TestRecord.class,
                                                         _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS).pathKey("key1", 1).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_SIMPLE_SUB_URI, Collections.singletonMap("key1", 1));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceNoEntity")
  public void testBuilderPathKeys22(URIDetails expectedURIDetails)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_SUB_URI, TestRecord.class, _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
      .pathKey("key1", 1).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_SIMPLE_SUB_URI, Collections.singletonMap("key1", 1));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "simpleSubResourceNoEntity")
  public void testBuilderPathKeys23(URIDetails expectedURIDetails)
  {
    Request<EmptyRecord> request = new UpdateRequestBuilder<Long, TestRecord>(SUBRESOURCE_SIMPLE_SUB_URI, TestRecord.class,
                                                         _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS).pathKey("key1", 1).build();

    URIDetails.testUriGeneration(request, expectedURIDetails);
    testPathKeys(request, SUBRESOURCE_SIMPLE_SUB_URI, Collections.singletonMap("key1", 1));
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
    //Sample URIs:
    //"test?foo=bar"
    //"test?foo=bar"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("foo", "bar");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public void testCrudBuilderParams1(URIDetails expectedURIDetails)
  {
    Request<EmptyRecord> request = new CreateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "entityWithParam")
  public Object[][] entityWithParam()
  {
    //Sample URIs"
    //"test/3?foo=bar"
    //"test/3?foo=bar"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("foo", "bar");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/3",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/3",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "entityWithParam")
  public void testCrudBuilderParams2(URIDetails expectedURIDetails)
  {
    Request<EmptyRecord> request = new DeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public void testCrudBuilderParams3(URIDetails expectedURIDetails)
  {
    Request<CollectionResponse<TestRecord>> request = new FindRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "entityWithParam")
  public void testCrudBuilderParams4(URIDetails expectedURIDetails)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "entityWithParam")
  public void testCrudBuilderParams5(URIDetails expectedURIDetails)
  {
    Request<EmptyRecord> request = new PartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "entityWithParam")
  public void testCrudBuilderParams6(URIDetails expectedURIDetails)
  {
    Request<EmptyRecord> request = new UpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchWithParam")
  public Object[][] batchWithParam()
  {
    //Sample URIs:
    //"test?foo=bar&ids=1&ids=2"
    //"test?foo=bar&ids=List(1,2)"

    final Set<String> idSet = new HashSet<String>();
    idSet.add("1");
    idSet.add("2");

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("foo", "bar");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        idSet, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        idSet, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchWithParam")
  public void testCrudBuilderParams7(URIDetails expectedURIDetails)
  {
    Request<BatchResponse<TestRecord>> request = new BatchGetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .ids(1L, 2L).setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public void testCrudBuilderParams8(URIDetails expectedURIDetails)
  {
    Request<CollectionResponse<CreateStatus>> request = new BatchCreateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .input(new TestRecord()).setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchWithParam")
  public void testCrudBuilderParams9(URIDetails expectedURIDetails)
  {
    Request<BatchKVResponse<Long, UpdateStatus>>  request = new BatchDeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .ids(1L, 2L).setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchSingleWithParam")
  public Object[][] batchSingleWithParam()
  {
    //"test?foo=bar&ids=1"
    //"test?foo=bar&ids=List(1)"

    final Set<String> idSet = new HashSet<String>();
    idSet.add("1");

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("foo", "bar");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test",
        idSet, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test",
        idSet, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchSingleWithParam")
  public void testCrudBuilderParams10(URIDetails expectedURIDetails)
  {
    Request<BatchKVResponse<Long, UpdateStatus>> request = new BatchUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .input(1L, new TestRecord()).setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "batchSingleWithParam")
  public void testCrudBuilderParams11(URIDetails expectedURIDetails)
  {
    Request<BatchKVResponse<Long, UpdateStatus>> request = new BatchPartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .input(1L, new PatchRequest<TestRecord>()).setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public void testCrudBuilderParams12(URIDetails expectedURIDetails)
  {
    //Simple resource tests
    Request<EmptyRecord> request = new DeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public void testCrudBuilderParams13(URIDetails expectedURIDetails)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "noEntityWithParam")
  public void testCrudBuilderParams14(URIDetails expectedURIDetails)
  {
    Request<EmptyRecord> request = new UpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _SIMPLE_RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .setParam("foo", "bar").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingEqualsAndValue")
  public Object[][] encodingEqualsAnd1()
  {
    //Sample URIs:
    //"test/3?foo=bar%26baz%3Dqux"
    //"test/3?foo=bar%26baz%3Dqux"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("foo", "bar&baz=qux");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/3",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/3",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingEqualsAndValue")
  public void testParamEncodingEqualsAndValue(URIDetails expectedURIDetails)
  {
    GetRequest<?> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo", "bar&baz=qux").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingEqualsAndKey")
  public Object[][] encodingEqualsAnd2()
  {
    //Sample URIs:
    //"test/3?foo%26bar%3Dbaz=qux"
    //"test/3?foo%26bar%3Dbaz=qux"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("foo&bar=baz", "qux");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/3",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/3",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingEqualsAndKey")
  public void testParamEncodingEqualsAndKey(URIDetails expectedURIDetails)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo&bar=baz", "qux").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingSlash")
  public Object[][] encodingSlash()
  {
    //Sample URIs:
    //"test/3?foo/bar=baz/qux"
    //"test/3?foo/bar=baz/qux"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("foo/bar", "baz/qux");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/3",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/3",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingSlash")
  public void testParamEncodingSlash(URIDetails expectedURIDetails)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo/bar", "baz/qux").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingColon")
  public Object[][] encodingColon()
  {
    //Sample URIs:
    //"test/3?foo:bar=baz:qux"
    //"test/3?foo%3Abar=baz%3Aqux"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("foo:bar", "baz:qux");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/3",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/3",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingColon")
  public void testParamEncodingColon(URIDetails expectedURIDetails)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo:bar", "baz:qux").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingQuestionMark")
  public Object[][] encodingQuestionMark()
  {
    //Sample URIs:
    //"test/3?foo?bar=baz?qux"
    //"test/3?foo?bar=baz?qux"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("foo?bar", "baz?qux");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "test/3",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "test/3",
        null, queryParamsMap, null);

    return new Object[][] {
        { uriDetails1 },
        { uriDetails2 }
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "encodingQuestionMark")
  public void testParamEncodingQuestionMark(URIDetails expectedURIDetails)
  {
    Request<TestRecord> request = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(3L).setParam("foo?bar", "baz?qux").build();
    URIDetails.testUriGeneration(request, expectedURIDetails);
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
   * Tests equality of path keys and base uri template.
   */
  private void testPathKeys(Request<?> request,
                            String expectedBaseUriTemplate,
                            Map<String, ?> expectedPathKeys)
  {
    Assert.assertEquals(request.getBaseUriTemplate(), expectedBaseUriTemplate);
    Assert.assertEquals(request.getPathKeys(), expectedPathKeys);
  }

  private void testBaseUriGeneration(Request<?> request, ProtocolVersion version)
  {
    URI expectedBaseUri = URI.create(TEST_URI);
    Assert.assertEquals(RestliUriBuilderUtil.createUriBuilder(request, version).buildBaseUri(), expectedBaseUri);
    Assert.assertEquals(request.getBaseUriTemplate(), TEST_URI);
  }

  private void checkBasicRequest(Request<?> request,
                                 URIDetails expectedURIDetails,
                                 ResourceMethod expectedMethod,
                                 CollectionRequest<?> expectedInput,
                                 BatchRequest<?> expectedBatchInput,
                                 Map<String, String> expectedHeaders)
  {
    final ProtocolVersion version = expectedURIDetails.getProtocolVersion();
    checkBasicRequest(request, expectedURIDetails, expectedMethod, expectedInput, expectedHeaders);
    if (request.getMethod() == ResourceMethod.BATCH_UPDATE ||
        request.getMethod() == ResourceMethod.BATCH_PARTIAL_UPDATE)
    {
      // check the conversion
      checkInputForBatchUpdateAndPatch(request, expectedBatchInput, version);
    }
  }

  private void checkBasicRequest(Request<?> request,
                                 URIDetails expectedURIDetails,
                                 ResourceMethod expectedMethod,
                                 RecordTemplate expectedInput,
                                 Map<String, String> expectedHeaders)
  {
    URIDetails.testUriGeneration(request, expectedURIDetails);
    checkRequestIsReadOnly(request);
    Assert.assertEquals(request.getMethod(), expectedMethod);
    Assert.assertEquals(request.getHeaders(), expectedHeaders);

    if(expectedInput != null && (expectedMethod == ResourceMethod.BATCH_UPDATE ||
        expectedMethod == ResourceMethod.BATCH_PARTIAL_UPDATE || expectedMethod == ResourceMethod.BATCH_CREATE))
    {
      //The list of elements will need to be compared order independently because CollectionRequest has a list
      //which is constructed in a non-deterministic order for these 3 method types
      final List<Object> expectedElementList = (DataList)expectedInput.data().get(CollectionRequest.ELEMENTS);
      final Multiset<Object> expectedElementSet = HashMultiset.create(expectedElementList);
      final List<Object> actualElementList = (DataList)request.getInputRecord().data().get(CollectionRequest.ELEMENTS);
      final Multiset<Object> actualElementSet = HashMultiset.create(actualElementList);
      //TestNG has sporadic issues with comparing Sets for equality
      Assert.assertTrue(actualElementSet.equals(expectedElementSet), "The CollectRequest must be correct");
    }
    else
    {
      Assert.assertEquals(request.getInputRecord(), expectedInput);
    }
  }

  @SuppressWarnings("unchecked")
  private void checkRequestIsReadOnly(final Request<?> request)
  {
    final Set<PathSpec> fields = request.getFields();

    if (fields != null)
    {
      checkReadOnlyOperation(new Runnable()
      {
        @Override
        public void run()
        {
          fields.add(new PathSpec("abc"));
        }
      });
    }

    checkReadOnlyOperation(new Runnable()
    {
      @Override
      public void run()
      {
        request.getHeaders().put("abc", "abc");
      }
    });

    final RecordTemplate input = request.getInputRecord();

    if (input != null)
    {
      checkReadOnlyOperation(new Runnable()
      {
        @Override
        public void run()
        {
          input.data().put("abc", "abc");
        }
      });
    }

    final Map<String, Object> pathKeys = request.getPathKeys();

    if (pathKeys != null)
    {
      checkReadOnlyOperation(new Runnable()
      {
        @Override
        public void run()
        {
          pathKeys.put("abc", "abc");
        }
      });

      final List<Object> keysToEdit = new ArrayList<Object>();
      for (Object key: pathKeys.values())
      {
        if (key instanceof CompoundKey || key instanceof ComplexResourceKey)
        {
          keysToEdit.add(key);
        }
        else
        {
          Assert.assertTrue(isPrimitiveOrEnum(key));
        }
      }

      for (final Object keytoEdit: keysToEdit)
      {
        checkReadOnlyOperation(new Runnable()
        {
          @Override
          public void run()
          {
            if (keytoEdit instanceof ComplexResourceKey)
            {
              ((ComplexResourceKey) keytoEdit).getKey().data().put("abc", "abc");
            }
            else if (keytoEdit instanceof CompoundKey)
            {
              ((CompoundKey) keytoEdit).append("abc", "abc");
            }
          }
        });
      }

      Collection<Object> queryParamObjects = request.getQueryParamsObjects().values();
      List<Object> readOnlyTargets = new ArrayList<Object>();

      for (Object queryParamObject: queryParamObjects)
      {
        collectReadOnlyQueryParamObjectTargets(queryParamObject, readOnlyTargets);
      }

      for (final Object readOnlyTarget: readOnlyTargets)
      {
        checkReadOnlyOperation(new Runnable()
        {
          @Override
          public void run()
          {
            if (readOnlyTarget instanceof DataTemplate)
            {
              Object data = ((DataTemplate)readOnlyTarget).data();

              if (data instanceof DataMap)
              {
                ((DataMap) data).put("abc", "abc");
              }
              else if (data instanceof DataList)
              {
                ((DataList) data).add("abc");
              }
            }
            else if (readOnlyTarget instanceof CompoundKey)
            {
              ((CompoundKey) readOnlyTarget).append("abc", "abc");
            }
            else if (readOnlyTarget instanceof ComplexResourceKey)
            {
              ((ComplexResourceKey) readOnlyTarget).getKey().data().put("abc", "abc");
            }
            else if (readOnlyTarget instanceof List)
            {
              ((List<Object>) readOnlyTarget).add("abc");
            }
          }
        });
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void collectReadOnlyQueryParamObjectTargets(Object queryParamObject, List<Object> readOnlyTargets)
  {
    if (queryParamObject instanceof List)
    {
      readOnlyTargets.add(queryParamObject);
      for (Object item: ((List<Object>) queryParamObject))
      {
        collectReadOnlyQueryParamObjectTargets(item, readOnlyTargets);
      }
    }
    else if (queryParamObject instanceof DataTemplate)
    {
      Object data = ((DataTemplate)queryParamObject).data();

      if (data instanceof DataMap || data instanceof DataList)
      {
        readOnlyTargets.add(queryParamObject);
      }
      else
      {
        //As more tests are added it is likely to add more custom types as data for data templates.
        Assert.assertTrue(isPrimitiveOrEnum(data),
                          "Unknown type for data template data object: " + data.getClass().toString());
      }
    }
    else if (queryParamObject instanceof CompoundKey ||
             queryParamObject instanceof ComplexResourceKey)
    {
      readOnlyTargets.add(queryParamObject);
    }
    else
    {
      //As more tests are added it is likely to add more custom types as query parameter object types.
      Assert.assertTrue(isPrimitiveOrEnum(queryParamObject) || queryParamObject instanceof PathSpec,
                        "Unknown type for query parameter object: " + queryParamObject.getClass().toString());

    }
  }

  private boolean isPrimitiveOrEnum(Object o)
  {
    Class<?> clazz = o.getClass();

    return clazz.isEnum() ||
          clazz == String.class ||
          clazz == Integer.class ||
          clazz == Double.class ||
          clazz == Boolean.class ||
          clazz == Long.class ||
          clazz == Float.class ||
          clazz == ByteString.class ||
          clazz == Null.class;
  }

  private void checkReadOnlyOperation(Runnable runnable)
  {
    try
    {
      runnable.run();
      Assert.fail("Read-only field updated.");
    }
    catch (UnsupportedOperationException ex)
    {
    }
  }

  /**
   * Converts the new request body encoding into the old format and then checks that the conversion matches the expected
   * old format input.
   *
   * This method can be removed once we stop conversion of CollectionRequest to BatchRequest for BatchUpdates and
   * BatchPartialUpdates.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void checkInputForBatchUpdateAndPatch(Request<?> request, RecordTemplate expectedInput, ProtocolVersion version)
  {
    final TypeSpec<? extends RecordTemplate> valueType =
        request.getMethod() == ResourceMethod.BATCH_PARTIAL_UPDATE ?
          new TypeSpec<PatchRequest>(PatchRequest.class) :
          request.getResourceProperties().getValueType();

    Assert.assertEquals(
      CollectionRequestUtil.convertToBatchRequest(
        (CollectionRequest<KeyValueRecord>) request.getInputRecord(),
        request.getResourceProperties().getKeyType(),
        request.getResourceProperties().getComplexKeyType(),
        request.getResourceProperties().getKeyParts(),
        valueType,
        version),
      expectedInput);
  }

  /**
   * Builds a {@link CollectionRequest} consisting of {@link KeyValueRecord}s
   */
  @SuppressWarnings({"rawtypes"})
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
