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

/**
 * $Id: $
 */

package com.linkedin.r2.message.stream;

import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestStreamException
{
  private static final boolean WRITABLE_STACKTRACE_DISABLED = false;

  @Test
  public void testWritableStacktraceDisabled()
  {
    Throwable throwable = new Exception("Inner exception message");
    StreamResponse resposne = new StreamResponseBuilder().build(EntityStreams.emptyStream());
    StreamException exception = new StreamException(resposne, "Outer exception message", throwable,
        WRITABLE_STACKTRACE_DISABLED);

    Assert.assertEquals(exception.getMessage(), "Outer exception message");
    Assert.assertEquals(exception.getStackTrace().length, 0);
    Assert.assertEquals(exception.getResponse().getStatus(), 200);
    Assert.assertNotNull(exception.getCause());
    Assert.assertSame(exception.getCause(), throwable);
    Assert.assertTrue(exception.getCause().getStackTrace().length > 0);
    Assert.assertEquals(exception.getCause().getMessage(), "Inner exception message");
  }
}
