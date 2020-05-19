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


import com.linkedin.common.callback.CallbackAdapter;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.TestConstants;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.client.StringKeysRequestBuilders;

import com.linkedin.test.util.retry.ThreeRetries;
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests for client response decoders.
 *
 * @author Jun Chen
 */
public class TestResponseDecoder extends RestLiIntegrationTest
{
  /* header size (in KBs) that likely exceed server request limit and will cause container exception. */
  private static int SERVER_HEADER_OVERLOAD_SIZE = 50;

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  /**
   * This test tests 2 things in combo here:
   * 1) BatchEntityResponseDecoder could be invoked in some cases to try to decode a empty response dataMap when
   * non-rest.li server error returns, in this test, we simulate that by passing a over-size URL param
   * {@link com.linkedin.restli.internal.client.ExceptionUtil#exceptionForThrowable(java.lang.Throwable, com.linkedin.restli.internal.client.RestResponseDecoder)}
   *
   * 2) CallbackAdapter and its subclasses could have error while its dealing with error itself, this test make sure it
   * pass the 'new' error to its inner callback's onError method.
   * {@link CallbackAdapter#onError(java.lang.Throwable)}
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "dataProvider", retryAnalyzer = ThreeRetries.class)
  public void testNonRestliServerErrorHandling(RestliRequestOptions requestOptions) throws Exception
  {
    Set<String> keys = new HashSet<String>();
    keys.add(createDataSize(SERVER_HEADER_OVERLOAD_SIZE));
    BatchGetEntityRequest<String, Message> req = new StringKeysRequestBuilders(requestOptions).batchGet().ids(keys).build();
    ResponseFuture<BatchKVResponse<String, EntityResponse<Message>>> batchKVResponseResponseFuture = getClient().sendRequest(req);
    try {
      batchKVResponseResponseFuture.getResponse();
      Assert.fail("Exception should have thrown before this point!");
    } catch (Throwable e) {
      Assert.assertTrue(e instanceof RestLiResponseException);
      Assert.assertEquals(((RestLiResponseException) e).getStatus(), 414);
    }
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "dataProvider")
  private static Object[][] dataProvider()
  {
    return new Object[][] {
      { RestliRequestOptions.DEFAULT_OPTIONS },
      { TestConstants.FORCE_USE_NEXT_OPTIONS }
    };
  }

  /**
   * Creates a string of size @msgSize in KB.
   */
  private static String createDataSize(int msgSize) {
    msgSize = msgSize / 2 * 1024;
    StringBuilder sb = new StringBuilder(msgSize);
    for (int i = 0; i < msgSize; i++)
        sb.append('a');
    return sb.toString();
  }
}
