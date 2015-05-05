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
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.pegasus.generator.examples.Foo;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.LinkArray;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiResponseDataException;
import com.linkedin.restli.server.RestLiServiceException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestCollectionResponseBuilder
{
  @DataProvider(name = "testData")
  public Object[][] dataProvider() throws CloneNotSupportedException
  {
    Foo metadata = new Foo().setStringField("metadata").setIntField(7);
    Foo projectedMetadata = new Foo().setIntField(7);
    final List<Foo> generatedList = generateTestList();
    final List<Foo> testListWithProjection = generateTestListWithProjection();
    CollectionResult<Foo, Foo> collectionResult = new CollectionResult<Foo, Foo>(generatedList, generatedList.size(), metadata);

    DataMap dataProjectionDataMap = new DataMap();
    dataProjectionDataMap.put("stringField", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    MaskTree dataMaskTree = new MaskTree(dataProjectionDataMap);

    DataMap metadataProjectionDataMap = new DataMap();
    metadataProjectionDataMap.put("intField", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    MaskTree metadataMaskTree = new MaskTree(metadataProjectionDataMap);

    DataMap pagingProjectDataMap = new DataMap();
    pagingProjectDataMap.put("count", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    MaskTree pagingMaskTree = new MaskTree(pagingProjectDataMap);

    CollectionMetadata collectionMetadata1 = new CollectionMetadata().setCount(10).setStart(0).setLinks(new LinkArray());
    CollectionMetadata collectionMetadata2 = collectionMetadata1.clone().setTotal(2);
    CollectionMetadata collectionMetadataWithProjection = new CollectionMetadata().setCount(10);

    ProjectionMode auto = ProjectionMode.AUTOMATIC;
    ProjectionMode manual = ProjectionMode.MANUAL;

    return new Object[][]
        {
            // auto projection for data and metadata with null projection masks
            {generatedList, null, generatedList, collectionMetadata1, null, null, null, auto, auto},
            {collectionResult,
                metadata.data(),
                collectionResult.getElements(),
                collectionMetadata2,
                null,
                null,
                null,
                auto,
                auto},

            // manual projection for data and metadata with null projection masks
            {generatedList, null, generatedList, collectionMetadata1, null, null, null, manual, manual},
            {collectionResult,
                metadata.data(),
                collectionResult.getElements(),
                collectionMetadata2,
                null,
                null,
                null,
                manual,
                manual},

            // NOTE - we always apply projections to the CollectionMetaData if the paging MaskTree is non-null
            //        since ProjectionMode.AUTOMATIC is used.
            // manual projection for data and metadata with non-null projection masks
            {generatedList,
                null,
                generatedList,
                collectionMetadataWithProjection,
                dataMaskTree,
                metadataMaskTree,
                pagingMaskTree,
                manual,
                manual},
            {collectionResult,
                metadata.data(),
                collectionResult.getElements(),
                collectionMetadataWithProjection,
                dataMaskTree,
                metadataMaskTree,
                pagingMaskTree,
                manual,
                manual},

            // auto projection for data with non-null data and paging projection masks
            {generatedList,
                null,
                testListWithProjection,
                collectionMetadataWithProjection,
                dataMaskTree,
                null,
                pagingMaskTree,
                auto,
                auto},

            // auto projection for data and metadata with non-null projection masks
            {collectionResult,
                projectedMetadata.data(),
                testListWithProjection,
                collectionMetadataWithProjection,
                dataMaskTree,
                metadataMaskTree,
                pagingMaskTree,
                auto,
                auto},

            // auto data projection, manual metadata projection, and auto (default) paging projection
            {collectionResult,
                metadata.data(),
                testListWithProjection,
                collectionMetadataWithProjection,
                dataMaskTree,
                metadataMaskTree,
                pagingMaskTree,
                auto,
                manual},
        };
  }

  @SuppressWarnings("unchecked")
  @Test(dataProvider = "testData")
  public void testBuilder(Object results,
                          DataMap expectedMetadata,
                          List<Foo> expectedElements,
                          CollectionMetadata expectedPaging,
                          MaskTree dataMaskTree,
                          MaskTree metaDataMaskTree,
                          MaskTree pagingMaskTree,
                          ProjectionMode dataProjectionMode,
                          ProjectionMode metadataProjectionMode) throws URISyntaxException
  {
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    ResourceContext mockContext = getMockResourceContext(dataMaskTree,
                                                         metaDataMaskTree,
                                                         pagingMaskTree,
                                                         dataProjectionMode,
                                                         metadataProjectionMode);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    CollectionResponseBuilder responseBuilder = new CollectionResponseBuilder();
    RestLiResponseEnvelope responseData = responseBuilder.buildRestLiResponseData(getRestRequest(),
                                                                                       routingResult,
                                                                                       results,
                                                                                       headers);
    PartialRestResponse restResponse = responseBuilder.buildResponse(routingResult, responseData);

    EasyMock.verify(mockContext, mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    CollectionResponse<Foo> actualResults = (CollectionResponse<Foo>) restResponse.getEntity();
    Assert.assertEquals(actualResults.getElements(), expectedElements);
    Assert.assertEquals(actualResults.getMetadataRaw(), expectedMetadata);
    Assert.assertEquals(actualResults.getPaging(), expectedPaging);
  }

  @DataProvider(name = "exceptionTestData")
  public Object[][] exceptionDataProvider()
  {
    Foo f1 = new Foo().setStringField("f1");

    return new Object[][]
        {
            {Arrays.asList(f1, null),
                "Unexpected null encountered. Null element inside of a List returned by the resource method: "},
            {new CollectionResult<Foo, Foo>(null),
                "Unexpected null encountered. Null elements List inside of CollectionResult returned by the resource method: "}
        };
  }

  @Test(dataProvider = "exceptionTestData")
  public void testBuilderExceptions(Object results, String expectedErrorMessage)
      throws URISyntaxException
  {
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    ResourceContext mockContext = getMockResourceContext(null, null, null, null, null);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);
    CollectionResponseBuilder responseBuilder = new CollectionResponseBuilder();
    try
    {
      responseBuilder.buildRestLiResponseData(getRestRequest(), routingResult, results, headers);
      Assert.fail("An exception should have been thrown because of null elements!");
    }
    catch (RestLiServiceException e)
    {
      Assert.assertTrue(e.getMessage().contains(expectedErrorMessage));
    }
  }

  private static ResourceContext getMockResourceContext(MaskTree dataMaskTree,
                                                        MaskTree metadataMaskTree,
                                                        MaskTree pagingMaskTree,
                                                        ProjectionMode dataProjectionMode,
                                                        ProjectionMode metadataProjectionMode)
      throws URISyntaxException
  {
    ResourceContext mockContext = EasyMock.createMock(ResourceContext.class);
    EasyMock.expect(mockContext.getParameter(EasyMock.<String>anyObject())).andReturn(null).times(2);
    EasyMock.expect(mockContext.getRequestHeaders()).andReturn(ResponseBuilderUtil.getHeaders()).once();
    EasyMock.expect(mockContext.getRawRequest()).andReturn(getRestRequest()).once();

    //Field Projection
    EasyMock.expect(mockContext.getProjectionMode()).andReturn(dataProjectionMode).times(generateTestList().size());
    EasyMock.expect(mockContext.getProjectionMask()).andReturn(dataMaskTree).times(generateTestList().size());

    //Metadata Projection
    EasyMock.expect(mockContext.getMetadataProjectionMode()).andReturn(metadataProjectionMode).anyTimes();
    EasyMock.expect(mockContext.getMetadataProjectionMask()).andReturn(metadataMaskTree).anyTimes();

    //Paging Projection
    EasyMock.expect(mockContext.getPagingProjectionMask()).andReturn(pagingMaskTree).once();

    EasyMock.replay(mockContext);
    return mockContext;
  }

  private static ResourceMethodDescriptor getMockResourceMethodDescriptor()
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    EasyMock.expect(mockDescriptor.getParametersWithType(EasyMock.<Parameter.ParamType>anyObject())).andReturn(Collections.<Parameter<?>>emptyList()).once();
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }

  private static List<Foo> generateTestList()
  {
    Foo f1 = new Foo().setStringField("f1").setIntField(1);
    Foo f2 = new Foo().setStringField("f2").setIntField(2);
    List<Foo> results = Arrays.asList(f1, f2);
    return results;
  }

  private static List<Foo> generateTestListWithProjection()
  {
    Foo f1 = new Foo().setStringField("f1");
    Foo f2 = new Foo().setStringField("f2");
    List<Foo> results = Arrays.asList(f1, f2);
    return results;
  }

  private static RestRequest getRestRequest()
      throws URISyntaxException
  {
    return new RestRequestBuilder(new URI("/?q=finder")).build();
  }
}
