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


import com.google.common.collect.Sets;
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
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.CookieUtil;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.response.ActionResponseEnvelope;
import com.linkedin.restli.internal.server.response.GetResponseEnvelope;
import com.linkedin.restli.internal.server.response.ResponseUtils;
import com.linkedin.restli.internal.server.response.RestLiResponse;
import com.linkedin.restli.internal.server.response.RestLiResponseHandler;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
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
import com.linkedin.restli.server.UnstructuredDataWriter;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.GetResult;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.twitter.FeedDownloadResource;
import com.linkedin.restli.server.twitter.StatusCollectionResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
  private RestLiResponseHandler _responseHandler = new RestLiResponseHandler();

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

  private RestLiResponse invokeResponseHandler(String path,
                                             Object body,
                                             ResourceMethod method,
                                             Map<String, String> headers,
                                             ProtocolVersion protocolVersion) throws Exception
  {
    RestRequest req = buildRequest(path, headers, protocolVersion);
    RoutingResult routing = buildRoutingResult(method, req, headers);
    return buildPartialRestResponse(req, routing, body);
  }

  private enum AcceptTypeData
  {
    JSON  (JSON_ACCEPT_HEADERS,   APPLICATION_JSON,   JACKSON_DATA_CODEC),
    PSON  (PSON_ACCEPT_HEADERS,   APPLICATION_PSON,   PSON_DATA_CODEC),
    EMPTY (EMPTY_ACCEPT_HEADERS,  APPLICATION_JSON,   JACKSON_DATA_CODEC),
    ANY   (ANY_ACCEPT_HEADERS,    APPLICATION_JSON,   JACKSON_DATA_CODEC);

    public Map<String, String> acceptHeaders;
    public String              responseContentType;
    public DataCodec           dataCodec;

    AcceptTypeData(Map<String, String> acceptHeaders, String responseContentType, DataCodec dataCodec)
    {
      this.acceptHeaders = acceptHeaders;
      this.responseContentType = responseContentType;
      this.dataCodec = dataCodec;
    }
  }

  @Test
  public void testInvalidAcceptHeaders() throws Exception
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

  @Test
  public void testCustomAcceptHeaders() throws Exception
  {
    Map<String, String> customAcceptHeaders = Collections.singletonMap("Accept", "application/json+2.0");

    // check response with out codec support (expect 406 error)
    try
    {
      invokeResponseHandler("/test", buildStatusRecord(), ResourceMethod.GET,
          customAcceptHeaders, AllProtocolVersions.LATEST_PROTOCOL_VERSION);
      Assert.fail();
    }
    catch (RestLiServiceException e)
    {
      Assert.assertEquals(e.getStatus().getCode(), 406);
    }

    // check response without creating a custom codec (expect 406 error)
    try
    {
      RestRequest req = buildRequest("/test", customAcceptHeaders, AllProtocolVersions.LATEST_PROTOCOL_VERSION);
      RoutingResult routing = buildRoutingResult(ResourceMethod.GET, req, customAcceptHeaders, Sets.newHashSet("application/json+2.0"));
      RestLiResponse restLiResponse = buildPartialRestResponse(req, routing, buildStatusRecord());
      ResponseUtils.buildResponse(routing, restLiResponse);
      Assert.fail();
    }
    catch (RestLiServiceException e)
    {
      Assert.assertEquals(e.getStatus().getCode(), 406);
    }

    // Register custom codec
    ContentType.createContentType("application/json+2.0", JACKSON_DATA_CODEC);
    RestRequest req = buildRequest("/test", customAcceptHeaders, AllProtocolVersions.LATEST_PROTOCOL_VERSION);
    RoutingResult routing = buildRoutingResult(ResourceMethod.GET, req, customAcceptHeaders, Sets.newHashSet("application/json+2.0"));
    RestLiResponse response =  buildPartialRestResponse(req, routing, buildStatusRecord());

    checkResponse(response, 200, 1, true, RestConstants.HEADER_RESTLI_ERROR_RESPONSE);
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
    RestLiResponse response;
    // #1 simple record template
    response = invokeResponseHandler("/test", buildStatusRecord(), ResourceMethod.GET, acceptTypeData.acceptHeaders, protocolVersion);

    checkResponse(response, 200, 1, true, errorResponseHeaderName);
    if (acceptTypeData != AcceptTypeData.PSON)
    {
      assertEquals(DataMapUtils.mapToByteString(response.getDataMap()).asAvroString(), expectedStatus);
    }
    RestRequest req = buildRequest("/test", acceptTypeData.acceptHeaders, protocolVersion);
    RoutingResult routing = buildRoutingResult(ResourceMethod.GET, req, acceptTypeData.acceptHeaders);
    RestResponse restResponse = ResponseUtils.buildResponse(routing, response);
    assertEquals(restResponse.getEntity().asAvroString(), expectedStatus);

    // #2 create (with id)
    response = invokeResponseHandler("/test", new CreateResponse(1), ResourceMethod.CREATE, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response, 201, 3, false, errorResponseHeaderName);
    assertEquals(response.getHeader(RestConstants.HEADER_LOCATION), "/test/1");
    assertEquals(response.getHeader(idHeaderName), "1");

    // #2.1 create (without id)
    response = invokeResponseHandler("/test", new CreateResponse(HttpStatus.S_201_CREATED),
                                     ResourceMethod.CREATE, acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkResponse(response, 201, 1, false, errorResponseHeaderName);

    // #2.2 create (with id and slash at the end of uri)
    response = invokeResponseHandler("/test/", new CreateResponse(1), ResourceMethod.CREATE, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response, 201, 3, false, errorResponseHeaderName);
    assertEquals(response.getHeader(RestConstants.HEADER_LOCATION), "/test/1");
    assertEquals(response.getHeader(idHeaderName), "1");

    // #2.3 create (without id and slash at the end of uri)
    response = invokeResponseHandler("/test/", new CreateResponse(HttpStatus.S_201_CREATED),
                                     ResourceMethod.CREATE, acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkResponse(response, 201, 1, false, errorResponseHeaderName);

    // #3 update
    response = invokeResponseHandler("/test", new UpdateResponse(HttpStatus.S_204_NO_CONTENT),
                                     ResourceMethod.UPDATE, acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkResponse(response, 204, 1, false, errorResponseHeaderName);
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
    RestLiResponse response;

    // #4 batch
    Map<Long, Status> map = new HashMap<>();
    map.put(1L, buildStatusRecord());
    map.put(2L, buildStatusRecord());
    map.put(3L, buildStatusRecord());
    response = invokeResponseHandler("/test", map, ResourceMethod.BATCH_GET, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response,
                  200,
                  1, true,
                  errorResponseHeaderName);

    Map<Long, UpdateResponse> updateStatusMap = new HashMap<>();
    updateStatusMap.put(1L, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    updateStatusMap.put(2L, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    updateStatusMap.put(3L, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
    BatchUpdateResult<Long, Status> batchUpdateResult = new BatchUpdateResult<>(updateStatusMap);
    response = invokeResponseHandler("/test", batchUpdateResult, ResourceMethod.BATCH_UPDATE, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response, 200, 1, true, errorResponseHeaderName);

    response = invokeResponseHandler("/test", batchUpdateResult, ResourceMethod.BATCH_PARTIAL_UPDATE, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response, 200, 1, true, errorResponseHeaderName);

    response = invokeResponseHandler("/test", batchUpdateResult, ResourceMethod.BATCH_DELETE, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response,
                  200,
                  1, true,
                  errorResponseHeaderName);

    List<CreateResponse> createResponses = new ArrayList<>();
    createResponses.add(new CreateResponse("42", HttpStatus.S_204_NO_CONTENT));
    createResponses.add(new CreateResponse(HttpStatus.S_400_BAD_REQUEST));
    createResponses.add(new CreateResponse(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
    BatchCreateResult<Long, Status> batchCreateResult = new BatchCreateResult<>(createResponses);

    response = invokeResponseHandler("/test", batchCreateResult, ResourceMethod.BATCH_CREATE, acceptTypeData.acceptHeaders, protocolVersion); // here
    checkResponse(response, 200, 1, true, errorResponseHeaderName);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  public void testCollections(AcceptTypeData acceptTypeData,
                              ProtocolVersion protocolVersion,
                              String errorResponseHeaderName) throws Exception
  {
    ResourceModel resourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = resourceModel.findFinderMethod("search");

    RestLiResponse response;
    // #1 check datamap/entity structure

    ServerResourceContext context = new ResourceContextImpl();
    RestUtils.validateRequestHeadersAndUpdateResourceContext(acceptTypeData.acceptHeaders,
        Collections.emptySet(),
        context);
    RoutingResult routingResult = new RoutingResult(context, methodDescriptor);
    response = buildPartialRestResponse(buildRequest(acceptTypeData.acceptHeaders, protocolVersion),
        routingResult,
        buildStatusList(3));
    checkResponse(response, 200, 1, true, errorResponseHeaderName);

    String baseUri = "/test?someParam=foo";

    // #1.1 using CollectionResult
    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     new BasicCollectionResult<>(buildStatusList(5)),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkCollectionResponse(response, 5, 0, 5, 1, null, null, null, acceptTypeData);

    // #1.1 using CollectionResult (with total)
    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     new BasicCollectionResult<>(buildStatusList(5), 10),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkCollectionResponse(response, 5, 0, 5, 1, 10, null, null, acceptTypeData);

    // using CollectionResult with metadata RecordTemplate
    CollectionMetadata metadata = new CollectionMetadata();
    metadata.setCount(42);

    response = invokeResponseHandler(baseUri + "&start=0&count=5",
                                     methodDescriptor,
                                     new CollectionResult<>(buildStatusList(5), 10, metadata),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    checkCollectionResponse(response, 5, 0, 5, 1, 10, null, null, acceptTypeData);
    DataMap dataMap = response.getDataMap();
    CollectionResponse<Status> collectionResponse = new CollectionResponse<>(dataMap, Status.class);
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
    final Map<String, String> queryParamsMap3next = new HashMap<>();
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
    final Map<String, String> queryParamsMap4prev = new HashMap<>();
    queryParamsMap4prev.put("count", "5");
    queryParamsMap4prev.put("start", "0");
    queryParamsMap4prev.put("someParam", "foo");
    final URIDetails expectedURIDetails4prev = new URIDetails(protocolVersion, "/test", null, queryParamsMap4prev, null);
    final Map<String, String> queryParamsMap4next = new HashMap<>();
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
    final Map<String, String> queryParamsMap5prev = new HashMap<>();
    queryParamsMap5prev.put("count", "5");
    queryParamsMap5prev.put("start", "5");
    queryParamsMap5prev.put("someParam", "foo");
    final URIDetails expectedURIDetails5prev = new URIDetails(protocolVersion, "/test", null, queryParamsMap5prev, null);
    checkCollectionResponse(response, 4, 10, 5, 1, null, expectedURIDetails5prev, null, acceptTypeData);

    response = invokeResponseHandler(baseUri + "&start=10&count=5",
                                     methodDescriptor,
                                     new BasicCollectionResult<>(buildStatusList(4), 15),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    //"/test?count=5&start=5&someParam=foo", "/test?count=5&start=14&someParam=foo"
    final Map<String, String> queryParamsMap6prev = new HashMap<>();
    queryParamsMap6prev.put("count", "5");
    queryParamsMap6prev.put("start", "5");
    queryParamsMap6prev.put("someParam", "foo");
    final URIDetails expectedURIDetails6prev = new URIDetails(protocolVersion, "/test", null, queryParamsMap6prev, null);
    final Map<String, String> queryParamsMap6next = new HashMap<>();
    queryParamsMap6next.put("count", "5");
    queryParamsMap6next.put("start", "14");
    queryParamsMap6next.put("someParam", "foo");
    final URIDetails expectedURIDetails6next = new URIDetails(protocolVersion, "/test", null, queryParamsMap6next, null);
    checkCollectionResponse(response, 4, 10, 5, 2, 15, expectedURIDetails6prev, expectedURIDetails6next, acceptTypeData);

    response = invokeResponseHandler(baseUri + "&start=10&count=5",
                                     methodDescriptor,
                                     new BasicCollectionResult<>(buildStatusList(4), 14),
                                     acceptTypeData.acceptHeaders,
                                     protocolVersion);
    //"/test?count=5&start=5&someParam=foo"
    final Map<String, String> queryParamsMap7prev = new HashMap<>();
    queryParamsMap7prev.put("count", "5");
    queryParamsMap7prev.put("start", "5");
    queryParamsMap7prev.put("someParam", "foo");
    final URIDetails expectedURIDetails7prev = new URIDetails(protocolVersion, "/test", null, queryParamsMap7prev, null);
    checkCollectionResponse(response, 4, 10, 5, 1, 14, expectedURIDetails7prev, null, acceptTypeData);
  }

  private RestLiResponse invokeResponseHandler(String uri,
                                             ResourceMethodDescriptor methodDescriptor,
                                             Object result,
                                             Map<String, String> headers,
                                             ProtocolVersion protocolVersion)
          throws IOException, URISyntaxException, RestLiSyntaxException
  {
    RestRequest request = buildRequest(uri, headers, protocolVersion);
    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    RestUtils.validateRequestHeadersAndUpdateResourceContext(headers, Collections.emptySet(), context);
    RoutingResult routingResult = new RoutingResult(context, methodDescriptor);
    return buildPartialRestResponse(request, routingResult, result);
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
    RestLiResponse response;

    // #1 simple record template
    RoutingResult routing = buildRoutingResultAction(Status.class, request, acceptTypeData.acceptHeaders);
    response = buildPartialRestResponse(request, routing, buildStatusRecord());

    checkResponse(response, 200, 1, true, errorResponseHeaderName);
    if (acceptTypeData != AcceptTypeData.PSON)
    {
      assertEquals(DataMapUtils.mapToByteString(response.getDataMap()).asAvroString(), response1);
    }
    RestResponse restResponse = ResponseUtils.buildResponse(routing, response);
    assertEquals(restResponse.getEntity().asAvroString(), response1);

    // #2 DataTemplate response
    StringMap map = new StringMap();
    map.put("key1", "value1");
    map.put("key2", "value2");
    response = buildPartialRestResponse(request,
                                              buildRoutingResultAction(StringMap.class, request, acceptTypeData.acceptHeaders),
                                              map);

    checkResponse(response, 200, 1, true, errorResponseHeaderName);

    //Convert both of these back into maps depending on their response type
    final DataMap actualMap;
    final DataMap expectedMap;
    if (acceptTypeData == AcceptTypeData.PSON)
    {
      routing = buildRoutingResultAction(StringMap.class, request, acceptTypeData.acceptHeaders);
      restResponse = ResponseUtils.buildResponse(routing, response);
      actualMap = PSON_DATA_CODEC.bytesToMap(restResponse.getEntity().copyBytes());
      expectedMap = PSON_DATA_CODEC.bytesToMap(ByteString.copyAvroString(response2, false).copyBytes());
    }
    else
    {
      actualMap = JACKSON_DATA_CODEC.bytesToMap(DataMapUtils.mapToByteString(response.getDataMap()).copyBytes());
      expectedMap = JACKSON_DATA_CODEC.stringToMap(response2);
    }
    assertEquals(actualMap, expectedMap);

    // #3 empty response
    response = buildPartialRestResponse(request,
                                              buildRoutingResultAction(Void.TYPE, request, acceptTypeData.acceptHeaders),
                                              null);

    checkResponse(response, 200, 1, false, errorResponseHeaderName);
    assertNull(response.getDataMap());
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
    RestLiResponse response;
    RoutingResult routingResult1 = buildRoutingResultAction(Status.class, request, acceptTypeData.acceptHeaders);
    // #1 simple record template
    response = buildPartialRestResponse(request, routingResult1, buildStatusRecord());
    checkResponse(response, HttpStatus.S_200_OK, 1, false, true, errorResponseHeaderName);
    assertEquals(response.getEntity().toString(), response1);
    // #2 DataTemplate response
    StringMap map = new StringMap();
    map.put("key1", "value1");
    map.put("key2", "value2");
    RoutingResult routingResult2 = buildRoutingResultAction(StringMap.class, request, acceptTypeData.acceptHeaders);
    response = buildPartialRestResponse(request, routingResult2, map);
    checkResponse(response, HttpStatus.S_200_OK, 1, false, true, errorResponseHeaderName);

    //Obtain the maps necessary for comparison
    final DataMap actualMap;
    final DataMap expectedMap;
    actualMap = response.getDataMap();
    expectedMap = JACKSON_DATA_CODEC.stringToMap(response2);
    assertEquals(actualMap, expectedMap);

    RoutingResult routingResult3 = buildRoutingResultAction(Void.TYPE, request, acceptTypeData.acceptHeaders);
    // #3 empty response
    response = buildPartialRestResponse(request, routingResult3, null);
    checkResponse(response,
                  HttpStatus.S_200_OK,
                  1,
                  false,
                  false,
                  errorResponseHeaderName);
    assertEquals(response.getEntity(), null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "statusActionDataPartial")
  @SuppressWarnings("unchecked")
  public void testRestLiResponseData(AcceptTypeData acceptTypeData,
                                      String response1,
                                      String response2,
                                      ProtocolVersion protocolVersion,
                                      String errorResponseHeaderName) throws Exception
  {
    final RestRequest request = buildRequest(acceptTypeData.acceptHeaders, protocolVersion);
    RestLiResponseData<ActionResponseEnvelope> responseData;
    RoutingResult routingResult1 = buildRoutingResultAction(Status.class, request, acceptTypeData.acceptHeaders);
    // #1 simple record template
    responseData = (RestLiResponseData<ActionResponseEnvelope>) _responseHandler.buildRestLiResponseData(request, routingResult1, buildStatusRecord());
    checkResponseData(responseData, HttpStatus.S_200_OK, 1, false, true, errorResponseHeaderName);
    assertEquals(responseData.getResponseEnvelope().getRecord().toString(), response1);
    // #2 DataTemplate response
    StringMap map = new StringMap();
    map.put("key1", "value1");
    map.put("key2", "value2");
    RoutingResult routingResult2 = buildRoutingResultAction(StringMap.class, request, acceptTypeData.acceptHeaders);
    responseData = (RestLiResponseData<ActionResponseEnvelope>) _responseHandler.buildRestLiResponseData(request, routingResult2, map);
    checkResponseData(responseData, HttpStatus.S_200_OK, 1, false, true, errorResponseHeaderName);

    //Obtain the maps necessary for comparison
    final DataMap actualMap;
    final DataMap expectedMap;
    actualMap = responseData.getResponseEnvelope().getRecord().data();
    expectedMap = JACKSON_DATA_CODEC.stringToMap(response2);
    assertEquals(actualMap, expectedMap);

    RoutingResult routingResult3 = buildRoutingResultAction(Void.TYPE, request, acceptTypeData.acceptHeaders);
    // #3 empty response
    responseData = (RestLiResponseData<ActionResponseEnvelope>) _responseHandler.buildRestLiResponseData(request, routingResult3, null);
    checkResponseData(responseData, HttpStatus.S_200_OK, 1, false, false, errorResponseHeaderName);
    assertEquals(responseData.getResponseEnvelope().getRecord(), null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  void testBuildRestException(AcceptTypeData acceptTypeData,
      ProtocolVersion protocolVersion,
      String errorResponseHeaderName) throws Exception
  {
    final RestRequest request = buildRequest(acceptTypeData.acceptHeaders, protocolVersion);
    final RoutingResult routingResult = buildRoutingResult(request, acceptTypeData.acceptHeaders);

    Map<String, String> requestHeaders = new HashMap<>();
    requestHeaders.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());

    RestLiServiceException ex = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "freeway not found").setServiceErrorCode(237);

    RestLiResponseData<?> responseData = _responseHandler.buildExceptionResponseData(routingResult, ex, requestHeaders, Collections.emptyList());

    RestLiResponse response = _responseHandler.buildPartialResponse(routingResult, responseData);
    checkResponse(response,
        404,
        2,
        // The response Content-Type should always be 'application/json'
        true,
        true,
        errorResponseHeaderName);
    DataMap dataMap = response.getDataMap();

    assertEquals(dataMap.getInteger("status"), Integer.valueOf(404));
    assertEquals(dataMap.getString("message"), "freeway not found");
    assertEquals(dataMap.getInteger("serviceErrorCode"), Integer.valueOf(237));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  void testBuildResponseWithExceptionObject(AcceptTypeData acceptTypeData,
                      ProtocolVersion protocolVersion,
                      String errorResponseHeaderName) throws Exception
  {
    RestLiResponse response;
    RestLiServiceException ex;
    final RestRequest request = buildRequest(acceptTypeData.acceptHeaders, protocolVersion);

    // #1
    ex = new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "missing fields");
    response = buildPartialRestResponse(request,
                                              buildRoutingResult(request, acceptTypeData.acceptHeaders),
                                              ex);

    checkResponse(response,
                  400,
                  2, true,
                  true,
                  errorResponseHeaderName);
    DataMap dataMap = response.getDataMap();

    assertEquals(dataMap.getInteger("status"), Integer.valueOf(400));
    assertEquals(dataMap.getString("message"), "missing fields");

    // #2
    ex = new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "missing fields").setServiceErrorCode(11);
    response = buildPartialRestResponse(request,
                                              buildRoutingResult(request, acceptTypeData.acceptHeaders),
                                              ex);

    checkResponse(response,
                  400,
                  2, true,
                  true,
                  errorResponseHeaderName);
    dataMap = response.getDataMap();

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
    RestLiResponse response;

    // #1 all fields
    RestRequest request1 = buildRequest("/test?fields=f1,f2,f3", acceptTypeData.acceptHeaders, protocolVersion);
    Status status = buildStatusWithFields("f1", "f2", "f3");
    response = buildPartialRestResponse(request1,
                                              buildRoutingResult(request1, acceptTypeData.acceptHeaders),
                                              status);

    checkResponse(response, 200, 1, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[] {"f1", "f2", "f3"}, new String[0]);

    // #2 some fields
    RestRequest request2 = buildRequest("/test?fields=f1,f3", acceptTypeData.acceptHeaders, protocolVersion);
    response = buildPartialRestResponse(request2,
                                              buildRoutingResult(request2, acceptTypeData.acceptHeaders),
                                              status);
    assertTrue(status.data().containsKey("f2"));

    checkResponse(response,
                  200,
                  1, true,
                  errorResponseHeaderName);
    checkProjectedFields(response, new String[]{"f1", "f3"}, new String[] {"f2"});

    // #3 no fields
    RestRequest request3 = buildRequest("/test?fields=", acceptTypeData.acceptHeaders, protocolVersion);
    response = buildPartialRestResponse(request3,
                                              buildRoutingResult(request3, acceptTypeData.acceptHeaders),
                                              status);

    checkResponse(response, 200, 1, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[]{}, new String[]{"f1", "f2", "f3"});
    assertTrue(status.data().containsKey("f1"));
    assertTrue(status.data().containsKey("f2"));
    assertTrue(status.data().containsKey("f3"));

    // #4 fields not in schema
    RestRequest request4 = buildRequest("/test?fields=f1,f99", acceptTypeData.acceptHeaders, protocolVersion);
    response = buildPartialRestResponse(request4,
                                              buildRoutingResult(request4, acceptTypeData.acceptHeaders),
                                              status);

    checkResponse(response, 200, 1, true, errorResponseHeaderName);
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
    RestLiResponse response;

    DataMap data = new DataMap(asMap(
            "f1", "value",
            "f2", new DataMap(asMap(
            "f3", "value",
            "f4", "value"
    ))));

    // #1 all fields
    Status status = new Status(data);

    RestRequest request1 = buildRequest("/test?fields=f1,f2:(f3,f4)", acceptTypeData.acceptHeaders, protocolVersion);
    response = buildPartialRestResponse(request1,
                                              buildRoutingResult(request1, acceptTypeData.acceptHeaders),
                                              status);

    checkResponse(response, 200, 1, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[] {"f1", "f2", "f3"}, new String[0]);

    // #2 some fields
    RestRequest request2 = buildRequest("/test?fields=f1,f2:(f3)", acceptTypeData.acceptHeaders, protocolVersion);
    response = buildPartialRestResponse(request2,
                                              buildRoutingResult(request2, acceptTypeData.acceptHeaders),
                                              status);
    assertTrue(status.data().containsKey("f2"));

    checkResponse(response, 200, 1, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[]{"f1", "f2", "f3"}, new String[] {"f4"});

    // #3 no fields
    RestRequest request3 = buildRequest("/test?fields=", acceptTypeData.acceptHeaders, protocolVersion);
    response = buildPartialRestResponse(request3,
                                              buildRoutingResult(request3, acceptTypeData.acceptHeaders),
                                              status);

    checkResponse(response, 200, 1, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[]{}, new String[]{"f1", "f2", "f3", "f4"});
    assertTrue(status.data().containsKey("f1"));
    assertTrue(status.data().containsKey("f2"));

    // #4 fields not in schema
    RestRequest request4 = buildRequest("/test?fields=f2:(f99)", acceptTypeData.acceptHeaders, protocolVersion);
    response = buildPartialRestResponse(request4,
                                              buildRoutingResult(request4, acceptTypeData.acceptHeaders),
                                              status);

    checkResponse(response, 200, 1, true, errorResponseHeaderName);
    checkProjectedFields(response, new String[]{"f2"}, new String[]{"f1", "f3", "f99"});
    assertTrue(status.data().containsKey("f2"));
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  public void testFieldProjection_collections_CollectionResult(AcceptTypeData acceptTypeData,
                                                               ProtocolVersion protocolVersion,
                                                               String errorResponseHeaderName)
          throws Exception
  {
    RestLiResponse response;

    BasicCollectionResult<Status> statusCollection = buildStatusCollectionResult(10, "f1", "f2",
                                                                                 "f3");
    RestRequest request = buildRequest("/test?fields=f1,f2", acceptTypeData.acceptHeaders, protocolVersion);
    response = buildPartialRestResponse(request,
                                              buildRoutingResultFinder(request, acceptTypeData.acceptHeaders),
                                              statusCollection);

    checkResponse(response, 200, 1, true, errorResponseHeaderName);

    DataMap dataMap = response.getDataMap();
    CollectionResponse<Status> collectionResponse = new CollectionResponse<>(dataMap, Status.class);
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
    RestLiResponse response;

    List<Status> statusCollection = buildStatusListResult(10, "f1", "f2", "f3");
    RestRequest request = buildRequest("/test?fields=f1,f2", acceptTypeData.acceptHeaders, protocolVersion);
    response = buildPartialRestResponse(request,
                                              buildRoutingResultFinder(request, acceptTypeData.acceptHeaders),
                                              statusCollection);

    checkResponse(response, 200, 1, true, errorResponseHeaderName);

    DataMap dataMap = response.getDataMap();
    CollectionResponse<Status> collectionResponse = new CollectionResponse<>(dataMap, Status.class);
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
    RestLiResponse response;

    Map<Integer, Status> statusBatch = buildStatusBatchResponse(10, "f1", "f2", "f3");
    RestRequest request = buildRequest(uri, acceptTypeData.acceptHeaders, protocolVersion);
    response = buildPartialRestResponse(request,
                                              buildRoutingResult(
                                                  ResourceMethod.BATCH_GET, request, acceptTypeData.acceptHeaders),
                                              statusBatch);

    checkResponse(response, 200, 1, true, errorResponseHeaderName);

    DataMap dataMap = response.getDataMap();
    BatchResponse<Status> batchResponse = new BatchResponse<>(dataMap, Status.class);
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
    ResourceMethodDescriptor methodDescriptor = resourceModel.findFinderMethod("search");
    ResourceContextImpl context = new ResourceContextImpl();
    context.setResponseHeader(testHeaderName, testHeaderValue);
    RestUtils.validateRequestHeadersAndUpdateResourceContext(acceptTypeData.acceptHeaders,
        Collections.emptySet(),
        context);
    RoutingResult routingResult = new RoutingResult(context, methodDescriptor);

    RestLiResponse response = buildPartialRestResponse(buildRequest(acceptTypeData.acceptHeaders, protocolVersion),
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
    RestLiResponse response;
    final Status status = buildStatusRecord();

    final GetResult<Status> getResult = new GetResult<>(status, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    response = invokeResponseHandler("/test", getResult, ResourceMethod.GET, acceptTypeData.acceptHeaders, protocolVersion);
    checkResponse(response,
                  HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode(),
                  1, true,
                  errorResponseHeaderName);
    if (acceptTypeData != AcceptTypeData.PSON)
    {
      assertEquals(DataMapUtils.mapToByteString(response.getDataMap()).asAvroString(), expectedStatus);
    }
    RestRequest req = buildRequest("/test", acceptTypeData.acceptHeaders, protocolVersion);
    RoutingResult routing = buildRoutingResult(ResourceMethod.GET, req, acceptTypeData.acceptHeaders);
    RestResponse restResponse = ResponseUtils.buildResponse(routing, response);
    assertEquals(restResponse.getEntity().asAvroString(), expectedStatus);

    final RestRequest request = buildRequest(acceptTypeData.acceptHeaders, protocolVersion);
    final ActionResult<Status> actionResult = new ActionResult<>(status, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    routing = buildRoutingResultAction(Status.class, request, acceptTypeData.acceptHeaders);
    response = buildPartialRestResponse(request, routing, actionResult);
    checkResponse(response,
                  HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode(),
                  1, true,
                  errorResponseHeaderName);
    if (acceptTypeData != AcceptTypeData.PSON)
    {
      assertEquals(DataMapUtils.mapToByteString(response.getDataMap()).asAvroString(), expectedActionStatus);
    }
    restResponse = ResponseUtils.buildResponse(routing, response);
    assertEquals(restResponse.getEntity().asAvroString(), expectedActionStatus);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  public void testSetResponseCookies(AcceptTypeData acceptTypeData, ProtocolVersion protocolVersion, String errorResponseHeaderName) throws Exception
  {
    String testHeaderName = "XXX";
    String testHeaderValue = "head";

    ResourceModel resourceModel = buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = resourceModel.findFinderMethod("search");
    ResourceContextImpl context = new ResourceContextImpl();
    context.setResponseHeader(testHeaderName, testHeaderValue);
    context.addResponseCookie(new HttpCookie("cook1", "value1"));
    context.addResponseCookie(new HttpCookie("cook2","value2"));
    RestUtils.validateRequestHeadersAndUpdateResourceContext(acceptTypeData.acceptHeaders,
        Collections.emptySet(),
        context);
    RoutingResult routingResult = new RoutingResult(context, methodDescriptor);

    RestLiResponse response = buildPartialRestResponse(buildRequest(acceptTypeData.acceptHeaders, protocolVersion), routingResult, buildStatusList(1)); // this is a valid response

    List<HttpCookie> cookies = Arrays.asList(new HttpCookie("cook1", "value1"), new HttpCookie("cook2", "value2"));
    Assert.assertEquals(response.getCookies(), cookies);

    response = buildPartialRestResponse(buildRequest(acceptTypeData.acceptHeaders, protocolVersion), routingResult,
                                                     new RestLiServiceException(HttpStatus.S_404_NOT_FOUND)); // this is an invalid response
    Assert.assertEquals(response.getCookies(), cookies);//but the cookie should still be valid
  }

  @Test
  public void testGetRequestCookies() throws URISyntaxException, RestLiSyntaxException
  {
    List<HttpCookie> cookies = Arrays.asList(new HttpCookie("cook1", "value1"), new HttpCookie("cook2", "value2"));
    RestRequest request = new RestRequestBuilder(new URI("http://www.abc.org/")).setMethod("DONT_CARE")
        .setHeaders(new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
        .setCookies(CookieUtil.encodeCookies(cookies)).build();
    ServerResourceContext resourceContext = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    Assert.assertEquals(resourceContext.getRequestCookies(), cookies );
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "basicData")
  @SuppressWarnings("unchecked")
  public void testBuildRestLiUnstructuredDataResponse(AcceptTypeData acceptTypeData,
                                          ProtocolVersion protocolVersion,
                                          String errorResponseHeaderName)
    throws Exception
  {
    final RestRequest request = buildRequest(Collections.EMPTY_MAP, protocolVersion);
    RoutingResult routingResult = buildUnstructuredDataRoutingResult(request);

    RestLiResponseData<GetResponseEnvelope> responseData = (RestLiResponseData<GetResponseEnvelope>) _responseHandler.buildRestLiResponseData(request, routingResult, null);
    assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_200_OK);
    assertEquals(responseData.getResponseEnvelope().getRecord(), new EmptyRecord());

    RestLiResponse restResponse = buildPartialRestResponse(request, routingResult, null);
    assertNotNull(restResponse);
  }

  // *****************
  // Helper methods
  // *****************

  private RestRequest buildRequest(Map<String, String> headers, ProtocolVersion protocolVersion) throws URISyntaxException
  {
    return buildRequest("/test", headers, protocolVersion);
  }

  private RestRequest buildRequest(String uri, Map<String, String> headers, ProtocolVersion protocolVersion) throws URISyntaxException
  {
    return new RestRequestBuilder(new URI(uri)).setMethod("DONT_CARE").setHeaders(headers).setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString()).build();
  }

  /**
   * Creates a RoutingResult for an Action with the given returnType.
   *
   * @param actionReturnType the return type of the action.
   * @return a RoutingResult
   */
  private RoutingResult buildRoutingResultAction(Class<?> actionReturnType, RestRequest request, Map<String, String> headers)
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
    RestUtils.validateRequestHeadersAndUpdateResourceContext(headers, Collections.emptySet(), resourceContext);
    return new RoutingResult(resourceContext, methodDescriptor);
  }

  private RoutingResult buildRoutingResult(RestRequest request, Map<String, String> acceptHeaders)
          throws SecurityException, NoSuchMethodException, RestLiSyntaxException
  {
    return buildRoutingResult(ResourceMethod.GET, request, acceptHeaders);
  }

  private RoutingResult buildRoutingResult(ResourceMethod resourceMethod, RestRequest request, Map<String, String> acceptHeaders)
      throws SecurityException, NoSuchMethodException, RestLiSyntaxException
  {
    return buildRoutingResult(resourceMethod, request, acceptHeaders, Collections.emptySet());
  }

  private RoutingResult buildRoutingResult(
      ResourceMethod resourceMethod, RestRequest request, Map<String, String> acceptHeaders, Set<String> customTypes)
          throws SecurityException, NoSuchMethodException, RestLiSyntaxException
  {
    Method method = ProjectionTestFixture.class.getMethod("batchGet", Set.class);
    ResourceModel model = RestLiTestHelper.buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor =
        ResourceMethodDescriptor.createForRestful(resourceMethod, method, InterfaceType.SYNC);
    model.addResourceMethodDescriptor(methodDescriptor);
    ServerResourceContext context =  new ResourceContextImpl(new PathKeysImpl(), request,
                            new RequestContext());
    RestUtils.validateRequestHeadersAndUpdateResourceContext(acceptHeaders, customTypes, context);
    return new RoutingResult(context, methodDescriptor);
  }

  private RoutingResult buildUnstructuredDataRoutingResult(RestRequest request)
          throws SecurityException, NoSuchMethodException, RestLiSyntaxException
  {
    Method method = FeedDownloadResource.class.getMethod("get", Long.class, UnstructuredDataWriter.class);
    ResourceModel model = RestLiTestHelper.buildResourceModel(FeedDownloadResource.class);
    ResourceMethodDescriptor methodDescriptor =
        ResourceMethodDescriptor.createForRestful(ResourceMethod.GET, method, InterfaceType.SYNC);
    model.addResourceMethodDescriptor(methodDescriptor);
    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    return new RoutingResult(context, methodDescriptor);
  }


  private RoutingResult buildRoutingResultFinder(RestRequest request, Map<String, String> acceptHeaders) throws SecurityException,
      NoSuchMethodException,
      RestLiSyntaxException
  {
    Method method = ProjectionTestFixture.class.getMethod("find");
    ResourceModel model = RestLiTestHelper.buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor =
        ResourceMethodDescriptor.createForRestful(ResourceMethod.FINDER, method, InterfaceType.SYNC);
    model.addResourceMethodDescriptor(methodDescriptor);
    ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), request, new RequestContext());
    RestUtils.validateRequestHeadersAndUpdateResourceContext(acceptHeaders, Collections.emptySet(), context);
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

  private void checkResponse(RestLiResponse response,
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

  private void checkResponseData(RestLiResponseData<ActionResponseEnvelope> responseData, HttpStatus status, int numHeaders,
                                 boolean hasError, boolean hasEntity, String errorResponseHeaderName)
  {
    assertEquals(responseData.getResponseEnvelope().getStatus(), status);
    assertEquals(responseData.getHeaders().size(), numHeaders);
    if (hasError)
    {
      assertEquals(responseData.getHeaders().get(errorResponseHeaderName), RestConstants.HEADER_VALUE_ERROR);
    }
    else
    {
      assertNull(responseData.getHeaders().get(errorResponseHeaderName));
    }

    assertEquals(responseData.getResponseEnvelope().getRecord() != null, hasEntity);
  }

  private void checkResponse(RestLiResponse response,
      int status,
      int numHeaders,
      boolean hasEntity,
      String errorResponseHeaderName)
  {
    checkResponse(response, status, numHeaders, false, hasEntity, errorResponseHeaderName);
  }

  private void checkResponse(RestLiResponse response,
      int status,
      int numHeaders,
      boolean hasError,
      boolean hasEntity,
      String errorResponseHeaderName)
  {
    assertEquals(response.getStatus().getCode(), status);
    assertEquals(response.getHeaders().size(), numHeaders);

    if (hasError)
    {
      assertEquals(response.getHeader(errorResponseHeaderName), RestConstants.HEADER_VALUE_ERROR);
    }
    else
    {
      assertNull(response.getHeader(errorResponseHeaderName));
    }

    assertEquals(response.getDataMap() != null, hasEntity);
  }

  private List<Status> buildStatusList(int num)
  {
    List<Status> list = new ArrayList<>();
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

    return new BasicCollectionResult<>(data, numResults);
  }

  private List<Status> buildStatusListResult(int numResults, String... fields)
  {
    List<Status> data = new ArrayList<>();

    for (int i = 0; i < numResults; i++)
    {
      Status status = buildStatusWithFields(fields);
      data.add(status);
    }
    return data;
  }


  private Map<Integer, Status> buildStatusBatchResponse(int numResults, String... fields)
  {
    Map<Integer, Status> map = new HashMap<>();

    for (int i = 0; i < numResults; i++)
    {
      Status status = buildStatusWithFields(fields);
      map.put(i, status);
    }

    return map;
  }

  private void checkCollectionResponse(RestLiResponse response,
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
    DataMap dataMap = response.getDataMap();
    CollectionResponse<Status> collectionResponse = new CollectionResponse<>(dataMap, Status.class);

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

  private static void checkProjectedFields(RestLiResponse response, String[] expectedFields, String[] missingFields)
          throws UnsupportedEncodingException
  {
    DataMap dataMap = response.getDataMap();

    for (String field : expectedFields)
    {
      assertTrue(containsField(dataMap, field));
    }
    for (String field : missingFields)
    {
      assertFalse(containsField(dataMap, field));
    }
  }

  private static boolean containsField(DataMap data, String field)
  {
    for (String key : data.keySet())
    {
      if (key.equals(field))
      {
        return true;
      }

      Object value = data.get(key);
      if (value instanceof DataMap && containsField((DataMap) value, field))
      {
        return true;
      }
    }
    return false;

  }

  @SuppressWarnings("unchecked")
  static public <V> Map<String, V> asMap(Object... objects)
  {
    int index = 0;
    String key = null;
    HashMap<String,V> map = new HashMap<>();
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

  private RestLiResponse buildPartialRestResponse(final Request request,
      final RoutingResult routingResult,
      final Object responseObject) throws IOException
  {
    return _responseHandler.buildPartialResponse(routingResult,
        _responseHandler.buildRestLiResponseData(request, routingResult, responseObject));
  }

}
