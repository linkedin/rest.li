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

package com.linkedin.test.util;

import java.util.Random;

/**
 * Generates random Gaussian random variables for testing purposes
 */
public class GaussianRandom
{
  private static final Random RANDOM = new Random();

  /**
   * Generate a random long that is centered at {@code delay} with the given {@code range}. The
   * result is guaranteed to be non-negative.
   *
   * @return random long
   */
  public static long delay(final double delay, final double range)
  {
    return (long) Math.abs((RANDOM.nextGaussian() * range) + delay);
  }
}
