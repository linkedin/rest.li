/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.d2.balancer.subsetting;

import com.google.common.annotations.VisibleForTesting;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.properties.UriProperties;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.http.annotation.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Listens to the peer cluster in ZooKeeper and provides deterministic subsetting strategy with
 * the metadata needed
 */
public class ZKDeterministicSubsettingMetadataProvider implements DeterministicSubsettingMetadataProvider
{
  private static final Logger _log = LoggerFactory.getLogger(ZKDeterministicSubsettingMetadataProvider.class);

  /**
   * Resolves a host name to its set of IP addresses. Production uses
   * {@link InetAddress#getAllByName(String)}; tests inject a deterministic implementation so
   * unit tests can exercise IPv4/IPv6 paths without depending on real DNS.
   */
  @FunctionalInterface
  @VisibleForTesting
  interface AddressResolver
  {
    InetAddress[] resolve(String host) throws UnknownHostException;
  }

  private final Set<String> _candidateIdentities;
  private final long _timeout;
  private final TimeUnit _unit;

  private final Object _lock = new Object();

  @GuardedBy("_lock")
  private long _peerClusterVersion = -1;
  @GuardedBy("_lock")
  private DeterministicSubsettingMetadata _subsettingMetadata;
  // Re-armed on every successful identity match so a flapping peer cluster logs once per
  // outage rather than on every URI update; mirrors the existing _warnedEmptyUriProperties
  // pattern in grpc-infra's PeerClusterListener.
  @GuardedBy("_lock")
  private boolean _warnedIdentityNotFound;
  private String _clusterName;

  public ZKDeterministicSubsettingMetadataProvider(String hostName, long timeout, TimeUnit unit)
  {
    this(null, hostName, timeout, unit);
  }

  public ZKDeterministicSubsettingMetadataProvider(String clusterName,
                                      String hostName,
                                      long timeout,
                                      TimeUnit unit)
  {
    this(clusterName, hostName, timeout, unit, InetAddress::getAllByName);
  }

  /** Package-private for tests; takes an injectable address resolver in place of DNS. */
  @VisibleForTesting
  ZKDeterministicSubsettingMetadataProvider(String clusterName,
                                      String hostName,
                                      long timeout,
                                      TimeUnit unit,
                                      AddressResolver addressResolver)
  {
    _clusterName = clusterName;
    _candidateIdentities = computeIdentities(hostName, addressResolver);
    _timeout = timeout;
    _unit = unit;
  }

  /**
   * Builds the set of host-form identities that may identify this instance in a peer cluster
   * URI list. The FQDN is always included, and the FQDN is resolved to its IPv4 and IPv6
   * addresses with each form added to the set. IPv6 addresses are added in both bracketed
   * (matches {@link URI#getHost()} on JDK 17, which returns {@code "[ipv6]"}) and unbracketed
   * (defensive fallback against d2/JDK rendering differences) forms. This lets the lookup
   * match itself regardless of how the d2 client materializes peer URIs (the choice depends on
   * JVM flags such as {@code java.net.preferIPv6Addresses}). DNS failures fail soft: the
   * FQDN-only identity is preserved so the legacy single-identity match still works.
   *
   * <p>Scope identifiers ({@code %eth0}, {@code %1}) on resolved local addresses are kept as
   * is. Different scopes are semantically different IPs; stripping them would conflate them. A
   * scoped local identity will not match any peer URI (d2 announcements never carry scope ids),
   * so the candidate is harmless dead weight and the comparison stays correct.
   */
  private static Set<String> computeIdentities(String hostName, AddressResolver resolver)
  {
    if (hostName == null || hostName.isEmpty())
    {
      return Collections.emptySet();
    }
    Set<String> identities = new LinkedHashSet<>();
    identities.add(hostName);
    try
    {
      InetAddress[] resolved = resolver.resolve(hostName);
      if (resolved != null)
      {
        for (InetAddress addr : resolved)
        {
          String addrStr = addr.getHostAddress();
          if (addrStr == null || addrStr.isEmpty())
          {
            continue;
          }
          if (addr instanceof Inet6Address)
          {
            // Funnel both forms through the same canonicalizer the peer-side resolver uses
            // (see canonicalizeIpv6IfApplicable below). For the common case this is a no-op
            // round-trip — Inet6Address.getHostAddress() already returns the canonical
            // expanded form — but routing both sides through the same function keeps the
            // local and peer identity strings in lockstep by construction, e.g. for
            // IPv4-mapped IPv6 (::ffff:a.b.c.d) which auto-converts to a plain IPv4 address.
            identities.add(canonicalizeIpv6IfApplicable("[" + addrStr + "]"));
            identities.add(canonicalizeIpv6IfApplicable(addrStr));
          }
          else
          {
            identities.add(addrStr);
          }
        }
      }
      if (_log.isDebugEnabled())
      {
        _log.debug("Resolved local identities for cluster subsetting (host={}): {}", hostName, identities);
      }
    }
    catch (UnknownHostException e)
    {
      _log.warn("Failed to resolve local addresses for {}; falling back to FQDN-only identity",
          hostName, e);
    }
    return Collections.unmodifiableSet(identities);
  }

