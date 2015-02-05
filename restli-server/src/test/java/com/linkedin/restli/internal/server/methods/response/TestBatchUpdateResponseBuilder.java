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

package com.linkedin.restli.internal.server.methods.response;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.StringDataSchema;
import com.linkedin.data.template.InvalidAlternativeKeyException;
import com.linkedin.data.template.KeyCoercer;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.pegasus.generator.examples.Foo;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.server.AlternativeKey;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.HttpCookie;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author kparikh
 */
public class TestBatchUpdateResponseBuilder
{
  @DataProvider(name = "testData")
  public Object[][] dataProvider()
  {
    CompoundKey c1 = new CompoundKey().append("a", "a1").append("b", 1);
    CompoundKey c2 = new CompoundKey().append("a", "a2").append("b", 2);
    CompoundKey c3 = new CompoundKey().append("a", "a3").append("b", 3);
    Map<CompoundKey, UpdateResponse> results = new HashMap<CompoundKey, UpdateResponse>();
    results.put(c1, new UpdateResponse(HttpStatus.S_202_ACCEPTED));
    results.put(c2, new UpdateResponse(HttpStatus.S_202_ACCEPTED));

    RestLiServiceException restLiServiceException = new RestLiServiceException(HttpStatus.S_404_NOT_FOUND);
    Map<CompoundKey, RestLiServiceException> errors = Collections.singletonMap(c3, restLiServiceException);

    BatchUpdateResult<CompoundKey, Foo> batchUpdateResult =
        new BatchUpdateResult<CompoundKey, Foo>(results, errors);

    Map<CompoundKey, UpdateResponse> keyOverlapResults = new HashMap<CompoundKey, UpdateResponse>();
    keyOverlapResults.put(c1, new UpdateResponse(HttpStatus.S_202_ACCEPTED));
    keyOverlapResults.put(c2, new UpdateResponse(HttpStatus.S_202_ACCEPTED));
    keyOverlapResults.put(c3, new UpdateResponse(HttpStatus.S_404_NOT_FOUND));
    BatchUpdateResult<CompoundKey, Foo> keyOverlapBatchUpdateResult =
        new BatchUpdateResult<CompoundKey, Foo>(keyOverlapResults, errors);

    UpdateStatus updateStatus = new UpdateStatus().setStatus(202);
    ErrorResponse errorResponse = new ErrorResponse().setStatus(404);

    Map<String, UpdateStatus> expectedProtocol1Results = new HashMap<String, UpdateStatus>();
    expectedProtocol1Results.put("a=a1&b=1", updateStatus);
    expectedProtocol1Results.put("a=a2&b=2", updateStatus);
    Map<String, ErrorResponse> expectedProtocol1Errors = new HashMap<String, ErrorResponse>();
    expectedProtocol1Errors.put("a=a3&b=3", errorResponse);

    Map<String, UpdateStatus> expectedProtocol2Results = new HashMap<String, UpdateStatus>();
    expectedProtocol2Results.put("(a:a1,b:1)", updateStatus);
    expectedProtocol2Results.put("(a:a2,b:2)", updateStatus);
    Map<String, ErrorResponse> expectedProtocol2Errors = new HashMap<String, ErrorResponse>();
    expectedProtocol2Errors.put("(a:a3,b:3)", errorResponse);

    Map<String, UpdateStatus> expectedAltKeyResults = new HashMap<String, UpdateStatus>();
    expectedAltKeyResults.put("aa1xb1", updateStatus);
    expectedAltKeyResults.put("aa2xb2", updateStatus);
    Map<String, ErrorResponse> expectedAltKeyErrors = new HashMap<String, ErrorResponse>();
    expectedAltKeyErrors.put("aa3xb3", errorResponse);

    Map<String, AlternativeKey<?, ?>> alternativeKeyMap = new HashMap<String, AlternativeKey<?, ?>>();
    alternativeKeyMap.put("alt", new AlternativeKey<String, CompoundKey>(new TestKeyCoercer(), String.class, new StringDataSchema()));

    return new Object[][]
        {
            { batchUpdateResult, AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), null, null, expectedProtocol1Results, expectedProtocol1Errors },
            { batchUpdateResult, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), null, null, expectedProtocol2Results, expectedProtocol2Errors },
            { keyOverlapBatchUpdateResult, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), null, null, expectedProtocol2Results, expectedProtocol2Errors },
            { batchUpdateResult,
              AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
              "alt",
              alternativeKeyMap,
              expectedAltKeyResults,
              expectedAltKeyErrors
            },
            { batchUpdateResult,
              AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
              "alt",
              alternativeKeyMap,
              expectedAltKeyResults,
              expectedAltKeyErrors
            }
        };
  }

  @Test(dataProvider = "testData")
  @SuppressWarnings("unchecked")
  public void testBuilder(Object results,
                          ProtocolVersion protocolVersion,
                          String altKeyName,
                          Map<String, AlternativeKey<?, ?>> alternativeKeyMap,
                          Map<String, UpdateStatus> expectedResults,
                          Map<String, ErrorResponse> expectedErrors)
  {
    ResourceContext mockContext = getMockResourceContext(protocolVersion, altKeyName);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor(alternativeKeyMap);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    BatchUpdateResponseBuilder batchUpdateResponseBuilder = new BatchUpdateResponseBuilder(new ErrorResponseBuilder());
    RestLiResponseEnvelope responseData = batchUpdateResponseBuilder.buildRestLiResponseData(null,
                                                                                                  routingResult,
                                                                                                  results,
                                                                                                  headers,
                                                                                                  Collections.<HttpCookie>emptyList());
    PartialRestResponse restResponse = batchUpdateResponseBuilder.buildResponse(routingResult, responseData);

    BatchResponse<UpdateStatus> batchResponse = (BatchResponse<UpdateStatus>) restResponse.getEntity();
    EasyMock.verify(mockContext, mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(batchResponse.getResults(), expectedResults);
    Assert.assertEquals(batchResponse.getErrors().size(), expectedErrors.size());
    for (Map.Entry<String, ErrorResponse> entry: batchResponse.getErrors().entrySet())
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
    Map<Object,RestLiServiceException> errors = new HashMap<Object, RestLiServiceException>();
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_402_PAYMENT_REQUIRED);
    errors.put("foo", exception);
    EasyMock.expect(context.hasParameter("altkey")).andReturn(false);
    EasyMock.expect(context.getBatchKeyErrors()).andReturn(errors);
    EasyMock.replay(context);
    RoutingResult routingResult = new RoutingResult(context, null);
    RestLiResponseEnvelope envelope = builder.buildRestLiResponseData(null,
                                                                      routingResult,
                                                                      new BatchUpdateResult<Object, Integer>(Collections.<Object, UpdateResponse>emptyMap()),
                                                                      Collections.<String, String>emptyMap(),
                                                                      Collections.<HttpCookie>emptyList());
    Assert.assertEquals(envelope.getBatchResponseEnvelope().getBatchResponseMap().get("foo").getException(),
                        exception);
    Assert.assertEquals(envelope.getBatchResponseEnvelope().getBatchResponseMap().size(), 1);
  }

  @DataProvider(name = "unsupportedNullKeyMapData")
  public Object[][] unsupportedNullKeyMapData()
  {
    final CompoundKey c1 = new CompoundKey().append("a", "a1").append("b", 1);
    final Map<CompoundKey, UpdateResponse> results = new ConcurrentHashMap<CompoundKey, UpdateResponse>();
    results.put(c1, new UpdateResponse(HttpStatus.S_202_ACCEPTED));

    final BatchUpdateResult<CompoundKey, Foo> batchUpdateResult =
        new BatchUpdateResult<CompoundKey, Foo>(results, new ConcurrentHashMap<CompoundKey, RestLiServiceException>());
    final UpdateStatus updateStatus = new UpdateStatus().setStatus(202);

    final Map<String, UpdateStatus> expectedProtocol1Results = new HashMap<String, UpdateStatus>();
    expectedProtocol1Results.put("a=a1&b=1", updateStatus);
    final Map<String, UpdateStatus> expectedProtocol2Results = new HashMap<String, UpdateStatus>();
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
  public void unsupportedNullKeyMapTest(Object results, ProtocolVersion protocolVersion, Map<String, UpdateStatus> expectedResults)
  {
    ResourceContext mockContext = getMockResourceContext(protocolVersion, null);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor(null);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    BatchUpdateResponseBuilder batchUpdateResponseBuilder = new BatchUpdateResponseBuilder(new ErrorResponseBuilder());
    RestLiResponseEnvelope responseData = batchUpdateResponseBuilder.buildRestLiResponseData(null,
        routingResult,
        results,
        headers, Collections.<HttpCookie>emptyList());
    PartialRestResponse restResponse = batchUpdateResponseBuilder.buildResponse(routingResult, responseData);

    BatchResponse<UpdateStatus> batchResponse = (BatchResponse<UpdateStatus>) restResponse.getEntity();
    EasyMock.verify(mockContext, mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    Assert.assertEquals(batchResponse.getResults(), expectedResults);
  }

  @Test(dataProvider = "updateStatusInstantiation")
  public void testUpdateStatusInstantiation(RestLiResponseEnvelope responseData, UpdateStatus expectedResult)
  {
    ResourceContext mockContext = getMockResourceContext(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), null);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor(null);
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    PartialRestResponse response = new BatchUpdateResponseBuilder(new ErrorResponseBuilder())
        .buildResponse(routingResult, responseData);
    Assert.assertEquals(((BatchResponse) response.getEntity()).getResults().get("key"), expectedResult);
  }

  @DataProvider(name = "updateStatusInstantiation")
  public Object[][] updateStatusInstantiation()
  {
    Map<String, BatchResponseEnvelope.BatchResponseEntry> normal = new HashMap<String, BatchResponseEnvelope.BatchResponseEntry>();
    UpdateStatus foo = new UpdateStatus();
    foo.setStatus(500); // should be overwritten
    foo.data().put("foo", "bar"); //should be preserved
    normal.put("key", new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_200_OK, foo));
    UpdateStatus normalStatus = new UpdateStatus();
    normalStatus.setStatus(200);
    normalStatus.data().put("foo", "bar");

    Map<String, BatchResponseEnvelope.BatchResponseEntry> missing = new HashMap<String, BatchResponseEnvelope.BatchResponseEntry>();
    missing.put("key", new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_200_OK, (RecordTemplate) null));
    UpdateStatus missingStatus = new UpdateStatus();
    missingStatus.setStatus(200);

    Map<String, BatchResponseEnvelope.BatchResponseEntry> mismatch = new HashMap<String, BatchResponseEnvelope.BatchResponseEntry>();
    mismatch.put("key", new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_200_OK, new AnyRecord(new DataMap())));
    UpdateStatus mismatchedStatus = new UpdateStatus();
    mismatchedStatus.setStatus(200);

    return new Object[][] {
        { new BatchResponseEnvelope(normal, Collections.<String, String>emptyMap(), Collections.<HttpCookie>emptyList()), normalStatus },
        { new BatchResponseEnvelope(missing, Collections.<String, String>emptyMap(), Collections.<HttpCookie>emptyList()), missingStatus },
        { new BatchResponseEnvelope(mismatch, Collections.<String, String>emptyMap(), Collections.<HttpCookie>emptyList()), mismatchedStatus }
    };
  }

  private static ResourceContext getMockResourceContext(ProtocolVersion protocolVersion, String altKeyName)
  {
    ServerResourceContext mockContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockContext.getBatchKeyErrors()).andReturn(Collections.<Object, RestLiServiceException>emptyMap()).once();
    EasyMock.expect(mockContext.getRestliProtocolVersion()).andReturn(protocolVersion).once();
    EasyMock.expect(mockContext.hasParameter(RestConstants.ALT_KEY_PARAM)).andReturn(altKeyName != null).anyTimes();
    if (altKeyName != null)
    {
      EasyMock.expect(mockContext.getParameter(RestConstants.ALT_KEY_PARAM)).andReturn(altKeyName).atLeastOnce();
    }
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
