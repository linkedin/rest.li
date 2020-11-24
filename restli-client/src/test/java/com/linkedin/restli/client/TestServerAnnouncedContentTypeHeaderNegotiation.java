/*
   Copyright (c) 2020 LinkedIn Corp.

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

import com.google.common.collect.ImmutableList;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.mockito.Mockito;
import com.linkedin.common.callback.Callback;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;


/**
 * Tests server announced content type header negotiation between the client and the server.
 */
public class TestServerAnnouncedContentTypeHeaderNegotiation
{
  private static final String TEST_URI_PREFIX = "http://localhost:1338/";
  private static final String TEST_SERVICE_NAME = "serviceName";

  @DataProvider(name = "successCases")
  public Object[][] getSuccessCases()
  {
    return new Object[][] {
        {new HashMap<>(), null},
        {Collections.singletonMap(
            RestConstants.RESTLI_SERVER_ANNOUNCED_CONTENT_TYPES_PROPERTY, ImmutableList.of("testType1", "testType2")
        ), ImmutableList.of("testType1", "testType2")}
    };
  }

  @Test(dataProvider = "successCases")
  public void testGetServerAnnouncedContentTypeHeadersErrorCases(Map<String, Object> properties,
      List<String> expectedServerAnnouncedContentTypeHeaders)
  {
    Assert.assertEquals(RestClient.getServerAnnouncedContentTypeHeaders(properties), expectedServerAnnouncedContentTypeHeaders);
  }

  @DataProvider(name = "errorCases")
  public Object[][] getErrorCases()
  {
    return new Object[][] {
        {null},
        {Collections.singletonMap(RestConstants.RESTLI_SERVER_ANNOUNCED_CONTENT_TYPES_PROPERTY, "BadString")},
        {Collections.singletonMap(RestConstants.RESTLI_SERVER_ANNOUNCED_CONTENT_TYPES_PROPERTY, ImmutableList.of("testType", 100))}
    };
  }

  @Test(dataProvider = "errorCases", expectedExceptions = RuntimeException.class)
  public void testGetServerAnnouncedContentTypeHeadersErrorCases(Map<String, Object> properties)
  {
    RestClient.getServerAnnouncedContentTypeHeaders(properties);
  }

  @Test
  public void testServiceMetadataCacheBehavior() {
    com.linkedin.r2.transport.common.Client mockClient = Mockito.mock(com.linkedin.r2.transport.common.Client.class);
    Request<?> mockRequest = Mockito.mock(Request.class);
    RestliRequestOptions mockRequestOptions = Mockito.mock(RestliRequestOptions.class);
    RequestContext mockRequestContext = Mockito.mock(RequestContext.class);

    final RestClient restClient = new RestClient(mockClient, TEST_URI_PREFIX);
    Mockito.when(mockRequest.getRequestOptions()).thenReturn(mockRequestOptions);
    Mockito.when(mockRequestOptions.getProtocolVersionOption()).thenReturn(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE);
    Mockito.when(mockRequest.getServiceName()).thenReturn(TEST_SERVICE_NAME);
    doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      Callback<Map<String, Object>> metadataCallback = (Callback<Map<String, Object>>) invocation.getArguments()[1];
      Map<String, Object> map = new HashMap<>();
      map.put(RestConstants.RESTLI_SERVER_ANNOUNCED_CONTENT_TYPES_PROPERTY,
          Collections.singletonList(ContentType.PROTOBUF2.getHeaderKey()));
      metadataCallback.onSuccess(map);
      return null;
    }).when(mockClient).getMetadata(any(), any());

    @SuppressWarnings("unchecked")
    final Callback<Pair<ProtocolVersion, List<String>>> mockCallback = Mockito.mock(Callback.class);
    // make multiple requests to test the cache behavior
    restClient.getServiceMetadata(mockRequest, mockRequestContext, mockCallback);
    restClient.getServiceMetadata(mockRequest, mockRequestContext, mockCallback);
    restClient.getServiceMetadata(mockRequest, mockRequestContext, mockCallback);
    // verify getMetadata is invoked only once. second request MUST be served from the cache.
    Mockito.verify(mockClient, times(1)).getMetadata(any(), any());
    // verify the same protocol version and ServerAnnounced content types is returned all 3 times.
    Mockito.verify(mockCallback, times(3)).onSuccess(
        Pair.of(AllProtocolVersions.BASELINE_PROTOCOL_VERSION, ImmutableList.of(ContentType.PROTOBUF2.getHeaderKey())));
  }

  @Test
  public void testServiceMetadataCacheBehaviorOnError() throws Exception {
    com.linkedin.r2.transport.common.Client mockClient = Mockito.mock(com.linkedin.r2.transport.common.Client.class);
    Request<?> mockRequest = Mockito.mock(Request.class);
    RestliRequestOptions mockRequestOptions = Mockito.mock(RestliRequestOptions.class);
    RequestContext mockRequestContext = Mockito.mock(RequestContext.class);

    final RestClient restClient = new RestClient(mockClient, TEST_URI_PREFIX);
    Mockito.when(mockRequest.getRequestOptions()).thenReturn(mockRequestOptions);
    Mockito.when(mockRequestOptions.getProtocolVersionOption()).thenReturn(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE);
    Mockito.when(mockRequest.getServiceName()).thenReturn(TEST_SERVICE_NAME);
    doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      Callback<Map<String, Object>> metadataCallback = (Callback<Map<String, Object>>) invocation.getArguments()[1];
      // throw exception to test the error scenario.
      metadataCallback.onError(new RuntimeException("TEST"));
      return null;
    }).when(mockClient).getMetadata(any(), any());

    @SuppressWarnings("unchecked")
    final Callback<Pair<ProtocolVersion, List<String>>> mockCallback = Mockito.mock(Callback.class);
    // make multiple requests to test the cache behavior
    restClient.getServiceMetadata(mockRequest, mockRequestContext, mockCallback);
    restClient.getServiceMetadata(mockRequest, mockRequestContext, mockCallback);
    restClient.getServiceMetadata(mockRequest, mockRequestContext, mockCallback);
    // getMetadata should be called all 3 times as cache will be invalidated after each error.
    Mockito.verify(mockClient, times(3)).getMetadata(any(), any());
    Mockito.verify(mockCallback, times(3)).onError(any(Throwable.class));
    Mockito.verify(mockCallback, times(0)).onSuccess(any());
  }
}