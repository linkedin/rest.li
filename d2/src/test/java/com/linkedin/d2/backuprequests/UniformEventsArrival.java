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

import java.util.concurrent.TimeUnit;


public class UniformEventsArrival implements EventsArrival
{
  private final double _nanosToNextEvent;

  public UniformEventsArrival(double events, TimeUnit perUnit)
  {
    _nanosToNextEvent = perUnit.toNanos(1) / events;
  }

  @Override
  public long nanosToNextEvent()
  {
    return (long) _nanosToNextEvent;
  }

  @Override
  public String toString()
  {
    return "UniformEventsArrival [nanosToNextEvent=" + _nanosToNextEvent + "]";
  }
}
