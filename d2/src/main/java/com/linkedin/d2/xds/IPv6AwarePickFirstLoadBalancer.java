package com.linkedin.d2.xds;

import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancerProvider;
import io.grpc.LoadBalancerRegistry;
import io.grpc.NameResolver.ConfigOrError;
import io.grpc.Status;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This {@link LoadBalancer} is an extension of the "pick_first" strategy which shuffles the addresses in a way that
 * remains aware of whether the client supports IPv6. Namely, {@link #acceptResolvedAddresses} will be invoked with a
 * list of addresses whose order represents its preference for IPv4 vs IPv6. For example, the list may contain only
 * IPv6 addresses, only IPv4 addresses, IPv6 addresses followed by IPv4 addresses, IPv4 addresses followed by IPv6
 * addresses, and everything in between. This load balancer shuffles the given addresses in a way that respects the
 * original interleaving of address types, such that the client's preference is respected, while still achieving the
 * desired effect of random pick_first load balancing.
 */
public class IPv6AwarePickFirstLoadBalancer extends LoadBalancer
{
  // The initialization of this variable deliberately prevents the compiler from using this string as a compile-time
  // constant. This way, referencing the policy by its name like the following code *always* executes this static
  // block, registering the Provider:
  //   LoadBalancerRegistry.getDefaultRegistry().getProvider(POLICY_NAME);
  // Similarly, referencing the policy during a ManagedChannel builder will also implicitly register the policy:
  //   builder.defaultLoadBalancingPolicy(POLICY_NAME);
  // Otherwise, POLICY_NAME would get resolved at compile-time, the static block would not get executed and the policy
  // would not be registered.
  public static final String POLICY_NAME;

  static
  {
    POLICY_NAME = "ipv6_aware_random_pick_first";
    LoadBalancerRegistry.getDefaultRegistry().register(new Provider());
  }

  private final LoadBalancer _delegate;

  IPv6AwarePickFirstLoadBalancer(Helper helper)
  {
    _delegate = LoadBalancerRegistry.getDefaultRegistry()
        .getProvider("pick_first")
        .newLoadBalancer(helper);
  }

  @Override
  public Status acceptResolvedAddresses(ResolvedAddresses resolvedAddresses)
  {
    return _delegate.acceptResolvedAddresses(resolvedAddresses.toBuilder()
        .setAddresses(ipAwareShuffle(resolvedAddresses.getAddresses()))
        .build());
  }

  /**
   * Shuffles the given addresses such that the original interleaving of IPv4 and IPv6 addresses is respected. For
   * example, the following input [IPv6a, IPv4a, IPv4b, IPv6b, IPv4c] could be shuffled into the following output
   * [IPv6b, IPv4c, IPv4b, IPv6a, IPv4a]. The IPs were shuffled relative to other IPs of the same version, but the
   * original interleaving is the same.
   */
  private static List<EquivalentAddressGroup> ipAwareShuffle(List<EquivalentAddressGroup> addresses)
  {
    List<EquivalentAddressGroup> ipv6EAGs = new ArrayList<>();
    List<EquivalentAddressGroup> ipv4EAGs = new ArrayList<>();
    for (EquivalentAddressGroup eag : addresses)
    {
      (hasIPv6Address(eag) ? ipv6EAGs : ipv4EAGs).add(eag);
    }

    Collections.shuffle(ipv6EAGs);
    Collections.shuffle(ipv4EAGs);

    List<EquivalentAddressGroup> shuffledEAGs = new ArrayList<>(addresses);

    Iterator<EquivalentAddressGroup> ipv4Iterator = ipv4EAGs.iterator();
    Iterator<EquivalentAddressGroup> ipv6Iterator = ipv6EAGs.iterator();
    for (int i = 0; i < shuffledEAGs.size(); i++)
    {
      if (hasIPv6Address(shuffledEAGs.get(i)))
      {
        shuffledEAGs.set(i, ipv6Iterator.next());
      }
      else
      {
        shuffledEAGs.set(i, ipv4Iterator.next());
      }
    }

    return shuffledEAGs;
  }

  /**
   * Checks whether the given {@link EquivalentAddressGroup} has any IPv6 addresses in it.
   */
  static boolean hasIPv6Address(EquivalentAddressGroup eag)
  {
    for (SocketAddress address : eag.getAddresses())
    {
      if (!(address instanceof InetSocketAddress))
      {
        continue;
      }
      if (((InetSocketAddress) address).getAddress() instanceof Inet6Address)
      {
        return true;
      }
    }
    return false;
  }

  @Override
  public void handleNameResolutionError(Status error)
  {
    _delegate.handleNameResolutionError(error);
  }

  @Override
  public void shutdown()
  {
    _delegate.shutdown();
  }

  @Override
  public void requestConnection()
  {
    _delegate.requestConnection();
  }

  static final class Provider extends LoadBalancerProvider
  {
    @Override
    public boolean isAvailable()
    {
      return true;
    }

    @Override
    public int getPriority()
    {
      // 5 is the same priority as PickFirstLoadBalancerProvider.
      return 5;
    }

    @Override
    public String getPolicyName()
    {
      return POLICY_NAME;
    }

    @Override
    public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper)
    {
      return new IPv6AwarePickFirstLoadBalancer(helper);
    }

    @Override
    public ConfigOrError parseLoadBalancingPolicyConfig(Map<String, ?> rawLoadBalancingPolicyConfig)
    {
      return ConfigOrError.fromConfig(new Object());
    }
  }
}
