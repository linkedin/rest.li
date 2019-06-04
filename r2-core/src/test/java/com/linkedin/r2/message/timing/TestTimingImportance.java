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

package com.linkedin.r2.message.timing;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link TimingImportance}.
 */
public class TestTimingImportance
{
  @Test
  public void testTimingImportanceIsAtLeast()
  {
    Assert.assertTrue(TimingImportance.HIGH.isAtLeast(TimingImportance.HIGH));
    Assert.assertTrue(TimingImportance.HIGH.isAtLeast(TimingImportance.MEDIUM));
    Assert.assertTrue(TimingImportance.MEDIUM.isAtLeast(TimingImportance.LOW));
  }
}
