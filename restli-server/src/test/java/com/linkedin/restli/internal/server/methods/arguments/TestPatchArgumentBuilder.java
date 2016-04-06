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

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.IntegerDataSchema;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.PatchRequest;
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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.verify;


/**
 * @author Soojung Ha
 */
public class TestPatchArgumentBuilder
{
  @DataProvider(name = "argumentData")
  private Object[][] argumentData()
  {
    @SuppressWarnings("rawtypes")
    Parameter<?> patchParam = new Parameter<PatchRequest>(
        "",
        PatchRequest.class,
        null,
        false,
        null,
        Parameter.ParamType.POST,
        false,
        new AnnotationSet(new Annotation[]{}));

    List<Parameter<?>> collectionResourceParams = new ArrayList<Parameter<?>>();
    collectionResourceParams.add(new Parameter<Integer>(
        "myComplexKeyCollectionId",
        Integer.class,
        new IntegerDataSchema(),
        false,
        null,
        Parameter.ParamType.RESOURCE_KEY,
        false,
        new AnnotationSet(new Annotation[]{})));
    collectionResourceParams.add(patchParam);

    List<Parameter<?>> simpleResourceParams = new ArrayList<Parameter<?>>();
    simpleResourceParams.add(patchParam);

    List<Parameter<?>> associationResourceParams = new ArrayList<Parameter<?>>();
    associationResourceParams.add(new Parameter<CompoundKey>(
        "myComplexKeyAssociationId",
        CompoundKey.class,
        null,
        false,
        null,
        Parameter.ParamType.RESOURCE_KEY,
        false,
        new AnnotationSet(new Annotation[]{})));
    associationResourceParams.add(patchParam);

    List<Parameter<?>> complexResourceKeyParams = new ArrayList<Parameter<?>>();
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
    complexResourceKeyParams.add(complexResourceKeyParam);
    complexResourceKeyParams.add(patchParam);

    return new Object[][]
        {
            {
                collectionResourceParams,
                new Key("myComplexKeyCollectionId", Integer.class, new IntegerDataSchema()),
                "myComplexKeyCollectionId",
                1234
            },
            {
                simpleResourceParams,
                null,
                null,
                null
            },
            {
                associationResourceParams,
                new Key("myComplexKeyAssociationId", CompoundKey.class, null),
                "myComplexKeyAssociationId",
                new CompoundKey().append("string1", "apples").append("string2", "oranges")
            },
            {
                complexResourceKeyParams,
                new Key("complexKeyTestId", ComplexResourceKey.class, null),
                "complexKeyTestId",
                new ComplexResourceKey<MyComplexKey, EmptyRecord>(
                    new MyComplexKey().setA("keyString").setB(1234L), new EmptyRecord())
            }
        };
  }

  @Test(dataProvider = "argumentData")
  public void testArgumentBuilderSuccess(List<Parameter<?>> params, Key key, String keyName, Object keyValue)
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, "{\"patch\":{\"$set\":{\"a\":\"someString\"}}}", 1);
    ResourceModel model = RestLiArgumentBuilderTestHelper.getMockResourceModel(null, key, true);
    ResourceMethodDescriptor descriptor;
    if (key != null)
    {
      descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, 2, params);
    }
    else
    {
      descriptor = RestLiArgumentBuilderTestHelper.getMockResourceMethodDescriptor(model, 1, params);
    }
    ResourceContext context = RestLiArgumentBuilderTestHelper.getMockResourceContext(keyName, keyValue, null, true);
    RoutingResult routingResult;
    if (key != null)
    {
      routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 3, context, 2);
    }
    else
    {
      routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult(descriptor, 2, context, 1);
    }

    RestLiArgumentBuilder argumentBuilder = new PatchArgumentBuilder();
    RestLiRequestData requestData = argumentBuilder.extractRequestData(routingResult, request);
    Object[] args = argumentBuilder.buildArguments(requestData, routingResult);

    if (keyValue != null)
    {
      Assert.assertEquals(args.length, 2);
      Assert.assertEquals(args[0], keyValue);
    }

    Assert.assertTrue(args[args.length - 1] instanceof PatchRequest);
    Map<String, Object> aMap = new HashMap<String, Object>();
    aMap.put("a", "someString");
    Map<String, Object> setMap = new HashMap<String, Object>();
    setMap.put("$set", new DataMap(aMap));
    Map<String, Object> data = new HashMap<String, Object>();
    data.put("patch", new DataMap(setMap));
    PatchRequest<MyComplexKey> patch = new PatchRequest<MyComplexKey>(new DataMap(data));
    Assert.assertEquals(args[args.length - 1], patch);

    verify(request, model, descriptor, context, routingResult);
  }

  @DataProvider
  private Object[][] failurePatchData()
  {
    return new Object[][]
        {
            {"{\"patch\":{\"$set\":{\"a\":\"someString\"}}"},
            {"{\"patch\":{\"$set\":{1:\"someString\"}}}"},
            {"{\"patch:{\"$set\":{\"a\":\"someString\"}}}"},
            {"random string"}
        };
  }

  @Test(dataProvider = "failurePatchData")
  public void testFailure(String entity)
  {
    RestRequest request = RestLiArgumentBuilderTestHelper.getMockRequest(false, entity, 1);
    RoutingResult routingResult = RestLiArgumentBuilderTestHelper.getMockRoutingResult();

    RestLiArgumentBuilder argumentBuilder = new PatchArgumentBuilder();
    try
    {
      argumentBuilder.extractRequestData(routingResult, request);
      Assert.fail("Expected RoutingException");
    }
    catch (RoutingException e)
    {
      Assert.assertTrue(e.getMessage().contains("Error parsing entity body"));
    }

    verify(request, routingResult);
  }
}
