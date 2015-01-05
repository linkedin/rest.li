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
import com.linkedin.data.DataMap;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import com.linkedin.restli.internal.common.ResourcePropertiesImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestRestClientRequestBuilder
{

  private static final DataMap ENTITY_BODY = new DataMap();
  private static final String  JSON_ENTITY_BODY = "{\"testFieldName\":\"testValue\",\"testInteger\":1}";
  private static final String  PSON_ENTITY_BODY = "#!PSON1\n!\u0081testFieldName\u0000\n\n\u0000\u0000\u0000testValue\u0000\u0083testInteger\u0000\u0002\u0001\u0000\u0000\u0000\u0080";
  private static final String JSON_ENTITIES_BODY = "{\"entities\":{}}";
  private static final String PSON_ENTITIES_BODY = "#!PSON1\n" + "!\u0081entities\u0000 \u0080";
  private static final String  CONTENT_TYPE_HEADER = "Content-Type";
  private static final String  ACCEPT_TYPE_HEADER = "Accept";
  private static final String HOST = "host";
  private static final String SERVICE_NAME = "foo";
  private static final String BASE_URI_TEMPLATE = "/foo";

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
                      boolean acceptContentTypePerClient)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(GetRequest.class, ResourceMethod.GET, null, contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertNull(restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(restRequest.getEntity().length(), 0);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

    RestRequest restRequestBatch = clientGeneratedRequest(BatchGetRequest.class, ResourceMethod.BATCH_GET, null, contentType, acceptTypes, acceptContentTypePerClient);
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
                         boolean acceptContentTypePerClient)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(FindRequest.class, ResourceMethod.FINDER, null, contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertNull(restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(restRequest.getEntity().length(), 0);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

    RestRequest restRequestAll = clientGeneratedRequest(GetAllRequest.class, ResourceMethod.GET_ALL, null, contentType, acceptTypes, acceptContentTypePerClient);
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
                         boolean acceptContentTypePerClient)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(ActionRequest.class,
                                                     ResourceMethod.ACTION,
                                                     ENTITY_BODY,
                                                     contentType,
                                                     acceptTypes,
                                                     acceptContentTypePerClient);
    Assert.assertEquals(restRequest.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
    Assert.assertEquals(restRequest.getEntity().asAvroString(), expectedRequestBody);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

    RestRequest restRequestNoEntity = clientGeneratedRequest(ActionRequest.class, ResourceMethod.ACTION, null, contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertNull(restRequestNoEntity.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(restRequestNoEntity.getEntity().length(), 0);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);
  }

  @Test(dataProvider = "data")
  public void testUpdate(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader,
                         boolean acceptContentTypePerClient)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(UpdateRequest.class, ResourceMethod.UPDATE, ENTITY_BODY, contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertEquals(restRequest.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
    Assert.assertEquals(restRequest.getEntity().asAvroString(), expectedRequestBody);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

    RestRequest restRequestBatch = clientGeneratedRequest(BatchUpdateRequest.class, ResourceMethod.BATCH_UPDATE, ENTITY_BODY, contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertEquals(restRequestBatch.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
    Assert.assertEquals(restRequestBatch.getEntity().asAvroString(), expectedEntitiesBody);
    Assert.assertEquals(restRequestBatch.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

    RestRequest restRequestPartial = clientGeneratedRequest(PartialUpdateRequest.class, ResourceMethod.PARTIAL_UPDATE, ENTITY_BODY, contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertEquals(restRequestPartial.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
    Assert.assertEquals(restRequestPartial.getEntity().asAvroString(), expectedRequestBody);
    Assert.assertEquals(restRequestPartial.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

    RestRequest restRequestBatchPartial = clientGeneratedRequest(BatchPartialUpdateRequest.class, ResourceMethod.BATCH_PARTIAL_UPDATE, ENTITY_BODY, contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertEquals(restRequestBatchPartial.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
    Assert.assertEquals(restRequestBatchPartial.getEntity().asAvroString(), expectedEntitiesBody);
    Assert.assertEquals(restRequestBatchPartial.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);
  }

  @Test(dataProvider = "data")
  public void testCreate(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader,
                         boolean acceptContentTypePerClient)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(CreateRequest.class, ResourceMethod.CREATE, ENTITY_BODY, contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertEquals(restRequest.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
    Assert.assertEquals(restRequest.getEntity().asAvroString(), expectedRequestBody);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

    RestRequest restRequestBatch = clientGeneratedRequest(BatchCreateRequest.class, ResourceMethod.BATCH_CREATE, ENTITY_BODY, contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertEquals(restRequestBatch.getHeader(CONTENT_TYPE_HEADER), expectedContentTypeHeader);
    Assert.assertEquals(restRequestBatch.getEntity().asAvroString(), expectedRequestBody);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);
  }

  @Test(dataProvider = "data")
  public void testDelete(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader,
                         boolean acceptContentTypePerClient)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(DeleteRequest.class, ResourceMethod.DELETE, null, contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertNull(restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(restRequest.getEntity().length(), 0);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);

    RestRequest restRequestBatch = clientGeneratedRequest(BatchDeleteRequest.class, ResourceMethod.BATCH_DELETE, null, contentType, acceptTypes, acceptContentTypePerClient);
    Assert.assertNull(restRequestBatch.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(restRequestBatch.getEntity().length(), 0);
    Assert.assertEquals(restRequest.getHeader(ACCEPT_TYPE_HEADER), expectedAcceptHeader);
  }

  @DataProvider(name = "data")
  public Object[][] contentTypeData()
  {
    return new Object[][]
      {
        // contentTypes and acceptTypes configured per client (deprecated)
        //
        // {
        //    RestClient.ContentType contentType
        //    String expectedContentTypeHeader,
        //    String expectedRequestBody,
        //    String expectedEntitiesBody,
        //    List<RestClient.AcceptType> acceptTypes,
        //    String expectedAcceptHeader
        //    boolean acceptContentTypePerClient
        // }
        {
          null,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          null,
          null,
          true
        }, // default client
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Collections.<RestClient.AcceptType>emptyList(),
          null,
          true
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Collections.<RestClient.AcceptType>emptyList(),
          null,
          true
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.ANY),
          "*/*",
          true
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.ANY),
          "*/*",
          true
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.JSON),
          "application/json",
          true
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.JSON),
          "application/json",
          true
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.PSON),
          "application/x-pson",
          true
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.PSON),
          "application/x-pson",
          true
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON),
          "application/json;q=1.0,application/x-pson;q=0.9",
          true
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON),
          "application/json;q=1.0,application/x-pson;q=0.9",
          true
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY),
          "application/json;q=1.0,*/*;q=0.9",
          true
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY),
          "application/json;q=1.0,*/*;q=0.9",
          true
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON),
          "application/x-pson;q=1.0,application/json;q=0.9",
          true
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON),
          "application/x-pson;q=1.0,application/json;q=0.9",
          true
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
          "application/x-pson;q=1.0,*/*;q=0.9",
          true
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
          "application/x-pson;q=1.0,*/*;q=0.9",
          true
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON),
          "*/*;q=1.0,application/json;q=0.9",
          true
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON),
          "*/*;q=1.0,application/json;q=0.9",
          true
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON),
          "*/*;q=1.0,application/x-pson;q=0.9",
          true
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON),
          "*/*;q=1.0,application/x-pson;q=0.9",
          true
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
          "application/json;q=1.0,application/x-pson;q=0.9,*/*;q=0.8",
          true
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
        // }
        {
          null,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          null,
          null,
          false
        }, // default client
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Collections.<RestClient.AcceptType>emptyList(),
          null,
          false
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Collections.<RestClient.AcceptType>emptyList(),
          null,
          false
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.ANY),
          "*/*",
          false
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.ANY),
          "*/*",
          false
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.JSON),
          "application/json",
          false
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.JSON),
          "application/json",
          false
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.PSON),
          "application/x-pson",
          false
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.PSON),
          "application/x-pson",
          false
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON),
          "application/json;q=1.0,application/x-pson;q=0.9",
          false
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON),
          "application/json;q=1.0,application/x-pson;q=0.9",
          false
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY),
          "application/json;q=1.0,*/*;q=0.9",
          false
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY),
          "application/json;q=1.0,*/*;q=0.9",
          false
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON),
          "application/x-pson;q=1.0,application/json;q=0.9",
          false
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON),
          "application/x-pson;q=1.0,application/json;q=0.9",
          false
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
          "application/x-pson;q=1.0,*/*;q=0.9",
          false
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
          "application/x-pson;q=1.0,*/*;q=0.9",
          false
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON),
          "*/*;q=1.0,application/json;q=0.9",
          false
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON),
          "*/*;q=1.0,application/json;q=0.9",
          false
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON),
          "*/*;q=1.0,application/x-pson;q=0.9",
          false
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON),
          "*/*;q=1.0,application/x-pson;q=0.9",
          false
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
          "application/json;q=1.0,application/x-pson;q=0.9,*/*;q=0.8",
          false
        }
      };
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
  private <T extends Request> RestRequest clientGeneratedRequest(Class<T> requestClass,
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
      requestOptions = new RestliRequestOptions(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, null, null, contentType, acceptTypes);
    }
    setCommonExpectations(mockRequest, method, mockResponseDecoder, requestOptions);

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

}
