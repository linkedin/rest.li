/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.client.multiplexer;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.JacksonDataTemplateCodec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.StringMap;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.client.CreateRequest;
import com.linkedin.restli.client.CreateRequestBuilder;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.GetRequestBuilder;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.multiplexer.IndividualBody;
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.common.multiplexer.IndividualRequestMap;
import com.linkedin.restli.common.multiplexer.IndividualResponse;
import com.linkedin.restli.common.multiplexer.MultiplexedRequestContent;
import com.linkedin.restli.internal.client.ResponseImpl;
import com.linkedin.restli.internal.common.CookieUtil;
import com.linkedin.restli.internal.common.DataMapConverter;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.MimeTypeParseException;

import org.testng.Assert;


/**
 * Base class for multiplexer client side unit tests sharing some helpers/constants.
 */
public class MultiplexerTestBase
{
  protected static final JacksonDataTemplateCodec CODEC = new JacksonDataTemplateCodec();
  protected static final ResourceSpecImpl RESOURCE_SPEC = new ResourceSpecImpl(Collections.<ResourceMethod>emptySet(),
                                                                               null,
                                                                               null,
                                                                               int.class,
                                                                               TestRecord.class,
                                                                               Collections.<String, Object>emptyMap());
  protected static final int ID1 = 1;
  protected static final int ID2 = 2;
  protected static final String BASE_URI = "/some/uri";
  protected static final Map<String, String> HEADERS = ImmutableMap.of("foo", "bar");
  protected static final List<String> COOKIES = Arrays.asList(CookieUtil.encodeSetCookie(new HttpCookie("fooCookie", "barCookie")));

  protected final Request<TestRecord> request1 = fakeGetRequest(ID1);
  protected final Request<TestRecord> request2 = fakeGetRequest(ID2);
  protected final Response<TestRecord> response1 = fakeResponse(ID1);
  protected final Response<TestRecord> response2 = fakeResponse(ID2);

