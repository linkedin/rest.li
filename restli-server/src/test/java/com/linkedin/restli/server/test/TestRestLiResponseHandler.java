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

package com.linkedin.restli.server.test;

import com.linkedin.data.codec.DataCodec;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.codec.PsonDataCodec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.FieldDef;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.GetResult;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.RestLiResponseHandler;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.BasicCollectionResult;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.StringMap;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor.InterfaceType;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.twitter.StatusCollectionResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;

import static com.linkedin.restli.server.test.RestLiTestHelper.buildResourceModel;
import static com.linkedin.restli.server.test.RestLiTestHelper.doubleQuote;
import static org.testng.Assert.*;


/**
 * @author dellamag
 */
public class TestRestLiResponseHandler
{
  private final RestLiResponseHandler _responseHandler = new RestLiResponseHandler();

  private static final String APPLICATION_JSON = "application/json";
  private static final String APPLICATION_PSON = "application/x-pson";

  private static final Map<String, String> JSON_ACCEPT_HEADERS  = Collections.singletonMap("Accept", APPLICATION_JSON);
  private static final Map<String, String> PSON_ACCEPT_HEADERS  = Collections.singletonMap("Accept", APPLICATION_PSON);
  private static final Map<String, String> EMPTY_ACCEPT_HEADERS = Collections.emptyMap();
  private static final Map<String, String> ANY_ACCEPT_HEADERS   = Collections.singletonMap("Accept", "*/*");

  private static final PsonDataCodec PSON_DATA_CODEC = new PsonDataCodec();
  private static final JacksonDataCodec JACKSON_DATA_CODEC = new JacksonDataCodec();

  private static final String EXPECTED_STATUS_JSON = doubleQuote("{'text':'test status'}");
  private static final String EXPECTED_STATUS_ACTION_RESPONSE_JSON = doubleQuote("{'value':") + EXPECTED_STATUS_JSON + '}';
  private static final String EXPECTED_STATUS_PSON = "#!PSON1\n!\u0081text\u0000\n\f\u0000\u0000\u0000test status\u0000\u0080";
  private static final String EXPECTED_STATUS_ACTION_RESPONSE_PSON = "#!PSON1\n!\u0081value\u0000!\u0083text\u0000\n\f\u0000\u0000\u0000test status\u0000\u0080\u0080";

  private RestResponse invokeResponseHandler(String path,
                                             Object body,
                                             ResourceMethod method,
                                             Map<String, String> headers) throws Exception
  {
    RestRequest req = buildRequest(path, headers);
    RoutingResult routing = buildRoutingResult(method, req);
    return _responseHandler.buildResponse(req, routing, body);
  }

  private static enum AcceptTypeData
  {
    JSON  (JSON_ACCEPT_HEADERS,   APPLICATION_JSON,   JACKSON_DATA_CODEC),
    PSON  (PSON_ACCEPT_HEADERS,   APPLICATION_PSON,   PSON_DATA_CODEC),
    EMPTY (EMPTY_ACCEPT_HEADERS,  APPLICATION_JSON,   JACKSON_DATA_CODEC),
    ANY   (ANY_ACCEPT_HEADERS,    APPLICATION_JSON,   JACKSON_DATA_CODEC);

    public Map<String, String> acceptHeaders;
    public String              responseContentType;
    public DataCodec           dataCodec;

    private AcceptTypeData(Map<String, String> acceptHeaders, String responseContentType, DataCodec dataCodec)
    {
      this.acceptHeaders = acceptHeaders;
      this.responseContentType = responseContentType;
      this.dataCodec = dataCodec;
    }
  }

  @Test
  private void testInvalidAcceptHeaders() throws Exception
  {
    Map<String, String> badAcceptHeaders = Collections.singletonMap("Accept", "foo/bar");

    // check response with body (expect 406 error)
    try
    {
      RestResponse response = invokeResponseHandler("/test", buildStatusRecord(), ResourceMethod.GET,
                                                    badAcceptHeaders);
      Assert.fail();
    }
    catch (RoutingException e)
    {
      Assert.assertEquals(e.getStatus(), 406);
    }

    // check response without body (expect no error)
    RestResponse response = invokeResponseHandler("/test", new CreateResponse(HttpStatus.S_201_CREATED), ResourceMethod.CREATE, badAcceptHeaders);
    checkResponse(response, 201, 0, null, null, null, false, false);
  }

  @DataProvider(name="statusData")
  public Object[][] statusData()
  {
    return new Object[][]
      {
        { AcceptTypeData.EMPTY, EXPECTED_STATUS_JSON },
        { AcceptTypeData.ANY, EXPECTED_STATUS_JSON },
        { AcceptTypeData.JSON, EXPECTED_STATUS_JSON },
        { AcceptTypeData.PSON, EXPECTED_STATUS_PSON }
      };
  }

