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
import com.linkedin.restli.common.RestConstants;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests protocol version negotiation between the client and the server.
 *
 * @author kparikh
 */
public class TestVersionNegotiation
{
  private static final ProtocolVersion _BASELINE_VERSION = new ProtocolVersion(1, 0, 0);
  private static final ProtocolVersion _LATEST_VERSION = new ProtocolVersion(2, 0, 0);
  private static final ProtocolVersion _NEXT_VERSION = new ProtocolVersion(3, 0, 0);
  
  @DataProvider(name = "data")
  public Object[][] getProtocolVersionClient()
  {
    ProtocolVersion lessThanDefaultVersion = new ProtocolVersion(0, 5, 0);
    ProtocolVersion betweenDefaultAndLatestVersion = new ProtocolVersion(1, 5, 0);
    ProtocolVersion greaterThanLatestVersion = new ProtocolVersion(2, 5, 0);
    ProtocolVersion greaterThanNextVersion = new ProtocolVersion(3, 5, 0);

    /*
    Generate data to test the following function:
      getProtocolVersion(ProtocolVersion defaultVersion,
                         ProtocolVersion latestVersion,
                         ProtocolVersion nextVersion,
                         ProtocolVersion announcedVersion,
                         ProtocolVersionOption versionOption)
    */
    return new Object[][]
        {
            // baseline protocol "advertised" + graceful option => baseline protocol version
            {_BASELINE_VERSION, ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, _BASELINE_VERSION},

            // latest protocol "advertised" + force latest option => latest protocol version
            { _LATEST_VERSION, ProtocolVersionOption.FORCE_USE_LATEST, _LATEST_VERSION },

            // baseline protocol "advertised" + force latest option => latest protocol version 
            {_BASELINE_VERSION, ProtocolVersionOption.FORCE_USE_LATEST, _LATEST_VERSION },

            // latest protocol "advertised" + graceful option => latest protocol version 
            { _LATEST_VERSION, ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, _LATEST_VERSION },

            // use the version "advertised" by the server as it is less than the latest protocol version
            { betweenDefaultAndLatestVersion, ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, betweenDefaultAndLatestVersion},

            // version greater than the next version "advertised" + graceful option => latest protocol version as
            // servers should support it as well.
            { greaterThanNextVersion, ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, _LATEST_VERSION },

            // force latest option => latest protocol version 
            { betweenDefaultAndLatestVersion, ProtocolVersionOption.FORCE_USE_LATEST, _LATEST_VERSION },

            // force latest option => latest protocol version 
            { lessThanDefaultVersion, ProtocolVersionOption.FORCE_USE_LATEST, _LATEST_VERSION },

            // if servers "advertise" a version that is greater than the latest version we always use the latest version
            { greaterThanLatestVersion, ProtocolVersionOption.FORCE_USE_LATEST, _LATEST_VERSION },

            // if servers "advertise" a version that is greater than the latest version we always use the latest version
            { greaterThanLatestVersion, ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, _LATEST_VERSION },

            // default version "advertised" + force next => next
            {_BASELINE_VERSION, ProtocolVersionOption.FORCE_USE_NEXT, _NEXT_VERSION },

            // latest version "advertised" + force next => next
            { _LATEST_VERSION, ProtocolVersionOption.FORCE_USE_NEXT, _NEXT_VERSION },

            // next "advertised" + force next => next
            { _NEXT_VERSION, ProtocolVersionOption.FORCE_USE_NEXT, _NEXT_VERSION },

            // version between default and latest "advertised" + force next => next
            { betweenDefaultAndLatestVersion, ProtocolVersionOption.FORCE_USE_NEXT, _NEXT_VERSION },

            // version greater than latest "advertised" + force next => next
            { greaterThanLatestVersion, ProtocolVersionOption.FORCE_USE_NEXT, _NEXT_VERSION }
        };
  }

  @DataProvider(name = "versionTestVariations")
  public Object[][] getVersionTestVariations()
  {
    return new Object[][]{
            // Expectation is to get baseline announced version if the input version is null
            {null, null, _BASELINE_VERSION},
            // Expectation is to get baseline announced version every time for input baseline version irrespective of the percentage
            {_BASELINE_VERSION, null, _BASELINE_VERSION},
            {_BASELINE_VERSION, "", _BASELINE_VERSION},
            {_BASELINE_VERSION, "0", _BASELINE_VERSION},
            {_BASELINE_VERSION, "100", _BASELINE_VERSION},
            {_BASELINE_VERSION, "-1", _BASELINE_VERSION},
            {_BASELINE_VERSION, "-32938", _BASELINE_VERSION},
            {_BASELINE_VERSION, "101", _BASELINE_VERSION},
            {_BASELINE_VERSION, "43984", _BASELINE_VERSION},
            {_BASELINE_VERSION, "jfk**j&&j888", _BASELINE_VERSION},
            // Expectation is to get baseline announced version for input latest version if the version percentage is zero
            {_LATEST_VERSION, "0", _BASELINE_VERSION},
            // Expectation is to get latest announced version for input latest version if the version percentage is hundred or incorrect percentage
            {_LATEST_VERSION, "100", _LATEST_VERSION},
            {_LATEST_VERSION, "-1", _LATEST_VERSION},
            {_LATEST_VERSION, "-32938", _LATEST_VERSION},
            {_LATEST_VERSION, "101", _LATEST_VERSION},
            {_LATEST_VERSION, "43984", _LATEST_VERSION},
            {_LATEST_VERSION, "jfk**j&&j888", _LATEST_VERSION},
      };
  }

  @Test(dataProvider = "data")
  public void testProtocolVersionNegotiation(ProtocolVersion announcedVersion,
                                             ProtocolVersionOption versionOption,
                                             ProtocolVersion expectedProtocolVersion)
  {
    Assert.assertEquals(RestClient.getProtocolVersion(_BASELINE_VERSION,
                                                      _LATEST_VERSION,
                                                      _NEXT_VERSION,
                                                      announcedVersion,
                                                      versionOption),
                        expectedProtocolVersion);
  }

  @Test
  public void testAnnouncedVersionLessThanBaseline()
  {
    try
    {
      RestClient.getProtocolVersion(_BASELINE_VERSION,
                                    _LATEST_VERSION,
                                    _NEXT_VERSION,
                                    new ProtocolVersion(0, 0, 0),
                                    ProtocolVersionOption.USE_LATEST_IF_AVAILABLE);
      Assert.fail("Expected a RuntimeException as the announced version is less than the default!");
    }
    catch (RuntimeException e)
    {
      Assert.assertTrue(e.getMessage().contains("Announced version is less than the default version!"));
    }
  }

  @Test(dataProvider = "versionTestVariations")
  public void testAnnouncedVersionWithVersionPercentages(ProtocolVersion versionInput, String versionPercentageInput, ProtocolVersion expectedAnnouncedVersion)
  {
      Map<String, Object> properties = new HashMap<String, Object>();
      properties.put(RestConstants.RESTLI_PROTOCOL_VERSION_PROPERTY, versionInput);
      properties.put(RestConstants.RESTLI_PROTOCOL_VERSION_PERCENTAGE_PROPERTY, versionPercentageInput);
      ProtocolVersion announcedVersion = RestClient.getAnnouncedVersion(properties);
      Assert.assertTrue(expectedAnnouncedVersion.equals(announcedVersion), "Expected Version: " + expectedAnnouncedVersion.toString() + " Actual Version: " + announcedVersion.toString());
  }

}

