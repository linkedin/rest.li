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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


public class GaussianResponseTimeDistribution implements ResponseTimeDistribution
{
  private final double _average;
  private final double _stdDev;
  private final double _min;
  private final long _max;

  public GaussianResponseTimeDistribution(long min, long average, long stdDev, TimeUnit unit)
  {
    this(min, average, stdDev, Long.MAX_VALUE, unit);
  }

  public GaussianResponseTimeDistribution(long min, long average, long stdDev, long max, TimeUnit unit)
  {
    _average = unit.toNanos(average);
    _stdDev = unit.toNanos(stdDev);
    _min = unit.toNanos(min);
    _max = unit.toNanos(max);
  }

  @Override
  public long responseTimeNanos()
  {
    double rnd = _average + ThreadLocalRandom.current().nextGaussian() * _stdDev;
    return (long) Math.min(Math.max(rnd, _min), _max);
  }

}
