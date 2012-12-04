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
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.TestConstants;
import com.linkedin.restli.client.BatchGetRequest;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.custom.types.DateCoercer;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.CustomTypes2Builders;
import com.linkedin.restli.examples.greetings.client.CustomTypes3Builders;
import com.linkedin.restli.examples.greetings.client.CustomTypes4Builders;
import com.linkedin.restli.examples.greetings.client.CustomTypesBuilders;
import com.linkedin.restli.examples.groups.api.GroupMembership;
import com.linkedin.restli.examples.groups.client.GroupMembershipsFindByGroupBuilder;
import com.linkedin.restli.internal.client.BatchResponseDecoder;
import com.linkedin.restli.internal.client.CollectionResponseDecoder;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestCustomTypesRequestBuilders
{

  private static final CustomTypesBuilders CUSTOM_TYPES_BUILDERS = new CustomTypesBuilders();
  private static final CustomTypes2Builders CUSTOM_TYPES_2_BUILDERS = new CustomTypes2Builders();
  private static final CustomTypes3Builders CUSTOM_TYPES_3_BUILDERS = new CustomTypes3Builders();
  private static final CustomTypes4Builders CUSTOM_TYPES_4_BUILDERS = new CustomTypes4Builders();

  // test correct request is built for customLong
  @Test
  public void testFinderCustomLong() throws IOException, RestException
  {
    // curl -v GET http://localhost:1338/customTypes?l=20&q=customLong
    String expectedUri = "customTypes?l=20&q=customLong";

    FindRequest<Greeting> request = CUSTOM_TYPES_BUILDERS.findByCustomLong().lParam(new CustomLong(20L)).build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Greeting.class, expectedUri, null);
  }

  // test correct request is built for customLongArray
  @Test
  public void testFinderCustomLongArray() throws IOException, RestException
  {
    // curl -v GET http://localhost:1338/customTypes?ls=2&ls=4&q=customLongArray
    String expectedUri = "customTypes?ls=2&ls=4&q=customLongArray";

    List<CustomLong> ls = new ArrayList<CustomLong>(2);
    ls.add(new CustomLong(2L));
    ls.add(new CustomLong(4L));
    FindRequest<Greeting> request = CUSTOM_TYPES_BUILDERS.findByCustomLongArray().lsParam(ls).build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Greeting.class, expectedUri, null);
  }

  // test correct request is built for custom type collection keys
  @Test
  public void testCollectionGetKey() throws IOException, RestException
  {
    // curl -v GET http://localhost:1338/customTypes2/5
    String expectedUri = "customTypes2/5";

    GetRequest<Greeting> request = CUSTOM_TYPES_2_BUILDERS.get().id(new CustomLong(5L)).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, Greeting.class, expectedUri, null);
  }

  @Test
  public void testCollectionBatchGetKey() throws IOException, RestException
  {
    // curl -v GET http://localhost:1338/customTypes2?ids=1&ids=2&ids=3
    String expectedUri = "customTypes2?ids=1&ids=2&ids=3";

    BatchGetRequest<Greeting> request = CUSTOM_TYPES_2_BUILDERS.batchGet().ids(new CustomLong(1L), new CustomLong(2L), new CustomLong(3L)).build();

    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchResponseDecoder.class, Greeting.class, expectedUri, null);
  }

  @Test
  public void testCollectionChildGetKey() throws IOException, RestException
  {
    //curl -v GET http://localhost:1338/customTypes2/1/customTypes4/7
    String expectedUri = "customTypes2/1/customTypes4/7";

    GetRequest<Greeting> request = CUSTOM_TYPES_4_BUILDERS.get().customTypes2IdKey(new CustomLong(1L)).id(new CustomLong(7L)).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, Greeting.class, expectedUri, null);
  }

  @Test
  public void testAssociationKeys() throws IOException, RestException
  {
    // curl -v GET http://localhost:1338/customTypes3/dateId=13&longId=5
    String expectedUri = "customTypes3/dateId=13&longId=5";

    // normally coercer registration is handled in RestliAnnotationReader,
    // but that isn't run here because this is just a unit test.  So, we need to
    // force registration of the DateCoercer because it isn't contained in Date itself.
    new DateCoercer();

    CustomTypes3Builders.Key key = new CustomTypes3Builders.Key().setLongId(new CustomLong(5L)).setDateId(new Date(13L));
    GetRequest<Greeting> request = CUSTOM_TYPES_3_BUILDERS.get().id(key).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, Greeting.class, expectedUri, null);
  }

  @Test
  public void testAssocKey() throws IOException, RestException
  {
    // curl -v GET http://localhost:1338/customTypes3/dateId=13?q=dateOnly
    String expectedUri = "customTypes3/dateId=13?q=dateOnly";

    // normally coercer registration is handled in RestliAnnotationReader,
    // but that isn't run here because this is just a unit test.  So, we need to
    // force registration of the DateCoercer because it isn't contained in Date itself.
    new DateCoercer();

    FindRequest<Greeting> request = CUSTOM_TYPES_3_BUILDERS.findByDateOnly().dateIdKey(new Date(13L)).build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Greeting.class, expectedUri, null);

  }


  private static void checkRequestBuilder(Request<?> request, ResourceMethod resourceMethod,
                                          Class<? extends RestResponseDecoder> responseDecoderClass, Class<?> templateClass,
                                          String expectedUri, RecordTemplate requestInput)
  {
    assertEquals(request.getInput(), requestInput);
    assertEquals(request.getMethod(), resourceMethod);
    assertEquals(request.getResponseDecoder().getClass(), responseDecoderClass);
    assertEquals(request.getUri().toString(), expectedUri);
  }

}
