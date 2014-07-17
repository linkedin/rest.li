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


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.jersey.core.util.MultivaluedMap;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.BatchResponseDecoder;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.common.QueryParamsDataMap;
import com.linkedin.restli.internal.common.URIElementParser;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Unit test for BatchGetRequestBuilder.
 *
 * @author Eran Leshem
 */
public class BatchGetRequestBuilderTest
{
  private static final TestRecord.Fields FIELDS = TestRecord.fields();
  private static final ResourceSpecImpl _complexResourceSpec =
      new ResourceSpecImpl(Collections.<ResourceMethod> emptySet(),
                           Collections.<String,DynamicRecordMetadata>emptyMap(),
                           Collections.<String,DynamicRecordMetadata>emptyMap(),
                           ComplexResourceKey.class,
                           TestRecord.class,
                           TestRecord.class,
                           null,
                           Collections.<String, Class<?>> emptyMap());

  private static final ResourceSpecImpl _compoundResourceSpec =
      new ResourceSpecImpl(Collections.<ResourceMethod> emptySet(),
                           Collections.<String,DynamicRecordMetadata>emptyMap(),
                           Collections.<String,DynamicRecordMetadata>emptyMap(),
                           MyCompoundKey.class,
                           TestRecord.class,
                           TestRecord.class,
                           null,
                           createKeyParts());

