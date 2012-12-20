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
}
