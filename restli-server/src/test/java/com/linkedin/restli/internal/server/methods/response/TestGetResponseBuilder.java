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
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.pegasus.generator.examples.Foo;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.GetResult;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.ResourceContext;

import java.util.HashMap;
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
    return new Object[][]
        {
            {getRecord(), HttpStatus.S_200_OK},
            {new GetResult<Foo>(getRecord(), HttpStatus.S_207_MULTI_STATUS),
                HttpStatus.S_207_MULTI_STATUS}
        };
  }

  @Test(dataProvider = "testData")
  public void testBuilder(Object record, HttpStatus httpStatus)
  {
    Map<String, String> headers = getHeaders();
    ResourceContext mockContext = getMockResourceContext();
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();

    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    GetResponseBuilder getResponseBuilder = new GetResponseBuilder();

    AugmentedRestLiResponseData responseData = getResponseBuilder.buildRestLiResponseData(null,
                                                                                          routingResult,
                                                                                          record,
                                                                                          headers);

    PartialRestResponse partialRestResponse = getResponseBuilder.buildResponse(null, responseData);

    EasyMock.verify(mockContext, mockDescriptor);
    Assert.assertEquals(partialRestResponse.getHeaders(), headers);
    Assert.assertEquals(partialRestResponse.getStatus(), httpStatus);
    Assert.assertEquals(partialRestResponse.getEntity(), getProjectedRecord());
  }

  private static ResourceMethodDescriptor getMockResourceMethodDescriptor()
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    EasyMock.expect(mockDescriptor.getMethodType()).andReturn(ResourceMethod.GET).once();
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }

  private static ResourceContext getMockResourceContext()
  {
    ResourceContext mockContext = EasyMock.createMock(ResourceContext.class);
    EasyMock.expect(mockContext.getProjectionMode()).andReturn(ProjectionMode.AUTOMATIC).once();
    final DataMap projectionMask = new DataMap();
    projectionMask.put("stringField", 1);
    EasyMock.expect(mockContext.getProjectionMask()).andReturn(new MaskTree(projectionMask)).once();
    EasyMock.replay(mockContext);
    return mockContext;
  }

  private static Foo getRecord()
  {
    return new Foo().setStringField("foo").setBooleanField(false);
  }

  private static Foo getProjectedRecord()
  {
    return new Foo().setStringField("foo");
  }

  private static Map<String, String> getHeaders()
  {
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("h1", "v1");
    headers.put("h2", "v2");
    return headers;
  }
}
