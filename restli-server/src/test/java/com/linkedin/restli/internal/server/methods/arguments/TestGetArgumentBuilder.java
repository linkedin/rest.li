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

import com.linkedin.common.callback.Callback;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.IntegerDataSchema;
import com.linkedin.parseq.Context;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.test.MyComplexKey;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.annotations.HeaderParam;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;


/**
 * @author Soojung Ha
 */
public class TestGetArgumentBuilder
{
  private Parameter<?> getIntegerParam()
  {
    return new Parameter<Integer>("myComplexKeyCollectionId",
        Integer.class,
        new IntegerDataSchema(),
        false,
        null,
        Parameter.ParamType.RESOURCE_KEY,
        false,
        new AnnotationSet(new Annotation[]{}));
  }

  @DataProvider(name = "keyArgumentData")
  private Object[][] keyArgumentData()
  {
    @SuppressWarnings("rawtypes")
    Parameter<ComplexResourceKey> complexResourceKeyParam = new Parameter<ComplexResourceKey>(
      "complexKeyTestId",
      ComplexResourceKey.class,
      null,
      false,
      null,
      Parameter.ParamType.RESOURCE_KEY,
      false,
      new AnnotationSet(new Annotation[]{}));

    return new Object[][]
        {
            {
                getIntegerParam(),
                "myComplexKeyCollectionId",
                new Integer(123),
                new IntegerDataSchema()
            },
            {
                null, // Test for RestLiSimpleResource
                null,
                null,
                null
            },
            {
                new Parameter<CompoundKey>("myComplexKeyAssociationId",
                    CompoundKey.class,
                    null,
                    false,
                    null,
                    Parameter.ParamType.RESOURCE_KEY,
                    false,
                    new AnnotationSet(new Annotation[]{})),
                "myComplexKeyAssociationId",
                new CompoundKey().append("string1", "str1").append("string2", "str2"),
                null
            },
            {
                complexResourceKeyParam,
                "complexKeyTestId",
                new ComplexResourceKey<MyComplexKey, EmptyRecord>(
                    new MyComplexKey().setA("keyString").setB(1234L), new EmptyRecord()),
                null
            }
        };
  }

