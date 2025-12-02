package com.linkedin.d2.jmx;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import java.util.Arrays;
import java.util.HashSet;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link SimpleLoadBalancerStateJmx}.
 */
public class TestSimpleLoadBalancerStateJmx
{
  @Test
  public void testDefaultNoOpProviderDoesNotThrow()
  {
    SimpleLoadBalancerState state = mock(SimpleLoadBalancerState.class);
    when(state.getClusterCount()).thenReturn(3);
  when(state.getClusters()).thenReturn(new HashSet<>(Arrays.asList("a","$b")));
    when(state.getServiceCount()).thenReturn(5);

    SimpleLoadBalancerStateJmx jmx = new SimpleLoadBalancerStateJmx(state);

    assertEquals(jmx.getClusterCount(), 3);
    assertEquals(jmx.getSymlinkClusterCount(), 1L);
    assertEquals(jmx.getServiceCount(), 5);
  }

  @Test
  public void testWithMockProviderReceivesCallbacksAndClientName()
  {
    SimpleLoadBalancerState state = mock(SimpleLoadBalancerState.class);
    when(state.getClusterCount()).thenReturn(2);
  when(state.getClusters()).thenReturn(new HashSet<>(Arrays.asList("c1","$c2")));
    when(state.getServiceCount()).thenReturn(7);

    LoadBalancerStateOtelMetricsProvider provider = mock(LoadBalancerStateOtelMetricsProvider.class);

    SimpleLoadBalancerStateJmx jmx = new SimpleLoadBalancerStateJmx(state, provider);

    // default client name is "-"
    jmx.getClusterCount();
    jmx.getSymlinkClusterCount();
    jmx.getServiceCount();

    // cluster count method is called twice (both getters call recordClusterCount)
  verify(provider, times(2)).recordClusterCount("-", 2L, 1L);
  verify(provider, times(1)).recordServiceCount("-", 7L);

    // set client name and verify subsequent calls use it
  jmx.setClientName("MyClient");
  // call both cluster getters and the service getter once — matches implementation
  jmx.getClusterCount();
  jmx.getSymlinkClusterCount();
  jmx.getServiceCount();

  verify(provider, times(2)).recordClusterCount("MyClient", 2L, 1L);
  verify(provider, times(1)).recordServiceCount("MyClient", 7L);
  }
}
