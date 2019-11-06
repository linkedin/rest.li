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
import com.linkedin.r2.util.finalizer.RequestFinalizerManager;


/**
 * Utility methods for modifying the request context.
 *
 * @author Soojung Ha
 */
public class RequestContextUtil
{

  private RequestContextUtil()
  {
    // Can't be instantiated.
  }

  /**
   * Forces the client compression filter to not decompress responses.
   * @param requestContext request context to be modified.
   */
  public static void turnOffResponseDecompression(RequestContext requestContext)
  {
    requestContext.putLocalAttr(R2Constants.RESPONSE_DECOMPRESSION_OFF, true);
  }

  /**
   * Get object with key in the provided {@link RequestContext}.
   *
   * @param key Request context attribute key.
   * @param requestContext Given request context.
   * @param clazz Object class.
   * @param <T> Object class.
   * @return The typed object or null.
   */
  @SuppressWarnings("unchecked")
  public static <T> T getObjectWithKey(String key, RequestContext requestContext, Class<T> clazz)
  {
    final Object object = requestContext.getLocalAttr(key);

    return (clazz.isInstance(object)) ? (T) object : null;
  }

  /**
   * Grabs the server-side {@link RequestFinalizerManager} from the request context.
   *
   * @param requestContext Given request context.
   * @return Server-side RequestFinalizerManager.
   */
  public static RequestFinalizerManager getServerRequestFinalizerManager(RequestContext requestContext)
  {
    return getObjectWithKey(R2Constants.SERVER_REQUEST_FINALIZER_MANAGER_REQUEST_CONTEXT_KEY,
        requestContext, RequestFinalizerManager.class);
  }
}