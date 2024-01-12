/*
   Copyright (c) 2023 LinkedIn Corp.

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

package com.linkedin.d2.balancer.dualread;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.util.RateLimitedLogger;
import com.linkedin.util.clock.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Monitors dual read service discovery data and compares the correctness of the data.
 *
 * For each service discovery properties, there will be a cache for old load balancer data, and a cache
 * for new load balancer data.
 *
 * When a new service discovery data is reported, it will check the cache of the other data source
 * and see if there is a match based on the property name and version. If there is a match, it will
 * use the {@link DualReadLoadBalancerMonitor#isEqual(CacheEntry, CacheEntry)} method to compare whether
 * the two entries are equal.
 */
public abstract class DualReadLoadBalancerMonitor<T>
{
  private static final Logger LOG = LoggerFactory.getLogger(DualReadLoadBalancerMonitor.class);
  public final static String DEFAULT_DATE_FORMAT = "YYYY/MM/dd HH:mm:ss.SSS";
  private static final long ERROR_REPORT_PERIOD = 10 * 1000; // Limit error report logging to every 10 seconds
  private static final int MAX_CACHE_SIZE = 10000;
  private final Cache<String, CacheEntry<T>> _oldLbPropertyCache;
  private final Cache<String, CacheEntry<T>> _newLbPropertyCache;
  private final RateLimitedLogger _rateLimitedLogger;
  private final Clock _clock;
  private final DateTimeFormatter _format;


  public DualReadLoadBalancerMonitor(Clock clock)
  {
    _oldLbPropertyCache = buildCache();
    _newLbPropertyCache = buildCache();
    _rateLimitedLogger = new RateLimitedLogger(LOG, ERROR_REPORT_PERIOD, clock);
    _clock = clock;
    _format = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
  }

  public void reportData(String propertyName, T property, String propertyVersion, boolean fromNewLb)
  {
    Cache<String, CacheEntry<T>> cacheToAdd = fromNewLb ? _newLbPropertyCache : _oldLbPropertyCache;
    CacheEntry<T> existingEntry = cacheToAdd.getIfPresent(propertyName);
    if (existingEntry != null)
    {
      if (existingEntry._version.equals(propertyVersion) && existingEntry._data.equals(property))
      {
        _rateLimitedLogger.debug("Reported duplicate for {} LB for property: {}, version: {}, data: {}",
            fromNewLb ? "New" : "Old", propertyName, propertyVersion, property);
        return; // skip setting duplicate data to avoid incorrectly incrementing OutOfSync metric
      }
      else if (existingEntry._data.equals(property))
      {
        _rateLimitedLogger.warn("Reported data that only differs in version for {} LB for property: {}. "
                + "Old version: {}, New version: {}, with the same data: {}", fromNewLb ? "New" : "Old", propertyName,
            existingEntry._version, propertyVersion, existingEntry._data);
        // since the version is different, we don't skipping setting it to the cache
      }
    }

    Cache<String, CacheEntry<T>> cacheToCompare = fromNewLb ? _oldLbPropertyCache : _newLbPropertyCache;

    CacheEntry<T> entryToCompare = cacheToCompare.getIfPresent(propertyName);
    CacheEntry<T> newEntry = new CacheEntry<>(propertyVersion, getTimestamp(), property);
    String entriesLogMsg = getEntriesMessage(fromNewLb, entryToCompare, newEntry);

    if (entryToCompare != null && Objects.equals(entryToCompare._version, propertyVersion))
    {
      if (!isEqual(entryToCompare, newEntry))
      {
        incrementEntryOutOfSyncCount(); // increment the out-of-sync count for the entry received later
        _rateLimitedLogger.warn("Received mismatched properties from dual read. {}", entriesLogMsg);
      }
      else
      { // entries are in-sync, decrement the out-of-sync count for the entry received earlier
        decrementEntryOutOfSyncCount();
        _rateLimitedLogger.debug("Matched properties from dual read. {}", entriesLogMsg);
      }
      cacheToCompare.invalidate(propertyName);
    }
    else
    {
      cacheToAdd.put(propertyName, newEntry);
      // if version is different, entries of both the old version and the new version will increment the out-of-sync count
      incrementEntryOutOfSyncCount();
      _rateLimitedLogger.debug("Added new entry to dual read cache for {} LB.",
          fromNewLb ? "New" : "Old");
    }
    entryToCompare = cacheToCompare.getIfPresent(propertyName);
    newEntry = cacheToAdd.getIfPresent(propertyName);
    entriesLogMsg = getEntriesMessage(fromNewLb, entryToCompare, newEntry);
    _rateLimitedLogger.debug("Current entries on dual read caches: {}", entriesLogMsg);
  }

  abstract void incrementEntryOutOfSyncCount();

  abstract void decrementEntryOutOfSyncCount();

  abstract boolean isEqual(CacheEntry<T> oldLbEntry, CacheEntry<T> newLbEntry);

  abstract void onEvict();

