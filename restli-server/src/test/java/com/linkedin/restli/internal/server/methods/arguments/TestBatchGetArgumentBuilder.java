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
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;


/**
 * @author Soojung Ha
 */
public class TestBatchGetArgumentBuilder
{
  @DataProvider(name = "argumentData")
  private Object[][] argumentData()
  {
    @SuppressWarnings("unchecked")
    Set<Object> complexResourceKeys = new HashSet<Object>(Arrays.asList(
        new ComplexResourceKey<MyComplexKey, EmptyRecord>(
            new MyComplexKey().setA("A1").setB(111L), new EmptyRecord()),
        new ComplexResourceKey<MyComplexKey, EmptyRecord>(
            new MyComplexKey().setA("A2").setB(222L), new EmptyRecord())
    ));

    return new Object[][]
        {
            {
                new HashSet<Object>(Arrays.asList(1, 2, 3))
            },
            {
                new HashSet<Object>(Arrays.asList(
                    new CompoundKey().append("string1", "a").append("string2", "b"),
                    new CompoundKey().append("string1", "x").append("string2", "y")
                ))
            },
            {
                new HashSet<Object>()
            },
            {
                complexResourceKeys
            }
        };
  }

  @Test(dataProvider = "argumentData")
  public void testArgumentBuilderSuccess(Set<Object> batchKeys)
  {
    @SuppressWarnings("rawtypes")
    Parameter<Set> param = new Parameter<Set>("", Set.class, null, false, null, Parameter.ParamType.BATCH, false, new AnnotationSet(new Annotation[]{}));
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, param);
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(null, null, batchKeys, true);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, context, 2);

    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);
    RestLiArgumentBuilder argumentBuilder = new BatchGetArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    Object[] expectedArgs = new Object[]{batchKeys};
    assertEquals(args, expectedArgs);

    verify(descriptor, context, routingResult, request);
  }
}