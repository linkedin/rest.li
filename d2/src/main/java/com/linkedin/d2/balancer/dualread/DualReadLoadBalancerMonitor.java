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
 * When a new service discovery data is reported, it will check if the cache of the other data source
 * has data for the same property name. If there is, it will compare whether the two data are equal.
 */
public abstract class DualReadLoadBalancerMonitor<T>
{
  private static final Logger LOG = LoggerFactory.getLogger(DualReadLoadBalancerMonitor.class);
  public final static String DEFAULT_DATE_FORMAT = "YYYY/MM/dd HH:mm:ss.SSS";
  public final static String VERSION_FROM_FS = "-1";
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
    // compare with existing data in the cache to add to
    Cache<String, CacheEntry<T>> cacheToAdd = fromNewLb ? _newLbPropertyCache : _oldLbPropertyCache;
    CacheEntry<T> existingEntry = cacheToAdd.getIfPresent(propertyName);
    String propertyClassName = property.getClass().getSimpleName();
    boolean isUriProp = property instanceof UriProperties;

    if (existingEntry != null && existingEntry._data.equals(property))
    {
      if (existingEntry._version.equals(propertyVersion))
      {
        LOG.debug("Reported duplicated {} for {} for {} LB, version: {}, data: {}",
            propertyClassName, propertyName, fromNewLb ? "New" : "Old", propertyVersion, property);
        return; // skip setting duplicate data to avoid incorrectly incrementing OutOfSync metric
      }
      else
      {
        // Existing data is the same but with a different version. Some scenarios can cause this:
        // 1) uris flip their status down-then-up quickly within an update receipt interval (ZK: ~10s,
        // xDS: rate limiter ~0.5s) will end up with the same uri properties that only differs in the
        // version.
        // 2) uris data read from FS has a version of "-1|x". When read new data from ZK/xDS, the version will be
        // different.
        if (!isReadFromFS(existingEntry._version, propertyVersion))
        {
          String msg = String.format("Received same data of different versions in %s LB for %s: %s."
                      + " Old version: %s, New version: %s, Data: %s",
                  fromNewLb ? "New" : "Old", propertyClassName, propertyName, existingEntry._version,
                  propertyVersion, property);

          logByPropType(isUriProp, msg);
        }
        // still need to put in the cache, don't skip
      }
    }

    // compare with data in the other cache
    Cache<String, CacheEntry<T>> cacheToCompare = fromNewLb ? _oldLbPropertyCache : _newLbPropertyCache;
    CacheEntry<T> entryToCompare = cacheToCompare.getIfPresent(propertyName);
    boolean isVersionEqual = entryToCompare != null && Objects.equals(entryToCompare._version, propertyVersion);
    boolean isDataEqual = entryToCompare != null && entryToCompare._data.equals(property);

    CacheEntry<T> newEntry = new CacheEntry<>(propertyVersion, getTimestamp(), property);
    String entriesLogMsg = getEntriesMessage(fromNewLb, entryToCompare, newEntry);

    if (isDataEqual && (isVersionEqual || isReadFromFS(propertyVersion, entryToCompare._version)))
    { // data is the same AND version is the same or read from FS. it's a match
      decrementEntryOutOfSyncCount(); // decrement the out-of-sync count for the entry received earlier
      if (!isVersionEqual)
      {
        LOG.debug("Matched {} for {} that only differ in version. {}",
            propertyClassName, propertyName, entriesLogMsg);
      }
      else {
        LOG.debug("Matched {} for {}. {}", propertyClassName, propertyName, entriesLogMsg);
      }
      cacheToCompare.invalidate(propertyName);
    }
    else if (!isDataEqual && isVersionEqual)
    { // data is not the same but version is the same, a mismatch!
      incrementPropertiesErrorCount();
      incrementEntryOutOfSyncCount(); // increment the out-of-sync count for the entry received later
      logByPropType(isUriProp,
          String.format("Received mismatched %s for %s. %s", propertyClassName, propertyName, entriesLogMsg));
      cacheToCompare.invalidate(propertyName);
    }
    else {
      if (isDataEqual)
      {
        String msg = String.format("Received same data of %s for %s but with different versions: %s",
            propertyClassName, propertyName, entriesLogMsg);
        logByPropType(isUriProp, msg);
      }
      cacheToAdd.put(propertyName, newEntry);
      incrementEntryOutOfSyncCount();
      LOG.debug("Added new entry {} for {} for {} LB.", propertyClassName, propertyName, fromNewLb ? "New" : "Old");
    }

    // after cache entry add/delete above, re-log the latest entries on caches
    entryToCompare = cacheToCompare.getIfPresent(propertyName);
    newEntry = cacheToAdd.getIfPresent(propertyName);
    entriesLogMsg = getEntriesMessage(fromNewLb, entryToCompare, newEntry);
    LOG.debug("Current entries of {} for {}: {}", propertyClassName, propertyName, entriesLogMsg);
  }

  @VisibleForTesting
  Cache<String, CacheEntry<T>> getOldLbCache()
  {
    return _oldLbPropertyCache;
  }

  @VisibleForTesting
  Cache<String, CacheEntry<T>> getNewLbCache()
  {
    return _newLbPropertyCache;
  }

  abstract void incrementEntryOutOfSyncCount();

  abstract void decrementEntryOutOfSyncCount();

  abstract void incrementPropertiesErrorCount();

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
            LOG.debug("Cache entry evicted since cache is full: {}", notification.getValue());
            onEvict();
          }
        })
        .build();
  }

  private boolean isReadFromFS(String v1, String v2)
  {
    // uri prop version from FS is "-1|x", where x is the number of uris, so we use startsWith here
    return v1.startsWith(VERSION_FROM_FS) || v2.startsWith(VERSION_FROM_FS);
  }

  private void logByPropType(boolean isUriProp, String msg)
  {
    if (isUriProp)
    {
      _rateLimitedLogger.debug(msg);
    }
    else
    {
      LOG.warn(msg);
    }
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
    void incrementPropertiesErrorCount()
    {
      _dualReadLoadBalancerJmx.incrementClusterPropertiesErrorCount();
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
    void incrementPropertiesErrorCount()
    {
      _dualReadLoadBalancerJmx.incrementServicePropertiesErrorCount();
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
    void incrementPropertiesErrorCount()
    {
      _dualReadLoadBalancerJmx.incrementUriPropertiesErrorCount();
    }

    @Override
    void onEvict()
    {
      _dualReadLoadBalancerJmx.incrementUriPropertiesEvictCount();
    }
  }
}
