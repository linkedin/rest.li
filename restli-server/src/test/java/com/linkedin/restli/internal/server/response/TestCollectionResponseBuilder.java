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
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.pegasus.generator.examples.Foo;
import com.linkedin.pegasus.generator.examples.Fruits;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.LinkArray;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import org.easymock.EasyMock;


/**
 * @author kparikh
 */
public class TestCollectionResponseBuilder
{
  private static final Map<ResourceMethod, CollectionResponseBuilder<?>> BUILDERS = new HashMap<>();
  static
  {
    BUILDERS.put(ResourceMethod.FINDER, new FinderResponseBuilder());
    BUILDERS.put(ResourceMethod.GET_ALL, new GetAllResponseBuilder());
  }

  @DataProvider(name = "testData")
  public Object[][] dataProvider() throws CloneNotSupportedException
  {
    Foo metadata = new Foo().setStringField("metadata").setIntField(7);
    Foo projectedMetadata = new Foo().setIntField(7);
    final List<Foo> generatedList = generateTestList();
    final List<Foo> testListWithProjection = generateTestListWithProjection();
    CollectionResult<Foo, Foo> collectionResult = new CollectionResult<>(generatedList, generatedList.size(), metadata);

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

    List<Object[]> data = new ArrayList<>();
    for (ResourceMethod resourceMethod: BUILDERS.keySet())
    {
      // auto projection for data and metadata with null projection masks
      data.add(new Object[] {generatedList, null, generatedList, collectionMetadata1, null, null, null, auto, auto, resourceMethod});
      data.add(new Object[] {collectionResult,
                metadata.data(),
                collectionResult.getElements(),
                collectionMetadata2,
                null,
                null,
                null,
                auto,
                auto,
                resourceMethod});

      // manual projection for data and metadata with null projection masks
      data.add(new Object[] {generatedList, null, generatedList, collectionMetadata1, null, null, null, manual, manual, resourceMethod});
      data.add(new Object[] {collectionResult,
                metadata.data(),
                collectionResult.getElements(),
                collectionMetadata2,
                null,
                null,
                null,
                manual,
                manual,
                resourceMethod});

      // NOTE - we always apply projections to the CollectionMetaData if the paging MaskTree is non-null
      //        since ProjectionMode.AUTOMATIC is used.
      // manual projection for data and metadata with non-null projection masks
      data.add(new Object[] {generatedList,
                null,
                generatedList,
                collectionMetadataWithProjection,
                dataMaskTree,
                metadataMaskTree,
                pagingMaskTree,
                manual,
                manual,
                resourceMethod});
      data.add(new Object[] {collectionResult,
                metadata.data(),
                collectionResult.getElements(),
                collectionMetadataWithProjection,
                dataMaskTree,
                metadataMaskTree,
                pagingMaskTree,
                manual,
                manual,
                resourceMethod});

      // auto projection for data with non-null data and paging projection masks
      data.add(new Object[] {generatedList,
                null,
                testListWithProjection,
                collectionMetadataWithProjection,
                dataMaskTree,
                null,
                pagingMaskTree,
                auto,
                auto,
                resourceMethod});

      // auto projection for data and metadata with non-null projection masks
      data.add(new Object[] {collectionResult,
                projectedMetadata.data(),
                testListWithProjection,
                collectionMetadataWithProjection,
                dataMaskTree,
                metadataMaskTree,
                pagingMaskTree,
                auto,
                auto,
                resourceMethod});

      // auto data projection, manual metadata projection, and auto (default) paging projection
      data.add(new Object[] {collectionResult,
                metadata.data(),
                testListWithProjection,
                collectionMetadataWithProjection,
                dataMaskTree,
                metadataMaskTree,
                pagingMaskTree,
                auto,
                manual,
                resourceMethod});
    }
    return data.toArray(new Object[data.size()][]);
  }

