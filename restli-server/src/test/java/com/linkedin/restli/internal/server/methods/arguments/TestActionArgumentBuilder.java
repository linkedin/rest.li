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
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.IntegerDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.StringDataSchema;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.parseq.Context;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.test.SimpleEnum;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RoutingException;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Soojung Ha
 */
public class TestActionArgumentBuilder
{
  private List<Parameter<?>> getStringAndIntParams()
  {
    List<Parameter<?>> params = new ArrayList<Parameter<?>>();
    params.add(new Parameter<String>(
        "param1",
        String.class,
        new StringDataSchema(),
        false,
        null,
        Parameter.ParamType.POST,
        true,
        new AnnotationSet(new Annotation[]{})));
    params.add(new Parameter<Integer>(
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
    simpleEnumSchema.setSymbols(Arrays.asList(new String[]{"A", "B", "C"}), null);
    List<Parameter<?>> enumParams = new ArrayList<Parameter<?>>();
    enumParams.add(new Parameter<SimpleEnum>(
        "simpleEnum",
        SimpleEnum.class,
        simpleEnumSchema,
        false,
        null,
        Parameter.ParamType.POST,
        true,
        new AnnotationSet(new Annotation[]{})));
    return enumParams;
  }

  private List<Parameter<?>> getCallbackParams()
  {
    List<Parameter<?>> params = new ArrayList<Parameter<?>>();
    @SuppressWarnings("rawtypes")
    Parameter<Callback> param = new Parameter<Callback>(
        "",
        Callback.class,
        null,
        false,
        null,
        Parameter.ParamType.CALLBACK,
        false,
        new AnnotationSet(new Annotation[]{}));
    params.add(param);
    return params;
  }

  private List<Parameter<?>> getParSeqContextParams()
  {
    List<Parameter<?>> params = new ArrayList<Parameter<?>>();
    params.add(new Parameter<Context>(
        "",
        Context.class,
        null,
        false,
        null,
        Parameter.ParamType.PARSEQ_CONTEXT_PARAM,
        false,
        new AnnotationSet(new Annotation[]{})));
    return params;
  }

  private List<Parameter<?>> getDeprecatedParSeqContextParams()
  {
    List<Parameter<?>> params = new ArrayList<Parameter<?>>();
    @SuppressWarnings("deprecation")
    Parameter<Context> param = new Parameter<Context>(
        "",
        Context.class,
        null,
        false,
        null,
        Parameter.ParamType.PARSEQ_CONTEXT,
        false,
        new AnnotationSet(new Annotation[]{}));
    params.add(param);
    return params;
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
  public void testArgumentBuilder(String entity, List<Parameter<?>> params, Object[] expectedArgs) throws RestLiSyntaxException
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, entity, 3);
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, params, null, null);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, null, 1);

    RestLiArgumentBuilder argumentBuilder = new ActionArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);
    Assert.assertEquals(args, expectedArgs);

    EasyMock.verify(request, descriptor, routingResult);
  }

  @DataProvider(name = "failureData")
  private Object[][] failureData()
  {
    return new Object[][]
        {
            {
                "{\"param1\":1234,\"param2\":\"someString\"}",
                getStringAndIntParams(),
                "Parameters of method 'testAction' failed validation with error 'ERROR :: /param1 :: 1234 cannot be coerced to String\n" +
                    "ERROR :: /param2 :: someString cannot be coerced to Integer\n'"
            },
            {
                "{\"simpleEnum\":\"D\"}",
                getEnumParams(),
                "Parameters of method 'testAction' failed validation with error 'ERROR :: /simpleEnum :: \"D\" is not an enum symbol\n'"
            }
        };
  }

  @Test(dataProvider = "failureData")
  public void testExtractRequestDataFailure(String entity, List<Parameter<?>> params, String errorMessage)
  {
    RecordDataSchema dataSchema = DynamicRecordMetadata.buildSchema("testAction", params);
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, entity, 3);
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, null, "testAction", dataSchema);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 1, null, 1);

    RestLiArgumentBuilder argumentBuilder = new ActionArgumentBuilder();
    try
    {
      argumentBuilder.extractRequestData(routingResult, request);
      Assert.fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      Assert.assertEquals(e.getMessage(), errorMessage);
    }

    EasyMock.verify(request, descriptor, routingResult);
  }

  @Test
  public void testBuildArgumentsFailure()
  {
    String entity = "{\"param2\":5678}";
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, entity, 3);
    ResourceMethodDescriptor descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(null, getStringAndIntParams(), "testAction", null);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, null, 1);

    RestLiArgumentBuilder argumentBuilder = new ActionArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    try
    {
      argumentBuilder.buildArguments(requestData, routingResult);
      Assert.fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      Assert.assertEquals(e.getMessage(), "Parameter 'param1' of method 'testAction' is required");
    }

    EasyMock.verify(request, descriptor, routingResult);
  }
}