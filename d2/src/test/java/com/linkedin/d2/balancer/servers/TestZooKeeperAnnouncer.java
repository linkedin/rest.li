package com.linkedin.d2.balancer.servers;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancerServer;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;

import com.linkedin.d2.discovery.event.LogOnlyServiceDiscoveryEventEmitter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * Tests {@link ZooKeeperAnnouncer}.
 */
public class TestZooKeeperAnnouncer
{
  private ZooKeeperAnnouncer _announcer;

  @Mock
  private ZooKeeperServer _server;
  @Mock
  private Callback<None> _callback;

  private static final Map<Integer, PartitionData> MAX_WEIGHT_BREACH_PARTITION_DATA =
      Collections.singletonMap(0, new PartitionData(1000));
  private static final Map<Integer, PartitionData> DECIMAL_PLACES_BREACH_PARTITION_DATA =
      Collections.singletonMap(0, new PartitionData(5.345));
  private static final Map<Integer, PartitionData> MAX_WEIGHT_AND_DECIMAL_PLACES_BREACH_PARTITION_DATA =
      Collections.singletonMap(0, new PartitionData(10.89));
  private static final Map<Integer, PartitionData> VALID_PARTITION_DATA =
      Collections.singletonMap(0, new PartitionData(2.3));

  @BeforeMethod
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);

    _announcer = new ZooKeeperAnnouncer((LoadBalancerServer) _server);
  }

  @Test
  public void testSetDoNotLoadBalance()
  {
    _announcer.setDoNotLoadBalance(_callback, true);

    verify(_server).addUriSpecificProperty(any(), any(), any(), any(), eq(PropertyKeys.DO_NOT_LOAD_BALANCE), eq(true), any());

    _announcer.setDoNotLoadBalance(_callback, false);

    verify(_server).addUriSpecificProperty(any(), any(), any(), any(), eq(PropertyKeys.DO_NOT_LOAD_BALANCE), eq(false), any());
  }

  @DataProvider(name = "validatePartitionDataDataProvider")
  public Object[][] getValidatePartitionDataDataProvider()
  {
    return new Object[][] {
        {
          // no weight rules
          null, null, MAX_WEIGHT_BREACH_PARTITION_DATA, MAX_WEIGHT_BREACH_PARTITION_DATA, null, 0, 0
        },
        {
          // negative weight throws
          null, null, Collections.singletonMap(0, new PartitionData(-1.0)), null,
            new IllegalArgumentException("Weight -1.0 in Partition 0 is negative. Please correct it."), 0, 0
        },
        {
          // valid weight
          "3.0", null, VALID_PARTITION_DATA, VALID_PARTITION_DATA, null, 0, 0
        },
        {
          // no action default to IGNORE, which won't correct the value BUT will increment the counts
          "10.0", null, MAX_WEIGHT_BREACH_PARTITION_DATA, MAX_WEIGHT_BREACH_PARTITION_DATA, null, 1, 0
        },
        {
          // warn action won't correct the value
          "10.0", ZooKeeperAnnouncer.ActionOnWeightBreach.WARN, MAX_WEIGHT_BREACH_PARTITION_DATA,
            MAX_WEIGHT_BREACH_PARTITION_DATA, null, 1, 0
        },
        {
          // max weight breach, correct the value
          "10.0", ZooKeeperAnnouncer.ActionOnWeightBreach.RECTIFY, MAX_WEIGHT_BREACH_PARTITION_DATA,
            Collections.singletonMap(0, new PartitionData(10)), null, 1, 0
        },
        {
          // decimal places breach, correct the value
          "10.0", ZooKeeperAnnouncer.ActionOnWeightBreach.RECTIFY, DECIMAL_PLACES_BREACH_PARTITION_DATA,
            Collections.singletonMap(0, new PartitionData(5.3)), null, 0, 1
        },
        {
          // max weight and decimal places breach, correct the value
          "10.0", ZooKeeperAnnouncer.ActionOnWeightBreach.RECTIFY, MAX_WEIGHT_AND_DECIMAL_PLACES_BREACH_PARTITION_DATA,
            Collections.singletonMap(0, new PartitionData(10)), null, 1, 0
        },
        {
          // throw action throws for max weight breach
          "10.0", ZooKeeperAnnouncer.ActionOnWeightBreach.THROW, MAX_WEIGHT_BREACH_PARTITION_DATA, null,
            new IllegalArgumentException("[ACTION NEEDED] Weight 1000.0 in Partition 0 is greater than the max weight "
                + "allowed: 10.0. Please correct the weight. It will be force-capped to the max weight in the future."),
            1, 0
        },
        {
          // throw action does not throw for decimal places breach
          "10.0", ZooKeeperAnnouncer.ActionOnWeightBreach.THROW, DECIMAL_PLACES_BREACH_PARTITION_DATA,
            DECIMAL_PLACES_BREACH_PARTITION_DATA, null, 0, 1
        }
    };
  }
  @Test(dataProvider = "validatePartitionDataDataProvider")
  public void testValidatePartitionData(String maxWeight, ZooKeeperAnnouncer.ActionOnWeightBreach action,
      Map<Integer, PartitionData> input, Map<Integer, PartitionData> expected, Exception expectedException,
      int expectedMaxWeightBreachedCount, int expectedWeightDecimalPlacesBreachedCount)
  {
    ZooKeeperAnnouncer announcer = new ZooKeeperAnnouncer(_server, true, false, null, 0,
        null, new LogOnlyServiceDiscoveryEventEmitter(),
        maxWeight == null ? null : new BigDecimal(maxWeight), action);

    if (expectedException != null)
    {
      try
      {
        announcer.validatePartitionData(input);
        fail("Expected exception not thrown");
      }
      catch (Exception ex)
      {
        assertTrue(ex instanceof IllegalArgumentException);
        assertEquals(expectedException.getMessage(), ex.getMessage());
      }
    }
    else
    {
      assertEquals(expected, announcer.validatePartitionData(input));
    }
    assertEquals(expectedMaxWeightBreachedCount, announcer.getMaxWeightBreachedCount());
    assertEquals(expectedWeightDecimalPlacesBreachedCount, announcer.getWeightDecimalPlacesBreachedCount());
  }
}
