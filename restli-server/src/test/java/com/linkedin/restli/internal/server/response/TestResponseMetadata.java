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

package com.linkedin.restli.internal.server.response;

import com.linkedin.data.DataMap;
import com.linkedin.pegasus.generator.examples.Foo;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;


/**
 * @author jhare
 */
public class TestResponseMetadata {
  private static final String TEST_META_DATA_ELEMENT_KEY = "element";
  private static final DataMap TEST_META_DATA_ELEMENT = new DataMap(Collections.singletonMap("child", "value"));
  private static final DataMap TEST_META_DATA = new DataMap(Collections.singletonMap(TEST_META_DATA_ELEMENT_KEY, TEST_META_DATA_ELEMENT));

  @DataProvider(name = "dataProvider")
  public Object[][] dataProvider()
  {
    return new Object[][] {
      {ResourceMethod.GET, buildFooRecord(), true, "{text=test Foo, $metadata={element={child=value}}}"},
      {ResourceMethod.BATCH_GET, new HashMap<>(), true,
          "{statuses={}, results={}, errors={}, $metadata={element={child=value}}}"},
      {ResourceMethod.FINDER,  new CollectionResult<>(Collections.singletonList(buildFooRecord())), true,
          "{elements=[{text=test Foo}], paging={count=10, start=0, links=[]}, $metadata={element={child=value}}}"},
      {ResourceMethod.CREATE, new CreateResponse(1000, HttpStatus.S_200_OK), true, null},
      {ResourceMethod.BATCH_CREATE, new BatchCreateResult<String, UpdateResponse>(Collections.singletonList(
          new CreateResponse(HttpStatus.S_200_OK))), true, "{elements=[{status=200}], $metadata={element={child=value}}}"},
      {ResourceMethod.PARTIAL_UPDATE, new UpdateResponse(HttpStatus.S_200_OK), false, null},
      {ResourceMethod.UPDATE, new UpdateResponse(HttpStatus.S_200_OK), false, null},
      {ResourceMethod.BATCH_UPDATE, new BatchUpdateResult<String, UpdateResponse>(new HashMap<>()), true,
          "{results={}, errors={}, $metadata={element={child=value}}}"},
      {ResourceMethod.DELETE, new UpdateResponse(HttpStatus.S_200_OK), false, null},
      {ResourceMethod.ACTION, buildFooRecord(), true, "{$metadata={element={child=value}}}"},
      {ResourceMethod.BATCH_PARTIAL_UPDATE, new BatchUpdateResult<String, UpdateResponse>(new HashMap<>()), true,
          "{results={}, errors={}, $metadata={element={child=value}}}"},
      {ResourceMethod.BATCH_DELETE, new BatchUpdateResult<String, UpdateResponse>(new HashMap<>()), true,
          "{results={}, errors={}, $metadata={element={child=value}}}"},
      {ResourceMethod.GET_ALL, new CollectionResult<>(Collections.singletonList(buildFooRecord())), true,
          "{elements=[{text=test Foo}], paging={count=10, start=0, links=[]}, $metadata={element={child=value}}}"}
    };
  }

  @Test(dataProvider = "dataProvider")
  public void testMetadata(ResourceMethod resourceMethod, Object responseObject,
      boolean hasEntity, String responseString) throws Exception {
    final RestRequest mockRequest = mock(RestRequest.class);
    final RoutingResult mockRoutingResult = mock(RoutingResult.class);
    final ResourceMethodDescriptor mockResourceMethodDescriptor = mock(ResourceMethodDescriptor.class);
    final ServerResourceContext mockContext = mock(ServerResourceContext.class);
    final ProtocolVersion mockProtocolVersion = AllProtocolVersions.LATEST_PROTOCOL_VERSION;
    final URI mockURI = new URI("http://fake.com");

    when(mockRequest.getURI()).thenReturn(mockURI);
    when(mockResourceMethodDescriptor.getMethodType()).thenReturn(resourceMethod);
    when(mockRoutingResult.getResourceMethod()).thenReturn(mockResourceMethodDescriptor);
    when(mockResourceMethodDescriptor.getType()).thenReturn(resourceMethod);
    when(mockRoutingResult.getContext()).thenReturn(mockContext);
    when(mockContext.getRestliProtocolVersion()).thenReturn(mockProtocolVersion);


    final RestLiResponseHandler responseHandler = new RestLiResponseHandler.Builder().build();

    // Test success path

    RestLiResponseData<?> responseData = responseHandler.buildRestLiResponseData(mockRequest, mockRoutingResult, responseObject);
    responseData.getResponseEnvelope().getResponseMetadata().put(TEST_META_DATA_ELEMENT_KEY, TEST_META_DATA_ELEMENT);
    PartialRestResponse response = responseHandler.buildPartialResponse(mockRoutingResult, responseData);

    assertEquals(response.getEntity() != null, hasEntity);

    if (hasEntity) {
      if (response.getEntity() instanceof IdResponse) {
        assertNotNull(((IdResponse) response.getEntity()).getId());
      } else {
        assertEquals(response.getEntity().data().get(RestConstants.METADATA_RESERVED_FIELD), TEST_META_DATA);
        assertEquals(response.getEntity().toString(), responseString);

      }
    }

    // Verify metadata is cleared when an exception is set by a filter

    responseData.getResponseEnvelope().setExceptionInternal(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
    assertEquals(responseData.getResponseEnvelope().getResponseMetadata().size(), 0);

    PartialRestResponse errorResponse = responseHandler.buildPartialResponse(mockRoutingResult, responseData);
    assertNull(errorResponse.getEntity().data().get(RestConstants.METADATA_RESERVED_FIELD));

    // Test case where resource method returns exception path

    RestLiResponseData<?> errorResponseData = responseHandler.buildExceptionResponseData(mockRequest, mockRoutingResult,
        new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR), new HashMap<>(), new ArrayList<>());
    responseData.getResponseEnvelope().getResponseMetadata().put(TEST_META_DATA_ELEMENT_KEY, TEST_META_DATA_ELEMENT);
    errorResponse = responseHandler.buildPartialResponse(mockRoutingResult, responseData);
    assertNull(errorResponse.getEntity().data().get(RestConstants.METADATA_RESERVED_FIELD));
  }

  private Foo buildFooRecord() {
    DataMap map = new DataMap();
    map.put("text", "test Foo");
    Foo foo = new Foo(map);
    return foo;
  }
}
