/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.restli.tools.clientgen.sample;


import com.linkedin.data.DataList;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.restli.client.*;
import com.linkedin.restli.client.base.*;
import com.linkedin.restli.tools.sample.*;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.internal.junit.ArrayAsserts;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Test generated request builders for SimpleGreetingResource
 *
 * @author Min Chen
 */
public class TestGreetingBuilders
{
  @Test
  public void testConstructors() throws IOException
  {
    try {
      Constructor<?>[] constructors = GreetingsRequestBuilders.class.getConstructors();
      Assert.assertNotNull(constructors);
      Assert.assertEquals(constructors.length, 4);
      Assert.assertNotNull(GreetingsRequestBuilders.class.getConstructor());
      Assert.assertNotNull(GreetingsRequestBuilders.class.getConstructor(RestliRequestOptions.class));
      Assert.assertNotNull(GreetingsRequestBuilders.class.getConstructor(String.class));
      Assert.assertNotNull(GreetingsRequestBuilders.class.getConstructor(String.class, RestliRequestOptions.class));
    } catch (NoSuchMethodException e) {
      Assert.fail("GreetingsRequestBuilders class is missing some constructors");
    }
    Assert.assertEquals(GreetingsRequestBuilders.getPrimaryResource(), "greetings", "Incorrect resource path!");
  }

  @Test(dataProvider = "methodDataProvider")
  public void testMethods(String methodName, Class<?> returnType) throws IOException
  {
    try {
      Method accessor = GreetingsRequestBuilders.class.getMethod(methodName);
      Assert.assertEquals(accessor.getReturnType(), returnType);
    } catch (NoSuchMethodException e) {
      Assert.fail("GreetingsRequestBuiders class is missing method " + methodName);
    }
    Assert.assertEquals(GreetingsRequestBuilders.getPrimaryResource(), "greetings", "Incorrect resource path!");
  }

  @Test(dataProvider = "builderParamTypes")
  public void testBuilderParamType(Class<?> builder, Class<?> builderSuper, Type[] superParams)
  {
    Assert.assertEquals(builder.getSuperclass(), builderSuper);
    ParameterizedType baseType = (ParameterizedType)builder.getGenericSuperclass();
    ArrayAsserts.assertArrayEquals(baseType.getActualTypeArguments(), superParams);
  }

  @Test
  public void testBuilderInit()
  {
    final String msg = "Hello";
    SimpleGreeting greeting = new SimpleGreeting().setMessage(msg);
    SimpleGreetingArray greetingArr = new SimpleGreetingArray();
    greetingArr.add(greeting);
    CreateIdRequest<Long, SimpleGreeting> createReq = new GreetingsRequestBuilders().create().input(greeting).build();
    Assert.assertEquals(((SimpleGreeting)createReq.getInputRecord()).getMessage(), msg);
    FindRequest<SimpleGreeting> findReq = new GreetingsRequestBuilders().findByMessage().messageParam(msg).build();
    Assert.assertEquals((String)findReq.getQueryParamsObjects().get("message"), msg);
    ActionRequest<SimpleGreetingArray> actionReq = new GreetingsRequestBuilders().actionGreetingArrayAction().greetingsParam(greetingArr).build();
    Assert.assertEquals((DataList)actionReq.getInputRecord().data().get("greetings"), greetingArr.data());
  }

  @DataProvider
  private static Object[][] methodDataProvider()
  {
    return new Object[][] {
      { "create", GreetingsCreateRequestBuilder.class},
      { "delete", GreetingsDeleteRequestBuilder.class},
      {"batchGet", GreetingsBatchGetRequestBuilder.class},
      {"partialUpdate", GreetingsPartialUpdateRequestBuilder.class},
      {"get", GreetingsGetRequestBuilder.class},
      {"findByMessage", GreetingsFindByMessageRequestBuilder.class},
      {"actionGreetingArrayAction", GreetingsDoGreetingArrayActionRequestBuilder.class},
      {"actionIntArrayAction", GreetingsDoIntArrayActionRequestBuilder.class},
      {"getPrimaryResource", String.class},
      {"options", OptionsRequestBuilder.class},
    };
  }

  @DataProvider
  private static Object[][] builderParamTypes()
  {
    return new Object[][] {
      { GreetingsCreateRequestBuilder.class, CreateIdRequestBuilderBase.class, new Type[] {Long.class, SimpleGreeting.class, GreetingsCreateRequestBuilder.class}},
      { GreetingsDeleteRequestBuilder.class, DeleteRequestBuilderBase.class, new Type[] {Long.class, SimpleGreeting.class, GreetingsDeleteRequestBuilder.class}},
      { GreetingsBatchGetRequestBuilder.class, BatchGetEntityRequestBuilderBase.class, new Type[] {Long.class, SimpleGreeting.class, GreetingsBatchGetRequestBuilder.class} },
      { GreetingsPartialUpdateRequestBuilder.class, PartialUpdateRequestBuilderBase.class, new Type[] {Long.class, SimpleGreeting.class, GreetingsPartialUpdateRequestBuilder.class}},
      { GreetingsGetRequestBuilder.class, GetRequestBuilderBase.class, new Type[] {Long.class, SimpleGreeting.class, GreetingsGetRequestBuilder.class}},
      { GreetingsFindByMessageRequestBuilder.class, FindRequestBuilderBase.class, new Type[] {Long.class, SimpleGreeting.class, GreetingsFindByMessageRequestBuilder.class}},
      { GreetingsDoGreetingArrayActionRequestBuilder.class, ActionRequestBuilderBase.class, new Type[] {Void.class, SimpleGreetingArray.class, GreetingsDoGreetingArrayActionRequestBuilder.class}},
      { GreetingsDoIntArrayActionRequestBuilder.class, ActionRequestBuilderBase.class, new Type[] {Void.class, IntegerArray.class, GreetingsDoIntArrayActionRequestBuilder.class}},
    };
  }

}
