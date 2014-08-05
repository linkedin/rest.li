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

  @Test(dataProvider = "data")
  public void testProtocolVersionNegotiation(ProtocolVersion announcedVersion,
                                             ProtocolVersionOption versionOption,
                                             ProtocolVersion expectedProtocolVersion)
  {
    Assert.assertEquals(RestClient.getProtocolVersion(_BASELINE_VERSION,
                                                      _LATEST_VERSION,
                                                      _NEXT_VERSION,
                                                      announcedVersion,
                                                      versionOption,
                                                      RestConstants.RESTLI_LATEST_VERSION_PERCENTAGE_DEFAULT),
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
                                    ProtocolVersionOption.USE_LATEST_IF_AVAILABLE,
                                    RestConstants.RESTLI_LATEST_VERSION_PERCENTAGE_DEFAULT);
      Assert.fail("Expected a RuntimeException as the announced version is less than the default!");
    }
    catch (RuntimeException e)
    {
      Assert.assertTrue(e.getMessage().contains("Announced version is less than the default version!"));
    }
  }

  @Test
  public void testLatestVersionAnnouncedHundredPercent()
  {
      //For 100% latest version, expectation is to get latest version everytime
      ProtocolVersion actualVersion = RestClient.getProtocolVersion(
              _BASELINE_VERSION,
              _LATEST_VERSION,
              _NEXT_VERSION,
              _LATEST_VERSION,
              ProtocolVersionOption.USE_LATEST_IF_AVAILABLE,
              100);
      Assert.assertEquals(_LATEST_VERSION.getMajor(), actualVersion.getMajor(), "Expected Version: " + _LATEST_VERSION.getMajor() + " Actual Version: " + actualVersion.getMajor());
  }

  @Test
  public void testLatestVersionAnnouncedAsZeroPercent()
  {
        //For 0% latest version, expectation is to get baseline version everytime
      ProtocolVersion actualVersion = RestClient.getProtocolVersion(
              _BASELINE_VERSION,
              _LATEST_VERSION,
              _NEXT_VERSION,
              _LATEST_VERSION,
              ProtocolVersionOption.USE_LATEST_IF_AVAILABLE,
              0);
      Assert.assertEquals(_BASELINE_VERSION.getMajor(), actualVersion.getMajor(), "Expected Version: " + _BASELINE_VERSION.getMajor() + " Actual Version: " + actualVersion.getMajor());
  }

  @Test
  public void testDefaultLatestVersionAnnouncedAsNullOrIncorrectFormat()
  {
      //For null or incorrectly formatted latest version percentage, expectation is to get latest version default everytime
      String[] versionInputs = new String[]{null, "", "ksfdjkf", "-1", "-93229", "101", "384948"};
      for(int i=0; i<versionInputs.length; i++)
      {
         Assert.assertEquals(RestClient.getLatestVersionPercentage(versionInputs[i]), RestConstants.RESTLI_LATEST_VERSION_PERCENTAGE_DEFAULT, "For input percentage " + versionInputs[i] +
                " the default latest version percentage of " + RestConstants.RESTLI_LATEST_VERSION_PERCENTAGE_DEFAULT + " is not returned correctly" );
      }
  }


}