  @Test(dataProvider = "statusData")
  public void testBasicResponses(AcceptTypeData acceptTypeData, String expectedStatus) throws Exception
  {
    RestResponse response;
    // #1 simple record template
    response = invokeResponseHandler("/test", buildStatusRecord(), ResourceMethod.GET, acceptTypeData.acceptHeaders);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true);
    assertEquals(response.getEntity().asAvroString(), expectedStatus);

    // #2 create (with id)
    response = invokeResponseHandler("/test", new CreateResponse(1), ResourceMethod.CREATE, acceptTypeData.acceptHeaders);
    checkResponse(response, 201, 2, null, null, null, false);
    assertEquals(response.getHeader(RestConstants.HEADER_LOCATION), "/test/1");
    assertEquals(response.getHeader(RestConstants.HEADER_ID), "1");

    // #2.1 create (without id)
    response = invokeResponseHandler("/test", new CreateResponse(HttpStatus.S_201_CREATED),
                                     ResourceMethod.CREATE, acceptTypeData.acceptHeaders);
    checkResponse(response, 201, 0, null, null, null, false);

    // #2.2 create (with id and slash at the end of uri)
    response = invokeResponseHandler("/test/", new CreateResponse(1), ResourceMethod.CREATE, acceptTypeData.acceptHeaders);
    checkResponse(response, 201, 2, null, null, null, false);
    assertEquals(response.getHeader(RestConstants.HEADER_LOCATION), "/test/1");
    assertEquals(response.getHeader(RestConstants.HEADER_ID), "1");

    // #2.3 create (without id and slash at the end of uri)
    response = invokeResponseHandler("/test/", new CreateResponse(HttpStatus.S_201_CREATED),
                                     ResourceMethod.CREATE, acceptTypeData.acceptHeaders);
    checkResponse(response, 201, 0, null, null, null, false);

