/*
   Copyright (c) 2014 LinkedIn Corp.

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
import com.linkedin.data.schema.StringDataSchema;
import com.linkedin.data.template.InvalidAlternativeKeyException;
import com.linkedin.data.template.KeyCoercer;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.pegasus.generator.examples.Foo;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.AlternativeKey;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Tests for the response builders of BATCH_UPDATE and BATCH_DELETE, since both resource methods use response builders
 * that are just simple subclasses of {@link BatchResponseBuilder}.
 *
 * @author kparikh
 */
public class TestBatchUpdateResponseBuilder
{
  private static final Map<ResourceMethod, BatchResponseBuilder<?>> BUILDERS = new HashMap<>();
  static
  {
    BUILDERS.put(ResourceMethod.BATCH_UPDATE, new BatchUpdateResponseBuilder(new ErrorResponseBuilder()));
    BUILDERS.put(ResourceMethod.BATCH_DELETE, new BatchDeleteResponseBuilder(new ErrorResponseBuilder()));
  }

  @DataProvider(name = "testData")
  public Object[][] dataProvider()
  {
    CompoundKey c1 = new CompoundKey().append("a", "a1").append("b", 1);
    CompoundKey c2 = new CompoundKey().append("a", "a2").append("b", 2);
    CompoundKey c3 = new CompoundKey().append("a", "a3").append("b", 3);
    Map<CompoundKey, UpdateResponse> results = new HashMap<>();
    results.put(c1, new UpdateResponse(HttpStatus.S_202_ACCEPTED));
    results.put(c2, new UpdateResponse(HttpStatus.S_202_ACCEPTED));

    RestLiServiceException restLiServiceException = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);
    Map<CompoundKey, RestLiServiceException> errors = Collections.singletonMap(c3, restLiServiceException);

    BatchUpdateResult<CompoundKey, Foo> batchUpdateResult =
        new BatchUpdateResult<>(results, errors);

    Map<CompoundKey, UpdateResponse> keyOverlapResults = new HashMap<>();
    keyOverlapResults.put(c1, new UpdateResponse(HttpStatus.S_202_ACCEPTED));
    keyOverlapResults.put(c2, new UpdateResponse(HttpStatus.S_202_ACCEPTED));
    keyOverlapResults.put(c3, new UpdateResponse(HttpStatus.S_404_NOT_FOUND));
    BatchUpdateResult<CompoundKey, Foo> keyOverlapBatchUpdateResult =
        new BatchUpdateResult<>(keyOverlapResults, errors);

    UpdateStatus updateStatus = new UpdateStatus().setStatus(202);
    ErrorResponse errorResponse = new ErrorResponse().setStatus(404);

    Map<String, UpdateStatus> expectedProtocol1Results = new HashMap<>();
    expectedProtocol1Results.put("a=a1&b=1", updateStatus);
    expectedProtocol1Results.put("a=a2&b=2", updateStatus);
    Map<String, ErrorResponse> expectedProtocol1Errors = new HashMap<>();
    expectedProtocol1Errors.put("a=a3&b=3", errorResponse);

    Map<String, UpdateStatus> expectedProtocol2Results = new HashMap<>();
    expectedProtocol2Results.put("(a:a1,b:1)", updateStatus);
    expectedProtocol2Results.put("(a:a2,b:2)", updateStatus);
    Map<String, ErrorResponse> expectedProtocol2Errors = new HashMap<>();
    expectedProtocol2Errors.put("(a:a3,b:3)", errorResponse);

    Map<String, UpdateStatus> expectedAltKeyResults = new HashMap<>();
    expectedAltKeyResults.put("aa1xb1", updateStatus);
    expectedAltKeyResults.put("aa2xb2", updateStatus);
    Map<String, ErrorResponse> expectedAltKeyErrors = new HashMap<>();
    expectedAltKeyErrors.put("aa3xb3", errorResponse);

    Map<String, AlternativeKey<?, ?>> alternativeKeyMap = new HashMap<>();
    alternativeKeyMap.put("alt", new AlternativeKey<>(new TestKeyCoercer(), String.class, new StringDataSchema()));

