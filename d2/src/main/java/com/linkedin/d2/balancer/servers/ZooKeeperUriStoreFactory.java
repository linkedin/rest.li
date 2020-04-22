package com.linkedin.d2.balancer.servers;

import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZookeeperChildFilter;
import com.linkedin.d2.discovery.stores.zk.ZookeeperEphemeralPrefixGenerator;
import com.linkedin.d2.discovery.stores.zk.builder.ZooKeeperEphemeralStoreBuilder;

/**
 * A factory class to create {@link ZooKeeperEphemeralStore < UriProperties >}
 *
 * @author Nizar Mankulangara (nmankulangara@linkedin.com)
 */
public class ZooKeeperUriStoreFactory implements ZooKeeperConnectionManager.ZKStoreFactory<UriProperties, ZooKeeperEphemeralStore<UriProperties>>
{
  private ZookeeperChildFilter _childFilter;
  private ZookeeperEphemeralPrefixGenerator _prefixGenerator;
  private boolean _useHashEphemeralPrefix;

  public ZooKeeperUriStoreFactory(ZookeeperChildFilter childFilter, ZookeeperEphemeralPrefixGenerator prefixGenerator, boolean useHashEphemeralPrefix)
  {

    _childFilter = childFilter;
    _prefixGenerator = prefixGenerator;
    _useHashEphemeralPrefix = useHashEphemeralPrefix;
  }

  @Override
  public ZooKeeperEphemeralStore<UriProperties> createStore(ZKConnection connection, String path)
  {
    ZooKeeperEphemeralStoreBuilder<UriProperties> storeBuilder = new ZooKeeperEphemeralStoreBuilder<>();
    storeBuilder.setZkConnection(connection);
    storeBuilder.setSerializer(new UriPropertiesJsonSerializer());
    storeBuilder.setMerger(new UriPropertiesMerger());
    storeBuilder.setPath(path);

    if (_useHashEphemeralPrefix)
    {
      storeBuilder.setZookeeperChildFilter(_childFilter);
      storeBuilder.setZookeeperEphemeralPrefixGenerator(_prefixGenerator);
    }

    return storeBuilder.build();
  }
}
