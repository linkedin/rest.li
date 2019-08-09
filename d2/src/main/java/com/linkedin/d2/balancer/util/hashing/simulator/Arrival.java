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

package com.linkedin.d2.balancer.util.hashing.simulator;

import java.util.Random;


/**
 * The class specifies the arrival pattern of the {@link Request}s.
 */
public class Arrival
{
  private final int _minInterval;
  private final int _maxInterval;
  private final double _stddev;
  private final ConsistentHashRingSimulatorConfig.RandomStrategy _randomStrategy;
  private final Random _random = new Random();

  public Arrival(ConsistentHashRingSimulatorConfig.Arrival arrival)
  {
    this(arrival.getMinInterval(), arrival.getMaxInterval(), arrival.getStddev(), arrival.getRandomStrategy());
  }

  /**
   * Creates an arrival pattern with minimum interval, max interval, standard deviation and random strategy.
   *
   * @param minInterval   Minimum interval in milliseconds
   * @param maxInterval   Maximum interval in milliseconds
   * @param stddev        Standard deviation
   * @param randomStrategy  Random strategy to use. See {@link com.linkedin.d2.balancer.util.hashing.simulator.ConsistentHashRingSimulatorConfig.RandomStrategy}
   */
  public Arrival(int minInterval, int maxInterval, double stddev,
      ConsistentHashRingSimulatorConfig.RandomStrategy randomStrategy)
  {
    _minInterval = minInterval;
    _maxInterval = maxInterval;
    _stddev = stddev;
    _randomStrategy = randomStrategy;
  }

  /**
   * Get the next interval using the arrival pattern specified
   *
   * @return Next interval in milliseconds
   */
  public int getNextInterval()
  {
    switch (_randomStrategy)
    {
      case UNIFORM:
        return _random.nextInt(_maxInterval - _minInterval) + _minInterval;
      case GAUSSIAN:
        return ConsistentHashRingSimulator.getNormal(_minInterval, _maxInterval, _stddev);
      default:
        throw new IllegalStateException(String.format("Error: cannot recognize random strategy %s", _randomStrategy));
    }
  }
}
