/*
   Copyright (c) 2018 LinkedIn Corp.

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
import com.linkedin.common.callback.Callbacks;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.URIMapper;
import com.linkedin.d2.balancer.util.URIKeyPair;
import com.linkedin.d2.balancer.util.URIMappingResult;
import com.linkedin.data.DataMap;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.client.ResponseDecoderUtil;
import com.linkedin.restli.internal.client.ResponseImpl;
import com.linkedin.restli.internal.client.response.BatchEntityResponse;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;


import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.net.URI;
import java.util.*;

import static org.mockito.Mockito.*;

/**
 * Unit tests for DefaultScatterGatherStrategy for batch requests.
 */
public class TestDefaultScatterGatherStrategy
{
  private static final String TARGET_HOST_KEY_NAME = "D2-KeyMapper-TargetHost";
  private static final String TEST_SERVICE = "testService";
  private static final String TEST_URI = "test";
  private static final ResourceSpec _COLL_SPEC      =
          new ResourceSpecImpl(EnumSet.allOf(ResourceMethod.class),
                  Collections.emptyMap(),
                  Collections.emptyMap(),
                  Long.class,
                  null,
                  null,
                  TestRecord.class,
                  Collections.emptyMap());

  // sample batch requests
  private static BatchGetRequest<TestRecord> _batchGetRequest;
  private static BatchGetEntityRequest<Long, TestRecord> _batchGetEntityRequest;
  private static BatchDeleteRequest<Long, TestRecord> _batchDeleteRequest;
  private static BatchUpdateRequest<Long, TestRecord> _batchUpdateRequest;
  private static BatchPartialUpdateRequest<Long, TestRecord> _batchPartialUpdateRequest;
  private static List<URIKeyPair<Long>> _batchToUris;
  private static Map<URI, Set<Long>> _mappedKeys;
  private static Map<Integer, Set<Long>> _unmappedKeys;
  private static URI _host1URI;
  private static URI _host2URI;

  private URIMapper _uriMapper;
  private DefaultScatterGatherStrategy _sgStrategy;

  @BeforeClass
  public void setup()
  {
    _uriMapper = mock(URIMapper.class);
    _sgStrategy = new DefaultScatterGatherStrategy(_uriMapper);
    _batchGetRequest = createBatchGetRequest(1L, 2L, 3L, 4L);
    _batchGetEntityRequest = createBatchGetEntityRequest(1L, 2L, 3L, 4L);
    _batchDeleteRequest = createBatchDeleteRequest(1L, 2L, 3L, 4L);
    _batchUpdateRequest = createBatchUpdateRequest(1L, 2L, 3L, 4L);
    _batchPartialUpdateRequest = createBatchPartialUpdateRequest(1L, 2L, 3L, 4L);
    // batch to individual URIs
    _batchToUris = new ArrayList<>();
    _batchToUris.add(new URIKeyPair<>(1L,  URI.create("d2://" + TEST_URI + "/1?foo=bar")));
    _batchToUris.add(new URIKeyPair<>(2L,  URI.create("d2://" + TEST_URI + "/2?foo=bar")));
    _batchToUris.add(new URIKeyPair<>(3L,  URI.create("d2://" + TEST_URI + "/3?foo=bar")));
    _batchToUris.add(new URIKeyPair<>(4L,  URI.create("d2://" + TEST_URI + "/4?foo=bar")));
    // D2 mapped keys
    _host1URI = URI.create("http://host1:8080/");
    _host2URI = URI.create("http://host2:8080/");
    _mappedKeys = new HashMap<>();
    _mappedKeys.put(_host1URI, new HashSet<>(Arrays.asList(1L, 2L)));
    _mappedKeys.put(_host2URI, new HashSet<>(Arrays.asList(3L)));
    _unmappedKeys = Collections.singletonMap(0, Collections.singleton(4L));
  }

