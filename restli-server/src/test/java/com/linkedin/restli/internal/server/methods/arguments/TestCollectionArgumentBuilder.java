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

import com.linkedin.data.schema.IntegerDataSchema;
import com.linkedin.data.schema.StringDataSchema;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Soojung Ha
 */
public class TestCollectionArgumentBuilder
{
  @DataProvider(name = "argumentData")
  private Object[][] argumentData()
  {
    List<Parameter<?>> getAllParams = new ArrayList<Parameter<?>>();
    Parameter<PagingContext> pagingContextParam = new Parameter<PagingContext>(
        "",
        PagingContext.class,
        null,
        false,
        new PagingContext(0, 10),
        Parameter.ParamType.PAGING_CONTEXT_PARAM,
        false,
        new AnnotationSet(new Annotation[]{}));
    getAllParams.add(pagingContextParam);
    Map<String, String> getAllContextParams = new HashMap<String, String>();
    getAllContextParams.put("start", "33");
    getAllContextParams.put("count", "444");

    List<Parameter<?>> finderParams = new ArrayList<Parameter<?>>();
    finderParams.add(pagingContextParam);
    Parameter<Integer> requiredIntParam = new Parameter<Integer>(
        "required",
        Integer.class,
        new IntegerDataSchema(),
        false,
        null,
        Parameter.ParamType.QUERY,
        true,
        new AnnotationSet(new Annotation[]{}));
    finderParams.add(requiredIntParam);
    Parameter<String> optionalStringParam = new Parameter<String>(
        "optional",
        String.class,
        new StringDataSchema(),
        true,
        null,
        Parameter.ParamType.QUERY,
        true,
        new AnnotationSet(new Annotation[]{}));
    finderParams.add(optionalStringParam);
    Map<String, String> finderContextParams = new HashMap<String, String>();
    finderContextParams.put("start", "33");
    finderContextParams.put("count", "444");
    finderContextParams.put("required", "777");
    finderContextParams.put("optional", null);

    Map<String, String> finderContextParamsWithOptionalString = new HashMap<String, String>(finderContextParams);
    finderContextParamsWithOptionalString.put("optional", "someString");

    List<Parameter<?>> finderWithAssocKeyParams = new ArrayList<Parameter<?>>();
    finderWithAssocKeyParams.add(new Parameter<String>(
        "string1",
        String.class,
        new StringDataSchema(),
        false,
        null,
        Parameter.ParamType.ASSOC_KEY_PARAM,
        true,
        new AnnotationSet(new Annotation[]{})));

    return new Object[][]
        {
            {
                getAllParams,
                getAllContextParams,
                null,
                new Object[]{new PagingContext(33, 444)}
            },
            {
                finderParams,
                finderContextParams,
                null,
                new Object[]{new PagingContext(33, 444), new Integer(777), null}
            },
            {
                finderParams,
                finderContextParamsWithOptionalString,
                null,
                new Object[]{new PagingContext(33, 444), new Integer(777), "someString"}
            },
            {
                finderWithAssocKeyParams,
                null,
                new PathKeysImpl().append("string1", "testString"),
                new Object[]{"testString"}
            }
        };
  }
  @Test(dataProvider = "argumentData")
  public void testArgumentBuilder(List<Parameter<?>> params, Map<String, String> contextParams, PathKeys pathKeys, Object[] expectedArgs) throws RestLiSyntaxException
  {
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, 1, params);
    ResourceContext context;
    if (contextParams != null)
    {
      context = RestLiArgumentBuilderTestHelper.getMockResourceContext(contextParams);
    }
    else
    {
      context = RestLiArgumentBuilderTestHelper.getMockResourceContext(pathKeys, false);
    }
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, context, 1);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);

    RestLiArgumentBuilder argumentBuilder = new CollectionArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    Assert.assertEquals(args, expectedArgs);

    EasyMock.verify(descriptor, context, routingResult, request);
  }
}