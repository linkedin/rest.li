/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer;


import com.linkedin.d2.balancer.util.HostSet;
import com.linkedin.d2.balancer.util.HostToKeyMapper;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.r2.message.RequestContext;

import java.net.URI;
import java.util.Collection;

/**
 * KeyMapper is used with D2's consistent hashing mechanism to maintain key to host affinity across
 * a set of functionally similar hosts.  A typical use case for this affinity is to allow the service
 * provider to maintain cache affinity, for better resource utilization.  Note that there are no
 * strict partitioning guarantees, since failure or overload conditions can cause the same key to
 * be mapped to another host on subsequent requests.  Similarly, different clients may have
 * temporally inconsistent views of the key mappings.
 *
 * KeyMapper allows clients to determine which host would be chosen to serve a request for a
 * specific key without requiring a request for the key to be fully constructed.  This allows clients
 * to map a set of keys across the hosts in the D2 cluster.  The client can use this information to
 * construct a single batch request to each host, containing the group of keys which map to
 * the respective host.
 *
 * Clients should not depend on a KeyMapper if strict partitioning is required.
 * @author Josh Walker
 * @version $Revision: $
 */

public interface KeyMapper
{
  /**
   * Maps keys to hosts according to the underlying strategy, e.g., consistent hashing.
   * Output is a map of host URI to group of keys.  Each key in the input set will
   * appear in exactly one of output groups, i.e., the groups are mutually exclusive
   * and collectively exhaustive (MECE). If a key is not mapped, either because we can
   * not find a partition for the key (illegal key, wrong config, etc), or we can not
   * find a server for the partition of the key (no servers registered for the partition,
   * or all servers are bad, or the call dropping happened), we put the key in the
   * unmapped keys in the return value, and also provide a error type for the unmappped
   * key.
   *
   * The ServiceUnavailableException will be thrown if 1) no such service configured
   * 2) no load balancer strategy for the service
   *
   * No ServiceUnavailableException will be thrown for the errors caused by the
   * unmapped keys. Instead, users should check the errors of unmapped keys
   * and handle them properly @see MapKeyResult
   *
   * Duplicate keys are not allowed.
   * V2 is here to differentiate it from the older API
   *
   * @param keys The set of keys to be mapped
   * @param serviceUri The URI for the service to which requests will be issued
   * @param <K> The key type
   * @return @link MapKeyResult contains mapped keys and also unmapped keys
   */
  public <K> MapKeyResult<URI, K> mapKeysV2(URI serviceUri, Iterable<K> keys)
      throws ServiceUnavailableException;

  /**
   * Given a d2 service URI (for example : d2://articles), a collection of keys and a desired number of
   * hosts per keys, this method returns a mapping of those keys to hosts that the caller can send to. The
   * reason why we want multiple hosts per key is to give users multiple options to send the request if
   * the first attempt to send request doesn't return result. This is the main difference between
   * mapKeysV2 and mapKeysV3.
   *
   * The returned hosts are picked semi randomly weighted based on the health of the hosts.
   * For example if keys 1,2,3 can be sent to host1, host2, host3 and you want us to return 2 hosts
   * we will return the top 2 "best" host ranked according to d2 load balancer algorithm.
   *
   * If there are not enough host in the to fulfill the requested number of hosts,
   * we will try to return as many as we can.
   *
   *
   * @param serviceUri
   * @param keys
   * @param limitNumHostsPerPartition
   * @param <K>
   * @return {@link HostToKeyMapper}
   * @throws ServiceUnavailableException
   */
  public <K> HostToKeyMapper<K> mapKeysV3(URI serviceUri, Collection<K> keys, int limitNumHostsPerPartition)
      throws ServiceUnavailableException;

  /**
   * Similar to the other mapKeysV3 method but accepting a sticky key to determine the order of hosts.
   * That means if the same sticky key is used in two different calls, the order of hosts in each partition will also be the same.
   */
  public <K, S> HostToKeyMapper<K> mapKeysV3(URI serviceUri,
                                             Collection<K> keys,
                                             int limitNumHostsPerPartition,
                                             S stickyKey)
          throws ServiceUnavailableException;

  /**
   * Get host uris for each partition that is available. The number of hosts returned per partition is
   * numHostPerPartition. The returned structure will contain the partitionId -> List of URI for that partitionId.
   * It will also provide information about partitions that are unaviailable.
   *
   * @param serviceUri the service uri
   * @param numHostPerPartition the number of hosts that we should return for each partition. Must be larger than 0.
   * @return {@link com.linkedin.d2.balancer.util.HostSet}
   */
  public HostSet getAllPartitionsMultipleHosts(URI serviceUri, int numHostPerPartition)
      throws ServiceUnavailableException;

  /**
   * Similar to the other getAllPartitionsMultipleHosts method but accepting a sticky key to determine the order of hosts.
   * That means if the same sticky key is used in two different calls, the order of hosts in each partition will also be the same.
   */
  public <S> HostSet getAllPartitionsMultipleHosts(URI serviceUri,
                                                                                    int limitHostPerPartition,
                                                                                    final S stickyKey)
      throws ServiceUnavailableException;

  public static class TargetHostHints
  {
    private static final String TARGET_HOST_KEY_NAME = "D2-KeyMapper-TargetHost";

    /**
     * Inserts a hint in RequestContext instructing D2 to bypass normal hashing behavior
     * and instead route to the specified target host.  Clients should obtain the URI
     * for target host from D2, e.g., by calling {@link KeyMapper}.mapKeys().
     * @param context RequestContext for the request which will be made
     * @param targetHost target host's URI to be used as a hint in the RequestContext
     */
    public static void setRequestContextTargetHost(RequestContext context, URI targetHost)
    {
      context.putLocalAttr(TARGET_HOST_KEY_NAME, targetHost);
    }

    /**
     * Looks for a target host hint in the RequestContext, returning it if found, or null if no
     * hint is present.
     * @param context RequestContext for the request
     * @return URI for target host hint, or null if no hint is present in the RequestContext
     */
    public static URI getRequestContextTargetHost(RequestContext context)
    {
      return (URI)context.getLocalAttr(TARGET_HOST_KEY_NAME);
    }
  }
}
