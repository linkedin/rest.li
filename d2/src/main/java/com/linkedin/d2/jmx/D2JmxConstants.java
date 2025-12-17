/*
   Copyright (c) 2025 LinkedIn Corp.

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

package com.linkedin.d2.jmx;

/**
 * Common constants used across D2 JMX and metrics components.
 */
public final class D2JmxConstants
{
  /**
   * Default client name used when a specific client name is not set.
   * Used for identifying metrics associated with unnamed clients.
   */
  public static final String NO_VALUE = "-";

  private D2JmxConstants()
  {
    // Utility class, prevent instantiation
  }
}
