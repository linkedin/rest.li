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


import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.multipart.MultiPartMIMEReader;
import com.linkedin.multipart.utils.MIMETestUtils.MultiPartMIMEFullReaderCallback;
import com.linkedin.multipart.utils.MIMETestUtils.SinglePartMIMEFullReaderCallback;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import com.linkedin.restli.internal.testutils.RestLiTestAttachmentDataSource;
import com.linkedin.restli.internal.common.ResourcePropertiesImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import org.easymock.Capture;
import org.easymock.EasyMock;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */
public class TestRestClientRequestBuilder
{
  private static final DataMap ENTITY_BODY = new DataMap();
  private static final String JSON_ENTITY_BODY = "{\"testFieldName\":\"testValue\",\"testInteger\":1}";
  private static final String PSON_ENTITY_BODY = "#!PSON1\n!\u0081testFieldName\u0000\n\n\u0000\u0000\u0000testValue\u0000\u0083testInteger\u0000\u0002\u0001\u0000\u0000\u0000\u0080";
  private static final String JSON_ENTITIES_BODY = "{\"entities\":{}}";
  private static final String PSON_ENTITIES_BODY = "#!PSON1\n" + "!\u0081entities\u0000 \u0080";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String ACCEPT_TYPE_HEADER = "Accept";
  private static final String HOST = "host";
  private static final String SERVICE_NAME = "foo";
  private static final String BASE_URI_TEMPLATE = "/foo";
  private static final String SERIALIZED_EMPTY_JSON = "{}";
  private static final String SERIALIZED_EMPTY_PSON = "#!PSON1\n ";

  //For streaming attachments. Note that the tests in this suite that test for attachments are only for rest.li methods
  //that use POST or PUT (i.e action, update, etc...).
  private static final String FIRST_PART_ID = "1";
  private static final String SECOND_PART_ID = "2";
  private static final ByteString FIRST_PART_PAYLOAD = ByteString.copyString("firstPart", Charset.defaultCharset());
  private static final ByteString SECOND_PART_PAYLOAD = ByteString.copyString("secondPart", Charset.defaultCharset());

  static
  {
    ENTITY_BODY.put("testFieldName", "testValue");
    ENTITY_BODY.put("testInteger", 1);
  }

