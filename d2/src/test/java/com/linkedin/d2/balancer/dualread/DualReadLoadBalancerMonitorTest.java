package com.linkedin.d2.balancer.dualread;

import com.google.common.cache.Cache;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServiceStoreProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.util.TestDataHelper;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.util.TestDataHelper.*;
import static org.mockito.Mockito.*;


public class DualReadLoadBalancerMonitorTest {

  private static class DualReadLoadBalancerMonitorTestFixure
  {
    @Mock
    DualReadLoadBalancerJmx _mockJmx;

    DualReadLoadBalancerMonitorTestFixure()
    {
      MockitoAnnotations.initMocks(this);
      doNothing().when(_mockJmx).incrementServicePropertiesOutOfSyncCount();
      doNothing().when(_mockJmx).decrementUriPropertiesOutOfSyncCount();
      doNothing().when(_mockJmx).incrementServicePropertiesErrorCount();
    }

    DualReadLoadBalancerMonitor.ServicePropertiesDualReadMonitor getServiceMonitor()
    {
      return new DualReadLoadBalancerMonitor.ServicePropertiesDualReadMonitor(_mockJmx, TestDataHelper.getClock());
    }

    DualReadLoadBalancerMonitor.UriPropertiesDualReadMonitor getUriMonitor()
    {
      return new DualReadLoadBalancerMonitor.UriPropertiesDualReadMonitor(_mockJmx, TestDataHelper.getClock());
    }
  }

  @Test
  public void testPut()
  {
    DualReadLoadBalancerMonitorTestFixure fixture = new DualReadLoadBalancerMonitorTestFixure();
    DualReadLoadBalancerMonitor.ServicePropertiesDualReadMonitor monitor = fixture.getServiceMonitor();

    // put in one entry for new lb
    putInService(monitor, SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "1", true, 1);
    verify(fixture._mockJmx).incrementServicePropertiesOutOfSyncCount();
    verifyNoMoreInteractions(fixture._mockJmx);

    // put in duplicate entry will be skipped
    monitor.reportData(SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "1", true);
    verifyServiceOnCache(monitor.getNewLbCache(), SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "1");
    verifyNoMoreInteractions(fixture._mockJmx);

    // put in same data with a different version will succeed
    monitor.reportData(SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "2", true);
    verifyServiceOnCache(monitor.getNewLbCache(), SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "2");
    verify(fixture._mockJmx, times(2)).incrementServicePropertiesOutOfSyncCount();
    verifyNoMoreInteractions(fixture._mockJmx);

    // put in an entry to old lb with the same data but a different version (not read from FS) will succeed
    monitor.reportData(SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "1", false);
    verifyServiceOnCache(monitor.getOldLbCache(), SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "1");
    verifyServiceOnCache(monitor.getNewLbCache(), SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "2");
    verify(fixture._mockJmx, times(3)).incrementServicePropertiesOutOfSyncCount();
    verifyNoMoreInteractions(fixture._mockJmx);
  }

  @Test
  public void testServiceDataMatch()
  {
    DualReadLoadBalancerMonitorTestFixure fixture = new DualReadLoadBalancerMonitorTestFixure();
    DualReadLoadBalancerMonitor.ServicePropertiesDualReadMonitor monitor = fixture.getServiceMonitor();

    // put in one new lb entry and one old lb entry, with different data and version
    putInService(monitor, SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "-1", true, 1);
    putInService(monitor, SERVICE_NAME, SERVICE_STORE_PROPERTIES_2, "1", false, 1);
    verify(fixture._mockJmx, times(2)).incrementServicePropertiesOutOfSyncCount();
    verifyNoMoreInteractions(fixture._mockJmx);

    // Exact match (data and version all the same): put in an old lb entry that matches the new lb one
    // will remove the new lb one
    monitor.reportData(SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "-1", false);
    Assert.assertEquals(monitor.getNewLbCache().size(), 0);
    verifyServiceOnCache(monitor.getOldLbCache(), SERVICE_NAME, SERVICE_STORE_PROPERTIES_2, "1");
    verify(fixture._mockJmx).decrementServicePropertiesOutOfSyncCount();
    verifyNoMoreInteractions(fixture._mockJmx);

    // put the new lb one back in
    putInService(monitor, SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "-1", true, 1);
    verify(fixture._mockJmx, times(3)).incrementServicePropertiesOutOfSyncCount();
    verifyNoMoreInteractions(fixture._mockJmx);

    // Data match, version differs but "-1" will be matched as an exception: put in an old lb entry that matches
    // the data of the new lb one will remove the new lb one
    monitor.reportData(SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "2", false);
    Assert.assertEquals(monitor.getNewLbCache().size(), 0);
    verifyServiceOnCache(monitor.getOldLbCache(), SERVICE_NAME, SERVICE_STORE_PROPERTIES_2, "1");
    verify(fixture._mockJmx, times(2)).decrementServicePropertiesOutOfSyncCount();
    verifyNoMoreInteractions(fixture._mockJmx);
  }

