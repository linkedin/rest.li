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

package com.linkedin.r2.transport.http.client.common.ssl;

import javax.net.ssl.SSLSession;

/**
 * The interface is used to verify the validity of a session.
 * The method will be invoked before each request is being sent to the server.
 *
 * An example can be verifying the certificate or principal of the server you are requesting resources to,
 * to confirm that the identity of the server is the expected one.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public interface SslSessionValidator
{
  void validatePeerSession(SSLSession sslSession) throws SslSessionNotTrustedException;
}
