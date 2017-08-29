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

package com.linkedin.test.util;

import org.testng.Assert;

final public class ExceptionTestUtil
{

  public static void verifyCauseChain(Throwable throwable, Class<?>... causes)
  {
    Throwable t = throwable;
    for (Class<?> c : causes)
    {
      Throwable cause = t.getCause();
      if (cause == null)
      {
        Assert.fail("Cause chain ended too early", throwable);
      }
      if (!c.isAssignableFrom(cause.getClass()))
      {
        Assert.fail("Expected cause " + c.getName() + " not " + cause.getClass().getName(), throwable);
      }
      t = cause;
    }
  }
}