    // #3 update
    response = invokeResponseHandler("/test", new UpdateResponse(HttpStatus.S_204_NO_CONTENT),
                                     ResourceMethod.UPDATE, acceptTypeData.acceptHeaders);
    checkResponse(response, 204, 0, null, null, null, false);

  }

  @DataProvider(name="basicData")
  public Object[][] basicData()
  {
    return new Object[][]
      {
        { AcceptTypeData.EMPTY },
        { AcceptTypeData.ANY },
        { AcceptTypeData.JSON },
        { AcceptTypeData.PSON }
      };
  }

  @Test(dataProvider = "basicData")
  public void testBatchResponses(AcceptTypeData acceptTypeData) throws Exception
  {
    RestResponse response;

    // #4 batch
    Map<Long, Status> map = new HashMap<Long, Status>();
    map.put(1L, buildStatusRecord());
    map.put(2L, buildStatusRecord());
    map.put(3L, buildStatusRecord());
    response = invokeResponseHandler("/test", map, ResourceMethod.BATCH_GET, acceptTypeData.acceptHeaders);
    checkResponse(response, 200, 3, acceptTypeData.responseContentType, BatchResponse.class.getName(), Status.class.getName(), true);

    Map<Long, UpdateResponse> updateStatusMap = new HashMap<Long, UpdateResponse>();
    updateStatusMap.put(1L, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    updateStatusMap.put(2L, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    updateStatusMap.put(3L, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    BatchUpdateResult<Long, Status> batchUpdateResult = new BatchUpdateResult<Long, Status>(updateStatusMap);
    response = invokeResponseHandler("/test", batchUpdateResult, ResourceMethod.BATCH_UPDATE, acceptTypeData.acceptHeaders);
    checkResponse(response, 200, 3, acceptTypeData.responseContentType, BatchResponse.class.getName(), UpdateStatus.class.getName(), true);

    response = invokeResponseHandler("/test", batchUpdateResult, ResourceMethod.BATCH_PARTIAL_UPDATE, acceptTypeData.acceptHeaders);
    checkResponse(response, 200, 3, acceptTypeData.responseContentType, BatchResponse.class.getName(), UpdateStatus.class.getName(), true);

    response = invokeResponseHandler("/test", batchUpdateResult, ResourceMethod.BATCH_DELETE, acceptTypeData.acceptHeaders);
    checkResponse(response, 200, 3, acceptTypeData.responseContentType, BatchResponse.class.getName(), UpdateStatus.class.getName(), true);

    List<CreateResponse> createResponses = new ArrayList<CreateResponse>();
    createResponses.add(new CreateResponse("42", HttpStatus.S_204_NO_CONTENT));
    createResponses.add(new CreateResponse(HttpStatus.S_400_BAD_REQUEST));
    createResponses.add(new CreateResponse(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
    BatchCreateResult<Long, Status> batchCreateResult = new BatchCreateResult<Long, Status>(createResponses);

    response = invokeResponseHandler("/test", batchCreateResult, ResourceMethod.BATCH_CREATE, acceptTypeData.acceptHeaders); // here
    checkResponse(response, 200, 3, acceptTypeData.responseContentType, CollectionResponse.class.getName(), CreateStatus.class.getName(), true);
  }

  @Test(dataProvider = "basicData")
  public void testCollections(AcceptTypeData acceptTypeData) throws Exception
  {
    ResourceModel resourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = resourceModel.findNamedMethod("search");

    RestResponse response;
    // #1 check datamap/entity structure
    response = _responseHandler.buildResponse(buildRequest(acceptTypeData.acceptHeaders),
                                              new RoutingResult(new ResourceContextImpl(), methodDescriptor),
                                              buildStatusList(3));

    checkResponse(response, 200, 3, acceptTypeData.responseContentType, CollectionResponse.class.getName(), Status.class.getName(), true);

    String baseUri = "/test?someParam=foo";

    // #1.1 using CollectionResult
    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     new BasicCollectionResult<Status>(buildStatusList(5)),
                                     acceptTypeData.acceptHeaders);
    checkCollectionResponse(response, 5, 0, 5, 1, null, null, null, acceptTypeData);

    // #1.1 using CollectionResult (with total)
    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     new BasicCollectionResult<Status>(buildStatusList(5), 10),
                                     acceptTypeData.acceptHeaders);
    checkCollectionResponse(response, 5, 0, 5, 1, 10, null, null, acceptTypeData);

    // using CollectionResult with metadata RecordTemplate
    CollectionMetadata metadata = new CollectionMetadata();
    metadata.setCount(42);

    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     new CollectionResult<Status, CollectionMetadata>(buildStatusList(5), 10, metadata),
                                     acceptTypeData.acceptHeaders);
    checkCollectionResponse(response, 5, 0, 5, 1, 10, null, null, acceptTypeData);
    DataMap dataMap = acceptTypeData.dataCodec.readMap(response.getEntity().asInputStream());
    CollectionResponse<Status> collectionResponse = new CollectionResponse<Status>(dataMap, Status.class);
    assertEquals(new CollectionMetadata(collectionResponse.getMetadataRaw()), metadata);


    // #2 pagination: first page, no next
    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     buildStatusList(3),
                                     acceptTypeData.acceptHeaders);
    checkCollectionResponse(response, 3, 0, 5, 0, null, null, null, acceptTypeData);

    // #3 pagination: first page, has next (boundary case)
    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     buildStatusList(5),
                                     acceptTypeData.acceptHeaders);
    checkCollectionResponse(response, 5, 0, 5, 1, null, null, "/test?count=5&start=5&someParam=foo", acceptTypeData);

    // #4 pagination: second page, has prev/ext
    response = invokeResponseHandler(baseUri + "&start=5&count=5",
                                     methodDescriptor,
                                     buildStatusList(5),
                                     acceptTypeData.acceptHeaders);
    checkCollectionResponse(response, 5, 5, 5, 2, null, "/test?count=5&start=0&someParam=foo", "/test?count=5&start=10&someParam=foo", acceptTypeData);


    // #5 pagination:last page, has prev
    response = invokeResponseHandler(baseUri + "&start=10&count=5",
                                     methodDescriptor,
                                     buildStatusList(4),
                                     acceptTypeData.acceptHeaders);
    checkCollectionResponse(response, 4, 10, 5, 1, null, "/test?count=5&start=5&someParam=foo", null, acceptTypeData);

    response = invokeResponseHandler(baseUri + "&start=10&count=5",
                                     methodDescriptor,
                                     new BasicCollectionResult<Status>(buildStatusList(4), 15),
                                     acceptTypeData.acceptHeaders);
    checkCollectionResponse(response, 4, 10, 5, 2, 15, "/test?count=5&start=5&someParam=foo", "/test?count=5&start=14&someParam=foo", acceptTypeData);

    response = invokeResponseHandler(baseUri + "&start=10&count=5",
                                     methodDescriptor,
                                     new BasicCollectionResult<Status>(buildStatusList(4), 14),
                                     acceptTypeData.acceptHeaders);
    checkCollectionResponse(response, 4, 10, 5, 1, 14, "/test?count=5&start=5&someParam=foo", null, acceptTypeData);

  }

  private RestResponse invokeResponseHandler(String uri,
                                             ResourceMethodDescriptor methodDescriptor,
                                             Object result,
                                             Map<String, String> headers)
          throws IOException, URISyntaxException, RestLiSyntaxException
  {
    RestRequest request = buildRequest(uri, headers);
    RoutingResult routingResult = new RoutingResult(new ResourceContextImpl(new PathKeysImpl(), request,
                                                                            new RequestContext()), methodDescriptor);
    RestResponse response;
    response = _responseHandler.buildResponse(request,
                                              routingResult,
                                              result);
    return response;
  }

  @Test
  public void testMetadata() throws Exception
  {
    // TODO HIGH
  }

  @DataProvider(name="statusActionData")
  public Object[][] statusActionData()
  {
    return new Object[][]
      {
        {
          AcceptTypeData.EMPTY,
          EXPECTED_STATUS_ACTION_RESPONSE_JSON,
          doubleQuote("{'value':{'key2':'value2','key1':'value1'}}")
        },
        {
          AcceptTypeData.ANY,
          EXPECTED_STATUS_ACTION_RESPONSE_JSON,
          doubleQuote("{'value':{'key2':'value2','key1':'value1'}}")
        },
        {
          AcceptTypeData.JSON,
          EXPECTED_STATUS_ACTION_RESPONSE_JSON,
          doubleQuote("{'value':{'key2':'value2','key1':'value1'}}")
        },
        {
          AcceptTypeData.PSON,
          EXPECTED_STATUS_ACTION_RESPONSE_PSON,
          "#!PSON1\n!\u0081value\u0000!\u0083key2\u0000\n\u0007\u0000\u0000\u0000value2\u0000\u0085key1\u0000\n\u0007\u0000\u0000\u0000value1\u0000\u0080\u0080"
        }
      };
  }

  @Test(dataProvider = "statusActionData")
  public void testActions(AcceptTypeData acceptTypeData, String response1, String response2) throws Exception
  {
    RestResponse response;

    // #1 simple record template
    response = _responseHandler.buildResponse(buildRequest(acceptTypeData.acceptHeaders),
                                              buildRoutingResultAction(Status.class),
                                              buildStatusRecord());

    checkResponse(response, 200, 3, acceptTypeData.responseContentType, ActionResponse.class.getName(), Status.class.getName(), true);
    assertEquals(response.getEntity().asAvroString(), response1);

    // #2 DataTemplate response
    StringMap map = new StringMap();
    map.put("key1", "value1");
    map.put("key2", "value2");
    response = _responseHandler.buildResponse(buildRequest(acceptTypeData.acceptHeaders),
                                              buildRoutingResultAction(StringMap.class),
                                              map);

    checkResponse(response, 200, 3, acceptTypeData.responseContentType, ActionResponse.class.getName(), StringMap.class.getName(), true);
    String actual = response.getEntity().asAvroString();
    assertEquals(actual, response2);

    // #3 empty response
    response = _responseHandler.buildResponse(buildRequest(acceptTypeData.acceptHeaders),
                                              buildRoutingResultAction(Void.TYPE),
                                              null);

    checkResponse(response, 200, 0, null, null, null, false);
    assertEquals(response.getEntity().asAvroString(), "");
  }

  @Test(dataProvider = "basicData")
  void testRestErrors(AcceptTypeData acceptTypeData) throws Exception
  {
    RestResponse response;
    RestLiServiceException ex;

    // #1
    ex = new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "missing fields");
    response = _responseHandler.buildResponse(buildRequest(acceptTypeData.acceptHeaders),
                                              buildRoutingResult(),
                                              ex);

    checkResponse(response, 400, 3, acceptTypeData.responseContentType, ErrorResponse.class.getName(), null, true, true);
    DataMap dataMap = acceptTypeData.dataCodec.readMap(response.getEntity().asInputStream());

    assertEquals(dataMap.getInteger("status"), Integer.valueOf(400));
    assertEquals(dataMap.getString("message"), "missing fields");

    // #2
    ex = new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "missing fields").setServiceErrorCode(11);
    response = _responseHandler.buildResponse(buildRequest(acceptTypeData.acceptHeaders),
                                              buildRoutingResult(),
                                              ex);

    checkResponse(response, 400, 3, acceptTypeData.responseContentType, ErrorResponse.class.getName(), null, true, true);
    dataMap = acceptTypeData.dataCodec.readMap(response.getEntity().asInputStream());

    assertEquals(dataMap.getInteger("status"), Integer.valueOf(400));
    assertEquals(dataMap.getString("message"), "missing fields");
    assertEquals(dataMap.getInteger("serviceErrorCode"), Integer.valueOf(11));
  }

  @Test(dataProvider = "basicData")
  public void testFieldProjection_records(AcceptTypeData acceptTypeData)
          throws Exception
  {
    RestResponse response;

    // #1 all fields
    RestRequest request1 = buildRequest("/test?fields=f1,f2,f3", acceptTypeData.acceptHeaders);
    Status status = buildStatusWithFields("f1", "f2", "f3");
    response = _responseHandler.buildResponse(request1,
                                              buildRoutingResult(request1),
                                              status);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true);
    checkProjectedFields(response, new String[] {"f1", "f2", "f3"}, new String[0]);

    // #2 some fields
    RestRequest request2 = buildRequest("/test?fields=f1,f3", acceptTypeData.acceptHeaders);
    response = _responseHandler.buildResponse(request2,
                                              buildRoutingResult(request2),
                                              status);
    assertTrue(status.data().containsKey("f2"));

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true);
    checkProjectedFields(response, new String[] {"f1", "f3"}, new String[] {"f2"});

    // #3 no fields
    RestRequest request3 = buildRequest("/test?fields=", acceptTypeData.acceptHeaders);
    response = _responseHandler.buildResponse(request3,
                                              buildRoutingResult(request3),
                                              status);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true);
    checkProjectedFields(response, new String[]{}, new String[]{"f1", "f2", "f3"});
    assertTrue(status.data().containsKey("f1"));
    assertTrue(status.data().containsKey("f2"));
    assertTrue(status.data().containsKey("f3"));

    // #4 fields not in schema
    RestRequest request4 = buildRequest("/test?fields=f1,f99", acceptTypeData.acceptHeaders);
    response = _responseHandler.buildResponse(request4,
                                              buildRoutingResult(request4),
                                              status);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true);
    checkProjectedFields(response, new String[] {"f1"}, new String[] {"f2", "f3", "f99"});
    assertTrue(status.data().containsKey("f2"));
    assertTrue(status.data().containsKey("f3"));
  }

  @Test(dataProvider = "basicData")
  public void testFieldProjectionRecordsPALSyntax(AcceptTypeData acceptTypeData)
          throws Exception
  {
    RestResponse response;

    DataMap data = new DataMap(asMap(
            "f1", "value",
            "f2", new DataMap(asMap(
            "f3", "value",
            "f4", "value"
    ))));

    // #1 all fields
    Status status = new Status(data);

    RestRequest request1 = buildRequest("/test?fields=f1,f2:(f3,f4)", acceptTypeData.acceptHeaders);
    response = _responseHandler.buildResponse(request1,
                                              buildRoutingResult(request1),
                                              status);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true);
    checkProjectedFields(response, new String[] {"f1", "f2", "f3"}, new String[0]);

    // #2 some fields
    RestRequest request2 = buildRequest("/test?fields=f1,f2:(f3)", acceptTypeData.acceptHeaders);
    response = _responseHandler.buildResponse(request2,
                                              buildRoutingResult(request2),
                                              status);
    assertTrue(status.data().containsKey("f2"));

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true);
    checkProjectedFields(response, new String[] {"f1", "f2", "f3"}, new String[] {"f4"});

    // #3 no fields
    RestRequest request3 = buildRequest("/test?fields=", acceptTypeData.acceptHeaders);
    response = _responseHandler.buildResponse(request3,
                                              buildRoutingResult(request3),
                                              status);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true);
    checkProjectedFields(response, new String[]{}, new String[]{"f1", "f2", "f3", "f4"});
    assertTrue(status.data().containsKey("f1"));
    assertTrue(status.data().containsKey("f2"));

    // #4 fields not in schema
    RestRequest request4 = buildRequest("/test?fields=f2:(f99)", acceptTypeData.acceptHeaders);
    response = _responseHandler.buildResponse(request4,
                                              buildRoutingResult(request4),
                                              status);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true);
    checkProjectedFields(response, new String[] {"f2"}, new String[] {"f1", "f3", "f99"});
    assertTrue(status.data().containsKey("f2"));
  }

  @Test(dataProvider = "basicData")
  public void testFieldProjection_collections_CollectionResult(AcceptTypeData acceptTypeData)
          throws Exception
  {
    RestResponse response;

    BasicCollectionResult<Status> statusCollection = buildStatusCollectionResult(10, "f1", "f2",
                                                                                 "f3");
    RestRequest request = buildRequest("/test?fields=f1,f2", acceptTypeData.acceptHeaders);
    response = _responseHandler.buildResponse(request,
                                              buildRoutingResultFinder(request),
                                              statusCollection);

    checkResponse(response, 200, 3, acceptTypeData.responseContentType, CollectionResponse.class.getName(), null, true);

    DataMap dataMap = acceptTypeData.dataCodec.readMap(response.getEntity().asInputStream());
    CollectionResponse<Status> collectionResponse = new CollectionResponse<Status>(dataMap, Status.class);
    assertEquals(collectionResponse.getElements().size(), 10);
    for (Status status : collectionResponse.getElements())
    {
      assertTrue(status.data().containsKey("f1"));
      assertTrue(status.data().containsKey("f2"));
      assertFalse(status.data().containsKey("f3"));
    }

    // ensure that output status objects were not modified by rest.li!
    Status status1 = statusCollection.getElements().get(1);
    assertNotNull(status1);
    assertTrue(status1.data().containsKey("f3"));
  }

  @Test(dataProvider = "basicData")
  public void testFieldProjection_collections_List(AcceptTypeData acceptTypeData) throws Exception
  {
    RestResponse response;

    List<Status> statusCollection = buildStatusListResult(10, "f1", "f2", "f3");
    RestRequest request = buildRequest("/test?fields=f1,f2", acceptTypeData.acceptHeaders);
    response = _responseHandler.buildResponse(request,
                                              buildRoutingResultFinder(request),
                                              statusCollection);

    checkResponse(response, 200, 3, acceptTypeData.responseContentType, CollectionResponse.class.getName(), null, true);

    DataMap dataMap = acceptTypeData.dataCodec.readMap(response.getEntity().asInputStream());
    CollectionResponse<Status> collectionResponse = new CollectionResponse<Status>(dataMap, Status.class);
    assertEquals(collectionResponse.getElements().size(), 10);
    for (Status status : collectionResponse.getElements())
    {
      assertTrue(status.data().containsKey("f1"));
      assertTrue(status.data().containsKey("f2"));
      assertFalse(status.data().containsKey("f3"));
    }

    // ensure that output status objects were not modified by rest.li!
    Status status1 = statusCollection.get(1);
    assertNotNull(status1);
    assertTrue(status1.data().containsKey("f3"));
  }

  @Test(dataProvider = "basicData")
  public void testFieldProjection_batch(AcceptTypeData acceptTypeData) throws Exception
  {
    RestResponse response;

    Map<Integer, Status> statusBatch = buildStatusBatchResponse(10, "f1", "f2", "f3");
    RestRequest request = buildRequest("/test?ids=1,2,3&fields=f1,f2", acceptTypeData.acceptHeaders);
    response = _responseHandler.buildResponse(request,
                                              buildRoutingResult(ResourceMethod.BATCH_GET, request),
                                              statusBatch);

    checkResponse(response, 200, 3, acceptTypeData.responseContentType, BatchResponse.class.getName(), Status.class.getName(), true);

    DataMap dataMap = acceptTypeData.dataCodec.readMap(response.getEntity().asInputStream());
    BatchResponse<Status> batchResponse = new BatchResponse<Status>(dataMap, Status.class);
    assertEquals(batchResponse.getResults().size(), 10);
    for (Status status : batchResponse.getResults().values())
    {
      assertTrue(status.data().containsKey("f1"));
      assertTrue(status.data().containsKey("f2"));
      assertFalse(status.data().containsKey("f3"));
    }

    // ensure that output map was not modified by rest.li!
    Status status1 = statusBatch.get(1);
    assertNotNull(status1);
    assertTrue(status1.data().containsKey("f3"));
  }

  @Test(dataProvider = "basicData")
  public void testApplicationSpecifiedHeaders(AcceptTypeData acceptTypeData) throws Exception
  {
    String testHeaderName = "X-LI-TEST-HEADER";
    String testHeaderValue = "test";

    ResourceModel resourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = resourceModel.findNamedMethod("search");
    ResourceContextImpl context = new ResourceContextImpl();
    context.setResponseHeader(testHeaderName, testHeaderValue);
    RoutingResult routingResult = new RoutingResult(context, methodDescriptor);

    RestResponse response;
    response = _responseHandler.buildResponse(buildRequest(acceptTypeData.acceptHeaders),
                                              routingResult,
                                              buildStatusList(3));

    Assert.assertEquals(response.getHeader(testHeaderName), testHeaderValue);
  }

  @DataProvider(name="statusesData")
  public Object[][] statusesData()
  {
    return new Object[][]
      {
        { AcceptTypeData.EMPTY, EXPECTED_STATUS_JSON, EXPECTED_STATUS_ACTION_RESPONSE_JSON },
        { AcceptTypeData.ANY, EXPECTED_STATUS_JSON, EXPECTED_STATUS_ACTION_RESPONSE_JSON },
        { AcceptTypeData.JSON, EXPECTED_STATUS_JSON, EXPECTED_STATUS_ACTION_RESPONSE_JSON },
        { AcceptTypeData.PSON, EXPECTED_STATUS_PSON, EXPECTED_STATUS_ACTION_RESPONSE_PSON }
      };
  }

  @Test(dataProvider = "statusesData")
  public void testWrapperResults(AcceptTypeData acceptTypeData, String expectedStatus, String expectedActionStatus) throws Exception
  {
    RestResponse response;
    final Status status = buildStatusRecord();

    final GetResult<Status> getResult = new GetResult<Status>(status, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    response = invokeResponseHandler("/test", getResult, ResourceMethod.GET, acceptTypeData.acceptHeaders);
    checkResponse(response, HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode(), 2, acceptTypeData.responseContentType, Status.class.getName(), null, true);
    assertEquals(response.getEntity().asAvroString(), expectedStatus);

    final ActionResult<Status> actionResult = new ActionResult<Status>(status, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    response = _responseHandler.buildResponse(buildRequest(acceptTypeData.acceptHeaders),
                                              buildRoutingResultAction(Status.class),
                                              actionResult);
    checkResponse(response,
                  HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode(),
                  3,
                  acceptTypeData.responseContentType,
                  ActionResponse.class.getName(),
                  Status.class.getName(),
                  true);
    assertEquals(response.getEntity().asAvroString(), expectedActionStatus);
  }

  // *****************
  // Helper methods
  // *****************

  private final RestRequest buildRequest(Map<String, String> headers) throws URISyntaxException
  {
    return buildRequest("/test", headers);
  }

  private final RestRequest buildRequest(String uri, Map<String, String> headers) throws URISyntaxException
  {
    return new RestRequestBuilder(new URI(uri)).setMethod("DONT_CARE").setHeaders(headers).build();
  }

  private final RoutingResult buildRoutingResult()
          throws SecurityException, NoSuchMethodException, RestLiSyntaxException
  {
    return buildRoutingResult(null);
  }

  /**
   * Creates a RoutingResult for an Action with the given returnType.
   *
   * @param actionReturnType the return type of the action.
   * @return a RoutingResult
   */
  private final RoutingResult buildRoutingResultAction(Class<?> actionReturnType)
          throws NoSuchMethodException, RestLiSyntaxException
  {

    if (actionReturnType == Void.class)
    {
      actionReturnType = Void.TYPE;
    }

    // actual method passed in is irrelevant, since we are constructing a ResourceMethodDescriptor by hand.
    Method method = ProjectionTestFixture.class.getMethod("batchGet", Set.class);

    ResourceModel model = RestLiTestHelper.buildResourceModel(StatusCollectionResource.class);

    String actionName = "return" + actionReturnType.getSimpleName();
    List<Parameter<?>> parameters = Collections.<Parameter<?>>emptyList();

    RecordDataSchema actionReturnRecordDataSchema;
    FieldDef<?> returnFieldDef;
    if (actionReturnType != Void.TYPE)
    {
      @SuppressWarnings({"unchecked","rawtypes"})
      FieldDef<?> nonVoidFieldDef = new FieldDef(ActionResponse.VALUE_NAME,
                                 actionReturnType,
                                 DataTemplateUtil.getSchema(actionReturnType));
      returnFieldDef = nonVoidFieldDef;
      actionReturnRecordDataSchema = DynamicRecordMetadata.buildSchema(actionName,
                                                                       Collections.singleton(returnFieldDef));
    }
    else
    {
      returnFieldDef = null;
      actionReturnRecordDataSchema = DynamicRecordMetadata.buildSchema(actionName, Collections.<FieldDef<?>>emptyList());
    }

    ResourceMethodDescriptor methodDescriptor =
            ResourceMethodDescriptor.createForAction(method,
                                                     parameters,
                                                     actionName,
                                                     ResourceLevel.COLLECTION,
                                                     returnFieldDef,
                                                     actionReturnRecordDataSchema,
                                                     DynamicRecordMetadata.buildSchema(actionName, parameters),
                                                     InterfaceType.SYNC,
                                                     new DataMap());

    model.addResourceMethodDescriptor(methodDescriptor);

    return new RoutingResult(new ResourceContextImpl(new PathKeysImpl(), null, new RequestContext()),
                             methodDescriptor);
  }

  private final RoutingResult buildRoutingResult(RestRequest request)
          throws SecurityException, NoSuchMethodException, RestLiSyntaxException
  {
    return buildRoutingResult(ResourceMethod.GET, request);
  }

  private final RoutingResult buildRoutingResult(ResourceMethod resourceMethod, RestRequest request)
          throws SecurityException, NoSuchMethodException, RestLiSyntaxException
  {
    Method method = ProjectionTestFixture.class.getMethod("batchGet", Set.class);
    ResourceModel model = RestLiTestHelper.buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor =
        ResourceMethodDescriptor.createForRestful(resourceMethod, method, InterfaceType.SYNC);
    model.addResourceMethodDescriptor(methodDescriptor);

    return new RoutingResult(new ResourceContextImpl(new PathKeysImpl(), request,
                                                     new RequestContext()), methodDescriptor);
  }


  private final RoutingResult buildRoutingResultFinder(RestRequest request)
          throws SecurityException, NoSuchMethodException, RestLiSyntaxException
  {
    Method method = ProjectionTestFixture.class.getMethod("find");
    ResourceModel model = RestLiTestHelper.buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor =
        ResourceMethodDescriptor.createForRestful(ResourceMethod.FINDER, method, InterfaceType.SYNC);
    model.addResourceMethodDescriptor(methodDescriptor);

    return new RoutingResult(new ResourceContextImpl(new PathKeysImpl(), request,
                                                     new RequestContext()), methodDescriptor);
  }

  @RestLiCollection(name="test")
  private static class ProjectionTestFixture extends CollectionResourceTemplate<Integer, Status>
  {
    public Map<Integer, Status> batchGet(Set<Integer> ids)
    {
      return null;
    }

    public BasicCollectionResult<Status> find()
    {
      return null;
    }
  }

  private void checkResponse(RestResponse response,
                             int status,
                             int numHeaders,
                             String contentType,
                             String type,
                             String subType,
                             boolean hasEntity)
  {
    checkResponse(response, status, numHeaders, contentType, type, subType, false, hasEntity);
  }

  private void checkResponse(RestResponse response,
                             int status,
                             int numHeaders,
                             String contentType,
                             String type,
                             String subType,
                             boolean hasError,
                             boolean hasEntity)
  {
    assertEquals(response.getStatus(), status);
    assertEquals(response.getHeaders().size(), numHeaders);
    assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), contentType);
    assertEquals(response.getHeader(RestConstants.HEADER_RESTLI_TYPE), type);
    if (subType != null)
    {
      assertEquals(response.getHeader(RestConstants.HEADER_RESTLI_SUB_TYPE), subType);
    }

    if (hasError)
    {
      assertEquals(response.getHeader(RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE), RestConstants.HEADER_VALUE_ERROR_APPLICATION);
    }
    else
    {
      assertNull(response.getHeader(RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE));
    }

    assertEquals(response.getEntity().length() > 0, hasEntity);
  }

  private List<Status> buildStatusList(int num)
  {
    List<Status> list = new ArrayList<Status>();
    for (int i = 0; i < num; i++)
    {
      list.add(buildStatusRecord());
    }

    return list;
  }

  private Status buildStatusRecord()
  {
    DataMap map = new DataMap();
    map.put("text", "test status");
    Status status = new Status(map);
    return status;
  }

  private Status buildStatusWithFields(String... fields)
  {
    DataMap map = new DataMap();
    for (String field : fields)
    {
      map.put(field, "value");
    }
    Status status = new Status(map);
    return status;
  }

  private BasicCollectionResult<Status> buildStatusCollectionResult(int numResults,
                                                                    String... fields)
  {

    List<Status> data = buildStatusListResult(numResults, fields);

    return new BasicCollectionResult<Status>(data, numResults);
  }

  private List<Status> buildStatusListResult(int numResults, String... fields)
  {
    List<Status> data = new ArrayList<Status>();

    for (int i = 0; i < numResults; i++)
    {
      Status status = buildStatusWithFields(fields);
      data.add(status);
    }
    return data;
  }


  private Map<Integer, Status> buildStatusBatchResponse(int numResults, String... fields)
  {
    Map<Integer, Status> map = new HashMap<Integer, Status>();

    for (int i = 0; i < numResults; i++)
    {
      Status status = buildStatusWithFields(fields);
      map.put(i, status);
    }

    return map;
  }

  private void checkCollectionResponse(RestResponse response,
                                       int numElements,
                                       int start,
                                       int count,
                                       int numLinks,
                                       Integer total,
                                       String prevLink,
                                       String nextLink,
                                       AcceptTypeData acceptTypeData)
          throws Exception
  {
    DataMap dataMap = acceptTypeData.dataCodec.readMap(response.getEntity().asInputStream());
    CollectionResponse<Status> collectionResponse = new CollectionResponse<Status>(dataMap, Status.class);

    assertEquals(collectionResponse.getElements().size(), numElements);
    assertEquals(collectionResponse.getPaging().getStart(), start);
    assertEquals(collectionResponse.getPaging().getCount(), count);
    assertEquals(collectionResponse.getPaging().getLinks().size(), numLinks);
    if (total == null)
    {
      assertFalse(collectionResponse.getPaging().hasTotal());
    }
    else
    {
      assertEquals(collectionResponse.getPaging().getTotal(), total.intValue());
    }
    if (prevLink != null)
    {
      checkLink(collectionResponse.getPaging().getLinks().get(0), "prev", prevLink, acceptTypeData.responseContentType);
    }
    if (nextLink != null)
    {
      int idx = prevLink != null ? 1 : 0;
      checkLink(collectionResponse.getPaging().getLinks().get(idx), "next", nextLink, acceptTypeData.responseContentType);
    }
  }

  private static void checkLink(Link link, String rel, String uri, String type)
  {
    assertEquals(link.getRel(), rel);
    assertFalse(link.hasTitle());
    assertEquals(link.getType(), type);
    assertEquals(link.getHref(), uri);
  }

  private static void checkProjectedFields(RestResponse response, String[] expectedFields, String[] missingFields)
          throws UnsupportedEncodingException
  {
    DataMap dataMap = DataMapUtils.readMap(response);

    for (String field : expectedFields)
    {
      assertTrue(DataMapContains(dataMap, field));
    }
    for (String field : missingFields)
    {
      assertFalse(DataMapContains(dataMap, field));
    }
  }

  private static boolean DataMapContains(DataMap data, String field)
  {
    for(String key : data.keySet())
    {
      if (key.equals(field))
        return true;

      Object value = data.get(key);
      if (value instanceof DataMap)
        return DataMapContains((DataMap)value, field);
    }
    return false;

  }

  @SuppressWarnings("unchecked")
  static public <V> Map<String, V> asMap(Object... objects)
  {
    int index = 0;
    String key = null;
    HashMap<String,V> map = new HashMap<String,V>();
    for (Object object : objects)
    {
      if (index % 2 == 0)
      {
        key = (String) object;
      }
      else
      {
        map.put(key, (V) object);
      }
      index++;
    }
    return map;
  }

}
