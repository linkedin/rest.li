package com.linkedin.d2.xds;

import com.google.common.collect.ImmutableList;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancerProvider;
import io.grpc.LoadBalancerRegistry;
import io.grpc.NameResolver.ConfigOrError;
import io.grpc.Status;
import io.grpc.internal.JsonUtil;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class IPv6AwarePickFirstLoadBalancer extends LoadBalancer
{
  public static final String POLICY_NAME = "ipv6_aware_random_pick_first";
  public static final String IPV4_FIRST = "ipv4_first";

  private final LoadBalancer _pickFirst;

  IPv6AwarePickFirstLoadBalancer(Helper helper)
  {
    _pickFirst = new PickFirstLoadBalancerProvider().newLoadBalancer(helper);
  }

  @Override
  public boolean acceptResolvedAddresses(ResolvedAddresses addresses)
  {
    List<EquivalentAddressGroup> ipv6EAGs = new ArrayList<>();
    List<EquivalentAddressGroup> ipv4EAGs = new ArrayList<>();
    for (EquivalentAddressGroup eag : addresses.getAddresses())
    {
      (hasIPv6Address(eag) ? ipv6EAGs : ipv4EAGs).add(eag);
    }

    Collections.shuffle(ipv6EAGs);
    Collections.shuffle(ipv4EAGs);

    List<EquivalentAddressGroup> shuffledEAGs = new ArrayList<>();
    if (addresses.getLoadBalancingPolicyConfig() instanceof Config
        && ((Config) addresses.getLoadBalancingPolicyConfig())._ipv4First)
    {
      shuffledEAGs.addAll(ipv4EAGs);
      shuffledEAGs.addAll(ipv6EAGs);
    }
    else
    {
      shuffledEAGs.addAll(ipv6EAGs);
      shuffledEAGs.addAll(ipv4EAGs);
    }

    return _pickFirst.acceptResolvedAddresses(
        ResolvedAddresses.newBuilder()
            .setAddresses(shuffledEAGs)
            .setAttributes(addresses.getAttributes())
            .setLoadBalancingPolicyConfig(addresses.getLoadBalancingPolicyConfig())
            .build()
    );
  }

  private static boolean hasIPv6Address(EquivalentAddressGroup eag)
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
    _pickFirst.handleNameResolutionError(error);
  }

  @Override
  public void shutdown()
  {
    _pickFirst.shutdown();
  }

  @Override
  public void requestConnection()
  {
    _pickFirst.requestConnection();
  }

  private static class Config
  {
    private final boolean _ipv4First;

    private Config(@Nullable Boolean ipv4First)
    {
      _ipv4First = (ipv4First != null) ? ipv4First : false;
    }
  }

  private static final class Provider extends LoadBalancerProvider
  {
    static
    {
      LoadBalancerRegistry.getDefaultRegistry().register(new Provider());
    }

    @Override
    public boolean isAvailable()
    {
      return true;
    }

    @Override
    public int getPriority()
    {
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
      try
      {
        return ConfigOrError.fromConfig(new Config(JsonUtil.getBoolean(rawLoadBalancingPolicyConfig, IPV4_FIRST)));
      }
      catch (RuntimeException e)
      {
        return ConfigOrError.fromError(
            Status.UNAVAILABLE.withCause(e).withDescription(
                "Failed parsing configuration for " + getPolicyName()));
      }
    }
  }
}