  private static Map<String, CompoundKey.TypeInfo> createKeyParts()
  {
    Map<String, CompoundKey.TypeInfo> keyParts = new HashMap<String, CompoundKey.TypeInfo>();
    keyParts.put("age", new CompoundKey.TypeInfo(Integer.class, Integer.class));
    return keyParts;
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  @SuppressWarnings("unchecked")
  public void testBuildFailureForComplexKeys()
  {
    BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> builder =
        new BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>(
            "http://greetings",
            new BatchResponseDecoder<TestRecord>(TestRecord.class),
            _complexResourceSpec,
            RestliRequestOptions.DEFAULT_OPTIONS);

    builder.ids(
        Arrays.asList(
            new ComplexResourceKey<TestRecord, TestRecord>(
                new TestRecord().setId(1L),
                new TestRecord().setId(5L))));
    builder.build();
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testBuildFailureForCompoundKeys()
  {
    BatchGetRequestBuilder<CompoundKey, TestRecord> builder =
        new BatchGetRequestBuilder<CompoundKey, TestRecord>(
            "http://greetings",
            new BatchResponseDecoder<TestRecord>(TestRecord.class),
            _compoundResourceSpec,
            RestliRequestOptions.DEFAULT_OPTIONS);

    builder.ids(
        Arrays.asList(
            new CompoundKey().append("abc", 5)));

    builder.build();
  }

  @Test
  public void testBatchConversion()
      throws URISyntaxException
  {
    String expectedProtocol1Uri = "/?fields=message,id&ids=1&param=paramValue";
    String expectedProtocol2Uri = "/?fields=message,id&ids=List(1)&param=paramValue";

    GetRequestBuilder<Integer, TestRecord> requestBuilder =
        new GetRequestBuilder<Integer, TestRecord>("/",
                                                   TestRecord.class,
                                                   new ResourceSpecImpl(Collections.<ResourceMethod>emptySet(),
                                                                        null,
                                                                        null,
                                                                        int.class,
                                                                        TestRecord.class,
                                                                        Collections.<String, Object>emptyMap()),
                                                   RestliRequestOptions.DEFAULT_OPTIONS);
    requestBuilder.id(1)
                  .fields(FIELDS.id(), FIELDS.message())
                  .setParam("param", "paramValue");
    GetRequest<TestRecord> request = requestBuilder.build();
    BatchGetRequest<TestRecord> batchRequest = BatchGetRequestBuilder.batch(request);
    Assert.assertEquals(batchRequest.getBaseUriTemplate(), request.getBaseUriTemplate());
    Assert.assertEquals(batchRequest.getPathKeys(), request.getPathKeys());
    testUriGeneration(batchRequest, expectedProtocol1Uri, expectedProtocol2Uri);
    Assert.assertEquals(batchRequest.getFields(), request.getFields());
    Assert.assertEquals(batchRequest.getObjectIds(), new HashSet<Object>(Arrays.asList(request.getObjectId())));
  }

  @Test
  public void testBatchKVConversion()
      throws URISyntaxException
  {
    String expectedProtocol1Uri = "/?fields=message,id&ids=1&param=paramValue";
    String expectedProtocol2Uri = "/?fields=message,id&ids=List(1)&param=paramValue";

    GetRequestBuilder<Integer, TestRecord> requestBuilder =
        new GetRequestBuilder<Integer, TestRecord>("/",
                                                   TestRecord.class,
                                                   new ResourceSpecImpl(Collections.<ResourceMethod>emptySet(),
                                                                        null,
                                                                        null,
                                                                        Integer.class,
                                                                        TestRecord.class,
                                                                        Collections.<String, Object>emptyMap()),
                                                   RestliRequestOptions.DEFAULT_OPTIONS);
    requestBuilder.id(1)
        .fields(FIELDS.id(), FIELDS.message())
        .setParam("param", "paramValue");
    GetRequest<TestRecord> request = requestBuilder.build();
    BatchGetKVRequest<Integer, TestRecord> batchRequest = BatchGetRequestBuilder.batchKV(request);
    Assert.assertEquals(batchRequest.getBaseUriTemplate(), request.getBaseUriTemplate());
    Assert.assertEquals(batchRequest.getPathKeys(), request.getPathKeys());
    testUriGeneration(batchRequest, expectedProtocol1Uri, expectedProtocol2Uri);
    Assert.assertEquals(batchRequest.getFields(), request.getFields());
    Assert.assertEquals(batchRequest.getObjectIds(), new HashSet<Object>(Arrays.asList(request.getObjectId())));
  }

  @Test
  public void testComplexKeyBatchConversion()
      throws URISyntaxException
  {
    // Comment for the review - this appears to be breaking wire protocol, but it doesn't. Request batching
    // for complex keys never worked, and the old format was incorrect.
    String expectedProtocol1Uri =
        "/?fields=message,id&ids%5B0%5D.$params.id=1&ids%5B0%5D.$params.message=paramMessage1&ids%5B0%5D.id=1&ids%5B0%5D.message=keyMessage1&param=paramValue";
    String expectedProtocol2Uri =
        "/?fields=message,id&ids=List(($params:(id:1,message:paramMessage1),id:1,message:keyMessage1))&param=paramValue";
    GetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> requestBuilder =
        new GetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>("/",
                                                                                      TestRecord.class,
                                                                                      _complexResourceSpec,
                                                                                      RestliRequestOptions.DEFAULT_OPTIONS);
    ComplexResourceKey<TestRecord, TestRecord> complexKey1 =
        buildComplexKey(1L, "keyMessage1", 1L, "paramMessage1");
    requestBuilder.id(complexKey1)
                  .fields(FIELDS.id(), FIELDS.message())
                  .setParam("param", "paramValue");
    GetRequest<TestRecord> request = requestBuilder.build();
    BatchGetKVRequest<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> batchRequest = BatchGetRequestBuilder.batchKV(request);
    Assert.assertEquals(batchRequest.getBaseUriTemplate(), request.getBaseUriTemplate());
    Assert.assertEquals(batchRequest.getPathKeys(), request.getPathKeys());
    testUriGeneration(batchRequest, expectedProtocol1Uri, expectedProtocol2Uri);
    Assert.assertEquals(batchRequest.getFields(), request.getFields());
    Assert.assertEquals(batchRequest.getObjectIds(), new HashSet<Object>(Arrays.asList(request.getObjectId())));
  }

  @Test
  public void testComplexKeyBatchingWithoutTypedKeys()
  {
    GetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> requestBuilder =
        new GetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>("/",
                                 TestRecord.class,
                                 _complexResourceSpec,
                                 RestliRequestOptions.DEFAULT_OPTIONS);
    ComplexResourceKey<TestRecord, TestRecord> complexKey1 =
        buildComplexKey(1L, "keyMessage1", 1L, "paramMessage1");
    requestBuilder.id(complexKey1);
    GetRequest<TestRecord> request = requestBuilder.build();

    try
    {
      BatchGetRequestBuilder.batch(request);
      Assert.fail("Complex Keys should not be supported by the non-KV batch operation.");
    }
    catch (UnsupportedOperationException exc)
    {
    }

    Map<String, Object> queryParams = new HashMap<String, Object>();

    queryParams.put("ids", Arrays.asList((Object)complexKey1));
    BatchGetRequest<TestRecord> request3 = new BatchGetRequest<TestRecord>(
        Collections.<String, String>emptyMap(),
        new BatchResponseDecoder<TestRecord>(TestRecord.class),
        queryParams,
        _complexResourceSpec,
        "/",
        Collections.<String, Object>emptyMap(),
        RestliRequestOptions.DEFAULT_OPTIONS);

    try
    {
      @SuppressWarnings("unchecked")
      List<BatchGetRequest<TestRecord>> requests = Arrays.asList(request3);
      BatchGetRequestBuilder.batch(requests);
      Assert.fail("Complex Keys should not be supported by the non-KV batch operation.");
    }
    catch (UnsupportedOperationException exc)
    {
    }
  }

  @Test
  public void testCompoundKeyBatchingWithoutTypedKeys()
  {
    GetRequestBuilder<CompoundKey, TestRecord> requestBuilder2 =
        new GetRequestBuilder<CompoundKey, TestRecord>("/",
                                 TestRecord.class,
                                 _compoundResourceSpec,
                                 RestliRequestOptions.DEFAULT_OPTIONS);
    CompoundKey key = new CompoundKey().append("abc", 1).append("def", 2);

    requestBuilder2.id(key);

    GetRequest<TestRecord> request2 = requestBuilder2.build();

    try
    {
      BatchGetRequestBuilder.batch(request2);
      Assert.fail("Compound Keys should not be supported by the non-KV batch operation.");
    }
    catch (UnsupportedOperationException exc)
    {
    }

    Map<String, Object> queryParams = new HashMap<String, Object>();

    queryParams.put("ids", Arrays.asList((Object)key));
    BatchGetRequest<TestRecord> request4 = new BatchGetRequest<TestRecord>(
        Collections.<String, String>emptyMap(),
        new BatchResponseDecoder<TestRecord>(TestRecord.class),
        queryParams,
        _compoundResourceSpec,
        "/",
        Collections.<String, Object>emptyMap(),
        RestliRequestOptions.DEFAULT_OPTIONS);

    try
    {
      @SuppressWarnings("unchecked")
      List<BatchGetRequest<TestRecord>> requests = Arrays.asList(request4);
      BatchGetRequestBuilder.batch(requests);
      Assert.fail("Compound Keys should not be supported by the non-KV batch operation.");
    }
    catch (UnsupportedOperationException exc)
    {
    }
  }

  @Test
  public void testSimpleBatching()
      throws URISyntaxException
  {
    String expectedProtocol1Uri =
        "/?fields=id,message&ids=1&ids=2&ids=3&param1=value1&param2=value2";
    String expectedProtocol2Uri =
        "/?fields=id,message&ids=List(1,2,3)&param1=value1&param2=value2";

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder1.ids(1, 2)
                        .fields(FIELDS.id())
                        .setParam("param2", "value2")
                        .setParam("param1", "value1");

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder2.ids(2, 3)
                        .fields(FIELDS.id(), FIELDS.message())
                        .setParam("param1", "value1")
                        .setParam("param2", "value2");

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    BatchGetRequest<TestRecord> batchRequest2 = batchRequestBuilder2.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest =
        BatchGetRequestBuilder.batch(Arrays.asList(batchRequest1, batchRequest2));
    testUriGeneration(batchingRequest, expectedProtocol1Uri, expectedProtocol2Uri);
    Assert.assertEquals(batchingRequest.getBaseUriTemplate(), batchRequest1.getBaseUriTemplate());
    Assert.assertEquals(batchingRequest.getPathKeys(), batchRequest1.getPathKeys());
    Assert.assertEquals(batchingRequest.getFields(),
                        new HashSet<PathSpec>(Arrays.asList(FIELDS.id(), FIELDS.message())));
    Assert.assertEquals(batchingRequest.getObjectIds(), new HashSet<Integer>(Arrays.asList(1, 2, 3)));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testComplexKeyBatching()
      throws URISyntaxException, PathSegmentSyntaxException {
    String expectedProtocol1Uri =
        "/?fields=id,message&ids%5B0%5D.$params.id=1&ids%5B0%5D.$params.message=paramMessage1&ids%5B0%5D.id=1&ids%5B0%5D.message=keyMessage1&ids%5B1%5D.$params.id=2&ids%5B1%5D.$params.message=paramMessage2&ids%5B1%5D.id=2&ids%5B1%5D.message=keyMessage2&ids%5B2%5D.$params.id=3&ids%5B2%5D.$params.message=paramMessage3&ids%5B2%5D.id=3&ids%5B2%5D.message=keyMessage3&param1=value1&param2=value2";

    ComplexResourceKey<TestRecord, TestRecord> complexKey1 =
        buildComplexKey(1L, "keyMessage1", 1L, "paramMessage1");
    ComplexResourceKey<TestRecord, TestRecord> complexKey2 =
        buildComplexKey(2L, "keyMessage2", 2L, "paramMessage2");
    ComplexResourceKey<TestRecord, TestRecord> complexKey3 =
        buildComplexKey(3L, "keyMessage3", 3L, "paramMessage3");

    BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>("/",
                                                                                           TestRecord.class,
                                                                                           _complexResourceSpec,
                                                                                           RestliRequestOptions.DEFAULT_OPTIONS);
    @SuppressWarnings("unchecked")
    ComplexResourceKey<TestRecord, TestRecord>[] complexKeys1 =
        (ComplexResourceKey<TestRecord, TestRecord>[]) Arrays.asList(complexKey1,
                                                                     complexKey2)
                                                             .toArray();
    batchRequestBuilder1.ids(complexKeys1)
                        .fields(FIELDS.id())
                        .setParam("param2", "value2")
                        .setParam("param1", "value1");

    BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>("/",
                                                                                           TestRecord.class,
                                                                                           _complexResourceSpec,
                                                                                           RestliRequestOptions.DEFAULT_OPTIONS);
    @SuppressWarnings("unchecked")
    ComplexResourceKey<TestRecord, TestRecord>[] complexKeys2 =
        (ComplexResourceKey<TestRecord, TestRecord>[]) Arrays.asList(complexKey2,
                                                                     complexKey3)
                                                             .toArray();
    batchRequestBuilder2.ids(complexKeys2)
                        .fields(FIELDS.id(), FIELDS.message())
                        .setParam("param1", "value1")
                        .setParam("param2", "value2");

    BatchGetKVRequest<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> batchRequest1 =
        batchRequestBuilder1.buildKV();
    BatchGetKVRequest<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> batchRequest2 =
        batchRequestBuilder2.buildKV();
    @SuppressWarnings("unchecked")
    BatchGetKVRequest<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> batchingRequest =
        BatchGetRequestBuilder.batchKV(Arrays.asList(batchRequest1, batchRequest2));

    URI actualProtocol1Uri = RestliUriBuilderUtil.createUriBuilder(batchingRequest, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()).build();

    MultivaluedMap actualParams = UriComponent.decodeQuery(actualProtocol1Uri, true);
    MultivaluedMap expectedUriParams =
        UriComponent.decodeQuery(URI.create(expectedProtocol1Uri), true);
    DataMap expectedParamsDataMap = null;
    DataMap actualParamsDataMap = null;
    try
    {
      expectedParamsDataMap = QueryParamsDataMap.parseDataMapKeys(expectedUriParams);
      actualParamsDataMap = QueryParamsDataMap.parseDataMapKeys(actualParams);
    }
    catch (PathSegmentSyntaxException e)
    {
      // Should never happen
      Assert.fail("Failed to parse data map keys!");
    }

    Assert.assertEquals(actualProtocol1Uri.getPath(), "/");
    // Apparently due to using set to compact the list of ids in
    // BatchGetRequestBuilder.batch() the order of the parameters on the url is no longer
    // reliable.
    DataList actualIds =
        (DataList) actualParamsDataMap.remove(RestConstants.QUERY_BATCH_IDS_PARAM);
    DataList expectedIds =
        (DataList) expectedParamsDataMap.remove(RestConstants.QUERY_BATCH_IDS_PARAM);
    Assert.assertEquals(new HashSet<Object>(actualIds), new HashSet<Object>(expectedIds));
    Assert.assertEquals(actualParamsDataMap, expectedParamsDataMap);
    Assert.assertEquals(batchingRequest.getBaseUriTemplate(), batchRequest1.getBaseUriTemplate());
    Assert.assertEquals(batchingRequest.getPathKeys(), batchRequest1.getPathKeys());
    Assert.assertEquals(batchingRequest.getFields(),
                        new HashSet<PathSpec>(Arrays.asList(FIELDS.id(), FIELDS.message())));
    Assert.assertEquals(batchingRequest.getObjectIds(), new HashSet<Object>(Arrays.asList(complexKey1, complexKey2, complexKey3)));

    String expectedProtocol2Uri =
        "/?fields=id,message&ids=List(($params:(id:1,message:paramMessage1),id:1,message:keyMessage1),($params:(id:2,message:paramMessage2),id:2,message:keyMessage2),($params:(id:3,message:paramMessage3),id:3,message:keyMessage3))&param1=value1&param2=value2";
    URI actualProtocol2Uri = RestliUriBuilderUtil.createUriBuilder(batchingRequest, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()).build();

    Assert.assertEquals(actualProtocol2Uri.getPath(), "/");

    actualParams = UriComponent.decodeQuery(actualProtocol2Uri, true);
    MultivaluedMap expectedParams = UriComponent.decodeQuery(URI.create(expectedProtocol2Uri), true);

    // we can't compare the query param "ids" directly as ID ordering is not preserved while batching in
    // BatchGetRequestBuilder.batch()

    Assert.assertEquals(actualParams.get("ids").size(), 1);
    Assert.assertEquals(actualParams.get("ids").size(), expectedParams.get("ids").size());

    // parse out the "ids" param into a DataList and then convert it into a set
    String actualProtocol2IdsAsString = actualParams.remove("ids").get(0);
    String expectedProtocol2IdsAsString = expectedParams.remove("ids").get(0);
    DataList actualProtocol2Ids = (DataList) URIElementParser.parse(actualProtocol2IdsAsString);
    DataList expectedProtocol2Ids = (DataList) URIElementParser.parse(expectedProtocol2IdsAsString);
    Assert.assertEquals(new HashSet<Object>(actualProtocol2Ids.values()),
                        new HashSet<Object>(expectedProtocol2Ids.values()));

    // apart from the "ids" fields everything else should be the same
    Assert.assertEquals(actualParams, expectedParams);
  }

  private static ComplexResourceKey<TestRecord, TestRecord> buildComplexKey(Long keyId,
                                                                            String keyMessage,
                                                                            Long paramId,
                                                                            String paramMessage)
  {
    return new ComplexResourceKey<TestRecord, TestRecord>(new TestRecord().setId(keyId)
                                                                          .setMessage(keyMessage),
                                                          new TestRecord().setId(paramId)
                                                                          .setMessage(paramMessage));
  }

  @Test
  public void testSimpleBatchingFailureWithDiffParams()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder1.ids(1, 2).fields(FIELDS.id()).setParam("param1", "value1");

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder2.ids(2, 3)
                        .fields(FIELDS.id(), FIELDS.message())
                        .setParam("param1", "value1")
                        .setParam("param2", "value2");

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    BatchGetRequest<TestRecord> batchRequest2 = batchRequestBuilder2.build();
    try
    {
      @SuppressWarnings("unchecked")
      List<BatchGetRequest<TestRecord>> batchRequestsList =
          Arrays.asList(batchRequest1, batchRequest2);
      BatchGetRequestBuilder.batch(batchRequestsList);
      Assert.fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException ignored)
    {
      // Expected
    }
  }

  @Test
  public void testBatchingWithDiffUris()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                          RestliRequestOptions.DEFAULT_OPTIONS);
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/a/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);

