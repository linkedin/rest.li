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

/**
 * $Id: $
 */

package test.r2.callback;

import com.linkedin.common.callback.MultiException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class MultiExceptionTest
{

  @Test
  public void testStack()
  {
    Exception e1 = new Exception();
    e1.setStackTrace(new StackTraceElement[]{ new StackTraceElement("fooClass", "fooMethod", "fooFile", 1) });
    MultiException me = new MultiException(Collections.singleton(e1));
    Exception e2 = new Exception("wraps", me);
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e2.printStackTrace(pw);
    pw.close();
    Assert.assertTrue(sw.toString().indexOf("fooClass") != -1);
  }

  @Test
  public void testStringNoMessage()
  {
    Exception e1 = new Exception("firstCause");
    MultiException me = new MultiException(Collections.singleton(e1));
    String string = me.toString();
    Assert.assertTrue(string.contains("firstCause"));
    Assert.assertTrue(string.contains("java.lang.Exception"));
    Assert.assertTrue(string.contains(MultiException.class.getName()));
  }

  @Test
  public void testStringMessage()
  {
    Exception e1 = new Exception("firstCause");
    MultiException me = new MultiException("overallMessage", Collections.singleton(e1));
    String string = me.toString();
    Assert.assertTrue(string.contains("overallMessage"));
    Assert.assertTrue(string.contains("firstCause"));
    Assert.assertTrue(string.contains("java.lang.Exception"));
    Assert.assertTrue(string.contains(MultiException.class.getName()));
  }

}
