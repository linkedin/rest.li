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
 * $Id: ImmutableLongStats.java 151859 2010-11-19 21:43:47Z slim $
 */
package com.linkedin.common.stats;

/**
 * @author Swee Lim
 * @version $Rev: 151859 $
 */

/**
 * Immutable LongStats.
 */
public class LongStats
{
  private final int    _count;
  private final double _average;
  private final double _standardDeviation;
  private final long   _minimum;
  private final long   _maximum;
  private final long   _50Pct;
  private final long   _90Pct;
  private final long   _95Pct;
  private final long   _99Pct;

  public LongStats()
  {
    _count = 0;
    _average = 0;
    _standardDeviation = 0;
    _minimum = 0;
    _maximum = 0;
    _50Pct = 0;
    _90Pct = 0;
    _95Pct = 0;
    _99Pct = 0;
  }

  public LongStats(final LongStats stats)
  {
    _count = stats.getCount();
    _average = stats.getAverage();
    _standardDeviation = stats.getStandardDeviation();
    _minimum = stats.getMinimum();
    _maximum = stats.getMaximum();

    _50Pct = stats.get50Pct();
    _90Pct = stats.get90Pct();
    _95Pct = stats.get95Pct();
    _99Pct = stats.get99Pct();
  }

  public LongStats(final int count,
                   final double average,
                   final double standardDeviation,
                   final long minimum,
                   final long maximum,
                   final long pct50,
                   final long pct90,
                   final long pct95,
                   final long pct99)
  {
    _count = count;
    _average = average;
    _standardDeviation = standardDeviation;
    _minimum = minimum;
    _maximum = maximum;

    _50Pct = pct50;
    _90Pct = pct90;
    _95Pct = pct95;
    _99Pct = pct99;
  }

  public final int getCount()
  {
    return _count;
  }

  public final double getAverage()
  {
    return _average;
  }

  public final double getStandardDeviation()
  {
    return _standardDeviation;
  }

  public final long getMinimum()
  {
    return _minimum;
  }

  public final long getMaximum()
  {
    return _maximum;
  }

  public final long get50Pct()
  {
    return _50Pct;
  }

  public final long get90Pct()
  {
    return _90Pct;
  }

  public final long get95Pct()
  {
    return _95Pct;
  }

  public final long get99Pct()
  {
    return _99Pct;
  }
}