  @Test(dataProvider = "data")
  public void testGet(RestClient.ContentType contentType,
                      String expectedContentTypeHeader,
                      String expectedRequestBody,
                      String expectedEntitiesBody,
                      List<RestClient.AcceptType> acceptTypes,
                      String expectedAcceptHeader,
                      boolean acceptContentTypePerClient,
                      boolean streamAttachments,
                      boolean acceptResponseAttachments) throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRestRequest(GetRequest.class, ResourceMethod.GET, null, contentType,
                                                         acceptTypes, acceptContentTypePerClient);
    Assert.assertNull(restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(restRequest.getEntity().length(), 0);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

    RestRequest restRequestBatch = clientGeneratedRestRequest(BatchGetRequest.class, ResourceMethod.BATCH_GET, null,
                                                              contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertNull(restRequestBatch.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(restRequestBatch.getEntity().length(), 0);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);
  }

  @Test(dataProvider = "data")
  public void testFinder(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader,
                         boolean acceptContentTypePerClient,
                         boolean streamAttachments,
                         boolean acceptResponseAttachments) throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRestRequest(FindRequest.class, ResourceMethod.FINDER, null, contentType,
                                                         acceptTypes, acceptContentTypePerClient);
    Assert.assertNull(restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(restRequest.getEntity().length(), 0);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

    RestRequest restRequestAll = clientGeneratedRestRequest(GetAllRequest.class, ResourceMethod.GET_ALL, null,
                                                            contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertNull(restRequestAll.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(restRequestAll.getEntity().length(), 0);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);
  }

  @Test(dataProvider = "data")
  public void testAction(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader,
                         boolean acceptContentTypePerClient,
                         boolean streamAttachments,
                         boolean acceptResponseAttachments) throws URISyntaxException
  {
    //We only proceed with StreamRequest tests if there are request attachments OR there is a desire for response
    //attachments. If there are no request attachments present AND no desire to accept response attachments, then
    //its a RestRequest.
    if (streamAttachments == false && acceptResponseAttachments == false)
    {
      //RestRequest with a request entity
      RestRequest restRequest = clientGeneratedRestRequest(ActionRequest.class, ResourceMethod.ACTION, ENTITY_BODY,
                                                           contentType, acceptTypes,
                                                           acceptContentTypePerClient);
      Assert.assertEquals(restRequest.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
      Assert.assertEquals(restRequest.getEntity().asAvroString(), expectedRequestBody);
      Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

      //RestRequest without a request entity
      RestRequest restRequestNoEntity = clientGeneratedRestRequest(ActionRequest.class, ResourceMethod.ACTION, new DataMap(),
                                                                   contentType, acceptTypes,
                                                                   acceptContentTypePerClient);
      Assert.assertEquals(restRequest.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);

      //Verify that that there is an empty payload based on the expected content type
      if (expectedContentTypeHeader.equalsIgnoreCase(RestConstants.HEADER_VALUE_APPLICATION_PSON))
      {
        Assert.assertEquals(restRequestNoEntity.getEntity().asAvroString(), SERIALIZED_EMPTY_PSON);
      }
      else
      {
        Assert.assertEquals(restRequestNoEntity.getEntity().asAvroString(), SERIALIZED_EMPTY_JSON);
      }

      Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);
    }
    else
    {
      //StreamRequest with a request entity
      StreamRequest streamRequest =
          clientGeneratedStreamRequest(ActionRequest.class, ResourceMethod.ACTION, ENTITY_BODY, contentType, acceptTypes,
                                       acceptContentTypePerClient, streamAttachments ? generateRequestAttachments() : null,
                                       acceptResponseAttachments);

      verifyStreamRequest(streamRequest, acceptResponseAttachments, expectedAcceptHeader, streamAttachments,
                          expectedContentTypeHeader, expectedRequestBody);

      //StreamRequest without a request entity
      StreamRequest streamRequestNoEntity =
          clientGeneratedStreamRequest(ActionRequest.class, ResourceMethod.ACTION, new DataMap(), contentType,
                                       acceptTypes, acceptContentTypePerClient,
                                       streamAttachments ? generateRequestAttachments() : null,
                                       acceptResponseAttachments);

      //Verify that that there is an empty payload based on the expected content type
      if (expectedContentTypeHeader.equalsIgnoreCase(RestConstants.HEADER_VALUE_APPLICATION_PSON))
      {
        verifyStreamRequest(streamRequestNoEntity, acceptResponseAttachments, expectedAcceptHeader, streamAttachments,
                            expectedContentTypeHeader, SERIALIZED_EMPTY_PSON);
      }
      else
      {
        verifyStreamRequest(streamRequestNoEntity, acceptResponseAttachments, expectedAcceptHeader, streamAttachments,
                            expectedContentTypeHeader, SERIALIZED_EMPTY_JSON);
      }
    }
  }

  @Test(dataProvider = "data")
  public void testUpdate(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader,
                         boolean acceptContentTypePerClient,
                         boolean streamAttachments,
                         boolean acceptResponseAttachments) throws URISyntaxException
  {
    //We only proceed with StreamRequest tests if there are request attachments OR there is a desire for response
    //attachments. If there are no request attachments present AND no desire to accept response attachments, then
    //its a RestRequest.
    if (streamAttachments == false && acceptResponseAttachments == false)
    {
      RestRequest restRequest = clientGeneratedRestRequest(UpdateRequest.class, ResourceMethod.UPDATE, ENTITY_BODY,
                                                           contentType, acceptTypes, acceptContentTypePerClient);
      Assert.assertEquals(restRequest.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
      Assert.assertEquals(restRequest.getEntity().asAvroString(), expectedRequestBody);
      Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

      RestRequest restRequestBatch = clientGeneratedRestRequest(BatchUpdateRequest.class, ResourceMethod.BATCH_UPDATE,
                                                                ENTITY_BODY, contentType, acceptTypes,
                                                                acceptContentTypePerClient);
      Assert.assertEquals(restRequestBatch.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
      Assert.assertEquals(restRequestBatch.getEntity().asAvroString(), expectedEntitiesBody);
      Assert.assertEquals(restRequestBatch.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

      RestRequest restRequestPartial = clientGeneratedRestRequest(PartialUpdateRequest.class,
                                                                  ResourceMethod.PARTIAL_UPDATE, ENTITY_BODY, contentType,
                                                                  acceptTypes, acceptContentTypePerClient);
      Assert.assertEquals(restRequestPartial.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
      Assert.assertEquals(restRequestPartial.getEntity().asAvroString(), expectedRequestBody);
      Assert.assertEquals(restRequestPartial.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

      RestRequest restRequestBatchPartial = clientGeneratedRestRequest(BatchPartialUpdateRequest.class,
                                                                       ResourceMethod.BATCH_PARTIAL_UPDATE, ENTITY_BODY,
                                                                       contentType, acceptTypes,
                                                                       acceptContentTypePerClient);
      Assert.assertEquals(restRequestBatchPartial.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
      Assert.assertEquals(restRequestBatchPartial.getEntity().asAvroString(), expectedEntitiesBody);
      Assert.assertEquals(restRequestBatchPartial.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);
    }
    else
    {
      StreamRequest streamRequest = clientGeneratedStreamRequest(UpdateRequest.class, ResourceMethod.UPDATE, ENTITY_BODY,
                                                                 contentType, acceptTypes, acceptContentTypePerClient,
                                                                 streamAttachments ? generateRequestAttachments() : null,
                                                                 acceptResponseAttachments);
      verifyStreamRequest(streamRequest, acceptResponseAttachments, expectedAcceptHeader, streamAttachments,
                          expectedContentTypeHeader, expectedRequestBody);

      StreamRequest streamRequestBatch = clientGeneratedStreamRequest(BatchUpdateRequest.class, ResourceMethod.BATCH_UPDATE,
                                                                      ENTITY_BODY, contentType, acceptTypes,
                                                                      acceptContentTypePerClient,
                                                                      streamAttachments ? generateRequestAttachments() : null,
                                                                      acceptResponseAttachments);
      verifyStreamRequest(streamRequestBatch, acceptResponseAttachments, expectedAcceptHeader, streamAttachments,
                          expectedContentTypeHeader, expectedEntitiesBody);

      StreamRequest streamRequestPartial = clientGeneratedStreamRequest(PartialUpdateRequest.class,
                                                                        ResourceMethod.PARTIAL_UPDATE, ENTITY_BODY, contentType,
                                                                        acceptTypes, acceptContentTypePerClient,
                                                                        streamAttachments ? generateRequestAttachments() : null,
                                                                        acceptResponseAttachments);
      verifyStreamRequest(streamRequestPartial, acceptResponseAttachments, expectedAcceptHeader, streamAttachments,
                          expectedContentTypeHeader, expectedRequestBody);

      StreamRequest streamRequestBatchPartial = clientGeneratedStreamRequest(BatchPartialUpdateRequest.class,
                                                                             ResourceMethod.BATCH_PARTIAL_UPDATE, ENTITY_BODY,
                                                                             contentType, acceptTypes,
                                                                             acceptContentTypePerClient,
                                                                             streamAttachments ? generateRequestAttachments() : null,
                                                                             acceptResponseAttachments);
      verifyStreamRequest(streamRequestBatchPartial, acceptResponseAttachments, expectedAcceptHeader, streamAttachments,
                          expectedContentTypeHeader, expectedEntitiesBody);
    }
  }

  @Test(dataProvider = "data")
  public void testCreate(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader,
                         boolean acceptContentTypePerClient,
                         boolean streamAttachments,
                         boolean acceptResponseAttachments) throws URISyntaxException
  {
    //We only proceed with StreamRequest tests if there are request attachments OR there is a desire for response
    //attachments. If there are no request attachments present AND no desire to accept response attachments, then
    //its a RestRequest.
    if (streamAttachments == false && acceptResponseAttachments == false)
    {
      RestRequest restRequest = clientGeneratedRestRequest(CreateRequest.class, ResourceMethod.CREATE, ENTITY_BODY,
                                                           contentType, acceptTypes, acceptContentTypePerClient);
      Assert.assertEquals(restRequest.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
      Assert.assertEquals(restRequest.getEntity().asAvroString(), expectedRequestBody);
      Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

      RestRequest restRequestBatch = clientGeneratedRestRequest(BatchCreateRequest.class, ResourceMethod.BATCH_CREATE,
                                                                ENTITY_BODY, contentType, acceptTypes,
                                                                acceptContentTypePerClient);
      Assert.assertEquals(restRequestBatch.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
      Assert.assertEquals(restRequestBatch.getEntity().asAvroString(), expectedRequestBody);
      Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);
    }
    else
    {
      StreamRequest streamRequest = clientGeneratedStreamRequest(CreateRequest.class, ResourceMethod.CREATE, ENTITY_BODY,
                                                                 contentType, acceptTypes, acceptContentTypePerClient,
                                                                 streamAttachments ? generateRequestAttachments() : null,
                                                                 acceptResponseAttachments);
      verifyStreamRequest(streamRequest, acceptResponseAttachments, expectedAcceptHeader, streamAttachments,
                          expectedContentTypeHeader, expectedRequestBody);

      StreamRequest streamRequestBatch = clientGeneratedStreamRequest(BatchCreateRequest.class, ResourceMethod.BATCH_CREATE,
                                                                      ENTITY_BODY, contentType, acceptTypes,
                                                                      acceptContentTypePerClient,
                                                                      streamAttachments ? generateRequestAttachments() : null,
                                                                      acceptResponseAttachments);
      verifyStreamRequest(streamRequestBatch, acceptResponseAttachments, expectedAcceptHeader, streamAttachments,
                          expectedContentTypeHeader, expectedRequestBody);
    }
  }

  @Test(dataProvider = "data")
  public void testDelete(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader,
                         boolean acceptContentTypePerClient,
                         boolean streamAttachments,
                         boolean acceptResponseAttachments) throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRestRequest(DeleteRequest.class, ResourceMethod.DELETE, null, contentType,
                                                         acceptTypes, acceptContentTypePerClient);
    Assert.assertNull(restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(restRequest.getEntity().length(), 0);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

    RestRequest restRequestBatch = clientGeneratedRestRequest(BatchDeleteRequest.class, ResourceMethod.BATCH_DELETE,
                                                              null, contentType, acceptTypes,
                                                              acceptContentTypePerClient);
    Assert.assertNull(restRequestBatch.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(restRequestBatch.getEntity().length(), 0);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);
  }

  @DataProvider(name = "data")
  public Object[][] requestData()
  {
    //We split the data sources to make the test data easier to reason about, and then we merge before the tests are actually run.
    List<Object[]> result = new ArrayList<>();
    result.addAll(Arrays.asList(restRequestData()));
    result.addAll(Arrays.asList(streamRequestData()));
    return result.toArray(new Object[result.size()][]);
  }

  public Object[][] restRequestData()
  {
    return new Object[][]
        {
            // ContentTypes and acceptTypes configured per client (deprecated).
            //
            // {
            //    RestClient.ContentType contentType
            //    String expectedContentTypeHeader,
            //    String expectedRequestBody,
            //    String expectedEntitiesBody,
            //    List<RestClient.AcceptType> acceptTypes,
            //    String expectedAcceptHeader
            //    boolean acceptContentTypePerClient
            //    boolean streamAttachments, //false for RestRequest
            //    boolean acceptResponseAttachments  //false for RestRequest
            // }
            {
                null,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                null,
                null,
                true,
                false,
                false
            }, // default client
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Collections.<RestClient.AcceptType>emptyList(),
                null,
                true,
                false,
                false,

            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Collections.<RestClient.AcceptType>emptyList(),
                null,
                true,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Collections.singletonList(RestClient.AcceptType.ANY),
                "*/*",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Collections.singletonList(RestClient.AcceptType.ANY),
                "*/*",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Collections.singletonList(RestClient.AcceptType.JSON),
                "application/json",
                true,
                false,
                false,
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Collections.singletonList(RestClient.AcceptType.JSON),
                "application/json",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Collections.singletonList(RestClient.AcceptType.PSON),
                "application/x-pson",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Collections.singletonList(RestClient.AcceptType.PSON),
                "application/x-pson",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON),
                "application/json;q=1.0,application/x-pson;q=0.9",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON),
                "application/json;q=1.0,application/x-pson;q=0.9",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY),
                "application/json;q=1.0,*/*;q=0.9",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY),
                "application/json;q=1.0,*/*;q=0.9",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON),
                "application/x-pson;q=1.0,application/json;q=0.9",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON),
                "application/x-pson;q=1.0,application/json;q=0.9",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
                "application/x-pson;q=1.0,*/*;q=0.9",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
                "application/x-pson;q=1.0,*/*;q=0.9",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON),
                "*/*;q=1.0,application/json;q=0.9",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON),
                "*/*;q=1.0,application/json;q=0.9",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON),
                "*/*;q=1.0,application/x-pson;q=0.9",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON),
                "*/*;q=1.0,application/x-pson;q=0.9",
                true,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
                "application/json;q=1.0,application/x-pson;q=0.9,*/*;q=0.8",
                true,
                false,
                false
            },
            // contentType and acceptTypes configured per request (recommended)
            //
            // {
            //    RestClient.ContentType contentType
            //    String expectedContentTypeHeader,
            //    String expectedRequestBody,
            //    String expectedEntitiesBody,
            //    List<RestClient.AcceptType> acceptTypes,
            //    String expectedAcceptHeader
            //    boolean acceptContentTypePerClient
            //    List<Object> streamingAttachmentDataSources, //null for RestRequest always
            //    boolean acceptResponseAttachments  //false for RestRequest
            // }
            {
                null,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                null,
                null,
                false,
                false,
                false
            }, // default client
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Collections.<RestClient.AcceptType>emptyList(),
                null,
                false,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Collections.<RestClient.AcceptType>emptyList(),
                null,
                false,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Collections.singletonList(RestClient.AcceptType.ANY),
                "*/*",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Collections.singletonList(RestClient.AcceptType.ANY),
                "*/*",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Collections.singletonList(RestClient.AcceptType.JSON),
                "application/json",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Collections.singletonList(RestClient.AcceptType.JSON),
                "application/json",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Collections.singletonList(RestClient.AcceptType.PSON),
                "application/x-pson",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Collections.singletonList(RestClient.AcceptType.PSON),
                "application/x-pson",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON),
                "application/json;q=1.0,application/x-pson;q=0.9",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON),
                "application/json;q=1.0,application/x-pson;q=0.9",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY),
                "application/json;q=1.0,*/*;q=0.9",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY),
                "application/json;q=1.0,*/*;q=0.9",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON),
                "application/x-pson;q=1.0,application/json;q=0.9",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON),
                "application/x-pson;q=1.0,application/json;q=0.9",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
                "application/x-pson;q=1.0,*/*;q=0.9",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
                "application/x-pson;q=1.0,*/*;q=0.9",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON),
                "*/*;q=1.0,application/json;q=0.9",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON),
                "*/*;q=1.0,application/json;q=0.9",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.JSON,
                "application/json",
                JSON_ENTITY_BODY,
                JSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON),
                "*/*;q=1.0,application/x-pson;q=0.9",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON),
                "*/*;q=1.0,application/x-pson;q=0.9",
                false,
                false,
                false
            },
            {
                RestClient.ContentType.PSON,
                "application/x-pson",
                PSON_ENTITY_BODY,
                PSON_ENTITIES_BODY,
                Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
                "application/json;q=1.0,application/x-pson;q=0.9,*/*;q=0.8",
                false,
                false,
                false
            }
        };
  }

  public Object[][] streamRequestData()
  {
    //For each result from restRequestDataSource, create 3 new permutations for streaming. This will result in 3 times as
    //many data sources for StreamRequest data as there were for RestRequest data. We do this programatically to reduce
    //verbosity.
    //For example, a sample entry from restRequestData()"
    //    {
    //      null,
    //      "application/json",
    //      JSON_ENTITY_BODY,
    //      JSON_ENTITIES_BODY,
    //      null,
    //      null,
    //      true,
    //      null,
    //      false,
    //      false, (this will change)
    //      false (this will change)
    //    }
    // will result instead in:
    //    {
    //      null,
    //      "application/json",
    //      JSON_ENTITY_BODY,
    //      JSON_ENTITIES_BODY,
    //      null,
    //      null,
    //      true,
    //      null,
    //      false,
    //      true,
    //      true
    //    }
    //    {
    //      null,
    //      "application/json",
    //      JSON_ENTITY_BODY,
    //      JSON_ENTITIES_BODY,
    //      null,
    //      null,
    //      true,
    //      null,
    //      false,
    //      true,
    //      false
    //    }
    //    {
    //      null,
    //      "application/json",
    //      JSON_ENTITY_BODY,
    //      JSON_ENTITIES_BODY,
    //      null,
    //      null,
    //      true,
    //      null,
    //      false,
    //      false,
    //      true
    //    }

    List<Object[]> tempResult = new ArrayList<>();
    tempResult.addAll(Arrays.asList(restRequestData()));

    List<Object[]> newResult = new ArrayList<>();
    for (final Object[] objectArray : tempResult)
    {
      final Object[] requestAttachmentsResponseAllowed = objectArray.clone();
      requestAttachmentsResponseAllowed[7] = true;
      requestAttachmentsResponseAllowed[8] = true;
      newResult.add(requestAttachmentsResponseAllowed);

      final Object[] requestAttachmentsNoResponseAllowed = objectArray.clone();
      requestAttachmentsNoResponseAllowed[7] = true;
      requestAttachmentsNoResponseAllowed[8] = false;
      newResult.add(requestAttachmentsNoResponseAllowed);

      final Object[] noRequestAttachmentsResponseAllowed = objectArray.clone();
      noRequestAttachmentsResponseAllowed[7] = false;
      noRequestAttachmentsResponseAllowed[8] = true;
      newResult.add(noRequestAttachmentsResponseAllowed);
    }
    return newResult.toArray(new Object[newResult.size()][]);
  }

  @SuppressWarnings("rawtypes")
  private void setCommonExpectations(Request mockRequest,
                                     ResourceMethod method,
                                     RestResponseDecoder mockResponseDecoder,
                                     RestliRequestOptions requestOptions)
  {
    EasyMock.expect(mockRequest.getMethod()).andReturn(method).anyTimes();
    EasyMock.expect(mockRequest.getPathKeys()).andReturn(Collections.<String, String>emptyMap()).once();
    EasyMock.expect(mockRequest.getQueryParamsObjects()).andReturn(Collections.emptyMap()).once();
    EasyMock.expect(mockRequest.getQueryParamClasses()).andReturn(Collections.<String, Class<?>>emptyMap()).once();
    EasyMock.expect(mockRequest.getBaseUriTemplate()).andReturn(BASE_URI_TEMPLATE).times(2);
    EasyMock.expect(mockRequest.getServiceName()).andReturn(SERVICE_NAME).once();
    EasyMock.expect(mockRequest.getResponseDecoder()).andReturn(mockResponseDecoder).once();
    EasyMock.expect(mockRequest.getHeaders()).andReturn(Collections.<String, String>emptyMap()).once();
    EasyMock.expect(mockRequest.getCookies()).andReturn(Collections.<String>emptyList()).once();
    EasyMock.expect(mockRequest.getRequestOptions()).andReturn(requestOptions).anyTimes();
  }

  @SuppressWarnings({"rawtypes", "deprecation"})
  private void buildInputForBatchPatchAndUpdate(Request mockRequest)
  {
    CollectionRequest mockCollectionRequest = EasyMock.createMock(CollectionRequest.class);
    EasyMock.expect(mockCollectionRequest.getElements()).andReturn(Collections.emptyList()).once();
    EasyMock.expect(mockRequest.getInputRecord()).andReturn(mockCollectionRequest).times(2);
    EasyMock.replay(mockCollectionRequest);
    ResourceSpec resourceSpec = new ResourceSpecImpl(Collections.<ResourceMethod> emptySet(),
                                                     Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                     Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                     null,
                                                     null,
                                                     null,
                                                     EmptyRecord.class,
                                                     Collections.<String, CompoundKey.TypeInfo> emptyMap());
    EasyMock.expect(mockRequest.getResourceProperties()).andReturn(
        new ResourcePropertiesImpl(Collections.<ResourceMethod> emptySet(),
                               null,
                               null,
                               TypeSpec.forClassMaybeNull(EmptyRecord.class),
                               Collections.<String, CompoundKey.TypeInfo> emptyMap())).once();
  }

  @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
  private <T extends Request> RestRequest clientGeneratedRestRequest(Class<T> requestClass,
                                                                     ResourceMethod method,
                                                                     DataMap entityBody,
                                                                     RestClient.ContentType contentType,
                                                                     List<RestClient.AcceptType> acceptTypes,
                                                                     boolean acceptContentTypePerClient)
    throws URISyntaxException
  {
    // massive setup...
    Client mockClient = EasyMock.createMock(Client.class);

    @SuppressWarnings({"rawtypes"})
    Request<?> mockRequest = EasyMock.createMock(requestClass);

    RecordTemplate mockRecordTemplate = EasyMock.createMock(RecordTemplate.class);

    @SuppressWarnings({"rawtypes"})
    RestResponseDecoder mockResponseDecoder = EasyMock.createMock(RestResponseDecoder.class);

    RestliRequestOptions requestOptions = RestliRequestOptions.DEFAULT_OPTIONS;
    if (!acceptContentTypePerClient)
    {
      requestOptions = new RestliRequestOptions(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, null, null, contentType, acceptTypes, false);
    }

    setCommonExpectations(mockRequest, method, mockResponseDecoder, requestOptions);

    EasyMock.expect(mockRequest.getStreamingAttachments()).andReturn(null).times(2);

    setResourceMethodExpectations(method, mockRequest, mockRecordTemplate, entityBody);

    Capture<RestRequest> restRequestCapture = new Capture<RestRequest>();

    EasyMock.expect(mockClient.getMetadata(new URI(HOST + SERVICE_NAME)))
        .andReturn(Collections.<String, Object>emptyMap()).once();

    mockClient.restRequest(EasyMock.capture(restRequestCapture),
                           (RequestContext) EasyMock.anyObject(),
                           (Callback<RestResponse>) EasyMock.anyObject());
    EasyMock.expectLastCall().once();

    EasyMock.replay(mockClient, mockRequest, mockRecordTemplate);

    // do work!
    RestClient restClient;
    if (acceptContentTypePerClient)
    {
      // configuration per client
      restClient = new RestClient(mockClient, HOST, contentType, acceptTypes);
    }
    else
    {
      // configuration per request
      restClient = new RestClient(mockClient, HOST);
    }

    restClient.sendRequest(mockRequest);

    return restRequestCapture.getValue();
  }

  //This is similar to clientGeneratedRestRequest above except that it will generate a StreamRequest instead
  //of a RestRequest. Note that this will ONLY happen if either acceptResponseAttachments below is 'true' OR
  //streamingAttachmentDataSources below is non-null with a size greater then 0. If both of these do not hold,
  //then a StreamRequest will not be generated by the RestClient.
  @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
  private <T extends Request> StreamRequest clientGeneratedStreamRequest(Class<T> requestClass,
                                                                         ResourceMethod method,
                                                                         DataMap entityBody,
                                                                         RestClient.ContentType contentType,
                                                                         List<RestClient.AcceptType> acceptTypes,
                                                                         boolean acceptContentTypePerClient,
                                                                         List<Object> streamingAttachmentDataSources,
                                                                         boolean acceptResponseAttachments)
      throws URISyntaxException
  {
    // massive setup...
    Client mockClient = EasyMock.createMock(Client.class);

    @SuppressWarnings({"rawtypes"})
    Request<?> mockRequest = EasyMock.createMock(requestClass);

    RecordTemplate mockRecordTemplate = EasyMock.createMock(RecordTemplate.class);

    @SuppressWarnings({"rawtypes"})
    RestResponseDecoder mockResponseDecoder = EasyMock.createMock(RestResponseDecoder.class);

    RestliRequestOptions requestOptions = RestliRequestOptions.DEFAULT_OPTIONS;

    //If there is a desire to receive response attachments, then we must use request options.
    if (!acceptContentTypePerClient || acceptResponseAttachments)
    {
      requestOptions = new RestliRequestOptions(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, null, null, contentType, acceptTypes, acceptResponseAttachments);
    }

    setCommonExpectations(mockRequest, method, mockResponseDecoder, requestOptions);

    if (streamingAttachmentDataSources != null && streamingAttachmentDataSources.size() > 0)
    {
      EasyMock.expect(mockRequest.getStreamingAttachments()).andReturn(streamingAttachmentDataSources).times(2);
    }
    else
    {
      EasyMock.expect(mockRequest.getStreamingAttachments()).andReturn(null).times(2);
    }

    setResourceMethodExpectations(method, mockRequest, mockRecordTemplate, entityBody);

    Capture<StreamRequest> streamRequestCapture = new Capture<StreamRequest>();

    EasyMock.expect(mockClient.getMetadata(new URI(HOST + SERVICE_NAME)))
        .andReturn(Collections.<String, Object>emptyMap()).once();

    mockClient.streamRequest(EasyMock.capture(streamRequestCapture), (RequestContext) EasyMock.anyObject(),
                             (Callback<StreamResponse>) EasyMock.anyObject());
    EasyMock.expectLastCall().once();

    EasyMock.replay(mockClient, mockRequest, mockRecordTemplate);

    // do work!
    RestClient restClient;
    if (acceptContentTypePerClient)
    {
      // configuration per client
      restClient = new RestClient(mockClient, HOST, contentType, acceptTypes);
    }
    else
    {
      // configuration per request
      restClient = new RestClient(mockClient, HOST);
    }

    restClient.sendRequest(mockRequest);

    return streamRequestCapture.getValue();
  }

  private void setResourceMethodExpectations(final ResourceMethod method, final Request<?> mockRequest,
                                             final RecordTemplate mockRecordTemplate, final DataMap entityBody)
  {

    if (method == ResourceMethod.BATCH_PARTIAL_UPDATE || method == ResourceMethod.BATCH_UPDATE)
    {
      buildInputForBatchPatchAndUpdate(mockRequest);
    }
    else
    {
      EasyMock.expect(mockRequest.getInputRecord()).andReturn(mockRecordTemplate).times(2);
    }

    if (method == ResourceMethod.GET)
    {
      EasyMock.expect(((GetRequest)mockRequest).getObjectId()).andReturn(null).once();
      EasyMock.expect(mockRequest.getResourceProperties()).andReturn(
          new ResourcePropertiesImpl(Collections.<ResourceMethod> emptySet(),
                                     null,
                                     null,
                                     null,
                                     Collections.<String, CompoundKey.TypeInfo> emptyMap())).once();
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }
    else if (method == ResourceMethod.BATCH_GET)
    {
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }
    else if (method == ResourceMethod.ACTION)
    {
      EasyMock.expect(((ActionRequest)mockRequest).getId()).andReturn(null);
      EasyMock.expect(mockRequest.getMethodName()).andReturn("testAction");
    }
    else if (method == ResourceMethod.FINDER)
    {
      EasyMock.expect(((FindRequest)mockRequest).getAssocKey()).andReturn(new CompoundKey());
      EasyMock.expect(mockRequest.getMethodName()).andReturn("testFinder");
    }
    else if (method == ResourceMethod.GET_ALL)
    {
      EasyMock.expect(((GetAllRequest)mockRequest).getAssocKey()).andReturn(new CompoundKey());
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }
    else if (method == ResourceMethod.UPDATE)
    {
      EasyMock.expect(mockRequest.getResourceProperties()).andReturn(
          new ResourcePropertiesImpl(Collections.<ResourceMethod> emptySet(),
                                     null,
                                     null,
                                     null,
                                     Collections.<String, CompoundKey.TypeInfo> emptyMap())).once();
      EasyMock.expect(((UpdateRequest)mockRequest).getId()).andReturn(null);
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }
    else if (method == ResourceMethod.PARTIAL_UPDATE)
    {
      EasyMock.expect(mockRequest.getResourceProperties()).andReturn(
          new ResourcePropertiesImpl(Collections.<ResourceMethod> emptySet(),
                                     null,
                                     null,
                                     null,
                                     Collections.<String, CompoundKey.TypeInfo> emptyMap())).once();
      EasyMock.expect(((PartialUpdateRequest)mockRequest).getId()).andReturn(null);
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }
    else if (method == ResourceMethod.DELETE)
    {
      EasyMock.expect(mockRequest.getResourceProperties()).andReturn(
          new ResourcePropertiesImpl(Collections.<ResourceMethod> emptySet(),
                                     null,
                                     null,
                                     null,
                                     Collections.<String, CompoundKey.TypeInfo> emptyMap())).once();
      EasyMock.expect(((DeleteRequest)mockRequest).getId()).andReturn(null);
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }
    else
    {
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }

    EasyMock.expect(mockRecordTemplate.data()).andReturn(entityBody).once();
  }

  private void verifyStreamRequest(final StreamRequest streamRequest, final boolean acceptResponseAttachments,
                                   final String expectedAcceptHeader, final boolean streamAttachments,
                                   final String expectedContentTypeHeader, final String expectedRequestBody)
  {
    Assert.assertNotNull(streamRequest); //Otherwise it went down the RestRequest code path incorrectly.

    //The accept type header will look different based on whether or not attachments were expected.
    if (acceptResponseAttachments)
    {
      if (expectedAcceptHeader != null)
      {
        Assert.assertTrue(streamRequest.getHeader(ACCEPT_TYPE_HEADER).startsWith(expectedAcceptHeader));
        Assert.assertTrue(streamRequest.getHeader(ACCEPT_TYPE_HEADER).contains(RestConstants.HEADER_VALUE_MULTIPART_RELATED));
      }
      else
      {
        Assert.assertEquals(streamRequest.getHeader(ACCEPT_TYPE_HEADER), RestConstants.HEADER_VALUE_MULTIPART_RELATED + ";q=1.0");
      }
    }
    else
    {
      Assert.assertEquals(streamRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);
    }

    if (streamAttachments == false)
    {
      //If there are no attachments, then we can just read everything in
      Messages.toRestRequest(streamRequest, new Callback<RestRequest>()
      {
        @Override
        public void onError(Throwable e)
        {
          Assert.fail();
        }

        @Override
        public void onSuccess(RestRequest result)
        {
          //Verify content type and header after the conversion is complete.
          Assert.assertEquals(result.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
          Assert.assertEquals(result.getEntity().asAvroString(), expectedRequestBody);
        }
      });
    }
    else
    {
      //There were attachments so let's read using MultiPartMIMEReader to verify the wire format designed by RestClient
      //is indeed correct
      final MultiPartMIMEReader streamRequestReader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);
      final CountDownLatch streamRequestReaderLatch = new CountDownLatch(1);
      final MultiPartMIMEFullReaderCallback streamRequestReaderCallback = new MultiPartMIMEFullReaderCallback(streamRequestReaderLatch);
      streamRequestReader.registerReaderCallback(streamRequestReaderCallback);
      try
      {
        streamRequestReaderLatch.await(3000, TimeUnit.MILLISECONDS);
      }
      catch (InterruptedException interruptedException)
      {
        Assert.fail();
      }
      final List<SinglePartMIMEFullReaderCallback> singlePartMIMEReaderCallbacks = streamRequestReaderCallback.getSinglePartMIMEReaderCallbacks();

      //We have should have three parts. One for the rest.li payload and two for the attachments.
      Assert.assertEquals(singlePartMIMEReaderCallbacks.size(), 3);
      //Verify the first part by looking at its content type and payload.
      Assert.assertEquals(singlePartMIMEReaderCallbacks.get(0).getHeaders().get(CONTENT_TYPE_HEADER),
                          expectedContentTypeHeader);
      Assert.assertEquals(singlePartMIMEReaderCallbacks.get(0).getFinishedData().asAvroString(), expectedRequestBody);

      //Verify the top level content type is multipart mime related. Use startsWith() since the boundary is random.
      Assert.assertTrue(streamRequest.getHeader(CONTENT_TYPE_HEADER).startsWith(RestConstants.HEADER_VALUE_MULTIPART_RELATED));

      //Now verify the attachments. We have to remove the first part since we already read it.
      singlePartMIMEReaderCallbacks.remove(0);
      verifyAttachments(singlePartMIMEReaderCallbacks);
    }
  }

  private void verifyAttachments(final List<SinglePartMIMEFullReaderCallback> singlePartMIMEFullReaderCallbacks)
  {
    Assert.assertEquals(singlePartMIMEFullReaderCallbacks.size(), 2);

    //First attachment
    final SinglePartMIMEFullReaderCallback firstCallback = singlePartMIMEFullReaderCallbacks.get(0);
    Assert.assertEquals(firstCallback.getFinishedData(), FIRST_PART_PAYLOAD);
    Assert.assertEquals(firstCallback.getHeaders().size(), 1);
    Assert.assertEquals(firstCallback.getHeaders().get(RestConstants.HEADER_CONTENT_ID), FIRST_PART_ID);

    //Second attachment
    final SinglePartMIMEFullReaderCallback secondCallback = singlePartMIMEFullReaderCallbacks.get(1);
    Assert.assertEquals(secondCallback.getFinishedData(), SECOND_PART_PAYLOAD);
    Assert.assertEquals(secondCallback.getHeaders().size(), 1);
    Assert.assertEquals(secondCallback.getHeaders().get(RestConstants.HEADER_CONTENT_ID), SECOND_PART_ID);
  }

  private List<Object> generateRequestAttachments()
  {
    final List<Object> requestAttachments = new ArrayList<>();
    requestAttachments.add(new RestLiTestAttachmentDataSource(FIRST_PART_ID, FIRST_PART_PAYLOAD));
    requestAttachments.add(new RestLiTestAttachmentDataSource(SECOND_PART_ID, SECOND_PART_PAYLOAD));
    return requestAttachments;
  }
}