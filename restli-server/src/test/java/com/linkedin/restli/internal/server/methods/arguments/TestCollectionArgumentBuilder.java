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


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.IntegerDataSchema;
import com.linkedin.data.schema.StringDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.internal.server.MutablePathKeys;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RoutingException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * @author Soojung Ha
 */
public class TestCollectionArgumentBuilder
{
  private Parameter<?> getPagingContextParam()
  {
    return new Parameter<PagingContext>(
        "",
        PagingContext.class,
        null,
        false,
        new PagingContext(0, 10),
        Parameter.ParamType.PAGING_CONTEXT_PARAM,
        false,
        new AnnotationSet(new Annotation[]{}));
  }

  private List<Parameter<?>> getFinderParams()
  {
    List<Parameter<?>> finderParams = new ArrayList<Parameter<?>>();
    finderParams.add(getPagingContextParam());
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
    return finderParams;
  }

  @DataProvider(name = "argumentData")
  private Object[][] argumentData()
  {
    List<Parameter<?>> getAllParams = new ArrayList<Parameter<?>>();
    getAllParams.add(getPagingContextParam());
    Map<String, String> getAllContextParams = new HashMap<String, String>();
    getAllContextParams.put("start", "33");
    getAllContextParams.put("count", "444");

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
                getFinderParams(),
                finderContextParams,
                null,
                new Object[]{new PagingContext(33, 444), new Integer(777), null}
            },
            {
                getFinderParams(),
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
  public void testArgumentBuilderSuccess(List<Parameter<?>> params, Map<String, String> contextParams,
                                         MutablePathKeys pathKeys, Object[] expectedArgs)
  {
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, 1, params);
    ResourceContext context;
    if (contextParams != null)
    {
      context = RestLiArgumentBuilderTestHelper.getMockResourceContext(contextParams, true);
    }
    else
    {
      context = RestLiArgumentBuilderTestHelper.getMockResourceContext(pathKeys, false, true);
    }
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, context, 1);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);

    RestLiArgumentBuilder argumentBuilder = new CollectionArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    assertEquals(args, expectedArgs);

    verify(descriptor, context, routingResult, request);
  }

  @DataProvider
  private Object[][] failureData()
  {
    Map<String, String> finderContextParams = new HashMap<String, String>();
    finderContextParams.put("start", "33");
    finderContextParams.put("count", "444");

    Map<String, String> wrongFormatParams = new HashMap<String, String>(finderContextParams);
    wrongFormatParams.put("required", "3.14");

    Map<String, String> missingParams = new HashMap<String, String>(finderContextParams);
    missingParams.put("required", null);

    return new Object[][]
        {
            {
                getFinderParams(),
                wrongFormatParams,
                "must be of type 'java.lang.Integer'"
            },
            {
                getFinderParams(),
                missingParams,
                "Parameter 'required' is required"
            }
        };
  }

  @Test(dataProvider = "failureData")
  public void testFailure(List<Parameter<?>> params, Map<String, String> contextParams, String errorMessage)
  {
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, 1, params);

    //We cannot use RestLiArgumentBuilderTestHelper's getMockResourceContext since this is a failure scenario and
    //getRequestAttachmentReader() will not be called.
    ServerResourceContext context = createMock(ServerResourceContext.class);
    for (String key : contextParams.keySet())
    {
      expect(context.hasParameter(key)).andReturn(true).anyTimes();
      expect(context.getParameter(key)).andReturn(contextParams.get(key));
    }
    replay(context);

    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, context, 1);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);
    RestLiArgumentBuilder argumentBuilder = new CollectionArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    try
    {
      argumentBuilder.buildArguments(requestData, routingResult);
      fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      assertTrue(e.getMessage().contains(errorMessage));
    }
    verify(descriptor, context, routingResult, request);
  }

  @Test
  public void testProjectionParams()
  {
    List<Parameter<?>> finderWithProjectionParams = new ArrayList<Parameter<?>>();
    finderWithProjectionParams.add(new Parameter<String>(
        "key",
        String.class,
        new StringDataSchema(),
        false,
        null,
        Parameter.ParamType.QUERY,
        true,
        new AnnotationSet(new Annotation[]{})));
    finderWithProjectionParams.add(new Parameter<PagingContext>(
        "",
        PagingContext.class,
        null,
        false,
        new PagingContext(0, 10),
        Parameter.ParamType.PAGING_CONTEXT_PARAM,
        false,
        new AnnotationSet(new Annotation[]{})));
    finderWithProjectionParams.add(new Parameter<MaskTree>(
        "",
        MaskTree.class,
        null,
        false,
        null,
        Parameter.ParamType.PROJECTION_PARAM,
        false,
        new AnnotationSet(new Annotation[]{})));
    finderWithProjectionParams.add(new Parameter<MaskTree>(
        "",
        MaskTree.class,
        null,
        false,
        null,
        Parameter.ParamType.METADATA_PROJECTION_PARAM,
        false,
        new AnnotationSet(new Annotation[]{})));
    finderWithProjectionParams.add(new Parameter<MaskTree>(
        "",
        MaskTree.class,
        null,
        false,
        null,
        Parameter.ParamType.PAGING_PROJECTION_PARAM,
        false,
        new AnnotationSet(new Annotation[]{})));
    Map<String, String> finderWithProjectionContextParams = new HashMap<String, String>();
    finderWithProjectionContextParams.put("start", "100");
    finderWithProjectionContextParams.put("count", "15");
    finderWithProjectionContextParams.put("key", "keyString");
    Map<String, Integer> projectionMap = new HashMap<String, Integer>();
    projectionMap.put("a", 1);
    Map<String, Integer> metadataMap = new HashMap<String, Integer>();
    metadataMap.put("intField", 1);
    metadataMap.put("booleanField", 1);
    Map<String, Integer> pagingMap = new HashMap<String, Integer>();
    pagingMap.put("total", 1);

    MaskTree projectionMask = new MaskTree(new DataMap(projectionMap));
    MaskTree metadataMask = new MaskTree(new DataMap(metadataMap));
    MaskTree pagingMask = new MaskTree(new DataMap(pagingMap));

    Object[] expectedArgs = new Object[]{"keyString", new PagingContext(100, 15), projectionMask, metadataMask, pagingMask};

    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, 1, finderWithProjectionParams);
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(finderWithProjectionContextParams, projectionMask, metadataMask, pagingMask,
                                                                                     true);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, context, 1);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);

    RestLiArgumentBuilder argumentBuilder = new CollectionArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    assertEquals(args, expectedArgs);

    verify(descriptor, context, routingResult, request);
  }

  private Parameter<?> getIntArrayParam()
  {
    return new Parameter<int[]>(
        "ints",
        int[].class,
        DataTemplateUtil.getSchema(IntegerArray.class),
        false,
        null,
        Parameter.ParamType.QUERY,
        true,
        new AnnotationSet(new Annotation[]{}));
  }

  @DataProvider(name = "arrayArgument")
  private Object[][] arrayArgument()
  {
    return new Object[][]
        {
            {
                getIntArrayParam(),
                "ints",
                Arrays.asList("101", "102", "103"),
                new Object[]{new int[]{101, 102, 103}}
            },
            {
                getIntArrayParam(),
                "ints",
                Collections.EMPTY_LIST,
                new Object[]{new int[]{}}
            }
        };
  }

  @Test(dataProvider = "arrayArgument")
  public void testArrayArgument(Parameter<?> param, String parameterKey, List<String> parameterValues, Object[] expectedArgs)
  {
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, param);
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(parameterKey, parameterValues,
                                                                                     true);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, context, 1);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);

    RestLiArgumentBuilder argumentBuilder = new CollectionArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    assertEquals(args, expectedArgs);

    verify(descriptor, context, routingResult, request);
  }

  @DataProvider(name = "complexArrayArgument")
  private Object[][] complexArrayArgument()
  {
    Map<String, String> map1 = new HashMap<String, String>();
    map1.put("a", "A1");
    map1.put("b", "111");
    Map<String, String> map2 = new HashMap<String, String>();
    map2.put("a", "A2");
    map2.put("b", "222");
    Map<String, String> map3 = new HashMap<String, String>();
    map3.put("a", "A3");
    map3.put("b", "333");
    DataList data = new DataList();
    data.add(new DataMap(map1));
    data.add(new DataMap(map2));
    data.add(new DataMap(map3));

    return new Object[][]
        {
            {
                new Parameter<MyComplexKey[]>(
                    "myComplexKeys",
                    MyComplexKey[].class,
                    new ArrayDataSchema(DataTemplateUtil.getSchema(MyComplexKey.class)),
                    false,
                    null,
                    Parameter.ParamType.QUERY,
                    true,
                    new AnnotationSet(new Annotation[]{})),
                "myComplexKeys",
                "{b=111, a=A1}",
                data,
                new Object[] {
                    new MyComplexKey[] {
                        new MyComplexKey().setA("A1").setB(111L),
                        new MyComplexKey().setA("A2").setB(222L),
                        new MyComplexKey().setA("A3").setB(333L)
                    }
                }
            }
        };
  }

  @Test(dataProvider = "complexArrayArgument")
  public void testComplexArrayArgument(Parameter<?> param, String parameterKey, String parameterValue, Object structuredParameter, Object[] expectedArgs)
  {
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, param);
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContextWithStructuredParameter(parameterKey, parameterValue, structuredParameter,
                                                                                                            true);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, context, 1);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);

    RestLiArgumentBuilder argumentBuilder = new CollectionArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    assertEquals(args, expectedArgs);

    verify(descriptor, context, routingResult, request);
  }

  @DataProvider(name = "arrayArgumentFailure")
  private Object[][] arrayArgumentFailure()
  {
    return new Object[][]
        {
            {
                getIntArrayParam(),
                "ints",
                Arrays.asList("101", "102.1", "103"),
                "must be of type 'java.lang.Integer'"
            },
            {
                getIntArrayParam(),
                "ints",
                Arrays.asList("101", "apple", "103"),
                "must be of type 'java.lang.Integer'"
            },
            {
                getIntArrayParam(),
                "ints",
                Arrays.asList("101", "102", null),
                "cannot contain null values"
            },
            {
                // test for wrong data schema
                new Parameter<int[]>(
                    "ints",
                    int[].class,
                    new IntegerDataSchema(),
                    false,
                    null,
                    Parameter.ParamType.QUERY,
                    true,
                    new AnnotationSet(new Annotation[]{})),
                "ints",
                Arrays.asList("101", "102", "103"),
                "An array schema is expected"
            }
        };
  }

  @Test(dataProvider = "arrayArgumentFailure")
  public void testArrayArgumentFailure(Parameter<?> param, String parameterKey, List<String> parameterValues, String errorMessage)
  {
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, param);

    //We cannot use RestLiArgumentBuilderTestHelper's getMockResourceContext since this is a failure scenario and
    //getRequestAttachmentReader() will not be called.
    ServerResourceContext context = createMock(ServerResourceContext.class);
    expect(context.hasParameter(parameterKey)).andReturn(true);
    expect(context.getParameterValues(parameterKey)).andReturn(parameterValues);
    replay(context);

    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, context, 1);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);

    RestLiArgumentBuilder argumentBuilder = new CollectionArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    try
    {
      argumentBuilder.buildArguments(requestData, routingResult);
      fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      assertTrue(e.getMessage().contains(errorMessage));
    }

    verify(descriptor, context, routingResult, request);
  }
}