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
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.IntegerDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.StringDataSchema;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.parseq.Context;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.test.SimpleEnum;
import com.linkedin.restli.internal.server.MutablePathKeys;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RoutingException;

import java.io.IOException;
import java.util.Collections;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * @author Soojung Ha
 */
public class TestActionArgumentBuilder
{
  private List<Parameter<?>> getStringAndIntParams()
  {
    List<Parameter<?>> params = new ArrayList<>();
    params.add(new Parameter<>(
        "param1",
        String.class,
        new StringDataSchema(),
        false,
        null,
        Parameter.ParamType.POST,
        true,
        new AnnotationSet(new Annotation[]{})));
    params.add(new Parameter<>(
        "param2",
        Integer.class,
        new IntegerDataSchema(),
        true,
        "1234",
        Parameter.ParamType.POST,
        true,
        new AnnotationSet(new Annotation[]{})));
    return params;
  }

  private List<Parameter<?>> getEnumParams()
  {
    EnumDataSchema simpleEnumSchema = new EnumDataSchema(new Name("com.linkedin.restli.common.test.SimpleEnum"));
    simpleEnumSchema.setSymbols(Arrays.asList("A", "B", "C"), null);
    return Collections.singletonList(new Parameter<>(
        "simpleEnum",
        SimpleEnum.class,
        simpleEnumSchema,
        false,
        null,
        Parameter.ParamType.POST,
        true,
        new AnnotationSet(new Annotation[]{})));
  }

  @SuppressWarnings("rawtypes")
  private List<Parameter<?>> getCallbackParams()
  {
    return Collections.singletonList(new Parameter<>(
        "",
        Callback.class,
        null,
        false,
        null,
        Parameter.ParamType.CALLBACK,
        false,
        new AnnotationSet(new Annotation[]{})));
  }

  private List<Parameter<?>> getParSeqContextParams()
  {
    return Collections.singletonList(new Parameter<>(
        "",
        Context.class,
        null,
        false,
        null,
        Parameter.ParamType.PARSEQ_CONTEXT_PARAM,
        false,
        new AnnotationSet(new Annotation[]{})));
  }

  @SuppressWarnings("deprecation")
  private List<Parameter<?>> getDeprecatedParSeqContextParams()
  {
    return Collections.singletonList(new Parameter<>(
        "",
        Context.class,
        null,
        false,
        null,
        Parameter.ParamType.PARSEQ_CONTEXT,
        false,
        new AnnotationSet(new Annotation[]{})));
  }

  private List<Parameter<?>> getPathKeysParams()
  {
    return Collections.singletonList(new Parameter<>(
        "pathKeys",
        PathKeys.class,
        null,
        false,
        null,
        Parameter.ParamType.PATH_KEYS_PARAM,
        false,
        new AnnotationSet(new Annotation[]{})));
  }

  private List<Parameter<?>> getAssocKeyParams()
  {
    return Collections.singletonList(new Parameter<>(
        "string1",
        String.class,
        new StringDataSchema(),
        false,
        null,
        Parameter.ParamType.ASSOC_KEY_PARAM,
        true,
        new AnnotationSet(new Annotation[]{})));
  }

  @DataProvider(name = "successData")
  private Object[][] successData()
  {
    return new Object[][]
        {
            {
                "{\"param1\":\"testString\"}",
                getStringAndIntParams(),
                new Object[]{"testString", 1234}
            },
            {
                "{\"param1\":\"testString\",\"param2\":5678}",
                getStringAndIntParams(),
                new Object[]{"testString", 5678}
            },
            {
                "{\"simpleEnum\":\"A\"}",
                getEnumParams(),
                new Object[]{SimpleEnum.A}
            },
            {
                "{}",
                getCallbackParams(),
                new Object[]{null}
            },
            {
                "{}",
                getParSeqContextParams(),
                new Object[]{null}
            },
            {
                "{}",
                getDeprecatedParSeqContextParams(),
                new Object[]{null}
            }
        };
  }

  @Test(dataProvider = "successData")
  public void testArgumentBuilderSuccess(String entity, List<Parameter<?>> params, Object[] expectedArgs)
      throws IOException
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, entity);
    DataMap dataMap = DataMapUtils.readMapWithExceptions(request);
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, params, null, null);
    ServerResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(null, null, null, true);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, context, 1);

    RestLiArgumentBuilder argumentBuilder = new ActionArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, dataMap);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    assertEquals(args, expectedArgs);

    verify(request, descriptor, routingResult);
  }

  @DataProvider(name = "failureData")
  private Object[][] failureData()
  {
    return new Object[][]
        {
            {
                "{\"param1\":1234,\"param2\":\"someString\"}",
                getStringAndIntParams(),
                "Parameters of method 'testAction' failed validation.*param1.*cannot be coerced to String" +
                    "(?s).*param2.*cannot be coerced to Integer(?s).*"
            },
            {
                "{\"simpleEnum\":\"D\"}",
                getEnumParams(),
                "Parameters of method 'testAction' failed validation.*is not an enum symbol(?s).*"
            }
        };
  }

  @Test(dataProvider = "failureData")
  public void testExtractRequestDataFailure(String entity, List<Parameter<?>> params, String errorRegEx)
      throws IOException
  {
    RecordDataSchema dataSchema = DynamicRecordMetadata.buildSchema("testAction", params);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, entity);
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, null, "testAction", dataSchema);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, null, 1);

    RestLiArgumentBuilder argumentBuilder = new ActionArgumentBuilder();
    try
    {
      argumentBuilder.extractRequestData(routingResult, DataMapUtils.readMapWithExceptions(request));
      fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      assertTrue(e.getMessage().matches(errorRegEx));
    }

    verify(request, descriptor, routingResult);
  }

  @Test
  public void testBuildArgumentsFailure()
      throws IOException
  {
    String entity = "{\"param2\":5678}";
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, entity);
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, getStringAndIntParams(), null, null);
    ServerResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(null, null, null, false);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, context, 1);

    RestLiArgumentBuilder argumentBuilder = new ActionArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult,
        DataMapUtils.readMapWithExceptions(request));
    try
    {
      argumentBuilder.buildArguments(requestData, routingResult);
      fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      assertEquals(e.getMessage(), "Parameter 'param1' is required");
    }

    verify(request, descriptor, routingResult);
  }

  @DataProvider(name = "keyArgumentData")
  private Object[][] keyArgumentData()
  {
    MutablePathKeys pkeys = new PathKeysImpl().append("string1", "testString");
    return new Object[][]
        {
            {
                getPathKeysParams(),
                pkeys,
                new Object[]{pkeys}
            },
            {
                getAssocKeyParams(),
                pkeys,
                new Object[]{"testString"}
            }
        };
  }

  @Test(dataProvider = "keyArgumentData")
  public void testKeyArguments(List<Parameter<?>> params, MutablePathKeys pathKeys, Object[] expectedArgs)
      throws IOException
  {
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, params, null, null);
    ServerResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(pathKeys, false, true);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, context, 1);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, "{\"a\":\"xyz\",\"b\":123}");

    RestLiArgumentBuilder argumentBuilder = new ActionArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult,
        DataMapUtils.readMapWithExceptions(request));
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    assertEquals(args, expectedArgs);

    verify(descriptor, context, routingResult, request);
  }

}