  @DataProvider
  private static Object[][] requestMethodProvider()
  {
    return new Object[][] {
            { ResourceMethod.GET, false},
            { ResourceMethod.FINDER, false},
            { ResourceMethod.GET_ALL, false},
            { ResourceMethod.DELETE, false},
            { ResourceMethod.UPDATE, false},
            { ResourceMethod.PARTIAL_UPDATE, false},
            { ResourceMethod.ACTION, false},
            { ResourceMethod.BATCH_GET, true},
            { ResourceMethod.BATCH_DELETE, true},
            { ResourceMethod.BATCH_UPDATE, true},
            { ResourceMethod.BATCH_PARTIAL_UPDATE, true},
            { ResourceMethod.BATCH_CREATE, false}
    };
  }

  @Test(dataProvider = "requestMethodProvider")
  public void testIsScatterGatherNeeded(ResourceMethod requestMethod, boolean sgNeeded) throws ServiceUnavailableException
  {
    Request<?> request = mock(Request.class);
    when(request.getServiceName()).thenReturn(TEST_SERVICE);
    // service is not supporting scatter gather
    when(_uriMapper.needScatterGather(TEST_SERVICE)).thenReturn(false);
    Assert.assertFalse(_sgStrategy.needScatterGather(request));
    // resource method is not supported
    when(_uriMapper.needScatterGather(TEST_SERVICE)).thenReturn(true);
    when(request.getMethod()).thenReturn(requestMethod);
    Assert.assertEquals(_sgStrategy.needScatterGather(request), sgNeeded);
  }

  @DataProvider
  private static Object[][] illegalRequestMethodProvider()
  {
    return new Object[][] {
            { ResourceMethod.GET},
            { ResourceMethod.FINDER},
            { ResourceMethod.GET_ALL},
            { ResourceMethod.DELETE},
            { ResourceMethod.UPDATE},
            { ResourceMethod.PARTIAL_UPDATE},
            { ResourceMethod.ACTION},
            { ResourceMethod.BATCH_CREATE}
    };
  }


  @Test(dataProvider="illegalRequestMethodProvider", expectedExceptions = {UnsupportedOperationException.class})
  public void testUnsupportedRequestGetUris(ResourceMethod requestMethod)
  {
    Request<?> request = mock(Request.class);
    when(request.getMethod()).thenReturn(requestMethod);
    _sgStrategy.getUris(request, AllProtocolVersions.LATEST_PROTOCOL_VERSION);
  }

  @Test(expectedExceptions = {IllegalArgumentException.class})
  public void testUnsupportedRequestScatter()
  {
    Request<?> request = mock(Request.class);
    when(request.getMethod()).thenReturn(ResourceMethod.BATCH_CREATE);
    _sgStrategy.scatterRequest(request, new RequestContext(), Collections.emptyMap());
  }

