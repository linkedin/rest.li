/*
   Copyright (c) 2026 LinkedIn Corp.

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
package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.clients.PerCallDurationSemantics;
import java.util.ArrayList;
import java.util.List;


/**
 * Shared base for in-memory recording test doubles of the OTel metrics provider interfaces. The
 * {@code Test{Relative,Degrader}LoadBalancerStrategy*OtelMetricsProvider} test fixtures extend this
 * class and add only the provider-specific {@code @Override} methods that delegate to the {@code
 * record*} helpers below. Verification helpers (counts, last-value getters, latency replays, reset)
 * live here so behaviour cannot drift between fixtures.
 */
abstract class AbstractRecordingOtelMetricsProvider
{
  private final List<MetricsInvocation> _calls = new ArrayList<>();

  // -------------------------------------------------------------------------
  // Record helpers — concrete provider methods call these from their @Override
  // -------------------------------------------------------------------------

  protected void recordLong(String methodName, String serviceName, String scheme, long longValue)
  {
    _calls.add(new MetricsInvocation(methodName, serviceName, scheme, longValue));
  }

  protected void recordLong(String methodName, String serviceName, String scheme, long longValue,
      PerCallDurationSemantics semantics)
  {
    _calls.add(new MetricsInvocation(methodName, serviceName, scheme, longValue, semantics));
  }

  protected void recordInt(String methodName, String serviceName, String scheme, int intValue)
  {
    _calls.add(new MetricsInvocation(methodName, serviceName, scheme, intValue));
  }

  protected void recordDouble(String methodName, String serviceName, String scheme, double doubleValue)
  {
    _calls.add(new MetricsInvocation(methodName, serviceName, scheme, doubleValue));
  }

  // -------------------------------------------------------------------------
  // Verification helpers
  // -------------------------------------------------------------------------

  public int getCallCount(String methodName)
  {
    int count = 0;
    for (MetricsInvocation call : _calls)
    {
      if (call.methodName.equals(methodName))
      {
        count++;
      }
    }
    return count;
  }

  public String getLastServiceName(String methodName)
  {
    MetricsInvocation last = lastFor(methodName);
    return last == null ? null : last.serviceName;
  }

  public String getLastScheme(String methodName)
  {
    MetricsInvocation last = lastFor(methodName);
    return last == null ? null : last.scheme;
  }

  public Long getLastLongValue(String methodName)
  {
    MetricsInvocation last = lastFor(methodName);
    return last == null ? null : last.longValue;
  }

  public Integer getLastIntValue(String methodName)
  {
    MetricsInvocation last = lastFor(methodName);
    return last == null ? null : last.intValue;
  }

  public Double getLastDoubleValue(String methodName)
  {
    MetricsInvocation last = lastFor(methodName);
    return last == null ? null : last.doubleValue;
  }

  public PerCallDurationSemantics getLastPerCallDurationSemantics(String methodName)
  {
    MetricsInvocation last = lastFor(methodName);
    return last == null ? null : last.perCallDurationSemantics;
  }

  /**
   * Returns all recorded latency values for a given service name and scheme, in invocation order.
   * Useful for verifying histogram data points.
   *
   * @param serviceName the service name to filter by, or {@code null} for all services
   * @param scheme      the scheme to filter by, or {@code null} for all schemes
   * @return list of recorded latency values
   */
  public List<Long> getAllLatencyValues(String serviceName, String scheme)
  {
    List<Long> latencies = new ArrayList<>();
    for (MetricsInvocation call : _calls)
    {
      if (call.methodName.equals("recordHostLatency")
          && (serviceName == null || serviceName.equals(call.serviceName))
          && (scheme == null || scheme.equals(call.scheme)))
      {
        latencies.add(call.longValue);
      }
    }
    return latencies;
  }

  /**
   * Clears all recorded calls. Useful for resetting state between tests.
   */
  public void reset()
  {
    _calls.clear();
  }

  private MetricsInvocation lastFor(String methodName)
  {
    for (int i = _calls.size() - 1; i >= 0; i--)
    {
      MetricsInvocation call = _calls.get(i);
      if (call.methodName.equals(methodName))
      {
        return call;
      }
    }
    return null;
  }

  // -------------------------------------------------------------------------
  // Single record type — only the relevant numeric slot is populated per call.
  // -------------------------------------------------------------------------

  private static final class MetricsInvocation
  {
    final String methodName;
    final String serviceName;
    final String scheme;
    Long longValue;
    Integer intValue;
    Double doubleValue;
    PerCallDurationSemantics perCallDurationSemantics;

    MetricsInvocation(String methodName, String serviceName, String scheme, long longValue)
    {
      this.methodName = methodName;
      this.serviceName = serviceName;
      this.scheme = scheme;
      this.longValue = longValue;
    }

    MetricsInvocation(String methodName, String serviceName, String scheme, long longValue,
        PerCallDurationSemantics semantics)
    {
      this.methodName = methodName;
      this.serviceName = serviceName;
      this.scheme = scheme;
      this.longValue = longValue;
      this.perCallDurationSemantics = semantics;
    }

    MetricsInvocation(String methodName, String serviceName, String scheme, int intValue)
    {
      this.methodName = methodName;
      this.serviceName = serviceName;
      this.scheme = scheme;
      this.intValue = intValue;
    }

    MetricsInvocation(String methodName, String serviceName, String scheme, double doubleValue)
    {
      this.methodName = methodName;
      this.serviceName = serviceName;
      this.scheme = scheme;
      this.doubleValue = doubleValue;
    }
  }
}
