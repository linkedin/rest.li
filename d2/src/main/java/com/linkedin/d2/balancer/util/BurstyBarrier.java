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
package com.linkedin.d2.balancer.util;

/**
 * This class implements a concept of a barrier that passes through specified percent of events. This class is a
 * general purpose utility that can be used to implement higher level functionalities. One way of thinking
 * about this abstraction is that there is a stream of events arriving at the barrier and only specified percent of them
 * is allowed to pass through the barrier. Below are the properties of events and the barrier:
 * <ul>
 * <li>Events are identical</li>
 * <li>When an event arrives at the barrier it notifies the barrier about it's arrival</li>
 * <li>An event can stay at the barrier for an arbitrary amount of time</li>
 * <li>An event can make at most one attempt to pass through the barrier and it is either allowed to pass through or not</li>
 * <li>An event can leave the barrier at any time without attempting to pass through and without notifying the barrier about leaving</li>
 * <li>Barrier can only pass through specified percent of events</li>
 * <li>It is not known upfront how many events will arrive and how many of them will attempt to pass through</li>
 * </ul>
 *
 * The property that only specified percent of all events is allowed to pass through requires more details.
 * It has to be defined over what set of events it is being calculated. Here is an example scenario:
 * <ol>
 * <li>An event arrives at the barrier</li>
 * <li>An event arrives at the barrier</li>
 * <li>An event attempts to pass through the barrier and is allowed to do so</li>
 * <li>An event attempts to pass through the barrier and is allowed to do so</li>
 * <li>An event arrives at the barrier</li>
 * <li>An event arrives at the barrier</li>
 * </ol>
 *
 * When looking at entire sequence, the barrier passed through 50 percent of events. However, the sequence contains subsequences
 * during which the percent of events that were passed through varies between 0 (from point 1. until point 2.) to 100
 * (from point 1. until point 4.).
 * <p>
 * One potential definition is that barrier is allowed to pass through specified percent of events for any subsequence
 * that starts at point 1. In other words it is always true when looking at the entire past history. The problem with this
 * approach is that it allows arbitrary long subsequences in which all events are being passed through. For example when
 * first all events arrive and then all of them attempt to pass through. Often this is not a desired property.
 * <p>
 * Another possible definition is that every subsequence needs to conform to the condition that only specified percent
 * of events is allowed to pass through. This property is too strong for many practical purposes, especially when the percent
 * parameter is small, events arrive in groups and make an attempt to pass through in groups. For example, if percent
 * parameter is 5 and a group of 20 events attempts to pass through the barrier only 1 event would have to be allowed to pass
 * through. In practice this leads to situation where percent of events passed through over entire sequence is much
 * smaller than specified parameter. Often this is not a desired property.
 * <p>
 * Another possible definition is that in all subsequences of size exactly equal to some specified window the percent
 * of events that are allowed to be passed though the barrier is at most as configured. For subsequences that are smaller
 * than the window size the percent of events that are allowed to be passed though the barrier can be higher than configured.
 * It can be shown that:
 * <ul>
 * <li>For all subsequences of size equal to a number that is multiple of the window size a number of events that are
 * allowed to pass through is at most as configured</li>
 * <li>For all subsequences of size between window and 2 * window, a percent of events that are
 * allowed to pass through is at most ((2 * pct) / (100 + pct)) * 100. For example, if pct = 5, then result of this expression is
 * 9.52 percent, if pct = 50, then result of this expression is 66.6 percent.
 * </li>
 * <li>With growing size of subsequence the the percent of events that are passed through over the specified limit
 * quickly approaches 0</li>
 * </ul>
 * Notice that within the window there might be subsequences in which any percent of events is passed through. For example,
 * if window size is 1000 and pct = 10 then then there can exist a subsequence (a burst) in which 100 events can be allowed
 * to pass through the barrier one after the other. We call this number {@code maxBurst}. There is a simple relationship
 * between {@code maxBurst} and window size given pct: {@code maxBurst} = (window size * pct) / 100.
 * <p>
 * We now describe definition used in this class. We provide one more parameter: {@code maxBurst} and maintain a property
 * what percent of events that are passed through the window is maintained for every subsequence of size equal to:
 * ({@code maxBurst} / pct) * 100.
 * <p>
 * The API consists of two methods: {@link #arrive()} which notifies barrier that event has arrived
 * and {@link #canPassThrough()} which returns {@code true} only specified percent of times according to definition
 * explained above.
 * <p>
 * This class is thread safe.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 */
