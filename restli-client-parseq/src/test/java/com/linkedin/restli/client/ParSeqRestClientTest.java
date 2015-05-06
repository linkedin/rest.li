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

package com.linkedin.restli.client;


import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.restli.client.test.TestRecord;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Exercise {@link com.linkedin.restli.client.ParSeqRestClient}
 *
 * @author jnwang
 */
public class ParSeqRestClientTest
{
  private Engine                   _engine;
  private ScheduledExecutorService _scheduler;

  @BeforeClass
  public void setUp()
  {
    final int numCores = Runtime.getRuntime().availableProcessors();
    _scheduler = Executors.newScheduledThreadPool(numCores + 1);
    _engine = new EngineBuilder()
        .setTaskExecutor(_scheduler)
        .setTimerScheduler(_scheduler)
        .build();
  }

  @AfterClass
  public void tearDown()
  {
    _engine.shutdown();
    _scheduler.shutdown();
  }

  /**
   * Request that should succeed, using promise
   */
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testRestLiResponsePromise(ProtocolVersionOption versionOption,
                                        ProtocolVersion protocolVersion,
                                        String errorResponseHeaderName) throws InterruptedException
  {
    final long id = 123456789; // arbitrary test id
    final int httpCode = 200;

    final ParSeqRestClient client = mockClient(id, httpCode, protocolVersion);
    final Request<TestRecord> req = mockRequest(TestRecord.class, versionOption);

    final Promise<Response<TestRecord>> promise = client.sendRequest(req);
    promise.await();
    Assert.assertFalse(promise.isFailed());
    final Response<TestRecord> record = promise.get();
    Assert.assertEquals(id, record.getEntity().getId().longValue());
  }

  /**
   * Request that should succeed, using task
   */
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testRestLiResponseTask(ProtocolVersionOption versionOption,
                                     ProtocolVersion protocolVersion,
                                     String errorResponseHeaderName) throws InterruptedException
  {
    final long id = 123456789; // arbitrary test id
    final int httpCode = 200;

    final ParSeqRestClient client = mockClient(id, httpCode, protocolVersion);
    final Request<TestRecord> req = mockRequest(TestRecord.class, versionOption);

    final Task<Response<TestRecord>> task = client.createTask(req);

    _engine.run(task);

    task.await();
    Assert.assertFalse(task.isFailed());
    final Response<TestRecord> record = task.get();
    Assert.assertEquals(id, record.getEntity().getId().longValue());
  }

  /**
   * Request that should fail, using promise
   */
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testRestLiResponseExceptionPromise(ProtocolVersionOption versionOption,
                                                 ProtocolVersion protocolVersion,
                                                 String errorResponseHeaderName) throws InterruptedException
  {
    final String ERR_KEY = "someErr";
    final String ERR_VALUE = "WHOOPS!";
    final String ERR_MSG = "whoops2";
    final int HTTP_CODE = 400;
    final int APP_CODE = 666;

    final ParSeqRestClient client = mockClient(ERR_KEY, ERR_VALUE, ERR_MSG, HTTP_CODE, APP_CODE, protocolVersion, errorResponseHeaderName);
    final Request<EmptyRecord> req = mockRequest(EmptyRecord.class, versionOption);

    final Promise<Response<EmptyRecord>> promise = client.sendRequest(req);
    promise.await();
    Assert.assertTrue(promise.isFailed());
    final Throwable t = promise.getError();
    Assert.assertTrue(t instanceof RestLiResponseException);
    final RestLiResponseException e = (RestLiResponseException) t;
    Assert.assertEquals(HTTP_CODE, e.getStatus());
    Assert.assertEquals(ERR_VALUE, e.getErrorDetails().get(ERR_KEY));
    Assert.assertEquals(APP_CODE, e.getServiceErrorCode());
    Assert.assertEquals(ERR_MSG, e.getServiceErrorMessage());
  }

