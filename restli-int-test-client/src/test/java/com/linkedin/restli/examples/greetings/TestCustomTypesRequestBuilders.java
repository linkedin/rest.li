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


package com.linkedin.restli.examples.greetings;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.ProtocolVersion;
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
import com.linkedin.restli.internal.client.BatchEntityResponseDecoder;
import com.linkedin.restli.internal.client.BatchResponseDecoder;
import com.linkedin.restli.internal.client.CollectionResponseDecoder;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
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
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request1BuilderDataProviderCustomLongFinder")
  public void testFinderCustomLong(RootBuilderWrapper<Long, Greeting> builders, ProtocolVersion version, String expectedUri)
    throws IOException, RestException
  {
    Request<CollectionResponse<Greeting>> request = builders.findBy("CustomLong").setQueryParam("l", new CustomLong(20L)).build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, expectedUri, null, version);
  }

  // test correct request is built for customLongArray
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request1BuilderDataProviderCustomLongArrayFinder")
  public void testFinderCustomLongArray(RootBuilderWrapper<Long, Greeting> builders, ProtocolVersion version, String expectedUri)
    throws IOException, RestException
  {
    List<CustomLong> ls = new ArrayList<CustomLong>(2);
    ls.add(new CustomLong(2L));
    ls.add(new CustomLong(4L));
    Request<CollectionResponse<Greeting>> request = builders.findBy("CustomLongArray").setQueryParam("ls", ls).build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, expectedUri, null, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request2BuilderDataProviderEntity")
  public void testCollectionGetKey(RootBuilderWrapper<CustomLong, Greeting> builders, ProtocolVersion version, String expectedUri) throws IOException, RestException
  {
    Request<Greeting> request = builders.get().id(new CustomLong(5L)).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, expectedUri, null, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request2BatchDataProvider")
  public void testCollectionBatchGetKey(ProtocolVersion version, String expectedUri) throws IOException, RestException
  {
    Request<BatchResponse<Greeting>> request = new CustomTypes2Builders().batchGet().ids(new CustomLong(1L), new CustomLong(2L), new CustomLong(3L)).build();

    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchResponseDecoder.class, expectedUri, null, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request2BatchDataProvider")
  public void testCollectionBatchGetEntityKey(ProtocolVersion version, String expectedUri) throws IOException, RestException
  {
    Request<BatchKVResponse<CustomLong, EntityResponse<Greeting>>> request =
      new CustomTypes2RequestBuilders().batchGet().ids(new CustomLong(1L), new CustomLong(2L), new CustomLong(3L)).build();

    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchEntityResponseDecoder.class, expectedUri, null, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request4BuilderDataProviderEntity")
  public void testCollectionChildGetKey(RootBuilderWrapper<CustomLong, Greeting> builders, ProtocolVersion version, String expectedUri)
    throws IOException, RestException
  {
    Request<Greeting> request = builders.get().setPathKey("customTypes2Id", new CustomLong(1L)).id(new CustomLong(7L)).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, expectedUri, null, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request3BuilderDataProviderEntity")
  public void testAssociationKeys(RootBuilderWrapper<CompoundKey, Greeting> builders, ProtocolVersion version, String expectedUri)
    throws IOException, RestException
  {
    // normally coercer registration is handled in RestliAnnotationReader,
    // but that isn't run here because this is just a unit test.  So, we need to
    // force registration of the DateCoercer because it isn't contained in Date itself.
    new DateCoercer();

    CustomTypes3Builders.Key key = new CustomTypes3Builders.Key().setLongId(new CustomLong(5L)).setDateId(new Date(13L));
    Request<Greeting> request = builders.get().id(key).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, expectedUri, null, version);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request3BuilderDataProviderEntityFinder")
  public void testAssocKey(RootBuilderWrapper<CompoundKey, Greeting> builders, ProtocolVersion version, String expectedUri)
    throws IOException, RestException
  {
    // normally coercer registration is handled in RestliAnnotationReader,
    // but that isn't run here because this is just a unit test.  So, we need to
    // force registration of the DateCoercer because it isn't contained in Date itself.
    new DateCoercer();

    Request<CollectionResponse<Greeting>> request = builders.findBy("DateOnly").setPathKey("dateId", new Date(13L)).build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, expectedUri, null, version);

  }

  @SuppressWarnings({"rawtypes", "deprecation"})
  private static void checkRequestBuilder(Request<?> request, ResourceMethod resourceMethod,
                                          Class<? extends RestResponseDecoder> responseDecoderClass,
                                          String expectedUri,
                                          RecordTemplate requestInput,
                                          ProtocolVersion version)
  {
    assertEquals(request.getInputRecord(), requestInput);
    assertEquals(request.getInput(), requestInput);
    assertEquals(request.getMethod(), resourceMethod);
    assertEquals(request.getResponseDecoder().getClass(), responseDecoderClass);
    assertEquals(RestliUriBuilderUtil.createUriBuilder(request, version).build().toString(), expectedUri);
    if (version.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()) <= 0)
    {
      // getUri is deprecated and will only return a 1.0 version of the URI format.
      assertEquals(request.getUri().toString(), expectedUri);
    }
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request1BuilderDataProviderCustomLongFinder")
  private static Object[][] request1BuilderDataProviderCustomLongFinder()
  {
    String uriV1 = "customTypes?l=20&q=customLong";
    String uriV2 = "customTypes?l=20&q=customLong";
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new CustomTypesBuilders()), AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1 },
      { new RootBuilderWrapper<Long, Greeting>(new CustomTypesBuilders()), AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2 },
      { new RootBuilderWrapper<Long, Greeting>(new CustomTypesRequestBuilders()) , AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1},
      { new RootBuilderWrapper<Long, Greeting>(new CustomTypesRequestBuilders()) , AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2}
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request1BuilderDataProviderCustomLongArrayFinder")
  private static Object[][] request1BuilderDataProviderCustomLongArrayFinder()
  {
    String uriV1 = "customTypes?ls=2&ls=4&q=customLongArray";
    String uriV2 = "customTypes?ls=List(2,4)&q=customLongArray";
    return new Object[][] {
      { new RootBuilderWrapper<Long, Greeting>(new CustomTypesBuilders()), AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1 },
      { new RootBuilderWrapper<Long, Greeting>(new CustomTypesBuilders()), AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2 },
      { new RootBuilderWrapper<Long, Greeting>(new CustomTypesRequestBuilders()) , AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1},
      { new RootBuilderWrapper<Long, Greeting>(new CustomTypesRequestBuilders()) , AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2}
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request2BuilderDataProviderEntity")
  private static Object[][] request2BuilderDataProviderEntity()
  {
    String uriV1 = "customTypes2/5";
    String uriV2 = "customTypes2/5";
    return new Object[][] {
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes2Builders()), AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1 },
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes2Builders()), AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2 },
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes2RequestBuilders()), AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1 },
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes2RequestBuilders()), AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request2BatchDataProvider")
  private static Object[][] request2BatchDataProvider()
  {
    String uriV1 = "customTypes2?ids=1&ids=2&ids=3";
    String uriV2 = "customTypes2?ids=List(1,2,3)";
    return new Object[][] {
      { AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1 },
      { AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2 },
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request4BuilderDataProviderEntity")
  private static Object[][] request4BuilderDataProviderEntity()
  {
    String uriV1 = "customTypes2/1/customTypes4/7";
    String uriV2 = "customTypes2/1/customTypes4/7";
    return new Object[][] {
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes4Builders()), AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1 },
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes4Builders()), AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2 },
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes4RequestBuilders()), AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1 },
      { new RootBuilderWrapper<CustomLong, Greeting>(new CustomTypes4RequestBuilders()), AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request3BuilderDataProviderEntity")
  private static Object[][] request3BuilderDataProviderEntity()
  {
    String uriV1 = "customTypes3/dateId=13&longId=5";
    String uriV2 = "customTypes3/(dateId:13,longId:5)";
    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, Greeting>(new CustomTypes3Builders()), AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1 },
      { new RootBuilderWrapper<CompoundKey, Greeting>(new CustomTypes3Builders()), AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2 },
      { new RootBuilderWrapper<CompoundKey, Greeting>(new CustomTypes3RequestBuilders()), AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1 },
      { new RootBuilderWrapper<CompoundKey, Greeting>(new CustomTypes3RequestBuilders()), AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "request3BuilderDataProviderEntityFinder")
  private static Object[][] request3BuilderDataProviderEntityFinder()
  {
    String uriV1 = "customTypes3/dateId=13?q=dateOnly";
    String uriV2 = "customTypes3/(dateId:13)?q=dateOnly";
    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, Greeting>(new CustomTypes3Builders()), AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1 },
      { new RootBuilderWrapper<CompoundKey, Greeting>(new CustomTypes3Builders()), AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2 },
      { new RootBuilderWrapper<CompoundKey, Greeting>(new CustomTypes3RequestBuilders()), AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), uriV1 },
      { new RootBuilderWrapper<CompoundKey, Greeting>(new CustomTypes3RequestBuilders()), AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), uriV2 }
    };
  }
}
