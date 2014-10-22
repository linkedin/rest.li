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
import com.linkedin.pegasus.generator.examples.Foo;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.AugmentedRestLiResponseData;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
  public Object[][] dataProvider() {
    Foo f1 = new Foo().setStringField("f1");
    Foo f2 = new Foo().setStringField("f2");
    Foo metadata = new Foo().setStringField("metadata");

    List<Foo> results = Arrays.asList(f1, f2);
    CollectionResult<Foo, Foo> collectionResult = new CollectionResult<Foo, Foo>(results, 2, metadata);

    return new Object[][]
        {
            {results, null},
            {collectionResult, metadata.data()},
        };
  }

  @SuppressWarnings("unchecked")
  @Test(dataProvider = "testData")
  public void testBuilder(Object results, DataMap metadata) throws URISyntaxException
  {
    Map<String, String> headers = getHeaders();

    ResourceContext mockContext = getMockResourceContext();
    ResourceMethodDescriptor mockDescriptor = getMockResourceMethodDescriptor();
    RoutingResult routingResult = new RoutingResult(mockContext, mockDescriptor);

    CollectionResponseBuilder responseBuilder = new CollectionResponseBuilder();
    AugmentedRestLiResponseData responseData = responseBuilder.buildRestLiResponseData(getRestRequest(),
                                                                                       routingResult,
                                                                                       results,
                                                                                       headers);
    PartialRestResponse restResponse = responseBuilder.buildResponse(routingResult, responseData);

    EasyMock.verify(mockContext, mockDescriptor);
    Assert.assertEquals(restResponse.getHeaders(), headers);
    CollectionResponse<Foo> actualResults = (CollectionResponse<Foo>) restResponse.getEntity();
    if (results instanceof CollectionResult)
    {
      Assert.assertEquals(actualResults.getElements(), ((CollectionResult) results).getElements());
    }
    else
    {
      Assert.assertEquals(actualResults.getElements(), results);
    }
    Assert.assertEquals(actualResults.getMetadataRaw(), metadata);
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
    Map<String, String> headers = getHeaders();
    ResourceContext mockContext = getMockResourceContext();
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

  private static ResourceContext getMockResourceContext()
      throws URISyntaxException
  {
    ResourceContext mockContext = EasyMock.createMock(ResourceContext.class);
    EasyMock.expect(mockContext.getParameter(EasyMock.<String>anyObject())).andReturn(null).times(2);
    EasyMock.expect(mockContext.getRequestHeaders()).andReturn(getHeaders()).once();
    EasyMock.expect(mockContext.getRawRequest()).andReturn(getRestRequest()).once();
    EasyMock.expect(mockContext.getProjectionMode()).andReturn(ProjectionMode.MANUAL).times(2);
    EasyMock.replay(mockContext);
    return mockContext;
  }

  private static ResourceMethodDescriptor getMockResourceMethodDescriptor()
  {
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    EasyMock.expect(mockDescriptor.getMethodType()).andReturn(ResourceMethod.FINDER).once();
    EasyMock.expect(mockDescriptor.getParametersWithType(EasyMock.<Parameter.ParamType>anyObject())).andReturn(Collections.<Parameter<?>>emptyList()).once();
    EasyMock.replay(mockDescriptor);
    return mockDescriptor;
  }

  private static Map<String, String> getHeaders()
  {
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("h1", "v1");
    headers.put("h2", "v2");
    return headers;
  }

  private static RestRequest getRestRequest()
      throws URISyntaxException
  {
    return new RestRequestBuilder(new URI("/?q=finder")).build();
  }
}
