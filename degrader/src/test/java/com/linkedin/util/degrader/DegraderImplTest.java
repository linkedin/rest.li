package com.linkedin.util.degrader;

import com.google.common.collect.ImmutableMap;
import com.linkedin.util.clock.SystemClock;
import java.util.Map;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;


public class DegraderImplTest
{
  @DataProvider
  public Object[][] loadBalanceStreamExceptionDataProvider()
  {
    return new Object[][] {
        { false },
        { true }
    };
  }

  @Test(dataProvider = "loadBalanceStreamExceptionDataProvider")
  public void testGetErrorRateToDegrade(Boolean loadBalancerStreamException)
  {
    Map<ErrorType, Integer> errorTypeCounts = ImmutableMap.of(
        ErrorType.CONNECT_EXCEPTION, 1,
        ErrorType.CLOSED_CHANNEL_EXCEPTION, 1,
        ErrorType.SERVER_ERROR, 1,
        ErrorType.TIMEOUT_EXCEPTION, 1,
        ErrorType.STREAM_ERROR, 1
    );
    DegraderImpl degrader = new DegraderImplTestFixture().getDegraderImpl(loadBalancerStreamException, errorTypeCounts,
        10);
    assertEquals(degrader.getErrorRateToDegrade(), loadBalancerStreamException ? 0.5 : 0.4);
  }

  private static final class DegraderImplTestFixture
  {
    @Mock
    CallTracker _callTracker;
    @Mock
    CallTracker.CallStats _callStats;

    DegraderImplTestFixture()
    {
      MockitoAnnotations.initMocks(this);
      doReturn(_callStats).when(_callTracker).getCallStats();
      doNothing().when(_callTracker).addStatsRolloverEventListener(any());
    }

    DegraderImpl getDegraderImpl(boolean loadBalancerStreamException, Map<ErrorType, Integer> errorTypeCounts, int callCount)
    {
      when(_callStats.getErrorTypeCounts()).thenReturn(errorTypeCounts);
      when(_callStats.getCallCount()).thenReturn(callCount);

      DegraderImpl.Config config = new DegraderImpl.Config();
      config.setName("DegraderImplTest");
      config.setClock(SystemClock.instance());
      config.setCallTracker(_callTracker);
      config.setMaxDropDuration(1);
      config.setInitialDropRate(0.01);
      config.setLogger(LoggerFactory.getLogger(DegraderImplTest.class));
      config.setLoadBalanceStreamException(loadBalancerStreamException);
      return new DegraderImpl(config);
    }
  }
}
