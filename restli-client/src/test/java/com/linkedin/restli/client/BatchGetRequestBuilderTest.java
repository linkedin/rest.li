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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.jersey.core.util.MultivaluedMap;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.common.QueryParamsDataMap;

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

  @Test
  public void testBatchConversion()
  {
    String expectedUri = "/?fields=message,id&ids=1&param=paramValue";
    GetRequestBuilder<Integer, TestRecord> requestBuilder =
        new GetRequestBuilder<Integer, TestRecord>("/",
                                                   TestRecord.class,
                                                   new ResourceSpecImpl(Collections.<ResourceMethod>emptySet(),
                                                                        null,
                                                                        null,
                                                                        int.class,
                                                                        TestRecord.class,
                                                                        Collections.<String, Object>emptyMap()));
    requestBuilder.id(1)
                  .fields(FIELDS.id(), FIELDS.message())
                  .param("param", "paramValue");
    GetRequest<TestRecord> request = requestBuilder.build();
    BatchGetRequest<TestRecord> batchRequest = BatchGetRequestBuilder.batch(request);
    Assert.assertEquals(batchRequest.getBaseURI(), request.getBaseURI());
    Assert.assertEquals(batchRequest.getUri().toString(), expectedUri);
    Assert.assertEquals(batchRequest.getFields(), request.getFields());
    Assert.assertEquals(batchRequest.getIdObjects().size(), 1);
    Assert.assertEquals(batchRequest.getIdObjects().iterator().next(), request.getIdObject());
  }

  @Test
  public void testComplexKeyBatchConversion()
  {
    // Comment for the review - this appears to be breaking wire protocol, but it doesn't. Request batching
    // for complex keys never worked, and the old format was incorrect.
    String expectedUri = "/?fields=message,id&ids%5B0%5D.$params.id=1&ids%5B0%5D.$params.message=paramMessage1&ids%5B0%5D.id=1&ids%5B0%5D.message=keyMessage1&param=paramValue";
    GetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> requestBuilder =
        new GetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>("/",
                                                                                      TestRecord.class,
                                                                                      _complexResourceSpec);
    ComplexResourceKey<TestRecord, TestRecord> complexKey1 =
        buildComplexKey(1L, "keyMessage1", 1L, "paramMessage1");
    requestBuilder.id(complexKey1)
                  .fields(FIELDS.id(), FIELDS.message())
                  .param("param", "paramValue");
    GetRequest<TestRecord> request = requestBuilder.build();
    BatchGetRequest<TestRecord> batchRequest = BatchGetRequestBuilder.batch(request);
    Assert.assertEquals(batchRequest.getBaseURI(), request.getBaseURI());
    Assert.assertEquals(batchRequest.getUri().toString(), expectedUri);
    Assert.assertEquals(batchRequest.getFields(), request.getFields());
    Assert.assertEquals(batchRequest.getIdObjects().size(), 1);
    Assert.assertEquals(batchRequest.getIdObjects().iterator().next(), request.getIdObject());
  }

  @Test
  public void testSimpleBatching()
  {
    String expectedUri =
        "/?fields=id,message&ids=1&ids=2&ids=3&param1=value1&param2=value2";

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder1.ids(1, 2)
                        .fields(FIELDS.id())
                        .param("param2", "value2")
                        .param("param1", "value1");

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder2.ids(2, 3)
                        .fields(FIELDS.id(), FIELDS.message())
                        .param("param1", "value1")
                        .param("param2", "value2");

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    BatchGetRequest<TestRecord> batchRequest2 = batchRequestBuilder2.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest =
        BatchGetRequestBuilder.batch(Arrays.asList(batchRequest1, batchRequest2));
    Assert.assertEquals(batchingRequest.getUri().toString(), expectedUri);
    Assert.assertEquals(batchingRequest.getBaseURI(), batchRequest1.getBaseURI());
    Assert.assertEquals(batchingRequest.getFields(),
                        new HashSet<PathSpec>(Arrays.asList(FIELDS.id(), FIELDS.message())));
    Assert.assertEquals(batchingRequest.getIdObjects(),
                        new HashSet<String>(Arrays.asList("1", "2", "3")));
  }

  @Test
  public void testComplexKeyBatching()
  {
    String expectedUri =
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
                                                                                           _complexResourceSpec);
    @SuppressWarnings("unchecked")
    ComplexResourceKey<TestRecord, TestRecord>[] complexKeys1 =
        (ComplexResourceKey<TestRecord, TestRecord>[]) Arrays.asList(complexKey1,
                                                                     complexKey2)
                                                             .toArray();
    batchRequestBuilder1.ids(complexKeys1)
                        .fields(FIELDS.id())
                        .param("param2", "value2")
                        .param("param1", "value1");

    BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<ComplexResourceKey<TestRecord, TestRecord>, TestRecord>("/",
                                                                                           TestRecord.class,
                                                                                           _complexResourceSpec);
    @SuppressWarnings("unchecked")
    ComplexResourceKey<TestRecord, TestRecord>[] complexKeys2 =
        (ComplexResourceKey<TestRecord, TestRecord>[]) Arrays.asList(complexKey2,
                                                                     complexKey3)
                                                             .toArray();
    batchRequestBuilder2.ids(complexKeys2)
                        .fields(FIELDS.id(), FIELDS.message())
                        .param("param1", "value1")
                        .param("param2", "value2");

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    BatchGetRequest<TestRecord> batchRequest2 = batchRequestBuilder2.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest =
        BatchGetRequestBuilder.batch(Arrays.asList(batchRequest1, batchRequest2));

    URI actualUri = batchingRequest.getUri();
    MultivaluedMap actualParams = UriComponent.decodeQuery(actualUri, true);
    MultivaluedMap expectedUriParams =
        UriComponent.decodeQuery(URI.create(expectedUri), true);
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
      throw new RuntimeException(e);
    }

    Assert.assertEquals(actualUri.getPath(), "/");
    // Apparently due to using set to compact the list of ids in
    // BatchGetRequestBuilder.batch() the order of the parameters on the url is no longer
    // reliable.
    DataList actualIds =
        (DataList) actualParamsDataMap.remove(RestConstants.QUERY_BATCH_IDS_PARAM);
    DataList expectedIds =
        (DataList) expectedParamsDataMap.remove(RestConstants.QUERY_BATCH_IDS_PARAM);
    Assert.assertEquals(new HashSet<Object>(actualIds), new HashSet<Object>(expectedIds));
    Assert.assertEquals(actualParamsDataMap, expectedParamsDataMap);
    Assert.assertEquals(batchingRequest.getBaseURI(), batchRequest1.getBaseURI());
    Assert.assertEquals(batchingRequest.getFields(),
                        new HashSet<PathSpec>(Arrays.asList(FIELDS.id(), FIELDS.message())));
    Set<Object> ids = batchingRequest.getIdObjects();
    Set<String> idStrs = new HashSet<String>(ids.size());
    for (Object id : ids)
    {
      idStrs.add(id.toString());
    }
    Assert.assertEquals(idStrs,
                        new HashSet<String>(Arrays.asList("id=3&message=keyMessage3",
                                                          "id=1&message=keyMessage1",
                                                          "id=2&message=keyMessage2")));
  }

  private ComplexResourceKey<TestRecord, TestRecord> buildComplexKey(Long keyId,
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
  public void testSimpleBatchingWithDiffParams()
  {
    String expectedUri =
        "/?param1=value1&ids=1&param2=value2&ids=2&ids=3&fields=id,message";

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder1.ids(1, 2).fields(FIELDS.id()).param("param1", "value1");

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder2.ids(2, 3)
                        .fields(FIELDS.id(), FIELDS.message())
                        .param("param1", "value1")
                        .param("param2", "value2");

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
                                                        new ResourceSpecImpl());
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/a/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());

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
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields(FIELDS.id());

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder2.ids(2, 3);
    batchRequestBuilder2.fields(FIELDS.id());

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest =
        BatchGetRequestBuilder.batch(Arrays.asList(batchRequest1,
                                                   batchRequestBuilder2.build()),
                                     false);
    Assert.assertEquals(batchingRequest.getBaseURI(), batchRequest1.getBaseURI());
    Assert.assertEquals(batchingRequest.getFields(),
                        new HashSet<PathSpec>(Arrays.asList(FIELDS.id())));
    Assert.assertEquals(batchingRequest.getIdObjects(),
                        new HashSet<String>(Arrays.asList("1", "2", "3")));
  }

  @Test
  public void testNoFieldBatchingFailure()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields(FIELDS.id());

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
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
  public void testBatchingWithNoFields()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields();

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder2.ids(2, 3);
    batchRequestBuilder2.fields();

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest =
        BatchGetRequestBuilder.batch(Arrays.asList(batchRequest1,
                                                   batchRequestBuilder2.build()));
    Assert.assertEquals(batchingRequest.getBaseURI(), batchRequest1.getBaseURI());
    Assert.assertEquals(batchingRequest.getFields(), Collections.emptySet());
    Assert.assertEquals(batchingRequest.getIdObjects(),
                        new HashSet<String>(Arrays.asList("1", "2", "3")));
  }

  @Test
  public void testBatchingWithNullProjectionFirst()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder1.ids(1);

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder2.ids(2, 3);
    batchRequestBuilder2.fields(FIELDS.message());

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest =
        BatchGetRequestBuilder.batch(Arrays.asList(batchRequest1,
                                                   batchRequestBuilder2.build()));
    Assert.assertEquals(batchingRequest.getBaseURI(), batchRequest1.getBaseURI());
    Assert.assertEquals(batchingRequest.getFields(), Collections.emptySet());
    Assert.assertEquals(batchingRequest.getIdObjects(),
                        new HashSet<String>(Arrays.asList("1", "2", "3")));
  }

  @Test
  public void testBatchingWithNullProjectionLast()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields(FIELDS.message());

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 =
        new BatchGetRequestBuilder<Integer, TestRecord>("/",
                                                        TestRecord.class,
                                                        new ResourceSpecImpl());
    batchRequestBuilder2.ids(2, 3);

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest =
        BatchGetRequestBuilder.batch(Arrays.asList(batchRequest1,
                                                   batchRequestBuilder2.build()));
    Assert.assertEquals(batchingRequest.getBaseURI(), batchRequest1.getBaseURI());
    Assert.assertEquals(batchingRequest.getFields(), Collections.emptySet());
    Assert.assertEquals(batchingRequest.getIdObjects(),
                        new HashSet<String>(Arrays.asList("1", "2", "3")));
  }
}
