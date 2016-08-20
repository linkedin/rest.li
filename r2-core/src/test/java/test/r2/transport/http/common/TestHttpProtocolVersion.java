/*
   Copyright (c) 2016 LinkedIn Corp.

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

package test.r2.transport.http.common;

import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestHttpProtocolVersion
{
  private static final String HTTP_1_1_LITERALS = "HTTP/1.1";
  private static final String HTTP_2_LITERALS = "HTTP/2";
  private static final String HTTP_2_LITERALS_ALTERNATIVE = "HTTP/2.0";
  private static final String INVALID_HTTP_PROTOCOL = "HTTP/INVALID";

  @DataProvider(name = "versionLiterals")
  public Object[][] versionLiteralsProvider()
  {
    return new Object[][] {
        { HttpProtocolVersion.HTTP_1_1, HTTP_1_1_LITERALS },
        { HttpProtocolVersion.HTTP_2, HTTP_2_LITERALS },
    };
  }

  @Test(dataProvider = "versionLiterals")
  public void testLiterals(HttpProtocolVersion version, String literals)
  {
    Assert.assertEquals(version.literals(), literals);
  }

  @DataProvider(name = "literalVersions")
  public Object[][] literalVersionsProvider()
  {
    return new Object[][] {
        { HTTP_1_1_LITERALS, HttpProtocolVersion.HTTP_1_1 },
        { HTTP_2_LITERALS, HttpProtocolVersion.HTTP_2 },
        { HTTP_2_LITERALS_ALTERNATIVE, HttpProtocolVersion.HTTP_2 },
    };
  }

  @Test(dataProvider = "literalVersions")
  public void testParse(String literals, HttpProtocolVersion version)
  {
    Assert.assertEquals(HttpProtocolVersion.parse(literals), version);
  }

  @Test
  public void testParseInvalid()
  {
    Assert.assertNull(HttpProtocolVersion.parse(INVALID_HTTP_PROTOCOL));
  }
}
