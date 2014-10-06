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

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.IntegerDataSchema;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Soojung Ha
 */
public class TestGetArgumentBuilder
{
  @DataProvider(name = "argumentData")
  private Object[][] argumentData()
  {
    return new Object[][]
        {
            {
                new Parameter<Integer>("myComplexKeyCollectionId",
                    Integer.class,
                    new IntegerDataSchema(),
                    false,
                    null,
                    Parameter.ParamType.ASSOC_KEY_PARAM,
                    false,
                    new AnnotationSet(new Annotation[]{})),
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
                    Parameter.ParamType.ASSOC_KEY_PARAM,
                    false,
                    new AnnotationSet(new Annotation[]{})),
                "myComplexKeyAssociationId",
                new CompoundKey().append("string1", "str1").append("string2", "str2"),
                null
            }
        };
  }

  @Test(dataProvider = "argumentData")
  public void testArgumentBuilder(Parameter<?> param, String keyName, Object keyValue, final DataSchema keySchema) throws RestLiSyntaxException
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
    Assert.assertEquals(args, expectedArgs);

    EasyMock.verify(model, descriptor, context, routingResult, request);
  }
}