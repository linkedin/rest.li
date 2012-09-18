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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.data.schema.PathSpec;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.ResourceSpecImpl;


/**
 * Unit test for BatchGetRequestBuilder.
 *
 * @author Eran Leshem
 */
public class BatchGetRequestBuilderTest
{
  private static final TestRecord.Fields FIELDS = TestRecord.fields();

  @Test
  public void testBatchConversion()
  {
    GetRequestBuilder<Integer, TestRecord> requestBuilder = new GetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    requestBuilder.id(1);
    requestBuilder.fields(FIELDS.id());
    GetRequest<TestRecord> request = requestBuilder.build();
    BatchGetRequest<TestRecord> batchRequest = BatchGetRequestBuilder.batch(request);
    Assert.assertEquals(batchRequest.getBaseURI(), request.getBaseURI());
    Assert.assertEquals(batchRequest.getFields(), request.getFields());
    Assert.assertEquals(batchRequest.getIds().size(), 1);
    Assert.assertEquals(batchRequest.getIds().iterator().next(), request.getId());
  }

  @Test
  public void testSimpleBatching()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields(FIELDS.id());

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    batchRequestBuilder2.ids(2, 3);
    batchRequestBuilder2.fields(FIELDS.message());

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest = BatchGetRequestBuilder.batch(
                    Arrays.asList(batchRequest1, batchRequestBuilder2.build()));
    Assert.assertEquals(batchingRequest.getBaseURI(), batchRequest1.getBaseURI());
    Assert.assertEquals(batchingRequest.getFields(), new HashSet<PathSpec>(Arrays.asList(FIELDS.id(), FIELDS.message())));
    Assert.assertEquals(batchingRequest.getIds(), new HashSet<String>(Arrays.asList("1", "2", "3")));
  }

  @Test
  public void testBatchingWithDiffUris()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/a/", TestRecord.class, new ResourceSpecImpl());

    @SuppressWarnings("unchecked")
    List<BatchGetRequest<TestRecord>> requests = Arrays.asList(batchRequestBuilder1.build(),
                                                               batchRequestBuilder2.build());
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
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields(FIELDS.id());

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    batchRequestBuilder2.ids(2, 3);
    batchRequestBuilder2.fields(FIELDS.id());

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest = BatchGetRequestBuilder.batch(
                    Arrays.asList(batchRequest1, batchRequestBuilder2.build()), false);
    Assert.assertEquals(batchingRequest.getBaseURI(), batchRequest1.getBaseURI());
    Assert.assertEquals(batchingRequest.getFields(), new HashSet<PathSpec>(Arrays.asList(FIELDS.id())));
    Assert.assertEquals(batchingRequest.getIds(), new HashSet<String>(Arrays.asList("1", "2", "3")));
  }

  @Test
  public void testNoFieldBatchingFailure()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields(FIELDS.id());

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    batchRequestBuilder2.ids(2, 3);
    batchRequestBuilder2.fields(FIELDS.message());

    try
    {
      @SuppressWarnings("unchecked")
      List<BatchGetRequest<TestRecord>> requests = Arrays.asList(batchRequestBuilder1.build(), batchRequestBuilder2.build());
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
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields();

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    batchRequestBuilder2.ids(2, 3);
    batchRequestBuilder2.fields();

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest = BatchGetRequestBuilder.batch(
                    Arrays.asList(batchRequest1, batchRequestBuilder2.build()));
    Assert.assertEquals(batchingRequest.getBaseURI(), batchRequest1.getBaseURI());
    Assert.assertEquals(batchingRequest.getFields(), Collections.emptySet());
    Assert.assertEquals(batchingRequest.getIds(), new HashSet<String>(Arrays.asList("1", "2", "3")));
  }

  @Test
  public void testBatchingWithNullProjectionFirst()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    batchRequestBuilder1.ids(1);

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    batchRequestBuilder2.ids(2, 3);
    batchRequestBuilder2.fields(FIELDS.message());

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest = BatchGetRequestBuilder.batch(
                    Arrays.asList(batchRequest1, batchRequestBuilder2.build()));
    Assert.assertEquals(batchingRequest.getBaseURI(), batchRequest1.getBaseURI());
    Assert.assertNull(batchingRequest.getFields());
    Assert.assertEquals(batchingRequest.getIds(), new HashSet<String>(Arrays.asList("1", "2", "3")));
  }

  @Test
  public void testBatchingWithNullProjectionLast()
  {
    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder1 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    batchRequestBuilder1.ids(1);
    batchRequestBuilder1.fields(FIELDS.message());

    BatchGetRequestBuilder<Integer, TestRecord> batchRequestBuilder2 = new BatchGetRequestBuilder<Integer, TestRecord>(
                    "/", TestRecord.class, new ResourceSpecImpl());
    batchRequestBuilder2.ids(2, 3);

    BatchGetRequest<TestRecord> batchRequest1 = batchRequestBuilder1.build();
    @SuppressWarnings("unchecked")
    BatchGetRequest<TestRecord> batchingRequest = BatchGetRequestBuilder.batch(
                    Arrays.asList(batchRequest1, batchRequestBuilder2.build()));
    Assert.assertEquals(batchingRequest.getBaseURI(), batchRequest1.getBaseURI());
    Assert.assertNull(batchingRequest.getFields());
    Assert.assertEquals(batchingRequest.getIds(), new HashSet<String>(Arrays.asList("1", "2", "3")));
  }
}
