/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.transport.http.client.common.ChannelPoolManagerKey;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManagerKeyBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.security.NoSuchAlgorithmException;

/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class TestChannelPoolManagerKey
{
  private static final long SSL_IDLE_TIMEOUT = 3600;
  private static final long IDLE_TIMEOUT = 10;

  /**
   * checks if getIdleTimeout() returns SSL timeout in case of SSL client, and normal timeout in case of NON SSL client
   */
  @Test
  public void testReturnCorrectIdleTimeout() throws NoSuchAlgorithmException
  {
    ChannelPoolManagerKey SSLKey = getKeyBuilder().setSSLContext(SSLContext.getDefault()).setSSLParameters(new SSLParameters()).build();
    Assert.assertEquals(SSL_IDLE_TIMEOUT, SSLKey.getIdleTimeout());

    ChannelPoolManagerKey plainKey = getKeyBuilder().build();
    Assert.assertEquals(IDLE_TIMEOUT, plainKey.getIdleTimeout());
  }

  private ChannelPoolManagerKeyBuilder getKeyBuilder()
  {
    return new ChannelPoolManagerKeyBuilder().setSslIdleTimeout(SSL_IDLE_TIMEOUT).setIdleTimeout(IDLE_TIMEOUT);
  }
}
