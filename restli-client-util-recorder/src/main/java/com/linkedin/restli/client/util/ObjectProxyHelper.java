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

package com.linkedin.restli.client.util;


import java.lang.reflect.Method;


/**
 * Handle basic {@link Object} methods for a proxy object since they're also routed through the proxy interceptor.
 *
 * @author jflorencio
 */
final class ObjectProxyHelper
{
  public static Object handleObjectMethods(Class<?> clazz, Object proxy, Method method, Object[] args)
  {
    String methodName = method.getName();
    if ("hashCode".equals(methodName) && args.length == 0)
      return proxyHashCode(proxy);
    else if ("equals".equals(methodName) && args.length == 1)
      return proxyEquals(proxy, args[0]);
    else if ("toString".equals(methodName) && args.length == 0)
      return proxyToString(clazz, proxy);

    throw new UnsupportedOperationException("No support for method [" + method.toGenericString() + "] on this proxy");
  }

  private static Integer proxyHashCode(Object proxy)
  {
    return System.identityHashCode(proxy);
  }

  private static Boolean proxyEquals(Object proxy, Object other)
  {
    return (proxy == other ? Boolean.TRUE : Boolean.FALSE);
  }

  private static String proxyToString(Class<?> clazz, Object proxy)
  {
    return clazz.getName() + "(PatchRecorder Proxy)@" + Integer.toHexString(proxy.hashCode());
  }
}