    List<Object[]> data = new ArrayList<>();
    for (ResourceMethod resourceMethod: BUILDERS.keySet())
    {
      data.add(new Object[] {batchUpdateResult, null, null, expectedProtocol1Results, expectedProtocol1Errors, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), resourceMethod});
      data.add(new Object[] {batchUpdateResult, null, null, expectedProtocol2Results, expectedProtocol2Errors, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), resourceMethod});
      data.add(new Object[] {batchUpdateResult, "alt", alternativeKeyMap, expectedAltKeyResults, expectedAltKeyErrors, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), resourceMethod});
      data.add(new Object[] {batchUpdateResult, "alt", alternativeKeyMap, expectedAltKeyResults, expectedAltKeyErrors, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), resourceMethod});
      data.add(new Object[] {keyOverlapBatchUpdateResult, null, null, expectedProtocol2Results, expectedProtocol2Errors, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), resourceMethod});
    }

    return data.toArray(new Object[data.size()][]);
  }

  @Test(dataProvider = "testData")
  @SuppressWarnings("unchecked")
  public <D extends RestLiResponseData<? extends BatchResponseEnvelope>> void testBuilder(Object results,
      String altKeyName,
      Map<String, AlternativeKey<?, ?>> alternativeKeyMap,
      Map<String, UpdateStatus> expectedResults,
      Map<String, ErrorResponse> expectedErrors,
      ProtocolVersion protocolVersion,
      ResourceMethod resourceMethod)
  {
    ServerResourceContext mockContext = getMockResourceContext(protocolVersion, altKeyName);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor(alternativeKeyMap);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    BatchResponseBuilder<D> batchUpdateResponseBuilder = (BatchResponseBuilder<D>) BUILDERS.get(resourceMethod);
    D responseData = batchUpdateResponseBuilder.buildRestLiResponseData(null,
        routingResult,
        results,
        headers,
        Collections.emptyList());
    RestLiResponse restResponse = batchUpdateResponseBuilder.buildResponse(routingResult, responseData);

    @SuppressWarnings("unchecked")
    BatchResponse<UpdateStatus> batchResponse = (BatchResponse<UpdateStatus>) restResponse.getEntity();
    EasyMock.verify(mockContext, mockDescriptor);
    Assert.assertEquals(responseData.getResourceMethod(), resourceMethod);
    Assert.assertEquals(responseData.getResponseEnvelope().getResourceMethod(), resourceMethod);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(batchResponse.getResults(), expectedResults);
    Assert.assertEquals(batchResponse.getErrors().size(), expectedErrors.size());
    for (Map.Entry<String, ErrorResponse> entry : batchResponse.getErrors().entrySet())
    {
      String key = entry.getKey();
      ErrorResponse value = entry.getValue();
      Assert.assertEquals(value.getStatus(), expectedErrors.get(key).getStatus());
    }
  }

  @Test
  public void testContextErrors()
  {
    BatchUpdateResponseBuilder builder = new BatchUpdateResponseBuilder(new ErrorResponseBuilder());
    ServerResourceContext context = EasyMock.createMock(ServerResourceContext.class);
    Map<Object,RestLiServiceException> errors = new HashMap<>();
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_402_PAYMENT_REQUIRED);
    errors.put("foo", exception);
    EasyMock.expect(context.hasParameter("altkey")).andReturn(false).anyTimes();
    EasyMock.expect(context.getBatchKeyErrors()).andReturn(errors).anyTimes();
    EasyMock.expect(context.getRawRequestContext()).andReturn(new RequestContext()).anyTimes();
    EasyMock.replay(context);
    RoutingResult routingResult = new RoutingResult(context, getMockResourceMethodDescriptor(null));
    RestLiResponseData<BatchUpdateResponseEnvelope> responseData = builder.buildRestLiResponseData(null,
                                                     routingResult,
                                                     new BatchUpdateResult<>(Collections.emptyMap()),
                                                     Collections.emptyMap(),
                                                     Collections.emptyList());
    Assert.assertEquals(responseData.getResponseEnvelope().getBatchResponseMap().get("foo").getException(),
                        exception);
    Assert.assertEquals(responseData.getResponseEnvelope().getBatchResponseMap().size(), 1);
  }

  @DataProvider(name = "unsupportedNullKeyMapData")
  public Object[][] unsupportedNullKeyMapData()
  {
    final CompoundKey c1 = new CompoundKey().append("a", "a1").append("b", 1);
    final Map<CompoundKey, UpdateResponse> results = new ConcurrentHashMap<>();
    results.put(c1, new UpdateResponse(HttpStatus.S_202_ACCEPTED));

    final BatchUpdateResult<CompoundKey, Foo> batchUpdateResult =
        new BatchUpdateResult<>(results, new ConcurrentHashMap<>());
    final UpdateStatus updateStatus = new UpdateStatus().setStatus(202);

    final Map<String, UpdateStatus> expectedProtocol1Results = new HashMap<>();
    expectedProtocol1Results.put("a=a1&b=1", updateStatus);
    final Map<String, UpdateStatus> expectedProtocol2Results = new HashMap<>();
    expectedProtocol2Results.put("(a:a1,b:1)", updateStatus);

    return new Object[][]
        {
            {batchUpdateResult, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), expectedProtocol1Results},
            {batchUpdateResult, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), expectedProtocol2Results}
        };
  }

 /* Note that we use also need to test using java.util.concurrent.ConcurrentHashMap. This is because rest.li checks
  * for the presence of nulls returned from maps which are returned from resource methods. The checking for nulls
  * is prone to a NullPointerException since contains(null) can throw an NPE from certain map implementations such as
  * java.util.concurrent.ConcurrentHashMap. We want to make sure our check for the presence of nulls is done in a
  * way that doesn't throw an NullPointerException.
  */
  @Test(dataProvider = "unsupportedNullKeyMapData")
  @SuppressWarnings("unchecked")
  public void testUnsupportedNullKeyMap(Object results, ProtocolVersion protocolVersion, Map<String, UpdateStatus> expectedResults)
  {
    ServerResourceContext mockContext = getMockResourceContext(protocolVersion, null);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor(null);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    BatchUpdateResponseBuilder batchUpdateResponseBuilder = new BatchUpdateResponseBuilder(new ErrorResponseBuilder());
    RestLiResponseData<BatchUpdateResponseEnvelope> responseData = batchUpdateResponseBuilder.buildRestLiResponseData(null, routingResult, results,
                                                                        headers,
                                                                        Collections.emptyList());
    RestLiResponse restResponse = batchUpdateResponseBuilder.buildResponse(routingResult, responseData);

    BatchResponse<UpdateStatus> batchResponse = (BatchResponse<UpdateStatus>) restResponse.getEntity();
    EasyMock.verify(mockContext, mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(batchResponse.getResults(), expectedResults);
  }

  @Test(dataProvider = "updateStatusInstantiation")
  public void testUpdateStatusInstantiation(RestLiResponseData<BatchUpdateResponseEnvelope> responseData, UpdateStatus expectedResult)
  {
    ServerResourceContext mockContext = getMockResourceContext(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), null);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor(null);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    RestLiResponse response = new BatchUpdateResponseBuilder(new ErrorResponseBuilder())
        .buildResponse(routingResult, responseData);
    Assert.assertEquals(((BatchResponse<?>) response.getEntity()).getResults().get("key"), expectedResult);
  }

  @DataProvider(name = "updateStatusInstantiation")
  public Object[][] updateStatusInstantiation()
  {
    Map<String, BatchResponseEnvelope.BatchResponseEntry> normal = new HashMap<>();
    UpdateStatus foo = new UpdateStatus();
    foo.setStatus(500); // should be overwritten
    foo.data().put("foo", "bar"); //should be preserved
    normal.put("key", new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_200_OK, foo));
    UpdateStatus normalStatus = new UpdateStatus();
    normalStatus.setStatus(200);
    normalStatus.data().put("foo", "bar");

    Map<String, BatchResponseEnvelope.BatchResponseEntry> missing = new HashMap<>();
    missing.put("key", new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_200_OK, (RecordTemplate) null));
    UpdateStatus missingStatus = new UpdateStatus();
    missingStatus.setStatus(200);

    Map<String, BatchResponseEnvelope.BatchResponseEntry> mismatch = new HashMap<>();
    mismatch.put("key", new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_200_OK, new AnyRecord(new DataMap())));
    UpdateStatus mismatchedStatus = new UpdateStatus();
    mismatchedStatus.setStatus(200);

    RestLiResponseData<?> batchGetNormal = ResponseDataBuilderUtil.buildBatchGetResponseData(HttpStatus.S_200_OK, normal);

    RestLiResponseData<?> batchGetMissing = ResponseDataBuilderUtil.buildBatchGetResponseData(HttpStatus.S_200_OK, missing);

    RestLiResponseData<?> batchGetMismatch = ResponseDataBuilderUtil.buildBatchGetResponseData(HttpStatus.S_200_OK, mismatch);

    RestLiResponseData<?> batchUpdateNormal = ResponseDataBuilderUtil.buildBatchUpdateResponseData(HttpStatus.S_200_OK, normal);

    RestLiResponseData<?> batchUpdateMissing = ResponseDataBuilderUtil.buildBatchUpdateResponseData(HttpStatus.S_200_OK, missing);

    RestLiResponseData<?> batchUpdateMismatch = ResponseDataBuilderUtil.buildBatchUpdateResponseData(HttpStatus.S_200_OK, mismatch);

    RestLiResponseData<?> batchPartialUpdateNormal = ResponseDataBuilderUtil.buildBatchPartialUpdateResponseData(HttpStatus.S_200_OK, normal);

    RestLiResponseData<?> batchPartialUpdateMissing = ResponseDataBuilderUtil.buildBatchPartialUpdateResponseData(HttpStatus.S_200_OK, missing);

    RestLiResponseData<?> batchPartialUpdateMismatch = ResponseDataBuilderUtil.buildBatchPartialUpdateResponseData(HttpStatus.S_200_OK, mismatch);

    RestLiResponseData<?> batchDeleteNormal = ResponseDataBuilderUtil.buildBatchDeleteResponseData(HttpStatus.S_200_OK, normal);

    RestLiResponseData<?> batchDeleteMissing = ResponseDataBuilderUtil.buildBatchDeleteResponseData(HttpStatus.S_200_OK, missing);

    RestLiResponseData<?> batchDeleteMismatch = ResponseDataBuilderUtil.buildBatchDeleteResponseData(HttpStatus.S_200_OK, mismatch);

    return new Object[][] {
        { batchGetNormal, normalStatus },
        { batchGetMissing, missingStatus },
        { batchGetMismatch, mismatchedStatus},
        { batchUpdateNormal, normalStatus },
        { batchUpdateMissing, missingStatus },
        { batchUpdateMismatch, mismatchedStatus},
        { batchPartialUpdateNormal, normalStatus },
        { batchPartialUpdateMissing, missingStatus },
        { batchPartialUpdateMismatch, mismatchedStatus},
        { batchDeleteNormal, normalStatus },
        { batchDeleteMissing, missingStatus },
        { batchDeleteMismatch, mismatchedStatus}
    };
  }

  private static ServerResourceContext getMockResourceContext(ProtocolVersion protocolVersion, String altKeyName)
  {
    ServerResourceContext mockContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockContext.getBatchKeyErrors()).andReturn(Collections.emptyMap()).once();
    EasyMock.expect(mockContext.getRestliProtocolVersion()).andReturn(protocolVersion).once();
    EasyMock.expect(mockContext.hasParameter(RestConstants.ALT_KEY_PARAM)).andReturn(altKeyName != null).anyTimes();
    if (altKeyName != null)
    {
      EasyMock.expect(mockContext.getParameter(RestConstants.ALT_KEY_PARAM)).andReturn(altKeyName).atLeastOnce();
    }
    EasyMock.expect(mockContext.getRawRequestContext()).andReturn(new RequestContext()).anyTimes();
    EasyMock.replay(mockContext);
    return mockContext;
  }

  private static ResourceMethodDescriptor getMockResourceMethodDescriptor(Map<String, AlternativeKey<?, ?>> alternativeKeyMap)
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    if (alternativeKeyMap != null)
    {
      EasyMock.expect(mockDescriptor.getResourceModel()).andReturn(getMockResourceModel(alternativeKeyMap)).atLeastOnce();
    }
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }

  private static ResourceModel getMockResourceModel(Map<String, AlternativeKey<?, ?>> alternativeKeyMap)
  {
    ResourceModel mockResourceModel = EasyMock.createMock(ResourceModel.class);
    EasyMock.expect(mockResourceModel.getAlternativeKeys()).andReturn(alternativeKeyMap).anyTimes();
    EasyMock.replay(mockResourceModel);
    return mockResourceModel;
  }

  private class TestKeyCoercer implements KeyCoercer<String, CompoundKey>
  {
    @Override
    public CompoundKey coerceToKey(String object) throws InvalidAlternativeKeyException
    {
      CompoundKey compoundKey = new CompoundKey();
      compoundKey.append("a", object.substring(1, 3));
      compoundKey.append("b", Integer.parseInt(object.substring(4, 5)));
      return compoundKey;
    }

    @Override
    public String coerceFromKey(CompoundKey object)
    {
      return "a" + object.getPart("a") + "xb" + object.getPart("b");
    }
  }
}
