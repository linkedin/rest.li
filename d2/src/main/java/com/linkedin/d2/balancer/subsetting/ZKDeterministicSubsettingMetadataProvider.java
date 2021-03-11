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

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.properties.UriProperties;
import java.net.URI;
import java.util.List;
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
  private final String _clusterName;
  private final URI _nodeUri;
  private final long _timeout;
  private final TimeUnit _unit;

  private final Object _lock = new Object();

  @GuardedBy("_lock")
  private long _clusterGenerationId = -1;
  private DeterministicSubsettingMetadata _subsettingMetadata;

  public ZKDeterministicSubsettingMetadataProvider(String clusterName,
                                      URI nodeUri,
                                      long timeout,
                                      TimeUnit unit)
  {
    _clusterName = clusterName;
    _nodeUri = nodeUri;
    _timeout = timeout;
    _unit = unit;
  }

  @Override
  public DeterministicSubsettingMetadata getSubsettingMetadata(LoadBalancerState state)
  {
    FutureCallback<DeterministicSubsettingMetadata> metadataFutureCallback = new FutureCallback<>();

    state.listenToCluster(_clusterName, (type, name) ->
    {
      LoadBalancerStateItem<UriProperties> uriItem = state.getUriProperties(_clusterName);

      synchronized (_lock)
      {
        if (uriItem.getVersion() != _clusterGenerationId)
        {
          _clusterGenerationId = uriItem.getVersion();
          UriProperties uriProperties = uriItem.getProperty();
          if (uriProperties != null)
          {
            // Sort the URIs so each client sees the same ordering
            List<URI> sortedUris = uriProperties.getPartitionDesc().keySet().stream()
                .filter(uri -> uri.getScheme().equals(_nodeUri.getScheme()))
                .sorted()
                .collect(Collectors.toList());

            int instanceId = sortedUris.indexOf(_nodeUri);

            if (instanceId >= 0)
            {
              _subsettingMetadata = new DeterministicSubsettingMetadata(instanceId, sortedUris.size());
            }
            else
            {
              _subsettingMetadata = null;
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
      return null;
    }
  }
}