  @SuppressWarnings("unchecked")
  @Test(dataProvider = "testData")
  public <D extends RestLiResponseData<? extends CollectionResponseEnvelope>> void testBuilder(Object results,
                          DataMap expectedMetadata,
                          List<Foo> expectedElements,
                          CollectionMetadata expectedPaging,
                          MaskTree dataMaskTree,
                          MaskTree metaDataMaskTree,
                          MaskTree pagingMaskTree,
                          ProjectionMode dataProjectionMode,
                          ProjectionMode metadataProjectionMode,
                          ResourceMethod resourceMethod) throws URISyntaxException
  {
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();

    ServerResourceContext mockContext =
        getMockResourceContext(dataMaskTree, metaDataMaskTree, pagingMaskTree, dataProjectionMode,
                               metadataProjectionMode);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    CollectionResponseBuilder<D> responseBuilder = (CollectionResponseBuilder<D>) BUILDERS.get(resourceMethod);
    D responseData =
        responseBuilder.buildRestLiResponseData(getRestRequest(), routingResult, results, headers, Collections.emptyList());
    PartialRestResponse restResponse = responseBuilder.buildResponse(routingResult, responseData);

    Assert.assertEquals(responseData.getResourceMethod(), resourceMethod);
    Assert.assertEquals(responseData.getResponseEnvelope().getResourceMethod(), resourceMethod);

    EasyMock.verify(mockContext, mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restResponse, headers);
    CollectionResponse<Foo> actualResults = (CollectionResponse<Foo>) restResponse.getEntity();
    Assert.assertEquals(actualResults.getElements(), expectedElements);
    Assert.assertEquals(actualResults.getMetadataRaw(), expectedMetadata);
    Assert.assertEquals(actualResults.getPaging(), expectedPaging);

    EasyMock.verify(mockContext);
  }

  @DataProvider(name = "exceptionTestData")
  public Object[][] exceptionDataProvider()
  {
    Foo f1 = new Foo().setStringField("f1");

    return new Object[][]
        {
            {Arrays.asList(f1, null),
                "Unexpected null encountered. Null element inside of a List returned by the resource method: "},
            {new CollectionResult<>(null),
                "Unexpected null encountered. Null elements List inside of CollectionResult returned by the resource method: "}
        };
  }

  @Test(dataProvider = "exceptionTestData")
  public void testBuilderExceptions(Object results, String expectedErrorMessage)
      throws URISyntaxException
  {
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    ServerResourceContext mockContext = getMockResourceContext(null, null, null, null, null);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);
    FinderResponseBuilder responseBuilder = new FinderResponseBuilder();
    try
    {
      responseBuilder.buildRestLiResponseData(getRestRequest(), routingResult, results, headers, Collections.emptyList());
      Assert.fail("An exception should have been thrown because of null elements!");
    }
    catch (RestLiServiceException e)
    {
      Assert.assertTrue(e.getMessage().contains(expectedErrorMessage));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public <D extends RestLiResponseData<? extends CollectionResponseEnvelope>> void testProjectionInBuildRestliResponseData() throws URISyntaxException {

    for (Map.Entry<ResourceMethod, CollectionResponseBuilder<?>> entry: BUILDERS.entrySet())
    {
      ResourceMethod resourceMethod = entry.getKey();
      MaskTree maskTree = new MaskTree();
      maskTree.addOperation(new PathSpec("fruitsField"), MaskOperation.POSITIVE_MASK_OP);

      ServerResourceContext mockContext =
          getMockResourceContext(maskTree, null, null, ProjectionMode.AUTOMATIC, ProjectionMode.AUTOMATIC);
      RoutingResult routingResult = new RoutingResult(mockContext, getMockResourceMethodDescriptor());

      List<RecordTemplate> values = new ArrayList<>();
      Foo value = new Foo().setStringField("value").setFruitsField(Fruits.APPLE);
      values.add(value);

      CollectionResponseBuilder<D> responseBuilder = (CollectionResponseBuilder<D>) entry.getValue();
      D responseData = responseBuilder.buildRestLiResponseData(getRestRequest(), routingResult, values,
                                                                                Collections.emptyMap(),
                                                                                Collections.emptyList());
      RecordTemplate record = responseData.getResponseEnvelope().getCollectionResponse().get(0);
      Assert.assertEquals(record.data().size(), 1);
      Assert.assertEquals(record.data().get("fruitsField"), Fruits.APPLE.toString());
    }
  }

  @SuppressWarnings("deprecation")
  private static ServerResourceContext getMockResourceContext(MaskTree dataMaskTree,
                                                        MaskTree metadataMaskTree,
                                                        MaskTree pagingMaskTree,
                                                        ProjectionMode dataProjectionMode,
                                                        ProjectionMode metadataProjectionMode)
      throws URISyntaxException
  {
    ServerResourceContext mockContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockContext.getParameter(EasyMock.<String>anyObject())).andReturn(null).times(2);
    EasyMock.expect(mockContext.getRequestHeaders()).andReturn(ResponseBuilderUtil.getHeaders()).once();

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
    EasyMock.expect(mockDescriptor.getParametersWithType(EasyMock.anyObject())).andReturn(Collections.emptyList()).once();
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
