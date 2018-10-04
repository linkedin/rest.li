/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.util;

import io.netty.handler.ssl.SslHandler;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 * @author Dengpan Yin
 */
public class SslHandlerUtil
{
  private static final Logger LOG = LoggerFactory.getLogger(SslHandlerUtil.class);
  public final static String PIPELINE_SSL_HANDLER = "sslHandler";

  /**
   * @param host and port: specifying them, will enable the SSL session resumption features
   */
  public static SslHandler getClientSslHandler(SSLContext sslContext, SSLParameters sslParameters, String host, int port)
  {
    return getSslHandler(sslContext, sslParameters, true, host, port);
  }

  public static SslHandler getServerSslHandler(SSLContext sslContext, SSLParameters sslParameters)
  {
    return getSslHandler(sslContext, sslParameters, false);
  }

  public static SslHandler getSslHandler(SSLContext sslContext, SSLParameters sslParameters, boolean clientMode)
  {
    return getSslHandler(sslContext, sslParameters, clientMode, null, -1);
  }

  /**
   * @param host and port: specifying them, will enable the SSL session resumption features
   */
  private static SslHandler getSslHandler(SSLContext sslContext, SSLParameters sslParameters, boolean clientMode, String host, int port)
  {
    SSLEngine sslEngine;
    if (host == null || port == -1)
    {
      sslEngine = sslContext.createSSLEngine();
    }
    else
    {
      sslEngine = sslContext.createSSLEngine(host, port);
    }
    sslEngine.setUseClientMode(clientMode);
    if (sslParameters != null)
    {
      sslEngine.setSSLParameters(sslParameters);
    }

    return new SslHandler(sslEngine);
  }

  public static void validateSslParameters(SSLContext sslContext, SSLParameters sslParameters)
  {
    // Check if requested parameters are present in the supported params of the context.
    // Log warning for those not present. Throw an exception if none present.
    if (sslParameters != null)
    {
      if (sslContext == null)
      {
        throw new IllegalArgumentException("SSLParameters passed with no SSLContext");
      }

      SSLParameters supportedSSLParameters = sslContext.getSupportedSSLParameters();

      if (sslParameters.getCipherSuites() != null)
      {
        checkContained(supportedSSLParameters.getCipherSuites(),
            sslParameters.getCipherSuites(),
            "cipher suite");
      }

      if (sslParameters.getProtocols() != null)
      {
        checkContained(supportedSSLParameters.getProtocols(),
            sslParameters.getProtocols(),
            "protocol");
      }
    }
  }

  /**
   * Checks if an array is completely or partially contained in another. Logs warnings
   * for one array values not contained in the other. Throws IllegalArgumentException if
   * none are.
   *
   * @param containingArray array to contain another.
   * @param containedArray array to be contained in another.
   * @param valueName - name of the value type to be included in log warning or exception.
   */
  private static void checkContained(String[] containingArray, String[] containedArray, String valueName)
  {
    Set<String> containingSet = new HashSet<String>(Arrays.asList(containingArray));
    Set<String> containedSet = new HashSet<String>(Arrays.asList(containedArray));

    final boolean changed = containedSet.removeAll(containingSet);
    if (!changed)
    {
      throw new IllegalArgumentException("None of the requested " + valueName
          + "s: " + containedSet + " are found in SSLContext");
    }

    if (!containedSet.isEmpty())
    {
      for (String paramValue : containedSet)
      {
        LOG.warn("{} {} requested but not found in SSLContext", valueName, paramValue);
      }
    }
  }
}
