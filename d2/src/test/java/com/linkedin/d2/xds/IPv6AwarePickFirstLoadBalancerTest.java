package com.linkedin.d2.xds;

import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancerProvider;
import io.grpc.LoadBalancerRegistry;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

import static com.linkedin.d2.xds.IPv6AwarePickFirstLoadBalancer.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class IPv6AwarePickFirstLoadBalancerTest
{
  public List<List<EquivalentAddressGroup>> generateAddressGroups()
  {
    List<List<EquivalentAddressGroup>> addresses = new ArrayList<>();
    for (int i = 0; i < 10; i++)
    {
      // Addresses in random interleaving.
      addresses.add(IntStream.range(0, 100)
          .mapToObj(j -> newGroup(ThreadLocalRandom.current().nextBoolean()))
          .collect(Collectors.toList()));
    }
    // First half of list is IPv6, back half is IPv4
    addresses.add(IntStream.range(0, 100)
        .mapToObj(j -> newGroup(j < 50))
        .collect(Collectors.toList()));
    // Inverse, first half is IPv4, back half is IPv6
    addresses.add(IntStream.range(0, 100)
        .mapToObj(j -> newGroup(j >= 50))
        .collect(Collectors.toList()));
    return addresses;
  }

  @Test
  public void testShuffling()
  {
    LoadBalancer mockedPickFirst = mock(LoadBalancer.class);
    ArgumentCaptor<ResolvedAddresses> addressesCaptor = ArgumentCaptor.forClass(ResolvedAddresses.class);

    LoadBalancerRegistry.getDefaultRegistry().register(new LoadBalancerProvider()
    {
      @Override
      public boolean isAvailable()
      {
        return true;
      }

      @Override
      public int getPriority()
      {
        // Set the highest priority, so it overrides the built-in "pick_first" policy and return the mock.
        return Integer.MAX_VALUE;
      }

      @Override
      public String getPolicyName()
      {
        return "pick_first";
      }

      @Override
      public LoadBalancer newLoadBalancer(Helper helper)
      {
        return mockedPickFirst;
      }
    });
    LoadBalancer lb = LoadBalancerRegistry.getDefaultRegistry()
        .getProvider(POLICY_NAME)
        .newLoadBalancer(mock(Helper.class));
    assertTrue(lb instanceof IPv6AwarePickFirstLoadBalancer);

    for (List<EquivalentAddressGroup> addresses : generateAddressGroups())
    {
      reset(mockedPickFirst);

      lb.acceptResolvedAddresses(ResolvedAddresses.newBuilder().setAddresses(addresses).build());

      verify(mockedPickFirst).acceptResolvedAddresses(addressesCaptor.capture());


      List<EquivalentAddressGroup> shuffledAddresses = addressesCaptor.getValue().getAddresses();
      assertNotEquals(addresses, shuffledAddresses);
      assertEquals(new HashSet<>(addresses), new HashSet<>(shuffledAddresses));

      for (int i = 0; i < addresses.size(); i++)
      {
        assertEquals(hasIPv6Address(addresses.get(i)), hasIPv6Address(shuffledAddresses.get(i)));
      }
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