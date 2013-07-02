/**
 * $Id: $
 */

package com.linkedin.restli.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestRestClientRequestBuilder
{

  private static final DataMap ENTITY_BODY = new DataMap();
  private static final String  JSON_ENTITY_BODY = "{\"testFieldName\":\"testValue\",\"testInteger\":1}";
  private static final String  PSON_ENTITY_BODY = "#!PSON1\n!\u0081testFieldName\u0000\n\n\u0000\u0000\u0000testValue\u0000\u0083testInteger\u0000\u0002\u0001\u0000\u0000\u0000\u0080";
  private static final String  CONTENT_TYPE_HEADER = "Content-Type";
  private static final String  ACCEPT_TYPE_HEADER = "Accept";

  static
  {
    ENTITY_BODY.put("testFieldName", "testValue");
    ENTITY_BODY.put("testInteger", 1);
  }

  @Test(dataProvider = "data")
  public void testGet(RestClient.ContentType contentType,
                      String expectedContentTypeHeader,
                      String expectedRequestBody,
                      List<RestClient.AcceptType> acceptTypes,
                      String expectedAcceptHeader)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(ResourceMethod.GET, null, contentType, acceptTypes);
    Assert.assertNull(restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequest.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestBatch = clientGeneratedRequest(ResourceMethod.BATCH_GET, null, contentType, acceptTypes);
    Assert.assertNull(restRequestBatch.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequestBatch.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

  }

  @Test(dataProvider = "data")
  public void testFinder(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(ResourceMethod.FINDER, null, contentType, acceptTypes);
    Assert.assertNull(restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequest.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestAll = clientGeneratedRequest(ResourceMethod.GET_ALL, null, contentType, acceptTypes);
    Assert.assertNull(restRequestAll.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequestAll.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));
  }

  @Test(dataProvider = "data")
  public void testAction(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(ResourceMethod.ACTION, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedRequestBody, restRequest.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestNoEntity = clientGeneratedRequest(ResourceMethod.ACTION, null, contentType, acceptTypes);
    Assert.assertNull(restRequestNoEntity.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequestNoEntity.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));
  }

  @Test(dataProvider = "data")
  public void testUpdate(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(ResourceMethod.UPDATE, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedRequestBody, restRequest.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestBatch = clientGeneratedRequest(ResourceMethod.BATCH_UPDATE, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequestBatch.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedRequestBody, restRequestBatch.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestPartial = clientGeneratedRequest(ResourceMethod.PARTIAL_UPDATE, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequestPartial.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedRequestBody, restRequestPartial.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestBatchPartial = clientGeneratedRequest(ResourceMethod.BATCH_PARTIAL_UPDATE, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequestBatchPartial.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedRequestBody, restRequestBatchPartial.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));
  }

  @Test(dataProvider = "data")
  public void testCreate(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(ResourceMethod.CREATE, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedRequestBody, restRequest.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestBatch = clientGeneratedRequest(ResourceMethod.BATCH_CREATE, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequestBatch.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedRequestBody, restRequestBatch.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));
  }

  @Test(dataProvider = "data")
  public void testDelete(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(ResourceMethod.DELETE, null, contentType, acceptTypes);
    Assert.assertNull(restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequest.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestBatch = clientGeneratedRequest(ResourceMethod.BATCH_DELETE, null, contentType, acceptTypes);
    Assert.assertNull(restRequestBatch.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequestBatch.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));
  }

  @DataProvider(name = "data")
  public Object[][] contentTypeData()
  {
    return new Object[][]
      {
        { null,  "application/json", JSON_ENTITY_BODY, null, null }, // default client
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          Collections.<RestClient.AcceptType>emptyList(),
          null
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          Collections.<RestClient.AcceptType>emptyList(),
          null
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          Collections.singletonList(RestClient.AcceptType.ANY),
          "*/*"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          Collections.singletonList(RestClient.AcceptType.ANY),
          "*/*"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          Collections.singletonList(RestClient.AcceptType.JSON),
          "application/json"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          Collections.singletonList(RestClient.AcceptType.JSON),
          "application/json"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          Collections.singletonList(RestClient.AcceptType.PSON),
          "application/x-pson"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          Collections.singletonList(RestClient.AcceptType.PSON),
          "application/x-pson"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON),
          "application/json;q=1.0,application/x-pson;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON),
          "application/json;q=1.0,application/x-pson;q=0.9"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY),
          "application/json;q=1.0,*/*;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY),
          "application/json;q=1.0,*/*;q=0.9"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON),
          "application/x-pson;q=1.0,application/json;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON),
          "application/x-pson;q=1.0,application/json;q=0.9"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
          "application/x-pson;q=1.0,*/*;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
          "application/x-pson;q=1.0,*/*;q=0.9"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON),
          "*/*;q=1.0,application/json;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON),
          "*/*;q=1.0,application/json;q=0.9"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON),
          "*/*;q=1.0,application/x-pson;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON),
          "*/*;q=1.0,application/x-pson;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
          "application/json;q=1.0,application/x-pson;q=0.9,*/*;q=0.8"
        },
      };
  }

  @SuppressWarnings("unchecked")
  private RestRequest clientGeneratedRequest(ResourceMethod method,
                                             DataMap entityBody,
                                             RestClient.ContentType contentType,
                                             List<RestClient.AcceptType> acceptTypes)
    throws URISyntaxException
  {
    // massive setup...
    Client mockClient = EasyMock.createMock(Client.class);
    @SuppressWarnings({"rawtypes"})
    Request<?> mockRequest = EasyMock.createMock(Request.class);
    RecordTemplate mockRecordTemplate = EasyMock.createMock(RecordTemplate.class);
    @SuppressWarnings({"rawtypes"})
    RestResponseDecoder restResponseDecoder = EasyMock.createMock(RestResponseDecoder.class);

    // sendRequest
    EasyMock.expect(mockRequest.getInput()).andReturn(mockRecordTemplate).once();
    EasyMock.expect(mockRequest.getResponseDecoder()).andReturn(restResponseDecoder).once();
    EasyMock.expect(mockRequest.getUri()).andReturn(new URI("test"));
    EasyMock.expect(mockRequest.getMethod()).andReturn(method).once();
    EasyMock.expect(mockRecordTemplate.data()).andReturn(entityBody).once();
    EasyMock.expect(mockRequest.getHeaders()).andReturn(Collections.<String, String>emptyMap()).once();

    // sendRequestImpl

    Capture<RestRequest> restRequestCapture = new Capture<RestRequest>();
    mockClient.restRequest(EasyMock.capture(restRequestCapture),
                           (RequestContext) EasyMock.anyObject(),
                           (Callback<RestResponse>) EasyMock.anyObject());
    EasyMock.expectLastCall().once();

    EasyMock.replay(mockClient, mockRequest, mockRecordTemplate);

    // do work!
    String host = "host";
    RestClient restClient;
    if (acceptTypes == null)
    {
      restClient = new RestClient(mockClient, host);
    }
    else if (contentType == null)
    {
      restClient = new RestClient(mockClient, host, acceptTypes);
    }
    else
    {
      restClient = new RestClient(mockClient, host, contentType, acceptTypes);
    }

    restClient.sendRequest(mockRequest);

    return restRequestCapture.getValue();
  }

}
