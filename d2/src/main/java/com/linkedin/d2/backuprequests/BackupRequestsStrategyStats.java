/*
   Copyright (c) 2017 LinkedIn Corp.

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
package com.linkedin.d2.backuprequests;

/**
 * Immutable class that contains statistics of a single {@link BackupRequestsStrategy}.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 *
 */
public class BackupRequestsStrategyStats
{

  private final long _allowed;
  private final long _successful;
  private final long _minDelay;
  private final long _maxDelay;
  private final long _avgDelay;

  public BackupRequestsStrategyStats(long allowed, long successful, long minDelay, long maxDelay, long avgDelay)
  {
    _allowed = allowed;
    _successful = successful;
    _minDelay = minDelay;
    _maxDelay = maxDelay;
    _avgDelay = avgDelay;
  }

  /**
   * Returns number of allowed backup requests.
   * @return number of allowed backup requests
   */
  public long getAllowed()
  {
    return _allowed;
  }

  /**
   * Returns number of successful backup requests. Backup request is successful if it returns result
   * sooner than the original request.
   * @return number of successful backup requests
   */
  public long getSuccessful()
  {
    return _successful;
  }

  /**
   * Returns minimum delay in nanoseconds. Delay is an amount of time between original request was made and
   * when decision whether to make backup request or not is made.
   * @return minimum delay in nanoseconds
   */
  public long getMinDelayNano()
  {
    return _minDelay;
  }

  /**
   * Returns maximum delay in nanoseconds. Delay is an amount of time between original request was made and
   * when decision whether to make backup request or not is made.
   * @return maximum delay in nanoseconds
   */
  public long getMaxDelayNano()
  {
    return _maxDelay;
  }

  /**
   * Returns average delay in nanoseconds. Delay is an amount of time between original request was made and
   * when decision whether to make backup request or not is made.
   * @return average delay in nanoseconds
   */
  public long getAvgDelayNano()
  {
    return _avgDelay;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (_allowed ^ (_allowed >>> 32));
    result = prime * result + (int) (_avgDelay ^ (_avgDelay >>> 32));
    result = prime * result + (int) (_maxDelay ^ (_maxDelay >>> 32));
    result = prime * result + (int) (_minDelay ^ (_minDelay >>> 32));
    result = prime * result + (int) (_successful ^ (_successful >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    BackupRequestsStrategyStats other = (BackupRequestsStrategyStats) obj;
    if (_allowed != other._allowed)
      return false;
    if (_avgDelay != other._avgDelay)
      return false;
    if (_maxDelay != other._maxDelay)
      return false;
    if (_minDelay != other._minDelay)
      return false;
    if (_successful != other._successful)
      return false;
    return true;
  }

  @Override
  public String toString()
  {
    return "BackupRequestsStrategyStats [allowed=" + _allowed + ", successful=" + _successful + ", minDelay="
        + _minDelay + ", maxDelay=" + _maxDelay + ", avgDelay=" + _avgDelay + "]";
  }

}
