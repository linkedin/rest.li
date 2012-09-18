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
 * $Id: SettableClock.java 76085 2009-05-20 19:42:12Z dmccutch $ */
package com.linkedin.util.clock;

/**
 * @author Yan Pujante
 * @version $Revision: 76085 $
 */
public class SettableClock implements Clock
{
  private long _currentTimeMillis;

  /**
   * Constructor
   */
  public SettableClock()
  {
    this(System.currentTimeMillis());
  }

  /**
   * Constructor
   */
  public SettableClock(long currentTimeMillis)
  {
    setCurrentTimeMillis(currentTimeMillis);
  }

  /**
   * @return the current time of this clock in milliseconds.
   */
  public long currentTimeMillis()
  {
    return _currentTimeMillis;
  }

  public void setCurrentTimeMillis(long currentTimeMillis)
  {
    _currentTimeMillis = currentTimeMillis;
  }

  public void addDuration(long millis)
  {
    _currentTimeMillis += millis;
  }

  public void subtractDuration(long millis)
  {
    _currentTimeMillis -= millis;
  }
}
