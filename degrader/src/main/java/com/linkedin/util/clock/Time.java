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

package com.linkedin.util.clock;

public class Time
{
  public static final long MS_PER_SECOND = 1000;
  public static final long MS_PER_MINUTE = 60 * MS_PER_SECOND;
  public static final long MS_PER_HOUR = 60 * MS_PER_MINUTE;
  public static final long MS_PER_DAY = 24 * MS_PER_HOUR;

  public static final long milliseconds(long millis)
  {
    return millis;
  }

  public static final long seconds(long seconds)
  {
    return seconds * MS_PER_SECOND;
  }

  public static final long minutes(long minutes)
  {
    return minutes * MS_PER_MINUTE;
  }

  public static final long hours(long minutes)
  {
    return minutes * MS_PER_HOUR;
  }

  public static final long days(long days)
  {
    return days * MS_PER_DAY;
  }
}