public class BurstyBarrier
{

  /*
   * This implementation keeps in memory a circular buffer of slots where each slot in the buffer is reserved for one
   * approval to pass through event through the barrier and its value is equal to a total number of arrivals at the
   * time when decision to pass through the event was made: _passThroughHistory.
   * We also keep track of current number of arrivals so far: _arrivalsSoFar and index in circular buffer that points
   * to the oldest event that has been passed through: _oldestPassThroughIdx.
   *
   * The property that needs to be maintained is that percent of events that are passed through the barrier for
   * given window size must not exceed specified value. In order to maintain this property it is enough to make sure
   * that between oldest event that has been passed through _passThroughHistory[_oldestPassThroughIdx] and 'now' there
   * have been at least 'window size' number of arrivals:
   * _arrivalsSoFar - _passThroughHistory[_oldestPassThroughIdx] >= window size
   */

  /*
   * 2^43-1 is high value that guarantees good precision: adding 0.01 10000 times to it yields result that is only 4 away
   * from true value. Arrivals counter is reset after this value is reached in order to maintain numerical stability.
   * This number is high enough so that reset is unlikely to happen in practice, for example:
   * if arrive() happens every millisecond then reset will happen after 278.7 years.
   * For more info see "What Every Computer Scientist Should Know About Floating-Point Arithmetic":
   * https://docs.oracle.com/cd/E19957-01/806-3568/ncg_goldberg.html
   */
  static final double MAX_ARRIVALS_WITH_PRECISION = 0b111_1111111111_1111111111_1111111111_1111111111L;

  private final Object _lock = new Object();

  private final double _windowSize;
  private final double[] _passThroughHistory;
  private final int _maxBurst;

  private int _oldestPassThroughIdx = 0;
  private double _arrivalsSoFar;

  /**
   * Creates new barrier. See class level documentation for detailed explanation of parameters.
   * @param percent percent of events that are allowed to pass through the barrier
   * @param maxBurst every subsequence of size exactly equal to {@code maxBurst} is guaranteed to honor {@code percent}
   * parameter
   */
  public BurstyBarrier(double percent, int maxBurst)
  {
    if (percent <= 0 || percent >= 100)
    {
      throw new IllegalArgumentException(
          "percent parameter has to be within range: (0, 100), excluding 0 and 100, got: " + percent);
    }
    if (maxBurst <= 0)
    {
      throw new IllegalArgumentException("maxBurst parameter has to be a positive number, got: " + maxBurst);
    }
    _maxBurst = maxBurst;
    _passThroughHistory = new double[maxBurst];
    _windowSize = (maxBurst * 100d) / percent;
    reset();
  }

  /**
   * Notifies the barrier that event has arrived. See class level documentation for detailed explanation of this method.
   */
  public void arrive()
  {
    synchronized (_lock)
    {
      _arrivalsSoFar++;
      if (_arrivalsSoFar > MAX_ARRIVALS_WITH_PRECISION)
      {
        reset();
      }
    }
  }

  private void reset()
  {
    _arrivalsSoFar = _windowSize;
    //clear out history
    for (int i = 0; i < _maxBurst; i++)
    {
      _passThroughHistory[i] = 0;
    }
  }

  /**
   * This method is called when  event attempts to pass through the barrier. It returns {@code true} if event is
   * allowed to pass through and {@code false} otherwise. Overall the barrier will return true for specified percent
   * of all events that attempt to pass through. See class level documentation for detailed explanation of this method.
   * @return {@code true} if event is allowed to pass through and {@code false} otherwise
   */
  public boolean canPassThrough()
  {
    synchronized (_lock)
    {
      double nextAllowedToPass = _passThroughHistory[_oldestPassThroughIdx] + _windowSize;

      if (_arrivalsSoFar >= nextAllowedToPass)
      {

        _passThroughHistory[_oldestPassThroughIdx] = Math.max(nextAllowedToPass, _arrivalsSoFar - 1);

        _oldestPassThroughIdx += 1;
        if (_oldestPassThroughIdx == _maxBurst)
        {
          _oldestPassThroughIdx = 0;
        }

        return true;
      } else
      {
        return false;
      }
    }
  }

}