  @Test // since uri version read from FS is "-1|x", test for this case specifically
  public void testUriDataMatch()
  {
    DualReadLoadBalancerMonitorTestFixure fixture = new DualReadLoadBalancerMonitorTestFixure();
    DualReadLoadBalancerMonitor.UriPropertiesDualReadMonitor monitor = fixture.getUriMonitor();

    // put in one new lb entry and one old lb entry, with different data and version
    putInUri(monitor, CLUSTER_NAME, PROPERTIES_1, "1|2", true, 1);
    putInUri(monitor, CLUSTER_NAME, PROPERTIES_2, "-1|2", false, 1);
    verify(fixture._mockJmx, times(2)).incrementUriPropertiesOutOfSyncCount();
    verifyNoMoreInteractions(fixture._mockJmx);

    // data match, version differs with version read FS
    monitor.reportData(CLUSTER_NAME, PROPERTIES_2, "2|2", true);
    Assert.assertEquals(monitor.getOldLbCache().size(), 0);
    verifyUriOnCache(monitor.getNewLbCache(), CLUSTER_NAME, PROPERTIES_1, "1|2");
    verify(fixture._mockJmx).decrementUriPropertiesOutOfSyncCount();
    verifyNoMoreInteractions(fixture._mockJmx);
  }

  @Test
  public void testMismatch()
  {
    DualReadLoadBalancerMonitorTestFixure fixture = new DualReadLoadBalancerMonitorTestFixure();
    DualReadLoadBalancerMonitor.ServicePropertiesDualReadMonitor monitor = fixture.getServiceMonitor();

    // put in one new lb entry and one old lb entry, with different data and version
    putInService(monitor, SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "-1", true, 1);
    putInService(monitor, SERVICE_NAME, SERVICE_STORE_PROPERTIES_2, "1", false, 1);
    verify(fixture._mockJmx, times(2)).incrementServicePropertiesOutOfSyncCount();
    verifyNoMoreInteractions(fixture._mockJmx);

    // Mismatch (version is the same but data differs): put in a new lb entry that mismatch the old lb one
    // will remove the old lb one
    monitor.reportData(SERVICE_NAME, SERVICE_STORE_PROPERTIES_3, "1", true);
    Assert.assertEquals(monitor.getOldLbCache().size(), 0);
    verifyServiceOnCache(monitor.getNewLbCache(), SERVICE_NAME, SERVICE_STORE_PROPERTIES_1, "-1");
    verify(fixture._mockJmx).incrementServicePropertiesErrorCount();
    verify(fixture._mockJmx, times(3)).incrementServicePropertiesOutOfSyncCount();
    verifyNoMoreInteractions(fixture._mockJmx);
  }

  private void putInService(DualReadLoadBalancerMonitor.ServicePropertiesDualReadMonitor monitor,
      String name, ServiceStoreProperties prop, String version, boolean isFromNewLb, int expectedSizeAfter)
  {
    monitor.reportData(name, prop, version, isFromNewLb);
    Cache<String, DualReadLoadBalancerMonitor.CacheEntry<ServiceProperties>> cache =
        isFromNewLb ? monitor.getNewLbCache() : monitor.getOldLbCache();
    Assert.assertEquals(cache.size(), expectedSizeAfter);
    verifyServiceOnCache(cache, name, prop, version);
  }

