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

package com.linkedin.restli.internal.server.methods.arguments;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Soojung Ha
 */
public class TestBatchGetArgumentBuilder
{
  @DataProvider(name = "argumentData")
  private Object[][] argumentData()
  {
    return new Object[][]
        {
            {
                new HashSet<Object>(Arrays.asList(new Object[]{1, 2, 3}))
            },
            {
                new HashSet<Object>(Arrays.asList(new Object[]{
                    new CompoundKey().append("string1", "a").append("string2", "b"),
                    new CompoundKey().append("string1", "x").append("string2", "y")
                }))
            },
            {
                new HashSet<Object>()
            }
        };
  }

  @Test(dataProvider = "argumentData")
  public void testArgumentBuilder(Set<Object> batchKeys) throws RestLiSyntaxException
  {
    @SuppressWarnings("rawtypes")
    Parameter<Set> param = new Parameter<Set>("", Set.class, null, false, null, Parameter.ParamType.BATCH, false, new AnnotationSet(new Annotation[]{}));
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, param);
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(null, null, batchKeys);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, context, 2);

    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);
    RestLiArgumentBuilder argumentBuilder = new BatchGetArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    Object[] expectedArgs = new Object[]{batchKeys};
    Assert.assertEquals(args, expectedArgs);

    EasyMock.verify(descriptor, context, routingResult, request);
  }
}