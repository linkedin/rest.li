/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.d2.balancer.strategies.relative;


/**
 * Keeps the state of each tracker client for a partition
 */
public class TrackerClientState {
  enum HealthState
  {
    UNHEALTHY,
    NEUTRAL,
    HEALTHY
  }
  private static final int MIN_CALL_COUNT_THRESHOLD = 1;
  private static final int INITIAL_CALL_COUNT = 0;

  private final int _minCallCount;

  private int _callCount;
  private double _healthScore;
  private HealthState _healthState;

  public TrackerClientState(double initialHealthScore, int minCallCount)
  {
    _healthScore = initialHealthScore;
    _minCallCount = minCallCount;
    _callCount = INITIAL_CALL_COUNT;
    _healthState = HealthState.NEUTRAL;
  }

  public void setCallCount(int callCount)
  {
    _callCount = callCount;
  }

  public void setHealthState(HealthState healthState)
  {
    _healthState = healthState;
  }

  public void setHealthScore(double healthScore)
  {
    _healthScore = healthScore;
  }

  public int getCallCount()
  {
    return _callCount;
  }

  public int getAdjustedMinCallCount()
  {
    return Math.max((int) Math.round(_healthScore * _minCallCount), MIN_CALL_COUNT_THRESHOLD);
  }

  public double getHealthScore()
  {
    return _healthScore;
  }

  public boolean isUnhealthy()
  {
    return _healthState == HealthState.UNHEALTHY;
  }

  public String toString()
  {
    return "_healthScore=" + _healthScore;
  }
}