  private void verifyServiceOnCache(Cache<String, DualReadLoadBalancerMonitor.CacheEntry<ServiceProperties>> cache,
      String name, ServiceProperties prop, String v)
  {
    DualReadLoadBalancerMonitor.CacheEntry<ServiceProperties> entry = cache.getIfPresent(name);
    Assert.assertNotNull(entry);
    Assert.assertEquals(entry._data, prop);
    Assert.assertEquals(entry._version, v);
  }

  private void putInUri(DualReadLoadBalancerMonitor.UriPropertiesDualReadMonitor monitor,
      String name, UriProperties prop, String version, boolean isFromNewLb, int expectedSizeAfter)
  {
    monitor.reportData(name, prop, version, isFromNewLb);
    Cache<String, DualReadLoadBalancerMonitor.CacheEntry<UriProperties>> cache =
        isFromNewLb ? monitor.getNewLbCache() : monitor.getOldLbCache();
    Assert.assertEquals(cache.size(), expectedSizeAfter);
    verifyUriOnCache(cache, name, prop, version);
  }

  private void verifyUriOnCache(Cache<String, DualReadLoadBalancerMonitor.CacheEntry<UriProperties>> cache,
      String name, UriProperties prop, String v)
  {
    DualReadLoadBalancerMonitor.CacheEntry<UriProperties> entry = cache.getIfPresent(name);
    Assert.assertNotNull(entry);
    Assert.assertEquals(entry._data, prop);
    Assert.assertEquals(entry._version, v);
  }

  @DataProvider
  public Object[][] getEntriesMessageDataProvider()
  {
    return new Object[][] {
        {
          true, new DualReadLoadBalancerMonitor.CacheEntry<>("", "", PROPERTIES_1),
            new DualReadLoadBalancerMonitor.CacheEntry<>("", "", PROPERTIES_2),
            "\nOld LB: CacheEntry{_version=, _timeStamp='', _data=UriProperties [_clusterName=TestCluster,"
                + " _urisBySchemeAndPartition={http={0=[http://google.com:1], 1=[http://google.com:1]}},"
                + " _partitions={http://google.com:1={0=[ weight =1.0 ], 1=[ weight =2.0 ]}},"
                + " _uriSpecificProperties={}]}\n"
                + "New LB: CacheEntry{_version=, _timeStamp='', "
                + "_data=UriProperties [_clusterName=TestCluster, "
                + "_urisBySchemeAndPartition={http={1=[http://linkedin.com:2]}}, "
                + "_partitions={http://linkedin.com:2={1=[ weight =0.5 ]}}, _uriSpecificProperties={}]}"
        },
        {
          false, new DualReadLoadBalancerMonitor.CacheEntry<>("", "", PROPERTIES_1),
            new DualReadLoadBalancerMonitor.CacheEntry<>("", "", PROPERTIES_2),
            "\nOld LB: CacheEntry{_version=, _timeStamp='', _data=UriProperties [_clusterName=TestCluster,"
                + " _urisBySchemeAndPartition={http={1=[http://linkedin.com:2]}}, "
                + "_partitions={http://linkedin.com:2={1=[ weight =0.5 ]}}, _uriSpecificProperties={}]}\n"
                + "New LB: CacheEntry{_version=, _timeStamp='', _data=UriProperties [_clusterName=TestCluster,"
                + " _urisBySchemeAndPartition={http={0=[http://google.com:1], 1=[http://google.com:1]}},"
                + " _partitions={http://google.com:1={0=[ weight =1.0 ], 1=[ weight =2.0 ]}},"
                + " _uriSpecificProperties={}]}"
        }
    };
  }
  @Test(dataProvider = "getEntriesMessageDataProvider")
  public void testGetEntriesMessage(Boolean isFromNewLb,
      DualReadLoadBalancerMonitor.CacheEntry<UriProperties> oldE,
      DualReadLoadBalancerMonitor.CacheEntry<UriProperties> newE,
      String expected)
  {
    DualReadLoadBalancerMonitor.UriPropertiesDualReadMonitor monitor =
        new DualReadLoadBalancerMonitorTestFixure().getUriMonitor();

    Assert.assertEquals(monitor.getEntriesMessage(isFromNewLb, oldE, newE),
        expected,
        "entry message is not the same");
  }
}
