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

import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.mockito.Mockito;
import com.linkedin.common.callback.Callback;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;


/**
 * Tests protocol version negotiation between the client and the server.
 *
 * @author kparikh
 */
public class TestVersionNegotiation
{
  private static final ProtocolVersion _BASELINE_VERSION = AllProtocolVersions.BASELINE_PROTOCOL_VERSION;
  private static final ProtocolVersion _PREV_VERSION = AllProtocolVersions.PREVIOUS_PROTOCOL_VERSION;
  private static final ProtocolVersion _LATEST_VERSION = new ProtocolVersion(3, 0, 0);
  private static final ProtocolVersion _NEXT_VERSION = new ProtocolVersion(3, 0, 0);
  private static final String TEST_URI_PREFIX = "http://localhost:1338/";
  private static final String TEST_SERVICE_NAME = "serviceName";

  @DataProvider(name = "data")
  public Object[][] getProtocolVersionClient()
  {
    ProtocolVersion lessThanDefaultVersion = new ProtocolVersion(0, 5, 0);
    ProtocolVersion betweenDefaultAndLatestVersion = new ProtocolVersion(2, 5, 0);
    ProtocolVersion greaterThanLatestVersion = new ProtocolVersion(3, 5, 0);
    ProtocolVersion greaterThanNextVersion = new ProtocolVersion(3, 5, 0);

    /*
    Generate data to test the following function:
      getProtocolVersion(ProtocolVersion defaultVersion,
                         ProtocolVersion previousVersion,
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
            { greaterThanLatestVersion, ProtocolVersionOption.FORCE_USE_NEXT, _NEXT_VERSION },

            // default version "advertised" + force next => next
            {_BASELINE_VERSION, ProtocolVersionOption.FORCE_USE_PREVIOUS, _PREV_VERSION },

            // latest version "advertised" + force next => next
            { _LATEST_VERSION, ProtocolVersionOption.FORCE_USE_PREVIOUS, _PREV_VERSION },

            // next "advertised" + force next => next
            { _NEXT_VERSION, ProtocolVersionOption.FORCE_USE_PREVIOUS, _PREV_VERSION },

            // version between default and latest "advertised" + force next => next
            { betweenDefaultAndLatestVersion, ProtocolVersionOption.FORCE_USE_PREVIOUS, _PREV_VERSION },

            // version greater than latest "advertised" + force next => next
            { greaterThanLatestVersion, ProtocolVersionOption.FORCE_USE_PREVIOUS, _PREV_VERSION }
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
            // Expectation is to get baseline announced version for input latest version if the version percentage is below 1 or above 100 or incorrect
            {_LATEST_VERSION, "0", _BASELINE_VERSION},
            {_LATEST_VERSION, "-1", _BASELINE_VERSION},
            {_LATEST_VERSION, "-32938", _BASELINE_VERSION},
            {_LATEST_VERSION, "101", _BASELINE_VERSION},
            {_LATEST_VERSION, "43984", _BASELINE_VERSION},
            {_LATEST_VERSION, "jfk**j&&j888", _BASELINE_VERSION},
            // Expectation is to get latest announced version for input latest version if the version percentage is hundred
            {_LATEST_VERSION, "100", _LATEST_VERSION}
      };
  }

  @Test(dataProvider = "data")
  public void testProtocolVersionNegotiation(ProtocolVersion announcedVersion,
                                             ProtocolVersionOption versionOption,
                                             ProtocolVersion expectedProtocolVersion)
  {
    Assert.assertEquals(RestClient.getProtocolVersion(_BASELINE_VERSION,
                                                      _PREV_VERSION,
                                                      _LATEST_VERSION,
                                                      _NEXT_VERSION,
                                                      announcedVersion,
                                                      versionOption,
                                                      false),
                        expectedProtocolVersion);
  }

  @Test
  public void testAnnouncedVersionLessThanPrev()
  {
    try
    {
      RestClient.getProtocolVersion(_BASELINE_VERSION,
                                    _PREV_VERSION,
                                    _LATEST_VERSION,
                                    _NEXT_VERSION,
                                    new ProtocolVersion(0, 0, 0),
                                    ProtocolVersionOption.USE_LATEST_IF_AVAILABLE,
                                    false);
      Assert.fail("Expected a RuntimeException as the announced version is less than the earliest supported version!");
    }
    catch (RuntimeException e)
    {
      Assert.assertTrue(e.getMessage().contains("Announced version is less than the earliest supported version!"));
    }
  }

  @Test(dataProvider = "versionTestVariations")
  public void testAnnouncedVersionWithVersionPercentages(ProtocolVersion versionInput,
                                                         String versionPercentageInput,
                                                         ProtocolVersion expectedAnnouncedVersion)
  {
      Map<String, Object> properties = new HashMap<String, Object>();
      properties.put(RestConstants.RESTLI_PROTOCOL_VERSION_PROPERTY, versionInput);
      properties.put(RestConstants.RESTLI_PROTOCOL_VERSION_PERCENTAGE_PROPERTY, versionPercentageInput);
      ProtocolVersion announcedVersion = RestClient.getAnnouncedVersion(properties);
      Assert.assertEquals(announcedVersion, expectedAnnouncedVersion);
  }

  @Test
  public void testServiceMetadataCacheBehavior() {
    com.linkedin.r2.transport.common.Client mockClient = Mockito.mock(com.linkedin.r2.transport.common.Client.class);
    Request<?> mockRequest = Mockito.mock(Request.class);
    RestliRequestOptions mockRequestOptions = Mockito.mock(RestliRequestOptions.class);
    RequestContext mockRequestContext = Mockito.mock(RequestContext.class);

    final RestClient restClient = new RestClient(mockClient, TEST_URI_PREFIX);
    Mockito.when(mockRequest.getRequestOptions()).thenReturn(mockRequestOptions);
    Mockito.when(mockRequestOptions.getProtocolVersionOption()).thenReturn(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE);
    Mockito.when(mockRequest.getServiceName()).thenReturn(TEST_SERVICE_NAME);
    doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      Callback<Map<String, Object>> metadataCallback = (Callback<Map<String, Object>>) invocation.getArguments()[1];
      metadataCallback.onSuccess(new HashMap<>());
      return null;
    }).when(mockClient).getMetadata(any(), any());

    @SuppressWarnings("unchecked")
    final Callback<Pair<ProtocolVersion, List<String>>> mockCallback = Mockito.mock(Callback.class);
    // make multiple requests to test the cache behavior
    restClient.getServiceMetadata(mockRequest, mockRequestContext, mockCallback);
    restClient.getServiceMetadata(mockRequest, mockRequestContext, mockCallback);
    restClient.getServiceMetadata(mockRequest, mockRequestContext, mockCallback);
    // verify getMetadata is invoked only once. second request MUST be served from the cache.
    Mockito.verify(mockClient, times(1)).getMetadata(any(), any());
    // verify all same protocolVersion is returned all the 3 times.
    Mockito.verify(mockCallback, times(3)).onSuccess(
        Pair.of(AllProtocolVersions.BASELINE_PROTOCOL_VERSION, null));
  }

  @Test
  public void testServiceMetadataCacheBehaviorOnError() throws Exception {
    com.linkedin.r2.transport.common.Client mockClient = Mockito.mock(com.linkedin.r2.transport.common.Client.class);
    Request<?> mockRequest = Mockito.mock(Request.class);
    RestliRequestOptions mockRequestOptions = Mockito.mock(RestliRequestOptions.class);
    RequestContext mockRequestContext = Mockito.mock(RequestContext.class);

    final RestClient restClient = new RestClient(mockClient, TEST_URI_PREFIX);
    Mockito.when(mockRequest.getRequestOptions()).thenReturn(mockRequestOptions);
    Mockito.when(mockRequestOptions.getProtocolVersionOption()).thenReturn(ProtocolVersionOption.USE_LATEST_IF_AVAILABLE);
    Mockito.when(mockRequest.getServiceName()).thenReturn(TEST_SERVICE_NAME);
    doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      Callback<Map<String, Object>> metadataCallback = (Callback<Map<String, Object>>) invocation.getArguments()[1];
      // throw exception to test the error scenario.
      metadataCallback.onError(new RuntimeException("TEST"));
      return null;
    }).when(mockClient).getMetadata(any(), any());

    @SuppressWarnings("unchecked")
    final Callback<Pair<ProtocolVersion, List<String>>> mockCallback = Mockito.mock(Callback.class);
    // make multiple requests to test the cache behavior
    restClient.getServiceMetadata(mockRequest, mockRequestContext, mockCallback);
    restClient.getServiceMetadata(mockRequest, mockRequestContext, mockCallback);
    restClient.getServiceMetadata(mockRequest, mockRequestContext, mockCallback);
    // getMetadata should be called all 3 times as cache will be invalidated after each error.
    Mockito.verify(mockClient, times(3)).getMetadata(any(), any());
    Mockito.verify(mockCallback, times(3)).onError(any(Throwable.class));
    Mockito.verify(mockCallback, times(0)).onSuccess(any());
  }

  @DataProvider(name = "testForceUseNextVersionOverrideData")
  public Object[][] testForceUseVersionOverrideData()
  {
    return new Object[][]
        {
            {ProtocolVersionOption.FORCE_USE_NEXT, _NEXT_VERSION, true},
            {ProtocolVersionOption.FORCE_USE_PREVIOUS, _NEXT_VERSION, true},
            {ProtocolVersionOption.FORCE_USE_LATEST, _NEXT_VERSION, true},
            {ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, _NEXT_VERSION, true},

            {ProtocolVersionOption.FORCE_USE_PREVIOUS, _PREV_VERSION, false},
            {ProtocolVersionOption.FORCE_USE_NEXT, _NEXT_VERSION, false},
            {ProtocolVersionOption.FORCE_USE_LATEST, _LATEST_VERSION, false},
            {ProtocolVersionOption.USE_LATEST_IF_AVAILABLE, _BASELINE_VERSION, false},
        };
  }

  @Test(dataProvider = "testForceUseNextVersionOverrideData")
  public void testForceUseNextVersionOverride(ProtocolVersionOption protocolVersionOption,
                                              ProtocolVersion expectedProtocolVersion,
                                              boolean forceUseOverrideSystemProperty)
  {
    ProtocolVersion announcedVersion = new ProtocolVersion("2.0.0");
    ProtocolVersion actualProtocolVersion = RestClient.getProtocolVersion(_BASELINE_VERSION,
                                                                          _PREV_VERSION,
                                                                          _LATEST_VERSION,
                                                                          _NEXT_VERSION,
                                                                          announcedVersion,
                                                                          protocolVersionOption,
                                                                          forceUseOverrideSystemProperty);
    Assert.assertEquals(actualProtocolVersion, expectedProtocolVersion);
  }
}