  /**
   * Request that should fail, using task
   */
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  public void testRestLiResponseExceptionTask(ProtocolVersionOption versionOption,
                                              ProtocolVersion protocolVersion,
                                              String errorResponseHeaderName) throws InterruptedException
  {
    final String ERR_KEY = "someErr";
    final String ERR_VALUE = "WHOOPS!";
    final String ERR_MSG = "whoops2";
    final int HTTP_CODE = 400;
    final int APP_CODE = 666;

    final ParSeqRestClient client = mockClient(ERR_KEY, ERR_VALUE, ERR_MSG, HTTP_CODE, APP_CODE, protocolVersion, errorResponseHeaderName);
    final Request<EmptyRecord> req = mockRequest(EmptyRecord.class, versionOption);

    final Task<Response<EmptyRecord>> task = client.createTask(req);

    _engine.run(task);

    task.await();

    Assert.assertTrue(task.isFailed());
    final Throwable t = task.getError();
    Assert.assertTrue(t instanceof RestLiResponseException);
    final RestLiResponseException e = (RestLiResponseException) t;
    Assert.assertEquals(HTTP_CODE, e.getStatus());
    Assert.assertEquals(ERR_VALUE, e.getErrorDetails().get(ERR_KEY));
    Assert.assertEquals(APP_CODE, e.getServiceErrorCode());
    Assert.assertEquals(ERR_MSG, e.getServiceErrorMessage());
  }

  /**
   * @return a mock Request&lt;T&gt; of the given type.
   */
  private <T extends RecordTemplate> Request<T> mockRequest(final Class<T> clazz, ProtocolVersionOption versionOption)
  {
    return new GetRequest<T>(Collections.<String, String> emptyMap(),
                             clazz,
                             null,
                             new DataMap(),
                             new ResourceSpecImpl(),
                             "/foo",
                             Collections.<String, Object>emptyMap(),
                             new RestliRequestOptionsBuilder().setProtocolVersionOption(versionOption).build());
  }

  /**
   * @return a mock ParSeqRestClient that gives an error
   */
  private ParSeqRestClient mockClient(final String errKey,
                                      final String errValue,
                                      final String errMsg,
                                      final int httpCode,
                                      final int appCode,
                                      final ProtocolVersion protocolVersion,
                                      final String errorResponseHeaderName)
  {
    final ErrorResponse er = new ErrorResponse();

    final DataMap errMap = new DataMap();
    errMap.put(errKey, errValue);
    er.setErrorDetails(new ErrorDetails(errMap));
    er.setStatus(httpCode);
    er.setMessage(errMsg);
    er.setServiceErrorCode(appCode);

    final byte[] mapBytes;
    try
    {
      mapBytes = new JacksonDataCodec().mapToBytes(er.data());
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }

    final Map<String, String> headers = new HashMap<String, String>();
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());
    headers.put(errorResponseHeaderName, RestConstants.HEADER_VALUE_ERROR);

    return new ParSeqRestClient(new RestClient(new MockClient(httpCode, headers, mapBytes),
                                               "http://localhost"));
  }

  /**
   * @return a mock ParSeqRestClient that returns a TestRecord with the given id.
   */
  private ParSeqRestClient mockClient(final long id,
                                      final int httpCode,
                                      final ProtocolVersion protocolVersion)
  {
    final TestRecord record = new TestRecord().setId(id);

    byte[] mapBytes;
    try
    {
      mapBytes = new JacksonDataCodec().mapToBytes(record.data());
    }
    catch (final IOException e)
    {
      throw new RuntimeException(e);
    }

    final Map<String, String> headers = new HashMap<String, String>();
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());

    return new ParSeqRestClient(new RestClient(new MockClient(httpCode, headers, mapBytes),
                                               "http://localhost"));
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "protocolVersions")
  private Object[][] protocolVersions1And2DataProvider()
  {
    return new Object[][] {
        {
          ProtocolVersionOption.USE_LATEST_IF_AVAILABLE,
          AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
          RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE
        },
        {
          ProtocolVersionOption.FORCE_USE_NEXT,
          AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
          RestConstants.HEADER_RESTLI_ERROR_RESPONSE
        }
    };
  }
}