  /**
   * Returns the canonical form of {@code host} when it is an IPv6 literal (bracketed or
   * unbracketed); otherwise returns {@code host} unchanged. Parses any string containing a
   * {@code ':'} via {@link InetAddress#getByName(String)} and re-emits via
   * {@link InetAddress#getHostAddress()}, which produces the RFC 4291 fully-expanded form. The
   * original bracketing is preserved, except for IPv4-mapped IPv6 literals (e.g.
   * {@code ::ffff:1.2.3.4}) which {@code getByName} auto-converts to {@link java.net.Inet4Address}
   * — brackets are dropped in that case since IPv4 addresses are not bracketed.
   *
   * <p>FQDNs and IPv4 literals short-circuit on the colon check and pass through unchanged so
   * we never trigger a DNS lookup on the hot path. Unparseable IPv6-like strings (rare; e.g.
   * a malformed URI host) also pass through.
   *
   * <p>Used to bridge the gap between {@link Inet6Address#getHostAddress()} (always returns
   * the expanded form, used to build candidate identities at startup) and
   * {@link URI#getHost()} on a peer URI (returns whatever was literally in the URI string —
   * could be compressed or expanded). Calling this on both sides makes the equality check
   * insensitive to which form the d2 client materialized.
   */
  private static String canonicalizeIpv6IfApplicable(String host)
  {
    if (host == null)
    {
      return null;
    }
    boolean bracketed = host.startsWith("[") && host.endsWith("]");
    String inner = bracketed ? host.substring(1, host.length() - 1) : host;
    if (inner.indexOf(':') < 0)
    {
      return host;
    }
    try
    {
      InetAddress addr = InetAddress.getByName(inner);
      String canonical = addr.getHostAddress();
      if (addr instanceof Inet6Address)
      {
        return bracketed ? "[" + canonical + "]" : canonical;
      }
      // IPv4-mapped IPv6 (::ffff:a.b.c.d) collapses to Inet4Address. IPv4 isn't bracketed, so
      // drop any brackets so a canonicalized peer URI matches a local Inet4Address candidate
      // built from the same physical address.
      return canonical;
    }
    catch (UnknownHostException e)
    {
      // Not a parseable IP literal — pass through and let string equality decide.
      return host;
    }
  }

  public void setClusterName(String clusterName) {
    _clusterName = clusterName;
  }

  @VisibleForTesting
  Set<String> getCandidateIdentities()
  {
    return _candidateIdentities;
  }

  @VisibleForTesting
  boolean hasWarnedIdentityNotFound()
  {
    synchronized (_lock)
    {
      return _warnedIdentityNotFound;
    }
  }

  @Override
  public DeterministicSubsettingMetadata getSubsettingMetadata(LoadBalancerState state)
  {
    if (_clusterName == null)
    {
      _log.debug("Peer cluster name not provided.");
      return null;
    }

    FutureCallback<DeterministicSubsettingMetadata> metadataFutureCallback = new FutureCallback<>();

    state.listenToCluster(_clusterName, (type, name) ->
    {
      LoadBalancerStateItem<UriProperties> uriItem = state.getUriProperties(_clusterName);

      synchronized (_lock)
      {
        if (uriItem.getVersion() != _peerClusterVersion)
        {
          _peerClusterVersion = uriItem.getVersion();
          UriProperties uriProperties = uriItem.getProperty();
          if (uriProperties != null)
          {
            // Sort the URIs so each client sees the same ordering. Canonicalize IPv6 host
            // strings here so a peer URI in compressed form (e.g. "[2a04:f547::1a95]") still
            // matches a candidate identity built from the JDK's expanded form (e.g.
            // "[2a04:f547:0:0:0:0:0:1a95]"). FQDNs and IPv4 literals pass through unchanged,
            // so no DNS work happens on the hot path.
            List<String> sortedHosts = uriProperties.getPartitionDesc().keySet().stream()
                .map(URI::getHost)
                .map(ZKDeterministicSubsettingMetadataProvider::canonicalizeIpv6IfApplicable)
                .filter(Objects::nonNull)
                .sorted()
                .distinct()
                .collect(Collectors.toList());

            // Match any peer URI whose host string equals one of our candidate identities. The
            // candidate set may contain multiple forms (FQDN, IPv4, IPv6) so this lookup
            // succeeds whichever form the d2 client materialized peer URIs in.
            int instanceId = -1;
            String matchedIdentity = null;
            for (int i = 0; i < sortedHosts.size(); i++)
            {
              if (_candidateIdentities.contains(sortedHosts.get(i)))
              {
                instanceId = i;
                matchedIdentity = sortedHosts.get(i);
                break;
              }
            }

            if (instanceId >= 0)
            {
              _warnedIdentityNotFound = false;
              _subsettingMetadata = new DeterministicSubsettingMetadata(instanceId, sortedHosts.size(),
                  _peerClusterVersion);
              if (_log.isDebugEnabled())
              {
                _log.debug("Computed deterministic subsetting metadata for cluster {}: "
                        + "matchedIdentity={}, instanceId={}, totalInstanceCount={}",
                    _clusterName, matchedIdentity, instanceId, sortedHosts.size());
              }
            }
            else
            {
              _subsettingMetadata = null;
              if (!_warnedIdentityNotFound)
              {
                _log.warn("None of identities {} found in peer cluster '{}' membership ({} hosts); "
                        + "subsetting metadata is unavailable and the subsetter will fall back to all hosts",
                    _candidateIdentities, _clusterName, sortedHosts.size());
                _warnedIdentityNotFound = true;
              }
            }
          }
          else
          {
            _subsettingMetadata = null;
          }
        }
      }
      metadataFutureCallback.onSuccess(_subsettingMetadata);
    });

    try
    {
      return metadataFutureCallback.get(_timeout, _unit);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      _log.warn("Failed to fetch deterministic subsetting metadata from ZooKeeper for cluster " + _clusterName, e);
      return null;
    }
  }
}
