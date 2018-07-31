/*
 * Copyright (c) 2017 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.restli.examples;


import com.linkedin.restli.common.HttpMethod;

import com.linkedin.restli.server.validation.RestLiValidationFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.function.Consumer;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import static com.linkedin.restli.examples.RestLiIntTestServer.FILTERS_PORT;
import static org.testng.Assert.assertEquals;


abstract class UnstructuredDataResourceTestBase extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    super.init(Arrays.asList(new RestLiValidationFilter()));
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  /**
   * Send a GET request to the input url with default host and port.
   * @param getPartialUrl  partial url to send request to
   * @param validator      validator function for GET response
   * @throws Throwable
   */
  protected void sendGet(String getPartialUrl, Validator<HttpURLConnection> validator) throws Throwable
  {
    HttpURLConnection connection = null;
    try
    {
      URL url = new URL("http://localhost:" + FILTERS_PORT + getPartialUrl);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod(HttpMethod.GET.name());
      connection.setUseCaches(false);
      connection.setDoOutput(true);
      validator.validate(connection);
    }
    finally
    {
      connection.disconnect();
    }
  }

  /**
   * Compare equality of the content of InputStream with expected binaries byte-by-byte.
   */
  protected void assertUnstructuredDataResponse(InputStream actual, byte[] expected) throws IOException
  {
    if (expected != null)
    {
      for (int i = 0; i < expected.length; i++)
      {
        assertEquals(actual.read(), expected[i], "mismatched byte at index: " + i);
      }
    }
    assertEquals(actual.read(), -1, "mismatched EOF");
  }

  /**
   * Basically a {@link Consumer} that is allowed to complains.
   */
  interface Validator<T>
  {
    void validate(T input) throws Throwable;
  }

  protected boolean forceUseStreamServer()
  {
    return true;
  }
}
