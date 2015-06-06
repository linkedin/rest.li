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


import com.google.common.collect.ImmutableMap;

import com.linkedin.data.ByteString;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
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
import com.linkedin.restli.common.multiplexer.IndividualRequest;
import com.linkedin.restli.common.multiplexer.IndividualRequestArray;
import com.linkedin.restli.common.multiplexer.IndividualResponse;
import com.linkedin.restli.internal.client.ResponseImpl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

  protected final Request<TestRecord> request1 = fakeGetRequest(ID1);
  protected final Request<TestRecord> request2 = fakeGetRequest(ID2);
  protected final Response<TestRecord> response1 = fakeResponse(ID1);
  protected final Response<TestRecord> response2 = fakeResponse(ID2);

  protected GetRequest<TestRecord> fakeGetRequest(int id)
  {
    return new GetRequestBuilder<Integer, TestRecord>(BASE_URI, TestRecord.class, RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .id(id)
        .setHeaders(HEADERS)
        .build();
  }

  protected CreateRequest<TestRecord> fakeCreateRequest(TestRecord entity)
  {
    return new CreateRequestBuilder<Integer, TestRecord>(BASE_URI, TestRecord.class, RESOURCE_SPEC, RestliRequestOptions.DEFAULT_OPTIONS)
        .input(entity)
        .setHeaders(HEADERS)
        .build();
  }

  protected Response<TestRecord> fakeResponse(int id)
  {
    TestRecord record = fakeEntity(id);
    return new ResponseImpl<TestRecord>(HttpStatus.S_200_OK.getCode(), HEADERS, record, null);
  }

  protected TestRecord fakeEntity(int id)
  {
    return new TestRecord()
        .setId(id)
        .setMessage("message" + id);
  }

  protected String getUri(int id)
  {
    return BASE_URI + "/" + id;
  }

  protected IndividualRequest fakeIndividualRequest(int id, String url, List<IndividualRequest> dependentCalls)
  {
    IndividualRequest request = new IndividualRequest();
    request.setId(id);
    request.setMethod(HttpMethod.GET.name());
    request.setHeaders(new StringMap(HEADERS));
    request.setRelativeUrl(url);
    request.setDependentRequests(new IndividualRequestArray(dependentCalls));
    return request;
  }

  protected IndividualResponse fakeIndividualResponse(int id, RecordTemplate record) throws IOException
  {
    ByteString entity = ByteString.copy(getBytes(record));
    return new IndividualResponse()
        .setId(id)
        .setStatus(HttpStatus.S_200_OK.getCode())
        .setHeaders(new StringMap(HEADERS))
        .setBody(entity);
  }

  protected IndividualResponse fakeIndividualErrorResponse(int id) throws IOException
  {
    return new IndividualResponse()
        .setId(id)
        .setStatus(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode())
        .setHeaders(new StringMap(HEADERS));
  }

  protected RestResponse fakeRestResponse(RecordTemplate entity) throws IOException
  {
    byte[] bytes = getBytes(entity);
    return new RestResponseBuilder()
        .setStatus(HttpStatus.S_200_OK.getCode())
        .setHeaders(HEADERS)
        .setEntity(bytes)
        .build();
  }

  protected RestResponse fakeRestErrorResponse() throws IOException
  {
    return new RestResponseBuilder()
        .setStatus(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode())
        .setHeaders(HEADERS)
        .build();
  }

  protected byte[] getBytes(RecordTemplate entity) throws IOException
  {
    return CODEC.dataTemplateToBytes(entity, true);
  }

  protected void fixUpRecord(RecordTemplate record)
  {
    ValidationOptions validationOptions = new ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT);
    ValidateDataAgainstSchema.validate(record.data(), record.schema(), validationOptions);
  }

  protected void assertEquals(RecordTemplate actual, RecordTemplate expected)
  {
    fixUpRecord(actual);
    Assert.assertEquals(actual, expected);
  }
}
