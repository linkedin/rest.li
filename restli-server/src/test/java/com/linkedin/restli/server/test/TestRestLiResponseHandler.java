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


import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.DataCodec;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.codec.PsonDataCodec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.StringMap;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.RestLiResponseHandler;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.methods.response.PartialRestResponse;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor.InterfaceType;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.internal.testutils.URIDetails;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.BasicCollectionResult;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.GetResult;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.twitter.StatusCollectionResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;

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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.restli.server.test.RestLiTestHelper.buildResourceModel;
import static com.linkedin.restli.server.test.RestLiTestHelper.doubleQuote;
import static org.testng.Assert.*;


/**
 * @author dellamag
 */
public class TestRestLiResponseHandler
{
  private final RestLiResponseHandler _responseHandler = new RestLiResponseHandler.Builder().build();

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
  private static final String EXPECTED_STATUS_ACTION_RESPONSE_STRING = "{value={text=test status}}";
  private static final String EXPECTED_STATUS_PSON = "#!PSON1\n!\u0081text\u0000\n\f\u0000\u0000\u0000test status\u0000\u0080";
  private static final String EXPECTED_STATUS_ACTION_RESPONSE_PSON = "#!PSON1\n!\u0081value\u0000!\u0083text\u0000\n\f\u0000\u0000\u0000test status\u0000\u0080\u0080";

