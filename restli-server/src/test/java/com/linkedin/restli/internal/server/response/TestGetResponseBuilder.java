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
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.GetResult;
import com.linkedin.restli.server.ProjectionMode;

import com.linkedin.restli.server.RestLiResponseData;
import java.util.Collections;
import java.util.Map;

import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestGetResponseBuilder
{
  @DataProvider(name = "testData")
  public Object[][] dataProvider()
  {
    DataMap projectionDataMap = new DataMap();
    projectionDataMap.put("stringField", MaskOperation.POSITIVE_MASK_OP.getRepresentation());
    MaskTree maskTree = new MaskTree(projectionDataMap);

    ProjectionMode manual = ProjectionMode.MANUAL;
    ProjectionMode auto = ProjectionMode.AUTOMATIC;

    return new Object[][]
        {
            // no projections with null projection masks and auto projection mode
            {getRecord(), HttpStatus.S_200_OK, null, auto},
            {new GetResult<>(getRecord(), HttpStatus.S_207_MULTI_STATUS),
                HttpStatus.S_207_MULTI_STATUS, null, auto},

            // no projections with null projection masks and manual projection mode
            {getRecord(), HttpStatus.S_200_OK, null, manual},
            {new GetResult<>(getRecord(), HttpStatus.S_207_MULTI_STATUS),
                HttpStatus.S_207_MULTI_STATUS, null, manual},

            // no projections with non-null projection masks and manual projection mode
            {getRecord(), HttpStatus.S_200_OK, maskTree, manual},
            {new GetResult<>(getRecord(), HttpStatus.S_207_MULTI_STATUS),
                HttpStatus.S_207_MULTI_STATUS, maskTree, manual},

            // projections with non-null projection masks and auto projection mode
            {getRecord(), HttpStatus.S_200_OK, maskTree, auto},
            {new GetResult<>(getRecord(), HttpStatus.S_207_MULTI_STATUS),
                HttpStatus.S_207_MULTI_STATUS, maskTree, auto}
        };
  }

  @Test(dataProvider = "testData")
  public void testBuilder(Object record, HttpStatus expectedHttpStatus, MaskTree maskTree, ProjectionMode projectionMode)
  {
    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    ServerResourceContext mockContext = getMockResourceContext(maskTree, projectionMode);
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();

    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    GetResponseBuilder getResponseBuilder = new GetResponseBuilder();

    RestLiResponseData<GetResponseEnvelope> responseData = getResponseBuilder.buildRestLiResponseData(null,
                                                                              routingResult,
                                                                              record,
                                                                              headers,
                                                                              Collections.emptyList());

    RestLiResponse restLiResponse = getResponseBuilder.buildResponse(null, responseData);

    EasyMock.verify(mockContext, mockDescriptor);
    ResponseBuilderUtil.validateHeaders(restLiResponse, headers);
    Assert.assertEquals(restLiResponse.getStatus(), expectedHttpStatus);
    if (maskTree == null || projectionMode == ProjectionMode.MANUAL)
    {
      Assert.assertEquals(restLiResponse.getEntity(), getRecord());
    }
    else
    {
      Assert.assertEquals(restLiResponse.getEntity(), getProjectedRecord());
    }
  }

  @Test
  public void testProjectionInBuildRestliResponseData()
  {
    MaskTree maskTree = new MaskTree();
    maskTree.addOperation(new PathSpec("fruitsField"), MaskOperation.POSITIVE_MASK_OP);

    ServerResourceContext mockContext = getMockResourceContext(maskTree, ProjectionMode.AUTOMATIC);
    RoutingResult routingResult = new RoutingResult(mockContext, getMockResourceMethodDescriptor());

    Foo value = new Foo().setStringField("value").setFruitsField(Fruits.APPLE);

    GetResponseBuilder responseBuilder = new GetResponseBuilder();
    RestLiResponseData<GetResponseEnvelope>
        responseData = responseBuilder.buildRestLiResponseData(null, routingResult, value,
                                                                              Collections.emptyMap(),
                                                                              Collections.emptyList());
    RecordTemplate record = responseData.getResponseEnvelope().getRecord();
    Assert.assertEquals(record.data().size(), 1);
    Assert.assertEquals(record.data().get("fruitsField"), Fruits.APPLE.toString());

    EasyMock.verify(mockContext);
  }

  private static ResourceMethodDescriptor getMockResourceMethodDescriptor()
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }

  private static ServerResourceContext getMockResourceContext(MaskTree maskTree, ProjectionMode projectionMode)
  {
    ServerResourceContext mockContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockContext.getProjectionMode()).andReturn(projectionMode).once();
    EasyMock.expect(mockContext.getProjectionMask()).andReturn(maskTree).once();
    EasyMock.replay(mockContext);
    return mockContext;
  }

  private static Foo getRecord()
  {
    return new Foo().setStringField("foo").setBooleanField(false).setFruitsField(Fruits.ORANGE);
  }

  private static Foo getProjectedRecord()
  {
    return new Foo().setStringField("foo");
  }
}