  protected static GetRequest<TestRecord> fakeGetRequest(int id)
  {
    return new GetRequestBuilder<Integer, TestRecord>(BASE_URI, TestRecord.class, RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(id)
        .setHeaders(HEADERS)
        .build();
  }

  protected static CreateRequest<TestRecord> fakeCreateRequest(TestRecord entity)
  {
    return new CreateRequestBuilder<Integer, TestRecord>(BASE_URI, TestRecord.class, RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .input(entity)
        .setHeaders(HEADERS)
        .build();
  }

  protected static Response<TestRecord> fakeResponse(int id)
  {
    TestRecord record = fakeEntity(id);
    return new ResponseImpl<>(HttpStatus.S_200_OK.getCode(), HEADERS, Collections.<HttpCookie>emptyList(), record, null);
  }

  protected static TestRecord fakeEntity(int id)
  {
    return new TestRecord()
        .setId(id)
        .setMessage("message" + id);
  }

  protected static String getUri(int id)
  {
    return BASE_URI + "/" + id;
  }

  protected static IndividualRequest fakeIndividualRequest(String url)
  {
    return fakeIndividualRequest(url, Collections.<String, IndividualRequest>emptyMap());
  }

  protected static IndividualRequest fakeIndividualRequest(String url, Map<String, IndividualRequest> dependentCalls)
  {
    IndividualRequest request = new IndividualRequest();
    request.setMethod(HttpMethod.GET.name());
    request.setHeaders(new StringMap(HEADERS));
    request.setRelativeUrl(url);
    request.setDependentRequests(new IndividualRequestMap(dependentCalls));
    return request;
  }

  protected static IndividualResponse fakeIndividualResponse(RecordTemplate record) throws IOException
  {
    return new IndividualResponse()
        .setStatus(HttpStatus.S_200_OK.getCode())
        .setBody(new IndividualBody(record.data()));
  }

  protected static IndividualResponse fakeIndividualErrorResponse() throws IOException
  {
    return new IndividualResponse()
        .setStatus(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
  }

  private static Map<String, String> normalizeHeaderName(Map<String, String> headers)
  {
    Map<String, String> normalizedHeaders = new HashMap<>();
    for (Map.Entry<String, String> header : headers.entrySet())
    {
      // make all header names lower case
      normalizedHeaders.put(header.getKey().toLowerCase(), header.getValue());
    }
    return normalizedHeaders;
  }

  private static Map<String, String> normalizeSetCookies(List<String> cookies)
  {
    Map<String, String> normalizedSetCookies = new HashMap<>();
    for(HttpCookie cookie : CookieUtil.decodeSetCookies(cookies))
    {
      normalizedSetCookies.put(cookie.getName(), cookie.toString());
    }
    return normalizedSetCookies;
  }

  protected static void assertRestResponseEquals(RestResponse actual, RestResponse expected) throws MimeTypeParseException, IOException
  {
    // validate headers & cookies
    Assert.assertEquals(normalizeHeaderName(actual.getHeaders()), normalizeHeaderName(expected.getHeaders()));
    Assert.assertEquals(normalizeSetCookies(actual.getCookies()), normalizeSetCookies(expected.getCookies()));

    // After the IndividualBody is serialized into an entity byte array, it can no longer guarantee the order of its fields.
    // So, to compare entity, we should de-serialize the entity back to a DataMap in order to perform the equal assertion.
    ByteString actualEntity = actual.getEntity();
    ByteString expectedEntity = expected.getEntity();

    if (actualEntity.isEmpty() || expectedEntity.isEmpty())
    {
      Assert.assertEquals(actual, expected);
    }
    else
    {
      DataMap actualDataMap = DataMapConverter.bytesToDataMap(actual.getHeaders(), actualEntity);
      DataMap expectedDataMap = DataMapConverter.bytesToDataMap(expected.getHeaders(), expectedEntity);
      Assert.assertEquals(actualDataMap, expectedDataMap);
      // Compare the rest of RestResponse (excluding the entity)
      RestResponse actualRestResponseWithoutEntity = actual.builder().setEntity(ByteString.empty()).build();
      RestResponse expectedRestResponseWithoutEntity = expected.builder().setEntity(ByteString.empty()).build();
      Assert.assertEquals(actualRestResponseWithoutEntity, expectedRestResponseWithoutEntity);
    }
  }

  protected static RestResponse fakeRestResponse(RecordTemplate entity) throws IOException
  {
    byte[] bytes = getBytes(entity);

    return new RestResponseBuilder()
        .setStatus(HttpStatus.S_200_OK.getCode())
        .setHeaders(HEADERS)
        .setCookies(COOKIES)
        .setEntity(bytes)
        .build();
  }

  protected static RestResponse fakeRestErrorResponse() throws IOException
  {
    return new RestResponseBuilder()
        .setStatus(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode())
        .setHeaders(HEADERS)
        .setCookies(COOKIES)
        .build();
  }

  protected static void assertIndividualRequestEquals(IndividualRequest actual, IndividualRequest expected)
  {
    Assert.assertNotNull(actual);
    Assert.assertEquals(actual.getMethod(), expected.getMethod());
    Assert.assertEquals(actual.getRelativeUrl(), expected.getRelativeUrl());
    Assert.assertEquals(actual.getHeaders(), expected.getHeaders());
    Assert.assertEquals(actual.getBody(), expected.getBody());
    Assert.assertEquals(actual.getDependentRequests(), expected.getDependentRequests());
  }

  protected static void assertMultiplexedRequestContentEquals(MultiplexedRequestContent actual, MultiplexedRequestContent expected)
  {
    IndividualRequestMap actualRequests = actual.getRequests();
    IndividualRequestMap expectedRequests = expected.getRequests();
    Assert.assertEquals(actualRequests.size(), expectedRequests.size());
    for (String id : expectedRequests.keySet())
    {
      assertIndividualRequestEquals(actualRequests.get(id), expectedRequests.get(id));
    }
  }

  protected static byte[] getBytes(RecordTemplate entity) throws IOException
  {
    return CODEC.dataTemplateToBytes(entity, true);
  }
}
