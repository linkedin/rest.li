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

package com.linkedin.d2.discovery.util;

import org.slf4j.Logger;

/**
 * Logger util to postpone toString() on objects until it's actually needed.
 *
 * @author criccomini
 *
 */
public class LogUtil
{

  public static String join(Object... objectsToJoin)
  {
    StringBuffer joined = new StringBuffer();

    for (Object o : objectsToJoin)
    {
      if (o != null)
      {
        joined.append(o);
      }
      else
      {
        joined.append("null");
      }
    }

    return joined.toString();
  }

  public static void trace(Logger log, Object... objectsToString)
  {
    if (log.isTraceEnabled())
    {
      log.trace(join(objectsToString));
    }
  }

  public static void debug(Logger log, Object... objectsToString)
  {
    if (log.isDebugEnabled())
    {
      log.debug(join(objectsToString));
    }
  }

  public static void info(Logger log, Object... objectsToString)
  {
    if (log.isInfoEnabled())
    {
      log.info(join(objectsToString));
    }
  }

  public static void warn(Logger log, Object... objectsToString)
  {
    if (log.isWarnEnabled())
    {
      log.warn(join(objectsToString));
    }
  }

  public static void error(Logger log, Object... objectsToString)
  {
    if (log.isErrorEnabled())
    {
      log.error(join(objectsToString));
    }
  }
}
