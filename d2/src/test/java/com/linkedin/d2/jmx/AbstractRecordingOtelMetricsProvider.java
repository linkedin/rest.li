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


/** Shared recording base for strategy OTel metrics test doubles. */
abstract class AbstractRecordingOtelMetricsProvider
{
  private final List<MetricsInvocation> _calls = new ArrayList<>();

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

  protected void recordInt(String methodName, String serviceName, String scheme, int intValue,
      HostStatus hostStatus)
  {
    _calls.add(new MetricsInvocation(methodName, serviceName, scheme, intValue, hostStatus));
  }

  protected void recordDouble(String methodName, String serviceName, String scheme, double doubleValue)
  {
    _calls.add(new MetricsInvocation(methodName, serviceName, scheme, doubleValue));
  }

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
   * Returns the number of invocations of {@code methodName} whose recorded {@link HostStatus}
   * tag matched {@code status}. Used to assert that a single attribute-dimensioned gauge has
   * been emitted once per status value in the same emission cycle.
   */
  public int getCallCountForHostStatus(String methodName, HostStatus status)
  {
    int count = 0;
    for (MetricsInvocation call : _calls)
    {
      if (call.methodName.equals(methodName) && call.hostStatus == status)
      {
        count++;
      }
    }
    return count;
  }

  /**
   * Returns the most recent integer value recorded for {@code methodName} with the given
   * {@link HostStatus} attribute, or {@code null} if no such call was recorded.
   */
  public Integer getLastIntValueForHostStatus(String methodName, HostStatus status)
  {
    for (int i = _calls.size() - 1; i >= 0; i--)
    {
      MetricsInvocation call = _calls.get(i);
      if (call.methodName.equals(methodName) && call.hostStatus == status)
      {
        return call.intValue;
      }
    }
    return null;
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
    HostStatus hostStatus;

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

    MetricsInvocation(String methodName, String serviceName, String scheme, int intValue,
        HostStatus hostStatus)
    {
      this.methodName = methodName;
      this.serviceName = serviceName;
      this.scheme = scheme;
      this.intValue = intValue;
      this.hostStatus = hostStatus;
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
