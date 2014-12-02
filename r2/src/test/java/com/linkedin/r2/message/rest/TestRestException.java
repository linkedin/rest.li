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

package com.linkedin.r2.message.rest;

import com.linkedin.data.ByteString;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestRestException
{

  @Test
  public void testNoEntity()
  {
    final String TEST_STRING = "Entity body should not be contained in RestException.toString()";
    RestResponse response = new RestResponseBuilder().setEntity(ByteString.copyString(TEST_STRING, "UTF-8")).build();
    RestException e = new RestException(response);
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    pw.close();
    Assert.assertEquals(sw.toString().indexOf(TEST_STRING), -1, TEST_STRING);
  }

  @Test
  public void testRestExceptionForError()
  {
    Throwable throwable = new Exception("Some exception message");
    String message = throwable.getMessage();
    int expectedStatus = 400;

    RestException restException = RestException.forError(expectedStatus, throwable);

    Assert.assertSame(restException.getCause(), throwable);
    Assert.assertTrue(restException.getMessage().contains(message));
    Assert.assertEquals(restException.getResponse().getStatus(), expectedStatus);
  }
}
