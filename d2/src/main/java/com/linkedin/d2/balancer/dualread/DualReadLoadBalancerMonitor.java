package com.linkedin.d2.balancer.dualread;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DualReadLoadBalancerMonitor<T>
{
  private static final Logger _log = LoggerFactory.getLogger(DualReadLoadBalancerMonitor.class);
  private static final int MAX_CACHE_SIZE = 1000;
  private static final int CACHE_EXPIRATION_SECOND = 30;
  private final Cache<String, CacheEntry<T>> _oldLbPropertyCache;
  private final Cache<String, CacheEntry<T>> _newLbPropertyCache;

  public DualReadLoadBalancerMonitor()
  {
    _oldLbPropertyCache = buildCache();
    _newLbPropertyCache = buildCache();
  }

  public void reportData(String propertyName, T property, long propertyVersion, boolean fromNewLb)
  {
    Cache<String, CacheEntry<T>> cacheToAdd = fromNewLb ? _newLbPropertyCache : _oldLbPropertyCache;
    Cache<String, CacheEntry<T>> cacheToCompare = fromNewLb ? _oldLbPropertyCache : _newLbPropertyCache;

    CacheEntry<T> entry = cacheToCompare.getIfPresent(propertyName);

    if (entry != null && entry._version == propertyVersion)
    {
      // TODO: Compare service discovery data correctness here
      cacheToCompare.invalidate(propertyName);
    }

    cacheToAdd.put(propertyName, new CacheEntry<>(propertyVersion, property));
  }

  private Cache<String, CacheEntry<T>> buildCache()
  {
    return CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_SIZE)
        // Invalidate cache entry after 30s to avoid overflow
        .expireAfterWrite(CACHE_EXPIRATION_SECOND, TimeUnit.SECONDS)
        .removalListener(notification ->
        {
          // If a cache entry is evicted due to TTL, print a WARN log because it only
          // receives update from one source
          if (notification.getCause().equals(RemovalCause.EXPIRED))
          {
            _log.warn("Cache entry evicted after {} s: {}", CACHE_EXPIRATION_SECOND, notification.getValue());
          }
        })
        .build();
  }

  private static final class CacheEntry<T>
  {
    final long _version;
    final T _data;

    CacheEntry(long version, T data)
    {
      _version = version;
      _data = data;
    }

    @Override
    public String toString()
    {
      return "CacheEntry{" + "_version=" + _version + ", _data=" + _data + '}';
    }
  }
}