    @SuppressWarnings("unchecked")
    List<BatchGetRequest<TestRecord>> requests =
        Arrays.asList(batchRequestBuilder1.build(), batchRequestBuilder2.build());
    try
    {
      BatchGetRequestBuilder.batch(requests);
      Assert.fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException ignored)
    {
      // Expected
    }
  }

  @Test
  public void testNoFieldBatching()
      throws URISyntaxException
  {
    String expectedProtocol1Uri = "/?fields=id&ids=1&ids=2&ids=3";
    String expectedProtocol2Uri = "/?fields=id&ids=List(1,2,3)";

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields(FIELDS.id());

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder2.ids(2, 3);
    batchRequestBuilder2.fields(FIELDS.id());

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest =
        BatchGetRequestBuilder.batch(Arrays.asList(batchRequest1,
                                                   batchRequestBuilder2.build()),
                                     false);
    Assert.assertEquals(batchingRequest.getBaseUriTemplate(), batchRequest1.getBaseUriTemplate());
    Assert.assertEquals(batchingRequest.getPathKeys(), batchRequest1.getPathKeys());
    Assert.assertEquals(batchingRequest.getFields(),
                        new HashSet<PathSpec>(Arrays.asList(FIELDS.id())));
    Assert.assertEquals(batchingRequest.getObjectIds(), new HashSet<Integer>(Arrays.asList(1, 2, 3)));
    testUriGeneration(batchingRequest, expectedProtocol1Uri, expectedProtocol2Uri);
  }

  @Test
  public void testNoFieldBatchingFailure()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields(FIELDS.id());

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder2.ids(2, 3);
    batchRequestBuilder2.fields(FIELDS.message());

    try
    {
      @SuppressWarnings("unchecked")
      List<BatchGetRequest<TestRecord>> requests =
          Arrays.asList(batchRequestBuilder1.build(), batchRequestBuilder2.build());
      BatchGetRequestBuilder.batch(requests, false);
      Assert.fail("expected IllegalArgumentException");
    }
    catch (IllegalArgumentException ignored)
    {
      // Expected
    }
  }

  @Test
  public void testBatchingWithDifferentRequestOptionsFailure()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder1.ids(1);

    RestliRequestOptions customOptions =
        new RestliRequestOptionsBuilder().setProtocolVersionOption(ProtocolVersionOption.FORCE_USE_NEXT).build();
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        customOptions);
    batchRequestBuilder2.ids(2, 3);

    try
    {
      @SuppressWarnings("unchecked")
      List<BatchGetRequest<TestRecord>> requests =
          Arrays.asList(batchRequestBuilder1.build(), batchRequestBuilder2.build());
      BatchGetRequestBuilder.batch(requests, false);
      Assert.fail("expected IllegalArgumentException");
    }
    catch (IllegalArgumentException ignored)
    {
      Assert.assertTrue(ignored.getMessage().contains("Requests must have the same RestliRequestOptions to batch!"));
    }
  }

  @Test
  public void testBatchingWithNoFields()
      throws URISyntaxException
  {
    String expectedProtocol1Uri = "/?ids=1&ids=2&ids=3";
    String expectedProtocol2Uri = "/?ids=List(1,2,3)";

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields();

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder2.ids(2, 3);
    batchRequestBuilder2.fields();

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest =
        BatchGetRequestBuilder.batch(Arrays.asList(batchRequest1,
                                                   batchRequestBuilder2.build()));
    Assert.assertEquals(batchingRequest.getBaseUriTemplate(), batchRequest1.getBaseUriTemplate());
    Assert.assertEquals(batchingRequest.getPathKeys(), batchRequest1.getPathKeys());
    Assert.assertEquals(batchingRequest.getFields(), Collections.emptySet());
    Assert.assertEquals(batchingRequest.getObjectIds(), new HashSet<Integer>(Arrays.asList(1, 2, 3)));
    testUriGeneration(batchingRequest, expectedProtocol1Uri, expectedProtocol2Uri);
  }

  @Test
  public void testBatchingWithNullProjectionFirst()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder1.ids(1);

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder2.ids(2, 3);
    batchRequestBuilder2.fields(FIELDS.message());

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest =
        BatchGetRequestBuilder.batch(Arrays.asList(batchRequest1,
                                                   batchRequestBuilder2.build()));
    Assert.assertEquals(batchingRequest.getBaseUriTemplate(), batchRequest1.getBaseUriTemplate());
    Assert.assertEquals(batchingRequest.getPathKeys(), batchRequest1.getPathKeys());
    Assert.assertEquals(batchingRequest.getFields(), Collections.emptySet());
    Assert.assertEquals(batchingRequest.getObjectIds(), new HashSet<Integer>(Arrays.asList(1, 2, 3)));
  }

  @Test
  public void testBatchingWithNullProjectionLast()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields(FIELDS.message());

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl(
                                                          Collections.<ResourceMethod> emptySet(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                          Integer.class,
                                                          null,
                                                          null,
                                                          null,
                                                          Collections.<String, Object> emptyMap()),
                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    batchRequestBuilder2.ids(2, 3);

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest =
        BatchGetRequestBuilder.batch(Arrays.asList(batchRequest1,
                                                   batchRequestBuilder2.build()));
    Assert.assertEquals(batchingRequest.getBaseUriTemplate(), batchRequest1.getBaseUriTemplate());
    Assert.assertEquals(batchingRequest.getPathKeys(), batchRequest1.getPathKeys());
    Assert.assertEquals(batchingRequest.getFields(), Collections.emptySet());
    Assert.assertEquals(batchingRequest.getObjectIds(), new HashSet<Integer>(Arrays.asList(1, 2, 3)));
  }

  private static void testUriGeneration(Request<?> request, String protocol1UriString, String protocol2UriString)
      throws URISyntaxException {
    ProtocolVersion protocol1 = AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion();
    ProtocolVersion protocol2 = AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion();

    URI protocol1Uri = RestliUriBuilderUtil.createUriBuilder(request, protocol1).build();
    URI protocol2Uri = RestliUriBuilderUtil.createUriBuilder(request, protocol2).build();

    //V1:
    MultivaluedMap actualQueryParamMapV1 = UriComponent.decodeQuery(protocol1Uri, true);
    MultivaluedMap expectedQueryParamMapV1 = UriComponent.decodeQuery(new URI(protocol1UriString), true);
    assertProtocolURIsMatch(actualQueryParamMapV1, expectedQueryParamMapV1, "Protocol 1");

    //V2:
    MultivaluedMap actualQueryParamMapV2 = UriComponent.decodeQuery(protocol2Uri, true);
    MultivaluedMap expectedQueryParamMapV2 = UriComponent.decodeQuery(new URI(protocol2UriString), true);
    assertProtocolURIsMatch(actualQueryParamMapV2, expectedQueryParamMapV2, "Protocol 2");
  }

  private static void assertProtocolURIsMatch(final MultivaluedMap actualQueryParamMap, final MultivaluedMap expectedQueryParamMap,
      final String protocolName)
  {
    Assert.assertEquals(actualQueryParamMap.size(), expectedQueryParamMap.size(),
        protocolName + " URI generation did not match expected URI! Query parameter count is incorrect");

    for (final Map.Entry<String, List<String>> entry : actualQueryParamMap.entrySet())
    {
      if(!entry.getKey().equalsIgnoreCase(RestConstants.FIELDS_PARAM))
      {
        Assert.assertNotNull(entry.getValue(), "We should not have a null list of params for key: " + entry.getKey());
        Assert.assertEquals(entry.getValue(), expectedQueryParamMap.get(entry.getKey()),
            protocolName + " URI generation did not match expected URI! Values for a key mismatch!");
      }
      else
      {
        // Fields could be out of order, so we have to break it apart and compare using a set
        final Set<String> actualFieldSet = new HashSet<String>(Arrays.asList(entry.getValue().get(0).split(",")));
        final Set<String> expectedFieldSet = new HashSet<String>(Arrays.asList(expectedQueryParamMap.get(entry.getKey()).get(0).split(",")));
        Assert.assertEquals(actualFieldSet, expectedFieldSet,
            protocolName + " URI generation did not match expected URI! Projection field names have a mismatch!");
      }
    }
  }

  public static class MyCompoundKey extends CompoundKey
  {
    public MyCompoundKey()
    {
    }

    public MyCompoundKey setAge(Integer age)
    {
      append("age", age);

      return this;
    }

    public Integer getAge() {
      return ((Integer) getPart("age"));
    }
  }
}
