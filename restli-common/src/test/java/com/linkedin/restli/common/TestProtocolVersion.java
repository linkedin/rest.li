/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.restli.common;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author kparikh
 */
public class TestProtocolVersion
{
  @Test
  public void testInit()
  {
    checkVersions(new ProtocolVersion(1, 2, 3));
    checkVersions(new ProtocolVersion("1.2.3"));
    checkVersions(new ProtocolVersion("1.2.3-alpha"));
    String[] illegalVersions = {"", "1", "1.2", "1.2.illegal", "1.2-illegal", "1.2.3-alpha.beta"};

    for (String illegalVersion: illegalVersions)
    {
      try
      {
        new ProtocolVersion(illegalVersion);
        Assert.fail();
      }
      catch (IllegalArgumentException e)
      {
      }
    }
  }

  private void checkVersions(ProtocolVersion protocolVersion)
  {
    Assert.assertEquals(protocolVersion.getMajor(), 1);
    Assert.assertEquals(protocolVersion.getMinor(), 2);
    Assert.assertEquals(protocolVersion.getPatch(), 3);
  }

  @Test
  public void testComparison()
  {
    Assert.assertEquals(new ProtocolVersion("1.2.3").compareTo(new ProtocolVersion("0.2.3")), 1);
    Assert.assertEquals(new ProtocolVersion("1.2.3").compareTo(new ProtocolVersion("1.1.3")), 1);
    Assert.assertEquals(new ProtocolVersion("1.2.3").compareTo(new ProtocolVersion("1.2.2")), 1);
    Assert.assertEquals(new ProtocolVersion("0.9.1").compareTo(new ProtocolVersion("1.0.7")), -1);
    Assert.assertEquals(new ProtocolVersion("0.9.1").compareTo(new ProtocolVersion("0.9.2")), -1);
    Assert.assertEquals(new ProtocolVersion("4.5.6").compareTo(new ProtocolVersion("4.5.6")), 0);
  }

  @Test
  public void testHashCodeAndEquals()
  {
    ProtocolVersion p1 = new ProtocolVersion("1.0.0");
    ProtocolVersion p2 = new ProtocolVersion("2.0.0");
    ProtocolVersion p3 = new ProtocolVersion("1.0.0");

    Assert.assertEquals(p1, p3);
    Assert.assertEquals(p1.hashCode(), p3.hashCode());
    Assert.assertTrue(!p1.equals(p2));
  }
}