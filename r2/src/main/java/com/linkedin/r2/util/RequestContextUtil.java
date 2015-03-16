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

package com.linkedin.r2.util;


import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;

/**
 * Utility methods for modifying the request context.
 *
 * @author Soojung Ha
 */
public class RequestContextUtil
{
  /**
   * Forces the client compression filter to not decompress responses.
   * @param requestContext request context to be modified.
   */
  public static void turnOffResponseDecompression(RequestContext requestContext)
  {
    requestContext.putLocalAttr(R2Constants.RESPONSE_DECOMPRESSION_OFF, true);
  }
}