/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.netty.client;

import com.linkedin.common.stats.LongStats;
import com.linkedin.common.stats.LongTracker;
import com.linkedin.common.stats.LongTracking;
import java.util.concurrent.atomic.AtomicLong;


public class JmxDnsMetricsCallback implements HttpNettyClientJmxMBean, DnsMetricsCallback {
  private final AtomicLong _dnsResolutionErrors = new AtomicLong(0);
  private final AtomicLong _dnsResolutions = new AtomicLong(0);
  private final LongTracker _dnsResolutionLatencyMs = new LongTracking();

  @Override
  public long getDnsResolutions() {
    return _dnsResolutions.get();
  }

  @Override
  public long getDnsResolutionErrors() {
    return _dnsResolutionErrors.get();
  }

  @Override
  public LongStats getDnsResolutionLatencyMs() {
    return _dnsResolutionLatencyMs.getStats();
  }

  @Override
  public void start() {
    _dnsResolutions.getAndIncrement();
  }

  @Override
  public void success(long latencyMilliseconds) {
    synchronized (_dnsResolutionLatencyMs) {
      _dnsResolutionLatencyMs.addValue(latencyMilliseconds);
    }
  }

  @Override
  public void error() {
    _dnsResolutionErrors.getAndIncrement();
  }
}
