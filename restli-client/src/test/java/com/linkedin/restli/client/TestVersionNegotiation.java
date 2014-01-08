/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.client;


import com.linkedin.restli.common.ProtocolVersion;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests protocol version negotiation between the client and the server.
 *
 * @author kparikh
 */
public class TestVersionNegotiation
{
  private static final ProtocolVersion _DEFAULT_VERSION = new ProtocolVersion(1, 0, 0);
  private static final ProtocolVersion _LATEST_VERSION = new ProtocolVersion(2, 0, 0);
  
  @DataProvider(name = "data")
  public Object[][] getProtocolVersionClient()
  {
    ProtocolVersion lessThanDefaultVersion = new ProtocolVersion(0, 5, 0);
    ProtocolVersion betweenDefaultAndLatestVersion = new ProtocolVersion(1, 5, 0);
    ProtocolVersion greaterThanLatestVersion = new ProtocolVersion(2, 5, 0);

    /*
    Generate data to test the following function:
      getProtocolVersion(ProtocolVersion defaultVersion,
                         ProtocolVersion latestVersion,
                         ProtocolVersion announcedVersion,
                         ProtocolVersionOption versionOption)
    */
    return new Object[][]
        {
            // default protocol "advertised" + graceful option => default protocol version
            { _DEFAULT_VERSION, ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, _DEFAULT_VERSION },

            // latest protocol "advertised" + force latest option => latest protocol version
            { _LATEST_VERSION, ProtocolVersionOption.FORCE_USE_LATEST, _LATEST_VERSION },

            // default protocol "advertised" + force latest option => latest protocol version 
            { _DEFAULT_VERSION, ProtocolVersionOption.FORCE_USE_LATEST, _LATEST_VERSION },

            // latest protocol "advertised" + graceful option => latest protocol version 
            { _LATEST_VERSION, ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, _LATEST_VERSION },

            // use the version "advertised" by the server as it is less than the latest protocol version
            { betweenDefaultAndLatestVersion, ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, betweenDefaultAndLatestVersion},

            // force latest option => latest protocol version 
            { betweenDefaultAndLatestVersion, ProtocolVersionOption.FORCE_USE_LATEST, _LATEST_VERSION },

            // force latest option => latest protocol version 
            { lessThanDefaultVersion, ProtocolVersionOption.FORCE_USE_LATEST, _LATEST_VERSION },

            // if servers "advertise" a version that is greater than the latest version we always use the latest version
            { greaterThanLatestVersion, ProtocolVersionOption.FORCE_USE_LATEST, _LATEST_VERSION },

            // if servers "advertise" a version that is greater than the latest version we always use the latest version
            { greaterThanLatestVersion, ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, _LATEST_VERSION },
        };
  }

  @Test(dataProvider = "data")
  public void testProtocolVersionNegotiation(ProtocolVersion announcedVersion,
                                             ProtocolVersionOption versionOption,
                                             ProtocolVersion expectedProtocolVersion)
  {
    Assert.assertEquals(RestClient.getProtocolVersion(_DEFAULT_VERSION,
                                                      _LATEST_VERSION,
                                                      announcedVersion,
                                                      versionOption),
                        expectedProtocolVersion);
  }

  @Test
  public void testAnnouncedVersionLessThanDefault()
  {
    try
    {
      RestClient.getProtocolVersion(_DEFAULT_VERSION,
                                    _LATEST_VERSION,
                                    new ProtocolVersion(0, 0, 0),
                                    ProtocolVersionOption.USE_LATEST_IF_AVAILABLE);
      Assert.fail("Expected a RuntimeException as the announced version is less than the default!");
    }
    catch (RuntimeException e)
    {
      Assert.assertTrue(e.getMessage().contains("Announced version is less than the default version!"));
    }
  }
}
