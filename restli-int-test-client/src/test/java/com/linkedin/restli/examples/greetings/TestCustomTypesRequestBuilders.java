/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.restli.examples.greetings;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.custom.types.DateCoercer;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.CustomTypes2Builders;
import com.linkedin.restli.examples.greetings.client.CustomTypes2RequestBuilders;
import com.linkedin.restli.examples.greetings.client.CustomTypes3Builders;
import com.linkedin.restli.examples.greetings.client.CustomTypes3RequestBuilders;
import com.linkedin.restli.examples.greetings.client.CustomTypes4Builders;
import com.linkedin.restli.examples.greetings.client.CustomTypes4RequestBuilders;
import com.linkedin.restli.examples.greetings.client.CustomTypesBuilders;
import com.linkedin.restli.examples.greetings.client.CustomTypesRequestBuilders;
import com.linkedin.restli.internal.client.BatchResponseDecoder;
import com.linkedin.restli.internal.client.CollectionResponseDecoder;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestCustomTypesRequestBuilders
{
  // test correct request is built for customLong
  @Test(dataProvider = "request1BuilderDataProvider")
  public void testFinderCustomLong(RootBuilderWrapper<Long, Greeting> builders) throws IOException, RestException
  {
    // curl -v GET http://localhost:1338/customTypes?l=20&q=customLong
    String expectedUri = "customTypes?l=20&q=customLong";

    Request<CollectionResponse<Greeting>> request = builders.findBy("CustomLong").setQueryParam("l", new CustomLong(20L)).build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Greeting.class, expectedUri, null);
  }

  // test correct request is built for customLongArray
  @Test(dataProvider = "request1BuilderDataProvider")
  public void testFinderCustomLongArray(RootBuilderWrapper<Long, Greeting> builders) throws IOException, RestException
  {
    // curl -v GET http://localhost:1338/customTypes?ls=2&ls=4&q=customLongArray
    String expectedUri = "customTypes?ls=2&ls=4&q=customLongArray";

    List<CustomLong> ls = new ArrayList<CustomLong>(2);
    ls.add(new CustomLong(2L));
    ls.add(new CustomLong(4L));
    Request<CollectionResponse<Greeting>> request = builders.findBy("CustomLongArray").setQueryParam("ls", ls).build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Greeting.class, expectedUri, null);
  }

  // test correct request is built for custom type collection keys
  @Test(dataProvider = "request2BuilderDataProvider")
  public void testCollectionGetKey(RootBuilderWrapper<CustomLong, Greeting> builders) throws IOException, RestException
  {
    // curl -v GET http://localhost:1338/customTypes2/5
    String expectedUri = "customTypes2/5";

    Request<Greeting> request = builders.get().id(new CustomLong(5L)).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, Greeting.class, expectedUri, null);
  }

  @Test(dataProvider = "request2BuilderDataProvider")
  public void testCollectionBatchGetKey(RootBuilderWrapper<CustomLong, Greeting> builders) throws IOException, RestException
  {
    // curl -v GET http://localhost:1338/customTypes2?ids=1&ids=2&ids=3
    String expectedUri = "customTypes2?ids=1&ids=2&ids=3";

    Request<BatchResponse<Greeting>> request = builders.batchGet().ids(new CustomLong(1L), new CustomLong(2L), new CustomLong(3L)).build();

    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchResponseDecoder.class, Greeting.class, expectedUri, null);
  }

  @Test(dataProvider = "request4BuilderDataProvider")
  public void testCollectionChildGetKey(RootBuilderWrapper<CustomLong, Greeting> builders) throws IOException, RestException
  {
    //curl -v GET http://localhost:1338/customTypes2/1/customTypes4/7
    String expectedUri = "customTypes2/1/customTypes4/7";

    Request<Greeting> request = builders.get().setPathKey("customTypes2Id", new CustomLong(1L)).id(new CustomLong(7L)).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, Greeting.class, expectedUri, null);
  }

  @Test(dataProvider = "request3BuilderDataProvider")
  public void testAssociationKeys(RootBuilderWrapper<CompoundKey, Greeting> builders) throws IOException, RestException
  {
    // curl -v GET http://localhost:1338/customTypes3/dateId=13&longId=5
    String expectedUri = "customTypes3/dateId=13&longId=5";

    // normally coercer registration is handled in RestliAnnotationReader,
    // but that isn't run here because this is just a unit test.  So, we need to
    // force registration of the DateCoercer because it isn't contained in Date itself.
    new DateCoercer();

    CustomTypes3Builders.Key key = new CustomTypes3Builders.Key().setLongId(new CustomLong(5L)).setDateId(new Date(13L));
    Request<Greeting> request = builders.get().id(key).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, Greeting.class, expectedUri, null);
  }

  @Test(dataProvider = "request3BuilderDataProvider")
  public void testAssocKey(RootBuilderWrapper<CompoundKey, Greeting> builders) throws IOException, RestException
  {
    // curl -v GET http://localhost:1338/customTypes3/dateId=13?q=dateOnly
    String expectedUri = "customTypes3/dateId=13?q=dateOnly";

    // normally coercer registration is handled in RestliAnnotationReader,
    // but that isn't run here because this is just a unit test.  So, we need to
    // force registration of the DateCoercer because it isn't contained in Date itself.
    new DateCoercer();

    Request<CollectionResponse<Greeting>> request = builders.findBy("DateOnly").setPathKey("dateId", new Date(13L)).build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Greeting.class, expectedUri, null);

  }

  @SuppressWarnings({"rawtypes", "deprecation"})
  private static void checkRequestBuilder(Request<?> request, ResourceMethod resourceMethod,
                                          Class<? extends RestResponseDecoder> responseDecoderClass, Class<?> templateClass,
                                          String expectedUri, RecordTemplate requestInput)
  {
    assertEquals(request.getInputRecord(), requestInput);
    assertEquals(request.getInput(), requestInput);
    assertEquals(request.getMethod(), resourceMethod);
    assertEquals(request.getResponseDecoder().getClass(), responseDecoderClass);
    assertEquals(RestliUriBuilderUtil.createUriBuilder(request).build().toString(), expectedUri);
    assertEquals(request.getUri().toString(), expectedUri);
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] request1BuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new CustomTypesBuilders()) },
      { new RootBuilderWrapper(new CustomTypesRequestBuilders()) }
    };
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] request2BuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new CustomTypes2Builders()) },
      { new RootBuilderWrapper(new CustomTypes2RequestBuilders()) }
    };
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] request3BuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new CustomTypes3Builders()) },
      { new RootBuilderWrapper(new CustomTypes3RequestBuilders()) }
    };
  }

  @DataProvider
  @SuppressWarnings("rawtypes")
  private static Object[][] request4BuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new CustomTypes4Builders()) },
      { new RootBuilderWrapper(new CustomTypes4RequestBuilders()) }
    };
  }
}
