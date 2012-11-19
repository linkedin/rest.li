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

/**
 * $Id: $
 */

package com.linkedin.restli.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.DynamicRecordTemplate;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchRequest;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class TestClientBuilders
{
  public static final String TEST_URI = "test";
  public static final String SUBRESOURCE_URI = "foo/{key1}/bar/{key2}/baz";
  private static final ResourceSpec _COLL_SPEC      =
                                                        new ResourceSpecImpl(EnumSet.allOf(ResourceMethod.class),
                                                                             Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                             Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                             Long.class,
                                                                             null,
                                                                             null,
                                                                             TestRecord.class,
                                                                             Collections.<String, Class<?>> emptyMap());
  private static final ResourceSpec _ASSOC_SPEC     =
                                                        new ResourceSpecImpl(EnumSet.allOf(ResourceMethod.class),
                                                                             Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                             Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                             CompoundKey.class,
                                                                             null,
                                                                             null,
                                                                             TestRecord.class,
                                                                             Collections.<String, Class<?>> emptyMap());
  private static final ResourceSpec _COMPLEX_KEY_SPEC =
                                                          new ResourceSpecImpl(EnumSet.allOf(ResourceMethod.class),
                                                                               Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                               Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                                               ComplexResourceKey.class,
                                                                               TestRecord.class,
                                                                               TestRecord.class,
                                                                               TestRecord.class,
                                                                               Collections.<String, Class<?>> emptyMap());

  @Test
  public void testActionRequestBuilder()
  {
    FieldDef<String> pParam = new FieldDef<String>("p", String.class, DataTemplateUtil.getSchema(String.class));
    Map<String, DynamicRecordMetadata> requestMetadataMap = new HashMap<String, DynamicRecordMetadata>();
    DynamicRecordMetadata requestMetadata = new DynamicRecordMetadata("action", Collections.<FieldDef<?>>singleton(pParam));
    requestMetadataMap.put("action", requestMetadata);
    DynamicRecordMetadata responseMetadata = new DynamicRecordMetadata("action", Collections.<FieldDef<?>>emptyList());
    Map<String, DynamicRecordMetadata> responseMetadataMap = new HashMap<String, DynamicRecordMetadata>();
    responseMetadataMap.put("action", responseMetadata);
    ResourceSpec resourceSpec = new ResourceSpecImpl(Collections.<ResourceMethod>emptySet(), requestMetadataMap, responseMetadataMap);

    ActionRequestBuilder<Long, TestRecord> builder = new ActionRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class,
                                                                                                resourceSpec);

    ActionRequest<TestRecord> request = builder.name("action").param(pParam, "42").id(1L).build();

    Assert.assertEquals(request.getUri(), URI.create("test/1?action=action"));
    Assert.assertEquals(request.getMethod(), ResourceMethod.ACTION);
    Assert.assertEquals(request.getHeaders(), Collections.<String, String>emptyMap());
    Assert.assertEquals(request.getInput().data().get("p"), "42");
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);
  }

  @Test
  public void testBatchGetRequestBuilder()
  {
    BatchGetRequestBuilder<Long, TestRecord> builder = new BatchGetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class,
                                                                                                    _COLL_SPEC);
    BatchGetRequest<TestRecord> request = builder.ids(1L, 2L, 3L).fields(TestRecord.fields().id(), TestRecord.fields().message()).build();
    Assert.assertEquals(request.getBaseURI(), URI.create(TEST_URI));
    Assert.assertEquals(request.getIds(), new HashSet<String>(Arrays.asList("1", "2", "3")));
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
            TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, "test?fields=message,id&ids=1&ids=2&ids=3", ResourceMethod.BATCH_GET,
                      null, Collections.<String, String>emptyMap());
  }

  @Test
  public void testBatchGetCompKeyRequestBuilder()
  {
    BatchGetRequestBuilder<CompoundKey, TestRecord> builder = new BatchGetRequestBuilder<CompoundKey, TestRecord>(TEST_URI, TestRecord.class, _ASSOC_SPEC);
    CompoundKey key1 = new CompoundKey();
    key1.append("equals", "=");
    key1.append("ampersand", "&");
    CompoundKey key2 = new CompoundKey();
    key2.append("equals", "==");
    key2.append("ampersand", "&&");

    BatchGetRequest<TestRecord> request = builder.ids(key1,key2).fields(TestRecord.fields().id(), TestRecord.fields().message()).build();

    Assert.assertEquals(request.getBaseURI(), URI.create(TEST_URI));

    // Compare key sets. Note that have to convert keys to Strings as the request internally converts them to string
    HashSet<String> expectedIds = new HashSet<String>(Arrays.asList(key1.toString(), key2.toString()));
    Assert.assertEquals(request.getIds(), expectedIds);
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
            TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);
    // Note double encoding - one comes from CompoundKey.toString, another - from BatchGetRequestBuilder.ids().
    checkBasicRequest(request,
                      "test?fields=message,id&ids=ampersand%3D%2526%2526%26equals%3D%253D%253D&ids=ampersand%3D%2526%26equals%3D%253D",
                      ResourceMethod.BATCH_GET,
                      null,
                      Collections.<String, String>emptyMap());
  }

  @Test
  public void testBatchGetRequestBuilderCollectionIds()
  {
    BatchGetRequestBuilder<Long, TestRecord> builder = new BatchGetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC);
    List<Long> ids = Arrays.asList(1L, 2L, 3L);
    BatchGetRequest<TestRecord> request = builder.ids(ids).fields(TestRecord.fields().id(), TestRecord.fields().message()).build();
    Assert.assertEquals(request.getBaseURI(), URI.create(TEST_URI));
    Assert.assertEquals(request.getIds(), new HashSet<String>(Arrays.asList("1", "2", "3")));
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
            TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, "test?fields=message,id&ids=1&ids=2&ids=3", ResourceMethod.BATCH_GET, null, Collections.<String, String>emptyMap());
  }


  @Test
  public void testBatchUpdateRequestBuilder()
  {
    BatchUpdateRequestBuilder<Long, TestRecord> builder =
            new BatchUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC);
    Map<Long, TestRecord> updates = new HashMap<Long, TestRecord>();
    updates.put(1L, new TestRecord());
    updates.put(2L, new TestRecord());
    updates.put(3L, new TestRecord());
    BatchUpdateRequest<Long, TestRecord> request = builder.inputs(updates).build();
    Assert.assertEquals(request.getBaseURI(), URI.create(TEST_URI));
    Assert.assertEquals(request.getIds(), new HashSet<String>(Arrays.asList("1", "2", "3")));
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    BatchRequest<TestRecord> expectedRequest = new BatchRequest<TestRecord>(new DataMap(), TestRecord.class);
    expectedRequest.getEntities().put("1", new TestRecord());
    expectedRequest.getEntities().put("2", new TestRecord());
    expectedRequest.getEntities().put("3", new TestRecord());
    checkBasicRequest(request, "test?ids=1&ids=2&ids=3", ResourceMethod.BATCH_UPDATE,
                      expectedRequest,
                      Collections.<String, String>emptyMap());
  }

  @Test
  public void testBatchPartialUpdateRequestBuilder()
  {
    BatchPartialUpdateRequestBuilder<Long, TestRecord> builder =
            new BatchPartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC);

    builder.input(1L, new PatchRequest<TestRecord>());
    builder.input(2L, new PatchRequest<TestRecord>());
    builder.input(3L, new PatchRequest<TestRecord>());
    BatchPartialUpdateRequest<Long, TestRecord> request = builder.build();
    Assert.assertEquals(request.getBaseURI(), URI.create(TEST_URI));
    Assert.assertEquals(request.getIds(), new HashSet<String>(Arrays.asList("1", "2", "3")));
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    @SuppressWarnings({"unchecked"})
    BatchRequest<PatchRequest<TestRecord>> expectedRequest = new BatchRequest(new DataMap(), PatchRequest.class);
    expectedRequest.getEntities().put("1", new PatchRequest<TestRecord>());
    expectedRequest.getEntities().put("2", new PatchRequest<TestRecord>());
    expectedRequest.getEntities().put("3", new PatchRequest<TestRecord>());
    checkBasicRequest(request, "test?ids=1&ids=2&ids=3", ResourceMethod.BATCH_PARTIAL_UPDATE, expectedRequest,
                      Collections.<String, String>emptyMap());
  }

  @Test
  public void testBatchDeleteRequestBuilder()
  {
    BatchDeleteRequestBuilder<Long, TestRecord> builder =
            new BatchDeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC);
    BatchDeleteRequest<Long, TestRecord> request = builder.ids(1L, 2L, 3L).build();
    Assert.assertEquals(request.getBaseURI(), URI.create(TEST_URI));
    Assert.assertEquals(request.getIds(), new HashSet<String>(Arrays.asList("1", "2", "3")));
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, "test?ids=1&ids=2&ids=3", ResourceMethod.BATCH_DELETE, null, Collections.<String, String>emptyMap());
  }

  @Test
  public void testBatchCreateRequestBuilder()
  {
    BatchCreateRequestBuilder<Long, TestRecord> builder =
            new BatchCreateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC);
    List<TestRecord> newRecords = Arrays.asList(new TestRecord(), new TestRecord(), new TestRecord());
    BatchCreateRequest<TestRecord> request = builder.inputs(newRecords).build();
    Assert.assertEquals(request.getBaseURI(), URI.create(TEST_URI));
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    CollectionRequest<TestRecord> expectedRequest = new CollectionRequest<TestRecord>(new DataMap(), TestRecord.class);
    expectedRequest.getElements().addAll(newRecords);
    checkBasicRequest(request, "test", ResourceMethod.BATCH_CREATE,
                      expectedRequest,
                      Collections.<String, String>emptyMap());
  }


  @Test
  public void testCreateRequestBuilder()
  {
    CreateRequestBuilder<Long, TestRecord> builder = new CreateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class,
                                                                                                _COLL_SPEC);
    CreateRequest<TestRecord> request = builder.input(new TestRecord()).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    checkBasicRequest(request, "test", ResourceMethod.CREATE, new TestRecord(), Collections.<String, String>emptyMap());
  }

  @Test
  public void testDeleteRequestBuilder()
  {
    DeleteRequestBuilder<Long, TestRecord> builder = new DeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class,
                                                                                                _COLL_SPEC);
    DeleteRequest<TestRecord> request = builder.id(1L).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, "test/1", ResourceMethod.DELETE, null, Collections.<String, String>emptyMap());
  }

  @Test
  public void testFindRequestBuilder()
  {
    FindRequestBuilder<Long, TestRecord> builder = new FindRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class,
                                                                                            _COLL_SPEC);
    FindRequest<TestRecord> request =
        builder.name("search")
               .assocKey("key", "a:b")
               .fields(TestRecord.fields().id(), TestRecord.fields().message())
               .param("p", 42)
               .build();
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request,
                      "test/key=a%3Ab?fields=message,id&p=42&q=search",
                      ResourceMethod.FINDER,
                      null,
                      Collections.<String, String>emptyMap());
  }

  @Test
  public void testGetAllRequestBuilder()
  {
    GetAllRequestBuilder<Long, TestRecord> builder =
        new GetAllRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC);

    GetAllRequest<TestRecord> request =
        builder.paginate(1, 4)
               .fields(TestRecord.fields().id(), TestRecord.fields().message())
               .build();
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);
    checkBasicRequest(request,
                      "test?count=4&fields=message,id&start=1",
                      ResourceMethod.GET_ALL,
                      null,
                      Collections.<String, String>emptyMap());
  }

  @Test
  public void testGetRequestBuilder()
  {
    GetRequestBuilder<Long, TestRecord> builder = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC);
    GetRequest<TestRecord> request = builder.id(1L).fields(TestRecord.fields().id(), TestRecord.fields().message()).build();
    Assert.assertEquals(request.getBaseURI(), URI.create(TEST_URI));
    Assert.assertEquals(request.getId(), String.valueOf(1L));
    Assert.assertEquals(request.getFields(), new HashSet<PathSpec>(Arrays.asList(
            TestRecord.fields().id(), TestRecord.fields().message())));
    Assert.assertEquals(request.isSafe(), true);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, "test/1?fields=message,id", ResourceMethod.GET, null, Collections.<String, String>emptyMap());
  }

  @Test
  public void testPartialUpdateRequestBuilder() throws Exception
  {
    PartialUpdateRequestBuilder<Long, TestRecord> builder = new PartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class,
                                                                                                              _COLL_SPEC);
    TestRecord t1 = new TestRecord();
    TestRecord t2 = new TestRecord(t1.data().copy());
    t2.setId(1L);
    t2.setMessage("Foo Bar Baz");
    PatchRequest<TestRecord> patch = PatchGenerator.diff(t1, t2);
    PartialUpdateRequest<TestRecord> request = builder.id(1L).input(patch).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), false);

    checkBasicRequest(request, "test/1", ResourceMethod.PARTIAL_UPDATE, patch, Collections.<String, String>emptyMap());
  }

  @Test
  public void testUpdateRequestBuilder()
  {
    UpdateRequestBuilder<Long, TestRecord> builder = new UpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class,
                                                                                                _COLL_SPEC);
    UpdateRequest<TestRecord> request = builder.id(1L).input(new TestRecord()).build();
    Assert.assertEquals(request.isSafe(), false);
    Assert.assertEquals(request.isIdempotent(), true);

    checkBasicRequest(request, "test/1", ResourceMethod.UPDATE, new TestRecord(), Collections.<String, String>emptyMap());
  }

  @Test
  public void testComplexKeyGetRequestBuilder() throws Exception
  {
    GetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new GetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                      TestRecord.class,
                                                                                      _COMPLEX_KEY_SPEC);
    ComplexResourceKey<TestRecord, TestRecord> id =
        buildComplexKey(1L, "KeyMessage", 10L, "ParamMessage");
    RecordTemplate param1 = buildComplexParam(123, "ParamMessage");

    GetRequest<TestRecord> request = builder.id(id).param("testParam", param1).build();
    Assert.assertTrue(request.isSafe());
    Assert.assertTrue(request.isIdempotent());
    checkBasicRequest(request,
                      "test/id=1&message=KeyMessage&$params.message=ParamMessage&$params.id=10?testParam.id=123&testParam.message=ParamMessage",
                      ResourceMethod.GET,
                      null,
                      Collections.<String, String> emptyMap());
  }

  @Test
  public void testComplexKeyDeleteRequestBuilder() throws Exception
  {
    DeleteRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new DeleteRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                         TestRecord.class,
                                                                                         _COMPLEX_KEY_SPEC);
    ComplexResourceKey<TestRecord, TestRecord> id =
        buildComplexKey(1L, "KeyMessage", 10L, "ParamMessage");
    RecordTemplate param = buildComplexParam(123, "ParamMessage");

    DeleteRequest<TestRecord> request = builder.id(id).param("testParam", param).build();
    Assert.assertFalse(request.isSafe());
    Assert.assertTrue(request.isIdempotent());
    checkBasicRequest(request,
                      "test/id=1&message=KeyMessage&$params.message=ParamMessage&$params.id=10?testParam.id=123&testParam.message=ParamMessage",
                      ResourceMethod.DELETE,
                      null,
                      Collections.<String, String> emptyMap());
  }

  @Test
  public void testComplexKeyBatchGetRequestBuilder() throws Exception
  {
    BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                           TestRecord.class,
                                                                                           _COMPLEX_KEY_SPEC);
    ComplexResourceKey<TestRecord, TestRecord> id1 =
        buildComplexKey(1L, "KeyMessage1", 10L, "ParamMessage1");
    ComplexResourceKey<TestRecord, TestRecord> id2 =
        buildComplexKey(2L, "KeyMessage2", 20L, "ParamMessage2");
    RecordTemplate param = buildComplexParam(123, "ParamMessage");

    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> request = builder.ids(id1, id2).param("testParam", param).build();
    Assert.assertTrue(request.isIdempotent());
    Assert.assertTrue(request.isSafe());
    checkBasicRequest(request,
                      "test?ids%5B0%5D.$params.id=10&ids%5B0%5D.$params.message=ParamMessage1&ids%5B0%5D.id=1&ids%5B0%5D.message=KeyMessage1&ids%5B1%5D.$params.id=20&ids%5B1%5D.$params.message=ParamMessage2&ids%5B1%5D.id=2&ids%5B1%5D.message=KeyMessage2&testParam.id=123&testParam.message=ParamMessage",
                      ResourceMethod.BATCH_GET,
                      null,
                      Collections.<String, String>emptyMap());
  }

  @Test
  public void testComplexKeyBatchUpdateRequestBuilder() throws Exception
  {
    BatchUpdateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new BatchUpdateRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(TEST_URI,
                                                                                              TestRecord.class,
                                                                                              _COMPLEX_KEY_SPEC);
    ComplexResourceKey<TestRecord, TestRecord> id1 =
        buildComplexKey(1L, "KeyMessage1", 10L, "ParamMessage1");
    ComplexResourceKey<TestRecord, TestRecord> id2 =
        buildComplexKey(2L, "KeyMessage2", 20L, "ParamMessage2");
    RecordTemplate param = buildComplexParam(123, "ParamMessage");

    BatchUpdateRequest<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> request =
        builder.input(id1, new TestRecord()).input(id2, new TestRecord()).param("testParam", param).build();

    BatchRequest<TestRecord> expectedRequest = new BatchRequest<TestRecord>(new DataMap(), TestRecord.class);
    expectedRequest.getEntities().put(id1.toStringFull(), new TestRecord());
    expectedRequest.getEntities().put(id2.toStringFull(), new TestRecord());

    Assert.assertTrue(request.isIdempotent());
    Assert.assertFalse(request.isSafe());
    checkBasicRequest(request,
                      "test?ids%5B0%5D.$params.id=10&ids%5B0%5D.$params.message=ParamMessage1&ids%5B0%5D.id=1&ids%5B0%5D.message=KeyMessage1&ids%5B1%5D.$params.id=20&ids%5B1%5D.$params.message=ParamMessage2&ids%5B1%5D.id=2&ids%5B1%5D.message=KeyMessage2&testParam.id=123&testParam.message=ParamMessage",
                      ResourceMethod.BATCH_UPDATE,
                      expectedRequest,
                      Collections.<String, String> emptyMap());
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

  @Test
  public void testBuilderPathKeys()
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
    ResourceSpec resourceSpec = new ResourceSpecImpl(Collections.<ResourceMethod>emptySet(), requestMetadataMap, responseMetadataMap);

    URI uri;
    uri = new ActionRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, resourceSpec).name("action").pathKey("key1", 1).pathKey("key2", 2).build().getUri();
    Assert.assertEquals(uri, URI.create("foo/1/bar/2/baz?action=action"));

    uri = new BatchGetRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, _COLL_SPEC).ids(1L,2L).pathKey("key1", 1).pathKey("key2", 2).build().getUri();
    Assert.assertEquals(uri, URI.create("foo/1/bar/2/baz?ids=1&ids=2"));

    uri = new CreateRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, null).pathKey("key1", 1).pathKey("key2", 2).build().getUri();
    Assert.assertEquals(uri, URI.create("foo/1/bar/2/baz"));

    uri = new DeleteRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, null).id(3L).pathKey("key1", 1).pathKey("key2", 2).build().getUri();
    Assert.assertEquals(uri, URI.create("foo/1/bar/2/baz/3"));

    uri = new FindRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, null).pathKey("key1", 1).pathKey("key2", 2).build().getUri();
    Assert.assertEquals(uri, URI.create("foo/1/bar/2/baz"));

    uri = new GetRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, null).id(3L).pathKey("key1", 1).pathKey("key2", 2).build().getUri();
    Assert.assertEquals(uri, URI.create("foo/1/bar/2/baz/3"));

    uri = new PartialUpdateRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, null).id(3L).pathKey("key1", 1).pathKey("key2", 2).build().getUri();
    Assert.assertEquals(uri, URI.create("foo/1/bar/2/baz/3"));

    uri = new UpdateRequestBuilder<Long, TestRecord>(SUBRESOURCE_URI, TestRecord.class, null).id(3L).pathKey("key1", 1).pathKey("key2", 2).build().getUri();
    Assert.assertEquals(uri, URI.create("foo/1/bar/2/baz/3"));
  }

  @Test
  public void testCrudBuilderParams()
  {

    URI uri;
    uri = new CreateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, null).param("foo", "bar").build().getUri();
    Assert.assertEquals(uri, URI.create("test?foo=bar"));

    uri = new DeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, null).id(3L).param("foo", "bar").build().getUri();
    Assert.assertEquals(uri, URI.create("test/3?foo=bar"));

    uri = new FindRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, null).param("foo", "bar").build().getUri();
    Assert.assertEquals(uri, URI.create("test?foo=bar"));

    uri = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, null).id(3L).param("foo", "bar").build().getUri();
    Assert.assertEquals(uri, URI.create("test/3?foo=bar"));

    uri = new PartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, null).id(3L).param("foo", "bar").build().getUri();
    Assert.assertEquals(uri, URI.create("test/3?foo=bar"));

    uri = new UpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, null).id(3L).param("foo", "bar").build().getUri();
    Assert.assertEquals(uri, URI.create("test/3?foo=bar"));

    uri = new BatchGetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC).ids(1L,2L).param("foo", "bar").build().getUri();
    Assert.assertEquals(uri, URI.create("test?foo=bar&ids=1&ids=2"));

    uri = new BatchCreateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC).input(
            new TestRecord()).param("foo", "bar").build().getUri();
    Assert.assertEquals(uri, URI.create("test?foo=bar"));

    uri = new BatchDeleteRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC).ids(1L,2L).param("foo", "bar").build().getUri();
    Assert.assertEquals(uri, URI.create("test?foo=bar&ids=1&ids=2"));

    uri = new BatchUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC).input(
            1L, new TestRecord()).param("foo", "bar").build().getUri();
    Assert.assertEquals(uri, URI.create("test?foo=bar&ids=1"));

    uri = new BatchPartialUpdateRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, _COLL_SPEC).input(
            1L, new PatchRequest<TestRecord>()).param("foo", "bar").build().getUri();
    Assert.assertEquals(uri, URI.create("test?foo=bar&ids=1"));

  }

  @Test
  public void testParamEncoding()
  {
    URI uri;

    uri = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, null).id(3L).param("foo", "bar&baz=qux").build().getUri();
    Assert.assertEquals(uri, URI.create("test/3?foo=bar%26baz%3Dqux"));

    uri = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, null).id(3L).param("foo&bar=baz", "qux").build().getUri();
    Assert.assertEquals(uri, URI.create("test/3?foo%26bar%3Dbaz=qux"));

    uri = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, null).id(3L).param("foo/bar", "baz/qux").build().getUri();
    Assert.assertEquals(uri, URI.create("test/3?foo/bar=baz/qux"));

    uri = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, null).id(3L).param("foo:bar", "baz:qux").build().getUri();
    Assert.assertEquals(uri, URI.create("test/3?foo:bar=baz:qux"));

    uri = new GetRequestBuilder<Long, TestRecord>(TEST_URI, TestRecord.class, null).id(3L).param("foo?bar", "baz?qux").build().getUri();
    Assert.assertEquals(uri, URI.create("test/3?foo?bar=baz?qux"));
  }


  private void checkBasicRequest(Request<?> request,
                                 String expectedUri,
                                 ResourceMethod expectedMethod,
                                 RecordTemplate expectedInput,
                                 Map<String, String> expectedHeaders)
  {
    Assert.assertEquals(request.getUri(), URI.create(expectedUri));
    Assert.assertEquals(request.getMethod(), expectedMethod);
    Assert.assertEquals(request.getInput(), expectedInput);
    Assert.assertEquals(request.getHeaders(), expectedHeaders);
  }


}