  private Cache<String, CacheEntry<T>> buildCache()
  {
    return CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_SIZE)
        .removalListener(notification ->
        {
          // If a cache entry is evicted due to maximum capacity, print a WARN log because it only
          // receives update from one source
          if (notification.getCause().equals(RemovalCause.SIZE))
          {
            _rateLimitedLogger.warn("Cache entry evicted since cache is full: {}", notification.getValue());
            onEvict();
          }
        })
        .build();
  }

  @VisibleForTesting
  String getEntriesMessage(boolean fromNewLb, CacheEntry<T> oldE, CacheEntry<T> newE)
  {
    return String.format("\nOld LB: %s\nNew LB: %s",
        fromNewLb? oldE : newE, fromNewLb? newE : oldE);
  }

  private String getTimestamp() {
    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(_clock.currentTimeMillis()), ZoneId.systemDefault())
        .format(_format);
  }

  @VisibleForTesting
  static final class CacheEntry<T>
  {
    final String _version;
    final String _timeStamp;
    final T _data;

    CacheEntry(String version, String timeStamp, T data)
    {
      _version = version;
      _timeStamp = timeStamp;
      _data = data;
    }

    @Override
    public String toString()
    {
      return "CacheEntry{" + "_version=" + _version + ", _timeStamp='" + _timeStamp + '\'' + ", _data=" + _data + '}';
    }
  }

  public static final class ClusterPropertiesDualReadMonitor extends DualReadLoadBalancerMonitor<ClusterProperties>
  {
    private final DualReadLoadBalancerJmx _dualReadLoadBalancerJmx;

    public ClusterPropertiesDualReadMonitor(DualReadLoadBalancerJmx dualReadLoadBalancerJmx, Clock clock)
    {
      super(clock);
      _dualReadLoadBalancerJmx = dualReadLoadBalancerJmx;
    }

    @Override
    void incrementEntryOutOfSyncCount() {
      _dualReadLoadBalancerJmx.incrementClusterPropertiesOutOfSyncCount();
    }

    @Override
    void decrementEntryOutOfSyncCount() {
      _dualReadLoadBalancerJmx.decrementClusterPropertiesOutOfSyncCount();
    }

    @Override
    boolean isEqual(CacheEntry<ClusterProperties> oldLbEntry, CacheEntry<ClusterProperties> newLbEntry)
    {
      if (!oldLbEntry._data.equals(newLbEntry._data))
      {
        _dualReadLoadBalancerJmx.incrementClusterPropertiesErrorCount();
        return false;
      }

      return true;
    }

    @Override
    void onEvict()
    {
      _dualReadLoadBalancerJmx.incrementClusterPropertiesEvictCount();
    }
  }

  public static final class ServicePropertiesDualReadMonitor extends DualReadLoadBalancerMonitor<ServiceProperties>
  {
    private final DualReadLoadBalancerJmx _dualReadLoadBalancerJmx;

    public ServicePropertiesDualReadMonitor(DualReadLoadBalancerJmx dualReadLoadBalancerJmx, Clock clock)
    {
      super(clock);
      _dualReadLoadBalancerJmx = dualReadLoadBalancerJmx;
    }

    @Override
    void incrementEntryOutOfSyncCount() {
      _dualReadLoadBalancerJmx.incrementServicePropertiesOutOfSyncCount();
    }

    @Override
    void decrementEntryOutOfSyncCount() {
      _dualReadLoadBalancerJmx.decrementServicePropertiesOutOfSyncCount();
    }

    @Override
    boolean isEqual(CacheEntry<ServiceProperties> oldLbEntry, CacheEntry<ServiceProperties> newLbEntry)
    {
      if (!oldLbEntry._data.equals(newLbEntry._data))
      {
        _dualReadLoadBalancerJmx.incrementServicePropertiesErrorCount();
        return false;
      }

      return true;
    }

    @Override
    void onEvict()
    {
      _dualReadLoadBalancerJmx.incrementServicePropertiesEvictCount();
    }
  }

  public static final class UriPropertiesDualReadMonitor extends DualReadLoadBalancerMonitor<UriProperties>
  {
    private final DualReadLoadBalancerJmx _dualReadLoadBalancerJmx;

    public UriPropertiesDualReadMonitor(DualReadLoadBalancerJmx dualReadLoadBalancerJmx, Clock clock)
    {
      super(clock);
      _dualReadLoadBalancerJmx = dualReadLoadBalancerJmx;
    }

    @Override
    void incrementEntryOutOfSyncCount() {
      _dualReadLoadBalancerJmx.incrementUriPropertiesOutOfSyncCount();
    }

    @Override
    void decrementEntryOutOfSyncCount() {
      _dualReadLoadBalancerJmx.decrementUriPropertiesOutOfSyncCount();
    }

    @Override
    boolean isEqual(CacheEntry<UriProperties> oldLbEntry, CacheEntry<UriProperties> newLbEntry)
    {
      if (oldLbEntry._data.Uris().size() != newLbEntry._data.Uris().size())
      {
        _dualReadLoadBalancerJmx.incrementUriPropertiesErrorCount();
        return false;
      }

      return true;
    }

    @Override
    void onEvict()
    {
      _dualReadLoadBalancerJmx.incrementUriPropertiesEvictCount();
    }
  }
}
