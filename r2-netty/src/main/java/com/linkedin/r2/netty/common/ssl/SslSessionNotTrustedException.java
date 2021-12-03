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

package com.linkedin.r2.netty.common.ssl;

/**
 * Exception used internally when the client cannot confirm the identity of the server through the session validity check
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class SslSessionNotTrustedException extends RuntimeException
{
  static final long serialVersionUID = 1L;

  public SslSessionNotTrustedException()
  {
    super("The session established didn't pass the SSL Session validity test");
  }

  public SslSessionNotTrustedException(String message) {
    super(message);
  }

  public SslSessionNotTrustedException(String message, Throwable cause) {
    super(message, cause);
  }

  public SslSessionNotTrustedException(Throwable cause) {
    super(cause);
  }
}
