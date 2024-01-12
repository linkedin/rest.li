package com.linkedin.d2.balancer.dualread;

import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.util.TestDataHelper;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.util.TestDataHelper.*;


public class DualReadLoadBalancerMonitorTest {

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
        new DualReadLoadBalancerMonitor.UriPropertiesDualReadMonitor(
            new DualReadLoadBalancerJmx(), TestDataHelper.getClock());

    Assert.assertEquals(monitor.getEntriesMessage(isFromNewLb, oldE, newE),
        expected,
        "entry message is not the same");
  }
}
