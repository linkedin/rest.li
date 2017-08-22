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

package com.linkedin.d2.discovery.stores.zk;

import com.linkedin.util.ArgumentUtil;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Builder for {@link ZKConnection}
 */
public class ZKConnectionBuilder
{
  private final String _connectString;
  private int _sessionTimeout;
  private boolean _shutdownAsynchronously = false;
  private int _retryLimit = 0;
  private boolean _isSymlinkAware = false;
  private boolean _exponentialBackoff = false;
  private ScheduledExecutorService _retryScheduler = null;
  private long _initInterval = 0;

  /**
   * @param connectString comma separated host:port pairs, each corresponding to a zk
   *                      server. e.g. "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002" If
   *                      the optional chroot suffix is used the example would look
   *                      like: "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002/app/a"
   *                      where the client would be rooted at "/app/a" and all paths
   *                      would be relative to this root - ie getting/setting/etc...
   *                      "/foo/bar" would result in operations being run on
   *                      "/app/a/foo/bar" (from the server perspective).
   *
   *                      The connectString is always required or the class will throw
   *                      a NullException on ZKConnection.start()
   */
  public ZKConnectionBuilder(String connectString)
  {
    ArgumentUtil.notNull(connectString, "connectString");
    _connectString = connectString;
  }

  /**
   * @param sessionTimeout session timeout in milliseconds
   */
  public ZKConnectionBuilder setTimeout(int sessionTimeout)
  {
    _sessionTimeout = sessionTimeout;
    return this;
  }

  /**
   * @param shutdownAsynchronously Make the shutdown call asynchronous
   */
  public ZKConnectionBuilder setShutdownAsynchronously(boolean shutdownAsynchronously)
  {
    _shutdownAsynchronously = shutdownAsynchronously;
    return this;
  }

  /**
   * @param retryLimit limit of attempts for RetryZooKeeper reconnection
   */
  public ZKConnectionBuilder setRetryLimit(int retryLimit)
  {
    _retryLimit = retryLimit;
    return this;
  }

  /**
   * @param isSymlinkAware Resolves znodes whose name is prefixed with a
   *                       dollar sign '$' (eg. /$symlink1, /foo/bar/$symlink2)
   */
  public ZKConnectionBuilder setIsSymlinkAware(boolean isSymlinkAware)
  {
    _isSymlinkAware = isSymlinkAware;
    return this;
  }

  /**
   * @param exponentialBackoff enables exponential backoff for the RetryZooKeeper reconnection
   */
  public ZKConnectionBuilder setExponentialBackoff(boolean exponentialBackoff)
  {
    _exponentialBackoff = exponentialBackoff;
    return this;
  }

  /**
   * @param retryScheduler scheduler for retry attempts of RetryZooKeeper
   */
  public ZKConnectionBuilder setScheduler(ScheduledExecutorService retryScheduler)
  {
    _retryScheduler = retryScheduler;
    return this;
  }

  /**
   * @param initInterval sets the initial time interval between retrials
   *                     in the exponential backoff for the RetryZooKeeper reconnection
   */
  public ZKConnectionBuilder setInitInterval(long initInterval)
  {
    _initInterval = initInterval;
    return this;
  }

  public ZKConnection build()
  {
    return new ZKConnection(_connectString, _sessionTimeout, _retryLimit, _exponentialBackoff,
      _retryScheduler, _initInterval, _shutdownAsynchronously, _isSymlinkAware);
  }
}