  @Test(dataProvider="illegalRequestMethodProvider", expectedExceptions = {UnsupportedOperationException.class})
  public void testUnsupportedRequestOnCompletion(ResourceMethod requestMethod)
  {
    Request<?> request = mock(Request.class);
    when(request.getMethod()).thenReturn(requestMethod);
    _sgStrategy.onAllResponsesReceived(request, AllProtocolVersions.LATEST_PROTOCOL_VERSION, Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyMap(), Callbacks.empty());
  }


  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestUris")
  private static Object[][] batchRequestToUris()
  {
    return new Object[][] {
            { _batchGetRequest, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), _batchToUris},
            { _batchGetEntityRequest, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), _batchToUris},
            { _batchDeleteRequest, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), _batchToUris},
            { _batchUpdateRequest, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), _batchToUris},
            { _batchPartialUpdateRequest, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), _batchToUris},
            { _batchGetRequest, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), _batchToUris},
            { _batchGetEntityRequest, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), _batchToUris},
            { _batchDeleteRequest, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), _batchToUris},
            { _batchUpdateRequest, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), _batchToUris},
            { _batchPartialUpdateRequest, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), _batchToUris}
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestUris")
  @SuppressWarnings("unchecked")
  public void testGetUris(Request<?> request, ProtocolVersion version, List<URIKeyPair<Long>> expectedUris)
  {
    List<URIKeyPair<Long>> uris = _sgStrategy.getUris(request, version);
    Assert.assertNotNull(uris);
    Assert.assertEquals(uris.size(), 4);
    Assert.assertTrue(uris.containsAll(expectedUris));
  }

  @Test
  public void testMapUris() throws ServiceUnavailableException
  {
    URIMappingResult<Long> expectedMappingResult = new URIMappingResult<>(_mappedKeys, _unmappedKeys);
    when(_uriMapper.mapUris(_batchToUris)).thenReturn(expectedMappingResult);
    URIMappingResult<Long> mappingResult = _sgStrategy.mapUris(_batchToUris);
    Assert.assertEquals(mappingResult, expectedMappingResult);
  }

  @DataProvider(name = "scatterBatchRequestProvider")
  private static Object[][] scatterBatchRequestProvider()
  {
    return new Object[][] {
            { _batchGetRequest, createBatchGetRequest(1L, 2L), _host1URI,
                    createBatchGetRequest(3L), _host2URI},
            { _batchGetEntityRequest, createBatchGetEntityRequest(1L, 2L), _host1URI,
                    createBatchGetEntityRequest(3L), _host2URI},
            { _batchDeleteRequest, createBatchDeleteRequest(1L, 2L), _host1URI,
                    createBatchDeleteRequest(3L), _host2URI},
            { _batchUpdateRequest, createBatchUpdateRequest(1L, 2L), _host1URI,
                    createBatchUpdateRequest(3L), _host2URI},
            { _batchPartialUpdateRequest, createBatchPartialUpdateRequest(1L, 2L), _host1URI,
                    createBatchPartialUpdateRequest(3L), _host2URI},
    };
  }

  @Test(dataProvider = "scatterBatchRequestProvider")
  public void testScatterRequest(Request<?> request,
                                 Request<?> firstRequest,
                                 URI firstHost,
                                 Request<?> secondRequest,
                                 URI secondHost)
  {
    RequestContext requestContext = new RequestContext();
    List<RequestInfo> scatteredRequests = _sgStrategy.scatterRequest(request, requestContext, _mappedKeys);
    Assert.assertNotNull(scatteredRequests);
    Assert.assertEquals(scatteredRequests.size(), 2);
    for (RequestInfo req : scatteredRequests)
    {
      RequestContext context = req.getRequestContext();
      Assert.assertNotNull(context.getLocalAttr(TARGET_HOST_KEY_NAME));
      if (context.getLocalAttr(TARGET_HOST_KEY_NAME).equals(firstHost))
      {
        Assert.assertEquals(req.getRequest(), firstRequest);
      }
      else if (context.getLocalAttr(TARGET_HOST_KEY_NAME).equals(secondHost))
      {
        Assert.assertEquals(req.getRequest(), secondRequest);
      }
      else
      {
        Assert.fail("Scattered request should have " + TARGET_HOST_KEY_NAME + " set in request context!");
      }
    }
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocol")
  private static Object[][] protocolVersions()
  {
    return new Object[][]{
            {AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()},
            {AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()}
    };
  }


  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocol")
  public void testGatherBatchResponse(ProtocolVersion version)
  {
    Map<RequestInfo, Response<BatchResponse<TestRecord>>> successResponses = new HashMap<>();
    successResponses.put(
            new RequestInfo(createBatchGetRequest(1L, 2L), getTargetHostRequestContext(_host1URI)),
            createBatchResponse(Collections.singleton(1L), Collections.singleton(2L)));
    Map<RequestInfo, Throwable> failResponses = new HashMap<>();
    failResponses.put(
            new RequestInfo(createBatchGetRequest(3L), getTargetHostRequestContext(_host2URI)),
            new RestLiScatterGatherException("Partition host is unavailable!"));
    Callback<Response<BatchResponse<TestRecord>>> testCallback = new Callback<Response<BatchResponse<TestRecord>>>()
    {
      @Override
      public void onError(Throwable e)
      {

      }

      @Override
      public void onSuccess(Response<BatchResponse<TestRecord>> result)
      {
        Assert.assertNotNull(result.getEntity());
        Assert.assertEquals(result.getStatus(), HttpStatus.S_200_OK.getCode());
        Assert.assertTrue(result.getEntity().getResults().size() == 1);
        Assert.assertTrue(result.getEntity().getResults().containsKey("1"));
        Assert.assertTrue(result.getEntity().getErrors().size() == 3);
        ErrorResponse keyError = result.getEntity().getErrors().get("2");
        Assert.assertEquals(keyError.getStatus().intValue(), HttpStatus.S_404_NOT_FOUND.getCode());
        ErrorResponse failError = result.getEntity().getErrors().get("3");
        Assert.assertEquals(failError.getExceptionClass(), RestLiScatterGatherException.class.getName());
        Assert.assertEquals(failError.getMessage(), "Partition host is unavailable!");
        ErrorResponse unmappedError = result.getEntity().getErrors().get("4");
        Assert.assertEquals(unmappedError.getExceptionClass(), RestLiScatterGatherException.class.getName());
        Assert.assertEquals(unmappedError.getMessage(), "Unable to find a host for keys :[4]");
      }
    };
    _sgStrategy.onAllResponsesReceived(_batchGetRequest, version, successResponses, failResponses, _unmappedKeys, testCallback);
  }


  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "gatherBatchResponseProvider")
  private static Object[][] gatherBatchResponseProvider()
  {
    ProtocolVersion v1 = AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion();
    ProtocolVersion v2 = AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion();
    Set<Long> resultKeys = Collections.singleton(1L);
    Set<Long> errorKeys = Collections.singleton(2L);
    return new Object[][] {
            { _batchGetEntityRequest, v1, createBatchGetEntityRequest(1L, 2L), _host1URI,
                    createBatchEntityResponse(v1, resultKeys, errorKeys), createBatchGetEntityRequest(3L), _host2URI, 1},
            { _batchDeleteRequest, v1, createBatchDeleteRequest(1L, 2L), _host1URI,
                    createBatchKVResponse(v1, resultKeys, errorKeys), createBatchDeleteRequest(3L), _host2URI, 4},
            { _batchUpdateRequest, v1, createBatchUpdateRequest(1L, 2L), _host1URI,
                    createBatchKVResponse(v1, resultKeys, errorKeys), createBatchUpdateRequest(3L), _host2URI, 4},
            { _batchPartialUpdateRequest, v1, createBatchPartialUpdateRequest(1L), _host1URI,
                    createBatchKVResponse(v1, resultKeys, errorKeys), createBatchPartialUpdateRequest(3L), _host2URI, 4},
            { _batchGetEntityRequest, v2, createBatchGetEntityRequest(1L, 2L), _host1URI,
                    createBatchEntityResponse(v1, resultKeys, errorKeys), createBatchGetEntityRequest(3L), _host2URI, 1},
            { _batchDeleteRequest, v2, createBatchDeleteRequest(1L, 2L), _host1URI,
                    createBatchKVResponse(v2, resultKeys, errorKeys), createBatchDeleteRequest(3L), _host2URI, 4},
            { _batchUpdateRequest, v2, createBatchUpdateRequest(1L, 2L), _host1URI,
                    createBatchKVResponse(v2, resultKeys, errorKeys), createBatchUpdateRequest(3L), _host2URI, 4},
            { _batchPartialUpdateRequest, v2, createBatchPartialUpdateRequest(1L, 2L), _host1URI,
                    createBatchKVResponse(v2, resultKeys, errorKeys), createBatchPartialUpdateRequest(3L), _host2URI, 4},
    };
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "gatherBatchResponseProvider")
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testGatherBatchKVResponse(Request request,
                                        ProtocolVersion version,
                                        Request successRequest,
                                        URI successHost,
                                        Response successResponse,
                                        Request<?> failRequest,
                                        URI failHost,
                                        int resultDataMapSize)
  {
    Map<RequestInfo, Response<BatchKVResponse<Long, ?>>> successResponses = new HashMap<>();
    successResponses.put(
            new RequestInfo(successRequest, getTargetHostRequestContext(successHost)),
            successResponse);
    Map<RequestInfo, Throwable> failResponses = new HashMap<>();
    failResponses.put(
            new RequestInfo(failRequest, getTargetHostRequestContext(failHost)),
            new RestLiScatterGatherException("Partition host is unavailable!"));
    Callback<Response<BatchKVResponse<Long, ?>>> testCallback =
            new Callback<Response<BatchKVResponse<Long, ?>>> ()
            {
              @Override
              public void onError(Throwable e)
              {

              }

              @Override
              public void onSuccess(Response<BatchKVResponse<Long, ?>> result)
              {
                Assert.assertNotNull(result.getEntity());
                Assert.assertEquals(result.getStatus(), HttpStatus.S_200_OK.getCode());
                Assert.assertTrue(result.getEntity().data().getDataMap(BatchResponse.RESULTS).size() == resultDataMapSize);
                // BatchKVResponse.getResults() contains all entries including both successful and failed ones.
                Assert.assertTrue(result.getEntity().getResults().size() == 4);
                Assert.assertTrue(result.getEntity().getResults().containsKey(1L));
                // merged error can come from 3 cases:
                // - errored keys in successfully returned scattered batch response
                // - RemoteInvocationException from a scattered request
                // - unmapped keys
                Assert.assertTrue(result.getEntity().data().getDataMap(BatchResponse.ERRORS).size() == 3);
                Assert.assertTrue(result.getEntity().getErrors().size() == 3);
                ErrorResponse keyError = result.getEntity().getErrors().get(2L);
                Assert.assertEquals(keyError.getStatus().intValue(), HttpStatus.S_404_NOT_FOUND.getCode());
                ErrorResponse failError = result.getEntity().getErrors().get(3L);
                Assert.assertEquals(failError.getExceptionClass(), RestLiScatterGatherException.class.getName());
                Assert.assertEquals(failError.getMessage(), "Partition host is unavailable!");
                ErrorResponse unmappedError = result.getEntity().getErrors().get(4L);
                Assert.assertEquals(unmappedError.getExceptionClass(), RestLiScatterGatherException.class.getName());
                Assert.assertEquals(unmappedError.getMessage(), "Unable to find a host for keys :[4]");
              }
            };
    _sgStrategy.onAllResponsesReceived(request, version, successResponses, failResponses, _unmappedKeys, testCallback);
  }

  private static BatchGetRequest<TestRecord> createBatchGetRequest(Long... ids)
  {
    BatchGetRequestBuilder<Long, TestRecord> builder =
            new BatchGetRequestBuilder<>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    return builder.ids(ids).setParam("foo", "bar").addHeader("a", "b").build();
  }

  private static BatchGetEntityRequest<Long, TestRecord> createBatchGetEntityRequest(Long... ids)
  {
    BatchGetEntityRequestBuilder<Long, TestRecord> builder =
            new BatchGetEntityRequestBuilder<>(TEST_URI, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    return builder.ids(ids).setParam("foo", "bar").addHeader("a", "b").build();
  }

  private static BatchDeleteRequest<Long, TestRecord> createBatchDeleteRequest(Long... ids)
  {
    BatchDeleteRequestBuilder<Long, TestRecord> builder =
            new BatchDeleteRequestBuilder<>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    return builder.ids(ids).setParam("foo", "bar").addHeader("a", "b").build();
  }

  private static BatchUpdateRequest<Long, TestRecord> createBatchUpdateRequest(Long... ids)
  {
    BatchUpdateRequestBuilder<Long, TestRecord> builder =
            new BatchUpdateRequestBuilder<>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    Map<Long, TestRecord> updates = new HashMap<>();
    for (Long id: ids)
    {
      updates.put(id, new TestRecord());
    }
    return builder.inputs(updates).setParam("foo", "bar").addHeader("a", "b").build();
  }

  private static BatchPartialUpdateRequest<Long, TestRecord> createBatchPartialUpdateRequest(Long... ids)
  {
    BatchPartialUpdateRequestBuilder<Long, TestRecord> builder =
            new BatchPartialUpdateRequestBuilder<>(TEST_URI, TestRecord.class, _COLL_SPEC, RestliRequestOptions.DEFAULT_OPTIONS);
    for (Long id: ids)
    {
      builder.input(id, new PatchRequest<>());
    }
    return builder.setParam("foo", "bar").addHeader("a", "b").build();
  }


  private static Response<BatchResponse<TestRecord>> createBatchResponse(Set<Long> resultKeys, Set<Long> errorKeys)
  {
    BatchResponse<TestRecord> response = new BatchResponse<>(new DataMap(), TestRecord.class);
    for (Long id: resultKeys)
    {
      response.getResults().put(id.toString(), new TestRecord().setId(id));
    }
    for (Long id: errorKeys)
    {
      response.getErrors().put(id.toString(),
              new ErrorResponse().setStatus(HttpStatus.S_404_NOT_FOUND.getCode()));
    }

    return new ResponseImpl<>(HttpStatus.S_200_OK.getCode(),
            Collections.emptyMap(), Collections.emptyList(), response, null);
  }


  private static Response<BatchKVResponse<Long, EntityResponse<TestRecord>>>
  createBatchEntityResponse(ProtocolVersion version, Set<Long> resultKeys, Set<Long> errorKeys)
  {
    DataMap resultMap = new DataMap();
    for (Long id: resultKeys)
    {
      resultMap.put(id.toString(), new EntityResponse<>(TestRecord.class)
                      .setEntity(new TestRecord().setId(id))
                      .setStatus(HttpStatus.S_200_OK).data());
    }
    DataMap errorMap = new DataMap();
    for (Long id: errorKeys)
    {
      errorMap.put(id.toString(),
              new ErrorResponse().setStatus(HttpStatus.S_404_NOT_FOUND.getCode()).data());
    }
    DataMap responseMap = new DataMap();
    responseMap.put(BatchResponse.RESULTS, resultMap);
    responseMap.put(BatchResponse.ERRORS, errorMap);
    BatchEntityResponse<Long, TestRecord> response = new BatchEntityResponse<>(responseMap,
            new TypeSpec<>(Long.class),
            new TypeSpec<>(TestRecord.class),
            Collections.emptyMap(),
            null,
            version);
    return new ResponseImpl<>(HttpStatus.S_200_OK.getCode(),
            Collections.emptyMap(), Collections.emptyList(), response, null);
  }

  private static Response<BatchKVResponse<Long, UpdateStatus>> createBatchKVResponse(ProtocolVersion version,
                                                                                     Set<Long> resultKeys,
                                                                                     Set<Long> errorKeys)
  {
    DataMap resultMap = new DataMap();
    DataMap errorMap = new DataMap();

    for (Long id: resultKeys)
    {
      resultMap.put(id.toString(), new UpdateStatus().setStatus(HttpStatus.S_200_OK.getCode()).data());
    }
    for (Long id: errorKeys)
    {
      ErrorResponse err = new ErrorResponse().setStatus(HttpStatus.S_404_NOT_FOUND.getCode());
      errorMap.put(id.toString(), err.data());
    }
    DataMap responseMap = new DataMap();
    responseMap.put(BatchResponse.RESULTS, resultMap);
    responseMap.put(BatchResponse.ERRORS, errorMap);
    DataMap mergedMap = ResponseDecoderUtil.mergeUpdateStatusResponseData(responseMap);
    BatchKVResponse<Long, UpdateStatus> response = new BatchKVResponse<>(mergedMap,
            new TypeSpec<>(Long.class),
            new TypeSpec<>(UpdateStatus.class),
            Collections.emptyMap(),
            version);
    return new ResponseImpl<>(HttpStatus.S_200_OK.getCode(),
            Collections.emptyMap(), Collections.emptyList(), response, null);
  }

  private RequestContext getTargetHostRequestContext(URI host)
  {
    RequestContext context = new RequestContext();
    context.putLocalAttr(TARGET_HOST_KEY_NAME, host);
    return context;
  }
}