  private RestResponse invokeResponseHandler(String path,
                                             Object body,
                                             ResourceMethod method,
                                             Map<String, String> headers,
                                             ProtocolVersion protocolVersion) throws Exception
  {
    RestRequest req = buildRequest(path, headers, protocolVersion);
    RoutingResult routing = buildRoutingResult(method, req, headers);
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
      invokeResponseHandler("/test", buildStatusRecord(), ResourceMethod.GET,
                                                    badAcceptHeaders, AllProtocolVersions.LATEST_PROTOCOL_VERSION);
      Assert.fail();
    }
    catch (RestLiServiceException e)
    {
      Assert.assertEquals(e.getStatus().getCode(), 406);
    }
    // check response without body (expect 406 error)
    try
    {
      invokeResponseHandler("/test", new CreateResponse(HttpStatus.S_201_CREATED), ResourceMethod.CREATE,
                                                    badAcceptHeaders, AllProtocolVersions.LATEST_PROTOCOL_VERSION);
      Assert.fail();
    }
    catch (RestLiServiceException e)
    {
      Assert.assertEquals(e.getStatus().getCode(), 406);
    }
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusData")
  public Object[][] statusData()
  {
    return new Object[][]
      {
        { AcceptTypeData.EMPTY, EXPECTED_STATUS_JSON, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE, RestConstants.HEADER_ID },
        { AcceptTypeData.EMPTY, EXPECTED_STATUS_JSON, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), RestConstants.HEADER_RESTLI_ERROR_RESPONSE, RestConstants.HEADER_RESTLI_ID },
        { AcceptTypeData.ANY, EXPECTED_STATUS_JSON, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE, RestConstants.HEADER_ID },
        { AcceptTypeData.ANY, EXPECTED_STATUS_JSON, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), RestConstants.HEADER_RESTLI_ERROR_RESPONSE, RestConstants.HEADER_RESTLI_ID },
        { AcceptTypeData.JSON, EXPECTED_STATUS_JSON, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE, RestConstants.HEADER_ID },
        { AcceptTypeData.JSON, EXPECTED_STATUS_JSON, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), RestConstants.HEADER_RESTLI_ERROR_RESPONSE, RestConstants.HEADER_RESTLI_ID },
        { AcceptTypeData.PSON, EXPECTED_STATUS_PSON, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE, RestConstants.HEADER_ID },
        { AcceptTypeData.PSON, EXPECTED_STATUS_PSON, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), RestConstants.HEADER_RESTLI_ERROR_RESPONSE, RestConstants.HEADER_RESTLI_ID }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusData")
  public void testBasicResponses(AcceptTypeData acceptTypeData,
                                 String expectedStatus,
                                 ProtocolVersion protocolVersion,
                                 String errorResponseHeaderName,
                                 String idHeaderName) throws Exception
  {
    RestResponse response;
    // #1 simple record template
    response = invokeResponseHandler("/test", buildStatusRecord(), ResourceMethod.GET, acceptTypeData.acceptHeaders, protocolVersion);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true, errorResponseHeaderName);
    assertEquals(response.getEntity().asAvroString(), expectedStatus);

    // #2 create (with id)
    response = invokeResponseHandler("/test", new CreateResponse(1), ResourceMethod.CREATE, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response, 201, 3, null, null, null, false, errorResponseHeaderName);
    assertEquals(response.getHeader(RestConstants.HEADER_LOCATION), "/test/1");
    assertEquals(response.getHeader(idHeaderName), "1");

    // #2.1 create (without id)
    response = invokeResponseHandler("/test", new CreateResponse(HttpStatus.S_201_CREATED),
                                     ResourceMethod.CREATE, acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkResponse(response, 201, 1, null, null, null, false, errorResponseHeaderName);

    // #2.2 create (with id and slash at the end of uri)
    response = invokeResponseHandler("/test/", new CreateResponse(1), ResourceMethod.CREATE, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response, 201, 3, null, null, null, false, errorResponseHeaderName);
    assertEquals(response.getHeader(RestConstants.HEADER_LOCATION), "/test/1");
    assertEquals(response.getHeader(idHeaderName), "1");

    // #2.3 create (without id and slash at the end of uri)
    response = invokeResponseHandler("/test/", new CreateResponse(HttpStatus.S_201_CREATED),
                                     ResourceMethod.CREATE, acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkResponse(response, 201, 1, null, null, null, false, errorResponseHeaderName);

    // #3 update
    response = invokeResponseHandler("/test", new UpdateResponse(HttpStatus.S_204_NO_CONTENT),
                                     ResourceMethod.UPDATE, acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkResponse(response, 204, 1, null, null, null, false, errorResponseHeaderName);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  public Object[][] basicData()
  {
    return new Object[][]
      {
        { AcceptTypeData.EMPTY, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE },
        { AcceptTypeData.EMPTY, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), RestConstants.HEADER_RESTLI_ERROR_RESPONSE },
        { AcceptTypeData.ANY, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE },
        { AcceptTypeData.ANY, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), RestConstants.HEADER_RESTLI_ERROR_RESPONSE },
        { AcceptTypeData.JSON, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE },
        { AcceptTypeData.JSON, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), RestConstants.HEADER_RESTLI_ERROR_RESPONSE },
        { AcceptTypeData.PSON, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE },
        { AcceptTypeData.PSON, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), RestConstants.HEADER_RESTLI_ERROR_RESPONSE }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  public void testBatchResponses(AcceptTypeData acceptTypeData,
                                 ProtocolVersion protocolVersion,
                                 String errorResponseHeaderName) throws Exception
  {
    RestResponse response;

    // #4 batch
    Map<Long, Status> map = new HashMap<Long, Status>();
    map.put(1L, buildStatusRecord());
    map.put(2L, buildStatusRecord());
    map.put(3L, buildStatusRecord());
    response = invokeResponseHandler("/test", map, ResourceMethod.BATCH_GET, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response, 200, 2, acceptTypeData.responseContentType, BatchResponse.class.getName(), Status.class.getName(), true, errorResponseHeaderName);

    Map<Long, UpdateResponse> updateStatusMap = new HashMap<Long, UpdateResponse>();
    updateStatusMap.put(1L, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    updateStatusMap.put(2L, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    updateStatusMap.put(3L, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    BatchUpdateResult<Long, Status> batchUpdateResult = new BatchUpdateResult<Long, Status>(updateStatusMap);
    response = invokeResponseHandler("/test", batchUpdateResult, ResourceMethod.BATCH_UPDATE, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response, 200, 2, acceptTypeData.responseContentType, BatchResponse.class.getName(), UpdateStatus.class.getName(), true, errorResponseHeaderName);

    response = invokeResponseHandler("/test", batchUpdateResult, ResourceMethod.BATCH_PARTIAL_UPDATE, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response, 200, 2, acceptTypeData.responseContentType, BatchResponse.class.getName(), UpdateStatus.class.getName(), true, errorResponseHeaderName);

    response = invokeResponseHandler("/test", batchUpdateResult, ResourceMethod.BATCH_DELETE, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response, 200, 2, acceptTypeData.responseContentType, BatchResponse.class.getName(), UpdateStatus.class.getName(), true, errorResponseHeaderName);

    List<CreateResponse> createResponses = new ArrayList<CreateResponse>();
    createResponses.add(new CreateResponse("42", HttpStatus.S_204_NO_CONTENT));
    createResponses.add(new CreateResponse(HttpStatus.S_400_BAD_REQUEST));
    createResponses.add(new CreateResponse(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
    BatchCreateResult<Long, Status> batchCreateResult = new BatchCreateResult<Long, Status>(createResponses);

    response = invokeResponseHandler("/test", batchCreateResult, ResourceMethod.BATCH_CREATE, acceptTypeData.acceptHeaders, protocolVersion); // here
    checkResponse(response, 200, 2, acceptTypeData.responseContentType, CollectionResponse.class.getName(), CreateStatus.class.getName(), true, errorResponseHeaderName);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  public void testCollections(AcceptTypeData acceptTypeData,
                              ProtocolVersion protocolVersion,
                              String errorResponseHeaderName) throws Exception
  {
    ResourceModel resourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = resourceModel.findNamedMethod("search");

    RestResponse response;
    // #1 check datamap/entity structure

    ServerResourceContext context = new ResourceContextImpl();
    RestUtils.validateRequestHeadersAndUpdateResourceContext(acceptTypeData.acceptHeaders, context);
    response = _responseHandler.buildResponse(buildRequest(acceptTypeData.acceptHeaders, protocolVersion),
                                              new RoutingResult(context, methodDescriptor),
                                              buildStatusList(3));
    checkResponse(response, 200, 2, acceptTypeData.responseContentType, CollectionResponse.class.getName(), Status.class.getName(), true, errorResponseHeaderName);

    String baseUri = "/test?someParam=foo";

    // #1.1 using CollectionResult
    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     new BasicCollectionResult<Status>(buildStatusList(5)),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkCollectionResponse(response, 5, 0, 5, 1, null, null, null, acceptTypeData);

    // #1.1 using CollectionResult (with total)
    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     new BasicCollectionResult<Status>(buildStatusList(5), 10),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkCollectionResponse(response, 5, 0, 5, 1, 10, null, null, acceptTypeData);

    // using CollectionResult with metadata RecordTemplate
    CollectionMetadata metadata = new CollectionMetadata();
    metadata.setCount(42);

    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     new CollectionResult<Status, CollectionMetadata>(buildStatusList(5), 10, metadata),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkCollectionResponse(response, 5, 0, 5, 1, 10, null, null, acceptTypeData);
    DataMap dataMap = acceptTypeData.dataCodec.readMap(response.getEntity().asInputStream());
    CollectionResponse<Status> collectionResponse = new CollectionResponse<Status>(dataMap, Status.class);
    assertEquals(new CollectionMetadata(collectionResponse.getMetadataRaw()), metadata);


    // #2 pagination: first page, no next
    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     buildStatusList(3),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkCollectionResponse(response, 3, 0, 5, 0, null, null, null, acceptTypeData);

    // #3 pagination: first page, has next (boundary case)
    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     buildStatusList(5),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    //"/test?count=5&start=5&someParam=foo"
    final Map<String, String> queryParamsMap3next = new HashMap<String, String>();
    queryParamsMap3next.put("count", "5");
    queryParamsMap3next.put("start", "5");
    queryParamsMap3next.put("someParam", "foo");
    final URIDetails expectedURIDetails3next = new URIDetails(protocolVersion, "/test", null, queryParamsMap3next, null);
    checkCollectionResponse(response, 5, 0, 5, 1, null, null, expectedURIDetails3next, acceptTypeData);

    // #4 pagination: second page, has prev/ext
    response = invokeResponseHandler(baseUri + "&start=5&count=5",
                                     methodDescriptor,
                                     buildStatusList(5),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    //"/test?count=5&start=0&someParam=foo", "/test?count=5&start=10&someParam=foo",
    final Map<String, String> queryParamsMap4prev = new HashMap<String, String>();
    queryParamsMap4prev.put("count", "5");
    queryParamsMap4prev.put("start", "0");
    queryParamsMap4prev.put("someParam", "foo");
    final URIDetails expectedURIDetails4prev = new URIDetails(protocolVersion, "/test", null, queryParamsMap4prev, null);
    final Map<String, String> queryParamsMap4next = new HashMap<String, String>();
    queryParamsMap4next.put("count", "5");
    queryParamsMap4next.put("start", "10");
    queryParamsMap4next.put("someParam", "foo");
    final URIDetails expectedURIDetails4next = new URIDetails(protocolVersion, "/test", null, queryParamsMap4next, null);
    checkCollectionResponse(response, 5, 5, 5, 2, null, expectedURIDetails4prev, expectedURIDetails4next, acceptTypeData);

    // #5 pagination:last page, has prev
    response = invokeResponseHandler(baseUri + "&start=10&count=5",
                                     methodDescriptor,
                                     buildStatusList(4),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    //"/test?count=5&start=5&someParam=foo"
    final Map<String, String> queryParamsMap5prev = new HashMap<String, String>();
    queryParamsMap5prev.put("count", "5");
    queryParamsMap5prev.put("start", "5");
    queryParamsMap5prev.put("someParam", "foo");
    final URIDetails expectedURIDetails5prev = new URIDetails(protocolVersion, "/test", null, queryParamsMap5prev, null);
    checkCollectionResponse(response, 4, 10, 5, 1, null, expectedURIDetails5prev, null, acceptTypeData);

    response = invokeResponseHandler(baseUri + "&start=10&count=5",
                                     methodDescriptor,
                                     new BasicCollectionResult<Status>(buildStatusList(4), 15),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    //"/test?count=5&start=5&someParam=foo", "/test?count=5&start=14&someParam=foo"
    final Map<String, String> queryParamsMap6prev = new HashMap<String, String>();
    queryParamsMap6prev.put("count", "5");
    queryParamsMap6prev.put("start", "5");
    queryParamsMap6prev.put("someParam", "foo");
    final URIDetails expectedURIDetails6prev = new URIDetails(protocolVersion, "/test", null, queryParamsMap6prev, null);
    final Map<String, String> queryParamsMap6next = new HashMap<String, String>();
    queryParamsMap6next.put("count", "5");
    queryParamsMap6next.put("start", "14");
    queryParamsMap6next.put("someParam", "foo");
    final URIDetails expectedURIDetails6next = new URIDetails(protocolVersion, "/test", null, queryParamsMap6next, null);
    checkCollectionResponse(response, 4, 10, 5, 2, 15, expectedURIDetails6prev, expectedURIDetails6next, acceptTypeData);

    response = invokeResponseHandler(baseUri + "&start=10&count=5",
                                     methodDescriptor,
                                     new BasicCollectionResult<Status>(buildStatusList(4), 14),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    //"/test?count=5&start=5&someParam=foo"
    final Map<String, String> queryParamsMap7prev = new HashMap<String, String>();
    queryParamsMap7prev.put("count", "5");
    queryParamsMap7prev.put("start", "5");
    queryParamsMap7prev.put("someParam", "foo");
    final URIDetails expectedURIDetails7prev = new URIDetails(protocolVersion, "/test", null, queryParamsMap7prev, null);
    checkCollectionResponse(response, 4, 10, 5, 1, 14, expectedURIDetails7prev, null, acceptTypeData);
  }

  private RestResponse invokeResponseHandler(String uri,
                                             ResourceMethodDescriptor methodDescriptor,
                                             Object result,
                                             Map<String, String> headers,
                                             ProtocolVersion protocolVersion)
          throws IOException, URISyntaxException, RestLiSyntaxException
  {
    RestRequest request = buildRequest(uri, headers, protocolVersion);
    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    RestUtils.validateRequestHeadersAndUpdateResourceContext(headers, context);
    RoutingResult routingResult = new RoutingResult(context, methodDescriptor);
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

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusActionData")
  public Object[][] statusActionData()
  {
    return new Object[][]
      {
        {
          AcceptTypeData.EMPTY,
          EXPECTED_STATUS_ACTION_RESPONSE_JSON,
          doubleQuote("{'value':{'key2':'value2','key1':'value1'}}"),
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE
        },
        {
            AcceptTypeData.EMPTY,
            EXPECTED_STATUS_ACTION_RESPONSE_JSON,
            doubleQuote("{'value':{'key2':'value2','key1':'value1'}}"),
            AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
            RestConstants.HEADER_RESTLI_ERROR_RESPONSE
        },
        {
          AcceptTypeData.ANY,
          EXPECTED_STATUS_ACTION_RESPONSE_JSON,
          doubleQuote("{'value':{'key2':'value2','key1':'value1'}}"),
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE
        },
        {
            AcceptTypeData.ANY,
            EXPECTED_STATUS_ACTION_RESPONSE_JSON,
            doubleQuote("{'value':{'key2':'value2','key1':'value1'}}"),
            AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
            RestConstants.HEADER_RESTLI_ERROR_RESPONSE
        },
        {
          AcceptTypeData.JSON,
          EXPECTED_STATUS_ACTION_RESPONSE_JSON,
          doubleQuote("{'value':{'key2':'value2','key1':'value1'}}"),
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE
        },
        {
            AcceptTypeData.JSON,
            EXPECTED_STATUS_ACTION_RESPONSE_JSON,
            doubleQuote("{'value':{'key2':'value2','key1':'value1'}}"),
            AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
            RestConstants.HEADER_RESTLI_ERROR_RESPONSE
        },
        {
          AcceptTypeData.PSON,
          EXPECTED_STATUS_ACTION_RESPONSE_PSON,
          "#!PSON1\n!\u0081value\u0000!\u0083key2\u0000\n\u0007\u0000\u0000\u0000value2\u0000\u0085key1\u0000\n\u0007\u0000\u0000\u0000value1\u0000\u0080\u0080",
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE
        },
        {
            AcceptTypeData.PSON,
            EXPECTED_STATUS_ACTION_RESPONSE_PSON,
            "#!PSON1\n!\u0081value\u0000!\u0083key2\u0000\n\u0007\u0000\u0000\u0000value2\u0000\u0085key1\u0000\n\u0007\u0000\u0000\u0000value1\u0000\u0080\u0080",
            AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
            RestConstants.HEADER_RESTLI_ERROR_RESPONSE
        }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusActionData")
  public void testActions(AcceptTypeData acceptTypeData, String response1, String response2,
                          ProtocolVersion protocolVersion,
                          String errorResponseHeaderName) throws Exception
  {
    final RestRequest request = buildRequest(acceptTypeData.acceptHeaders, protocolVersion);
    RestResponse response;

    // #1 simple record template
    response = _responseHandler.buildResponse(request,
                                              buildRoutingResultAction(Status.class, request, acceptTypeData.acceptHeaders),
                                              buildStatusRecord());

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, ActionResponse.class.getName(), Status.class.getName(), true, errorResponseHeaderName);
    assertEquals(response.getEntity().asAvroString(), response1);

    // #2 DataTemplate response
    StringMap map = new StringMap();
    map.put("key1", "value1");
    map.put("key2", "value2");
    response = _responseHandler.buildResponse(request,
                                              buildRoutingResultAction(StringMap.class, request, acceptTypeData.acceptHeaders),
                                              map);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, ActionResponse.class.getName(), StringMap.class.getName(), true, errorResponseHeaderName);

    //Convert both of these back into maps depending on their response type
    final DataMap actualMap;
    final DataMap expectedMap;
    if (acceptTypeData == AcceptTypeData.PSON)
    {
      actualMap = PSON_DATA_CODEC.bytesToMap(response.getEntity().copyBytes());
      expectedMap = PSON_DATA_CODEC.bytesToMap(ByteString.copyAvroString(response2, false).copyBytes());
    }
    else
    {
      actualMap = JACKSON_DATA_CODEC.bytesToMap(response.getEntity().copyBytes());
      expectedMap = JACKSON_DATA_CODEC.stringToMap(response2);
    }
    assertEquals(actualMap, expectedMap);

    // #3 empty response
    response = _responseHandler.buildResponse(request,
                                              buildRoutingResultAction(Void.TYPE, request, acceptTypeData.acceptHeaders),
                                              null);

    checkResponse(response, 200, 1, null, null, null, false, errorResponseHeaderName);
    assertEquals(response.getEntity().asAvroString(), "");
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusActionDataPartial")
  public Object[][] statusActionDataPartial()
  {
    return new Object[][]
      {
        {
          AcceptTypeData.ANY,
          EXPECTED_STATUS_ACTION_RESPONSE_STRING,
          doubleQuote("{'value':{'key2':'value2','key1':'value1'}}"),
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE
        },
        {
            AcceptTypeData.ANY,
            EXPECTED_STATUS_ACTION_RESPONSE_STRING,
            doubleQuote("{'value':{'key2':'value2','key1':'value1'}}"),
            AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
            RestConstants.HEADER_RESTLI_ERROR_RESPONSE
        }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusActionDataPartial")
  public void testPartialRestResponse(AcceptTypeData acceptTypeData,
                                      String response1,
                                      String response2,
                                      ProtocolVersion protocolVersion,
                                      String errorResponseHeaderName) throws Exception
  {
    final RestRequest request = buildRequest(acceptTypeData.acceptHeaders, protocolVersion);
    PartialRestResponse response;
    RoutingResult routingResult1 = buildRoutingResultAction(Status.class, request, acceptTypeData.acceptHeaders);
    // #1 simple record template
    response =
        _responseHandler.buildPartialResponse(routingResult1,
                                              _responseHandler.buildRestLiResponseData(request,
                                                                                       routingResult1,
                                                                                       buildStatusRecord()));
    checkResponse(response, HttpStatus.S_200_OK, 1, false, true, errorResponseHeaderName);
    assertEquals(response.getEntity().toString(), response1);
    // #2 DataTemplate response
    StringMap map = new StringMap();
    map.put("key1", "value1");
    map.put("key2", "value2");
    RoutingResult routingResult2 = buildRoutingResultAction(StringMap.class, request, acceptTypeData.acceptHeaders);
    response =
        _responseHandler.buildPartialResponse(routingResult2,
                                              _responseHandler.buildRestLiResponseData(request, routingResult2, map));
    checkResponse(response, HttpStatus.S_200_OK, 1, false, true, errorResponseHeaderName);

    //Obtain the maps necessary for comparison
    final DataMap actualMap;
    final DataMap expectedMap;
    actualMap = response.getDataMap();
    expectedMap = JACKSON_DATA_CODEC.stringToMap(response2);
    assertEquals(actualMap, expectedMap);

    RoutingResult routingResult3 = buildRoutingResultAction(Void.TYPE, request, acceptTypeData.acceptHeaders);
    // #3 empty response
    response =
        _responseHandler.buildPartialResponse(routingResult3,
                                              _responseHandler.buildRestLiResponseData(request, routingResult3, null));
    checkResponse(response,
                  HttpStatus.S_200_OK,
                  1,
                  false,
                  false,
                  errorResponseHeaderName);
    assertEquals(response.getEntity(), null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusActionDataPartial")
  public void testRestLiResponseData(AcceptTypeData acceptTypeData,
                                      String response1,
                                      String response2,
                                      ProtocolVersion protocolVersion,
                                      String errorResponseHeaderName) throws Exception
  {
    final RestRequest request = buildRequest(acceptTypeData.acceptHeaders, protocolVersion);
    AugmentedRestLiResponseData responseData;
    RoutingResult routingResult1 = buildRoutingResultAction(Status.class, request, acceptTypeData.acceptHeaders);
    // #1 simple record template
    responseData = _responseHandler.buildRestLiResponseData(request, routingResult1, buildStatusRecord());
    checkResponseData(responseData, HttpStatus.S_200_OK, 1, false, true, errorResponseHeaderName);
    assertEquals(responseData.getEntityResponse().toString(), response1);
    // #2 DataTemplate response
    StringMap map = new StringMap();
    map.put("key1", "value1");
    map.put("key2", "value2");
    RoutingResult routingResult2 = buildRoutingResultAction(StringMap.class, request, acceptTypeData.acceptHeaders);
    responseData = _responseHandler.buildRestLiResponseData(request, routingResult2, map);
    checkResponseData(responseData, HttpStatus.S_200_OK, 1, false, true, errorResponseHeaderName);

    //Obtain the maps necessary for comparison
    final DataMap actualMap;
    final DataMap expectedMap;
    actualMap = responseData.getEntityResponse().data();
    expectedMap = JACKSON_DATA_CODEC.stringToMap(response2);
    assertEquals(actualMap, expectedMap);

    RoutingResult routingResult3 = buildRoutingResultAction(Void.TYPE, request, acceptTypeData.acceptHeaders);
    // #3 empty response
    responseData =
    _responseHandler.buildRestLiResponseData(request, routingResult3, null);
    checkResponseData(responseData, HttpStatus.S_200_OK, 1, false, false, errorResponseHeaderName);
    assertEquals(responseData.getEntityResponse(), null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  void testRestErrors(AcceptTypeData acceptTypeData,
                      ProtocolVersion protocolVersion,
                      String errorResponseHeaderName) throws Exception
  {
    RestResponse response;
    RestLiServiceException ex;
    final RestRequest request = buildRequest(acceptTypeData.acceptHeaders, protocolVersion);

    // #1
    ex = new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "missing fields");
    response = _responseHandler.buildResponse(request,
                                              buildRoutingResult(request, acceptTypeData.acceptHeaders),
                                              ex);

    checkResponse(response, 400, 3, acceptTypeData.responseContentType, ErrorResponse.class.getName(), null, true, true, errorResponseHeaderName);
    DataMap dataMap = acceptTypeData.dataCodec.readMap(response.getEntity().asInputStream());

    assertEquals(dataMap.getInteger("status"), Integer.valueOf(400));
    assertEquals(dataMap.getString("message"), "missing fields");

    // #2
    ex = new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "missing fields").setServiceErrorCode(11);
    response = _responseHandler.buildResponse(request,
                                              buildRoutingResult(request, acceptTypeData.acceptHeaders),
                                              ex);

    checkResponse(response, 400, 3, acceptTypeData.responseContentType, ErrorResponse.class.getName(), null, true, true, errorResponseHeaderName);
    dataMap = acceptTypeData.dataCodec.readMap(response.getEntity().asInputStream());

    assertEquals(dataMap.getInteger("status"), Integer.valueOf(400));
    assertEquals(dataMap.getString("message"), "missing fields");
    assertEquals(dataMap.getInteger("serviceErrorCode"), Integer.valueOf(11));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  public void testFieldProjection_records(AcceptTypeData acceptTypeData,
                                          ProtocolVersion protocolVersion,
                                          String errorResponseHeaderName)
          throws Exception
  {
    RestResponse response;

    // #1 all fields
    RestRequest request1 = buildRequest("/test?fields=f1,f2,f3", acceptTypeData.acceptHeaders, protocolVersion);
    Status status = buildStatusWithFields("f1", "f2", "f3");
    response = _responseHandler.buildResponse(request1,
                                              buildRoutingResult(request1, acceptTypeData.acceptHeaders),
                                              status);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[] {"f1", "f2", "f3"}, new String[0]);

    // #2 some fields
    RestRequest request2 = buildRequest("/test?fields=f1,f3", acceptTypeData.acceptHeaders, protocolVersion);
    response = _responseHandler.buildResponse(request2,
                                              buildRoutingResult(request2, acceptTypeData.acceptHeaders),
                                              status);
    assertTrue(status.data().containsKey("f2"));

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[] {"f1", "f3"}, new String[] {"f2"});

    // #3 no fields
    RestRequest request3 = buildRequest("/test?fields=", acceptTypeData.acceptHeaders, protocolVersion);
    response = _responseHandler.buildResponse(request3,
                                              buildRoutingResult(request3, acceptTypeData.acceptHeaders),
                                              status);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[]{}, new String[]{"f1", "f2", "f3"});
    assertTrue(status.data().containsKey("f1"));
    assertTrue(status.data().containsKey("f2"));
    assertTrue(status.data().containsKey("f3"));

    // #4 fields not in schema
    RestRequest request4 = buildRequest("/test?fields=f1,f99", acceptTypeData.acceptHeaders, protocolVersion);
    response = _responseHandler.buildResponse(request4,
                                              buildRoutingResult(request4, acceptTypeData.acceptHeaders),
                                              status);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[] {"f1"}, new String[] {"f2", "f3", "f99"});
    assertTrue(status.data().containsKey("f2"));
    assertTrue(status.data().containsKey("f3"));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  public void testFieldProjectionRecordsPALSyntax(AcceptTypeData acceptTypeData,
                                                  ProtocolVersion protocolVersion,
                                                  String errorResponseHeaderName)
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

    RestRequest request1 = buildRequest("/test?fields=f1,f2:(f3,f4)", acceptTypeData.acceptHeaders, protocolVersion);
    response = _responseHandler.buildResponse(request1,
                                              buildRoutingResult(request1, acceptTypeData.acceptHeaders),
                                              status);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[] {"f1", "f2", "f3"}, new String[0]);

    // #2 some fields
    RestRequest request2 = buildRequest("/test?fields=f1,f2:(f3)", acceptTypeData.acceptHeaders, protocolVersion);
    response = _responseHandler.buildResponse(request2,
                                              buildRoutingResult(request2, acceptTypeData.acceptHeaders),
                                              status);
    assertTrue(status.data().containsKey("f2"));

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[] {"f1", "f2", "f3"}, new String[] {"f4"});

    // #3 no fields
    RestRequest request3 = buildRequest("/test?fields=", acceptTypeData.acceptHeaders, protocolVersion);
    response = _responseHandler.buildResponse(request3,
                                              buildRoutingResult(request3, acceptTypeData.acceptHeaders),
                                              status);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[]{}, new String[]{"f1", "f2", "f3", "f4"});
    assertTrue(status.data().containsKey("f1"));
    assertTrue(status.data().containsKey("f2"));

    // #4 fields not in schema
    RestRequest request4 = buildRequest("/test?fields=f2:(f99)", acceptTypeData.acceptHeaders, protocolVersion);
    response = _responseHandler.buildResponse(request4,
                                              buildRoutingResult(request4, acceptTypeData.acceptHeaders),
                                              status);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, Status.class.getName(), null, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[] {"f2"}, new String[] {"f1", "f3", "f99"});
    assertTrue(status.data().containsKey("f2"));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  public void testFieldProjection_collections_CollectionResult(AcceptTypeData acceptTypeData,
                                                               ProtocolVersion protocolVersion,
                                                               String errorResponseHeaderName)
          throws Exception
  {
    RestResponse response;

    BasicCollectionResult<Status> statusCollection = buildStatusCollectionResult(10, "f1", "f2",
                                                                                 "f3");
    RestRequest request = buildRequest("/test?fields=f1,f2", acceptTypeData.acceptHeaders, protocolVersion);
    response = _responseHandler.buildResponse(request,
                                              buildRoutingResultFinder(request, acceptTypeData.acceptHeaders),
                                              statusCollection);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType,
                  CollectionResponse.class.getName(), null, true, errorResponseHeaderName);

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

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  public void testFieldProjection_collections_List(AcceptTypeData acceptTypeData,
                                                   ProtocolVersion protocolVersion,
                                                   String errorResponseHeaderName) throws Exception
  {
    RestResponse response;

    List<Status> statusCollection = buildStatusListResult(10, "f1", "f2", "f3");
    RestRequest request = buildRequest("/test?fields=f1,f2", acceptTypeData.acceptHeaders, protocolVersion);
    response = _responseHandler.buildResponse(request,
                                              buildRoutingResultFinder(request, acceptTypeData.acceptHeaders),
                                              statusCollection);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, CollectionResponse.class.getName(), null, true, errorResponseHeaderName);

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

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicDataWithBatchUri")
  public Object[][] basicDataWithBatchUri()
  {
    return new Object[][]
      {
        { AcceptTypeData.EMPTY, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "/test?ids=1&ids=2&ids=3&fields=f1,f2", RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE },
        { AcceptTypeData.EMPTY, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "/test?ids=List(1,2,3)&fields=f1,f2", RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE },
        { AcceptTypeData.ANY, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "/test?ids=1&ids=2&ids=3&fields=f1,f2", RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE },
        { AcceptTypeData.ANY, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "/test?ids=List(1,2,3)&fields=f1,f2", RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE },
        { AcceptTypeData.JSON, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "/test?ids=1&ids=2&ids=3&fields=f1,f2", RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE },
        { AcceptTypeData.JSON, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "/test?ids=List(1,2,3)&fields=f1,f2", RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE },
        { AcceptTypeData.PSON, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          "/test?ids=1&ids=2&ids=3&fields=f1,f2", RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE },
        { AcceptTypeData.PSON, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          "/test?ids=List(1,2,3)&fields=f1,f2", RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicDataWithBatchUri")
  public void testFieldProjection_batch(AcceptTypeData acceptTypeData,
                                        ProtocolVersion protocolVersion,
                                        String uri,
                                        String errorResponseHeaderName) throws Exception
  {
    RestResponse response;

    Map<Integer, Status> statusBatch = buildStatusBatchResponse(10, "f1", "f2", "f3");
    RestRequest request = buildRequest(uri, acceptTypeData.acceptHeaders, protocolVersion);
    response = _responseHandler.buildResponse(request,
                                              buildRoutingResult(
                                                  ResourceMethod.BATCH_GET, request, acceptTypeData.acceptHeaders),
                                              statusBatch);

    checkResponse(response, 200, 2, acceptTypeData.responseContentType, BatchResponse.class.getName(),
                  Status.class.getName(), true, errorResponseHeaderName);

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

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  public void testApplicationSpecifiedHeaders(AcceptTypeData acceptTypeData,
                                              ProtocolVersion protocolVersion,
                                              String errorResponseHeaderName) throws Exception
  {
    String testHeaderName = "X-LI-TEST-HEADER";
    String testHeaderValue = "test";

    ResourceModel resourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = resourceModel.findNamedMethod("search");
    ResourceContextImpl context = new ResourceContextImpl();
    context.setResponseHeader(testHeaderName, testHeaderValue);
    RestUtils.validateRequestHeadersAndUpdateResourceContext(acceptTypeData.acceptHeaders, context);
    RoutingResult routingResult = new RoutingResult(context, methodDescriptor);

    RestResponse response;
    response = _responseHandler.buildResponse(buildRequest(acceptTypeData.acceptHeaders, protocolVersion),
                                              routingResult,
                                              buildStatusList(3));

    Assert.assertEquals(response.getHeader(testHeaderName), testHeaderValue);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusesData")
  public Object[][] statusesData()
  {
    return new Object[][]
      {
        {
          AcceptTypeData.EMPTY,
          EXPECTED_STATUS_JSON,
          EXPECTED_STATUS_ACTION_RESPONSE_JSON,
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE
        },
        {
          AcceptTypeData.EMPTY,
          EXPECTED_STATUS_JSON,
          EXPECTED_STATUS_ACTION_RESPONSE_JSON,
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          RestConstants.HEADER_RESTLI_ERROR_RESPONSE
        },
        {
          AcceptTypeData.ANY,
          EXPECTED_STATUS_JSON,
          EXPECTED_STATUS_ACTION_RESPONSE_JSON,
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE
        },
        {
          AcceptTypeData.ANY,
          EXPECTED_STATUS_JSON,
          EXPECTED_STATUS_ACTION_RESPONSE_JSON,
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          RestConstants.HEADER_RESTLI_ERROR_RESPONSE
        },
        {
          AcceptTypeData.JSON,
          EXPECTED_STATUS_JSON,
          EXPECTED_STATUS_ACTION_RESPONSE_JSON,
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE
        },
        {
          AcceptTypeData.JSON,
          EXPECTED_STATUS_JSON,
          EXPECTED_STATUS_ACTION_RESPONSE_JSON,
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          RestConstants.HEADER_RESTLI_ERROR_RESPONSE
        },
        {
          AcceptTypeData.PSON,
          EXPECTED_STATUS_PSON,
          EXPECTED_STATUS_ACTION_RESPONSE_PSON,
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE
        },
        {
          AcceptTypeData.PSON,
          EXPECTED_STATUS_PSON,
          EXPECTED_STATUS_ACTION_RESPONSE_PSON,
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          RestConstants.HEADER_RESTLI_ERROR_RESPONSE
        }
      };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusesData")
  public void testWrapperResults(AcceptTypeData acceptTypeData,
                                 String expectedStatus,
                                 String expectedActionStatus,
                                 ProtocolVersion protocolVersion,
                                 String errorResponseHeaderName) throws Exception
  {
    RestResponse response;
    final Status status = buildStatusRecord();

    final GetResult<Status> getResult = new GetResult<Status>(status, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    response = invokeResponseHandler("/test", getResult, ResourceMethod.GET, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response, HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode(), 2, acceptTypeData.responseContentType, Status.class.getName(), null, true, errorResponseHeaderName);
    assertEquals(response.getEntity().asAvroString(), expectedStatus);

    final RestRequest request = buildRequest(acceptTypeData.acceptHeaders, protocolVersion);
    final ActionResult<Status> actionResult = new ActionResult<Status>(status, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    response = _responseHandler.buildResponse(request,
                                              buildRoutingResultAction(Status.class, request, acceptTypeData.acceptHeaders),
                                              actionResult);
    checkResponse(response,
                  HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode(),
                  2,
                  acceptTypeData.responseContentType,
                  ActionResponse.class.getName(),
                  Status.class.getName(),
                  true,
                  errorResponseHeaderName);
    assertEquals(response.getEntity().asAvroString(), expectedActionStatus);
  }

  // *****************
  // Helper methods
  // *****************

  private final RestRequest buildRequest(Map<String, String> headers, ProtocolVersion protocolVersion) throws URISyntaxException
  {
    return buildRequest("/test", headers, protocolVersion);
  }

  private final RestRequest buildRequest(String uri, Map<String, String> headers, ProtocolVersion protocolVersion) throws URISyntaxException
  {
    return new RestRequestBuilder(new URI(uri)).setMethod("DONT_CARE").setHeaders(headers).setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString()).build();
  }

  /**
   * Creates a RoutingResult for an Action with the given returnType.
   *
   * @param actionReturnType the return type of the action.
   * @return a RoutingResult
   */
  private final RoutingResult buildRoutingResultAction(Class<?> actionReturnType, RestRequest request, Map<String, String> headers)
          throws NoSuchMethodException, RestLiSyntaxException, URISyntaxException
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

    ServerResourceContext resourceContext = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    RestUtils.validateRequestHeadersAndUpdateResourceContext(headers, resourceContext);
    return new RoutingResult(resourceContext, methodDescriptor);
  }

  private final RoutingResult buildRoutingResult(RestRequest request, Map<String, String> acceptHeaders)
          throws SecurityException, NoSuchMethodException, RestLiSyntaxException
  {
    return buildRoutingResult(ResourceMethod.GET, request, acceptHeaders);
  }

  private final RoutingResult buildRoutingResult(ResourceMethod resourceMethod, RestRequest request, Map<String, String> acceptHeaders)
          throws SecurityException, NoSuchMethodException, RestLiSyntaxException
  {
    Method method = ProjectionTestFixture.class.getMethod("batchGet", Set.class);
    ResourceModel model = RestLiTestHelper.buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor =
        ResourceMethodDescriptor.createForRestful(resourceMethod, method, InterfaceType.SYNC);
    model.addResourceMethodDescriptor(methodDescriptor);
    ServerResourceContext context =  new ResourceContextImpl(new PathKeysImpl(), request,
                            new RequestContext());
    RestUtils.validateRequestHeadersAndUpdateResourceContext(acceptHeaders, context);
    return new RoutingResult(context, methodDescriptor);
  }


  private final RoutingResult buildRoutingResultFinder(RestRequest request, Map<String, String> acceptHeaders) throws SecurityException,
      NoSuchMethodException,
      RestLiSyntaxException
  {
    Method method = ProjectionTestFixture.class.getMethod("find");
    ResourceModel model = RestLiTestHelper.buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor =
        ResourceMethodDescriptor.createForRestful(ResourceMethod.FINDER, method, InterfaceType.SYNC);
    model.addResourceMethodDescriptor(methodDescriptor);
    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    RestUtils.validateRequestHeadersAndUpdateResourceContext(acceptHeaders, context);
    return new RoutingResult(context, methodDescriptor);
  }

  @RestLiCollection(name="test")
  private static class ProjectionTestFixture extends CollectionResourceTemplate<Integer, Status>
  {
    @Override
    public Map<Integer, Status> batchGet(Set<Integer> ids)
    {
      return null;
    }

    public BasicCollectionResult<Status> find()
    {
      return null;
    }
  }

  private void checkResponse(PartialRestResponse response,
                             HttpStatus status,
                             int numHeaders,
                             boolean hasError,
                             boolean hasEntity,
                             String errorResponseHeaderName)
  {
    assertEquals(response.getStatus(), status);
    assertEquals(response.getHeaders().size(), numHeaders);
    if (hasError)
    {
      assertEquals(response.getHeader(errorResponseHeaderName), RestConstants.HEADER_VALUE_ERROR);
    }
    else
    {
      assertNull(response.getHeader(errorResponseHeaderName));
    }

    assertEquals(response.getEntity() != null, hasEntity);
  }

  private void checkResponseData(AugmentedRestLiResponseData responseData, HttpStatus status, int numHeaders,
                                 boolean hasError, boolean hasEntity, String errorResponseHeaderName)
  {
    assertEquals(responseData.getStatus(), status);
    assertEquals(responseData.getHeaders().size(), numHeaders);
    if (hasError)
    {
      assertEquals(responseData.getHeaders().get(errorResponseHeaderName), RestConstants.HEADER_VALUE_ERROR);
    }
    else
    {
      assertNull(responseData.getHeaders().get(errorResponseHeaderName));
    }

    assertEquals(responseData.getEntityResponse() != null, hasEntity);
  }

  private void checkResponse(RestResponse response,
                             int status,
                             int numHeaders,
                             String contentType,
                             String type,
                             String subType,
                             boolean hasEntity,
                             String errorResponseHeaderName)
  {
    checkResponse(response, status, numHeaders, contentType, type, subType, false, hasEntity, errorResponseHeaderName);
  }

  private void checkResponse(RestResponse response,
                             int status,
                             int numHeaders,
                             String contentType,
                             String type,
                             String subType,
                             boolean hasError,
                             boolean hasEntity,
                             String errorResponseHeaderName)
  {
    assertEquals(response.getStatus(), status);
    assertEquals(response.getHeaders().size(), numHeaders);
    assertEquals(response.getHeader(RestConstants.HEADER_CONTENT_TYPE), contentType);

    if (hasError)
    {
      assertEquals(response.getHeader(errorResponseHeaderName), RestConstants.HEADER_VALUE_ERROR);
    }
    else
    {
      assertNull(response.getHeader(errorResponseHeaderName));
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
                                       URIDetails prevLink,
                                       URIDetails nextLink,
                                       AcceptTypeData acceptTypeData)
          throws Exception
  {
    DataMap dataMap = acceptTypeData.dataCodec.readMap(response.getEntity().asInputStream());
    CollectionResponse<Status> collectionResponse = new CollectionResponse<Status>(dataMap, Status.class);

    assertEquals(collectionResponse.getElements().size(), numElements);
    assertEquals(collectionResponse.getPaging().getStart().intValue(), start);
    assertEquals(collectionResponse.getPaging().getCount().intValue(), count);
    assertEquals(collectionResponse.getPaging().getLinks().size(), numLinks);
    if (total == null)
    {
      assertFalse(collectionResponse.getPaging().hasTotal());
    }
    else
    {
      assertEquals(collectionResponse.getPaging().getTotal().intValue(), total.intValue());
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

  private static void checkLink(Link link, String rel, URIDetails expectedURIDetails, String type)
  {
    assertEquals(link.getRel(), rel);
    assertEquals(link.getType(), type);
    URIDetails.testUriGeneration(link.getHref(), expectedURIDetails);
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