  @Test(dataProvider = "keyArgumentData")
  public void testKeyArguments(Parameter<?> param, String keyName, Object keyValue, final DataSchema keySchema)
  {
    ResourceModel model;
    if (keyName != null)
    {
      Key key = new Key(keyName, keyValue.getClass(), keySchema);
      model = RestLiArgumentBuilderTestHelper.getMockResourceModel(null, key, false);
    }
    else
    {
      model = RestLiArgumentBuilderTestHelper.getMockResourceModel(null, null, true);
    }
    ResourceMethodDescriptor descriptor;
    if (param != null)
    {
      List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();
      paramList.add(param);
      descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, 2, paramList);
    }
    else
    {
      descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, null);
    }
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(keyName, keyValue, null);
    RoutingResult routingResult;
    if (param != null)
    {
      routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 3, context, 2);
    }
    else
    {
      routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, context, 1);
    }
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);

    RestLiArgumentBuilder argumentBuilder = new GetArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    Object[] expectedArgs;
    if (keyValue == null)
    {
      expectedArgs = new Object[]{};
    }
    else
    {
      expectedArgs = new Object[]{keyValue};
    }
    assertEquals(args, expectedArgs);

    verify(model, descriptor, context, routingResult, request);
  }

  @DataProvider(name = "asyncArgumentData")
  private Object[][] asyncArgumentData()
  {
    List<Parameter<?>> callbackParams = new ArrayList<Parameter<?>>();
    callbackParams.add(getIntegerParam());
    @SuppressWarnings("rawtypes")
    Parameter<Callback> cParam = new Parameter<Callback>(
        "",
        Callback.class,
        null,
        false,
        null,
        Parameter.ParamType.CALLBACK,
        false,
        new AnnotationSet(new Annotation[]{}));
    callbackParams.add(cParam);

    List<Parameter<?>> parSeqContextParams = new ArrayList<Parameter<?>>();
    parSeqContextParams.add(getIntegerParam());
    parSeqContextParams.add(new Parameter<Context>(
        "",
        Context.class,
        null,
        false,
        null,
        Parameter.ParamType.PARSEQ_CONTEXT_PARAM,
        false,
        new AnnotationSet(new Annotation[]{})));

    List<Parameter<?>> deprecatedParSeqContextParams = new ArrayList<Parameter<?>>();
    deprecatedParSeqContextParams.add(getIntegerParam());
    @SuppressWarnings("deprecation")
    Parameter<Context> contextParam = new Parameter<Context>(
        "",
        Context.class,
        null,
        false,
        null,
        Parameter.ParamType.PARSEQ_CONTEXT,
        false,
        new AnnotationSet(new Annotation[]{}));
    deprecatedParSeqContextParams.add(contextParam);


    return new Object[][]
        {
            {
                callbackParams,
            },
            {
                parSeqContextParams
            },
            {
                deprecatedParSeqContextParams
            }
        };
  }

  @Test(dataProvider = "asyncArgumentData")
  public void testAsyncArguments(List<Parameter<?>> paramList)
  {
    String keyName = "myComplexKeyCollectionId";
    Object keyValue = new Integer(123);
    DataSchema keySchema = new IntegerDataSchema();
    Key key = new Key(keyName, keyValue.getClass(), keySchema);

    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(null, key, false);
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, 2, paramList);
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(keyName, keyValue, null);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 3, context, 2);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);

    RestLiArgumentBuilder argumentBuilder = new GetArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    Object[] expectedArgs = new Object[]{keyValue, null};
    assertEquals(args, expectedArgs);

    verify(model, descriptor, context, routingResult, request);
  }

  @Test
  public void testHeaderArgument()
  {
    String keyName = "myComplexKeyCollectionId";
    Object keyValue = new Integer(123);
    DataSchema keySchema = new IntegerDataSchema();
    Key key = new Key(keyName, keyValue.getClass(), keySchema);
    Map<String, String> headers = new HashMap<String, String>();
    String headerString = "An extra string.";
    headers.put("extra", headerString);
    List<Parameter<?>> headerParams = new ArrayList<Parameter<?>>();
    headerParams.add(getIntegerParam());
    HeaderParam annotation = createMock(HeaderParam.class);
    expect(annotation.value()).andReturn("extra");
    AnnotationSet annotationSet = createMock(AnnotationSet.class);
    expect(annotationSet.getAll()).andReturn(new Annotation[]{});
    expect(annotationSet.get(HeaderParam.class)).andReturn(annotation);
    replay(annotation, annotationSet);
    Parameter<String> headerParam = new Parameter<String>(
        "",
        String.class,
        null,
        false,
        null,
        Parameter.ParamType.HEADER,
        false,
        annotationSet);
    headerParams.add(headerParam);

    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(null, key, false);
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, 2, headerParams);
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(keyName, keyValue, null, headers);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 3, context, 2);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);

    RestLiArgumentBuilder argumentBuilder = new GetArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    Object[] expectedArgs = new Object[]{keyValue, headerString};
    assertEquals(args, expectedArgs);

    verify(model, descriptor, context, routingResult, request, annotation, annotationSet);
  }

  @DataProvider(name = "failureData")
  private Object[][] failureData()
  {
    return new Object[][]
        {
            {
                new Parameter<Integer>("myComplexKeyCollectionId",
                    Integer.class,
                    new IntegerDataSchema(),
                    false,
                    null,
                    Parameter.ParamType.RESOURCE_KEY,
                    false,
                    new AnnotationSet(new Annotation[]{})),
                "Parameter 'myComplexKeyCollectionId' is required"
            }
        };
  }

  @Test(dataProvider = "failureData")
  public void testFailure(Parameter<?> param, String errorMessage)
  {
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(null, null, true);
    ResourceMethodDescriptor descriptor;
    List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();
    paramList.add(param);
    descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, 1, paramList);
    Map<String, String> params = new HashMap<String, String>();
    params.put("myComplexKeyCollectionId", null);
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(params);
    RoutingResult routingResult;
    routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, context, 1);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, null, 0);

    RestLiArgumentBuilder argumentBuilder = new GetArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    try
    {
      argumentBuilder.buildArguments(requestData, routingResult);
      fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      assertEquals(e.getMessage(), errorMessage);
    }

    verify(model, descriptor, context, routingResult, request);
  }
}