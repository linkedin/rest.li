package com.linkedin.d2.xds;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.grpc.Attributes;
import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.NameResolver;
import io.grpc.NameResolver.ConfigOrError;
import io.grpc.SynchronizationContext;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.xds.IPv6AwarePickFirstLoadBalancer.*;
import static org.mockito.Matchers.intThat;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

public class IPv6AwarePickFirstLoadBalancerTest
{
  @Test(invocationCount = 10)
  public void testShuffling()
  {
    List<EquivalentAddressGroup> addresses = new ArrayList<>();
    for (int i = 0; i < 100; i++)
    {
      addresses.add(newGroup(ThreadLocalRandom.current().nextBoolean()));
    }

    LoadBalancer mock = Mockito.mock(LoadBalancer.class);
    IPv6AwarePickFirstLoadBalancer lb = new IPv6AwarePickFirstLoadBalancer(mock);
    lb.acceptResolvedAddresses(ResolvedAddresses.newBuilder().setAddresses(addresses).build());

    ArgumentCaptor<ResolvedAddresses> addressesCaptor = ArgumentCaptor.forClass(ResolvedAddresses.class);
    verify(mock).acceptResolvedAddresses(addressesCaptor.capture());

    List<EquivalentAddressGroup> shuffledAddresses = addressesCaptor.getValue().getAddresses();
    assertNotEquals(addresses, shuffledAddresses);
    assertEquals(new HashSet<>(addresses), new HashSet<>(shuffledAddresses));

    for (int i = 0; i < addresses.size(); i++)
    {
      assertEquals(hasIPv6Address(addresses.get(i)), hasIPv6Address(shuffledAddresses.get(i)));
    }
  }

  private static EquivalentAddressGroup newGroup(boolean ipv6)
  {
    byte[] addressBytes = new byte[ipv6 ? 4 : 16];
    ThreadLocalRandom.current().nextBytes(addressBytes);
    try
    {
      return new EquivalentAddressGroup(
          Collections.singletonList(new InetSocketAddress(InetAddress.getByAddress(addressBytes), 0)));
    }
    catch (UnknownHostException e)
    {
      throw new RuntimeException(e);
    }
  }
}