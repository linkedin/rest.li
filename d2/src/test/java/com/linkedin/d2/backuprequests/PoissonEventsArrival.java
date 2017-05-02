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


public class PoissonEventsArrival implements EventsArrival
{

  private final double _nanosToNextEventMean;

  public PoissonEventsArrival(double events, TimeUnit perUnit)
  {
    if (events <= 0)
    {
      throw new IllegalArgumentException("events must be a positive number");
    }
    _nanosToNextEventMean = perUnit.toNanos(1) / events;
  }

  @Override
  public long nanosToNextEvent()
  {
    //rand is uniformly distributed form 0.0d inclusive up to 1.0d exclusive
    double rand = ThreadLocalRandom.current().nextDouble();
    return (long) (-_nanosToNextEventMean * Math.log(1 - rand));
  }

  @Override
  public String toString()
  {
    return "PoissonEventsArrival [nanosToNextEventMean=" + _nanosToNextEventMean + "]";
  }

}
