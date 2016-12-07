/*
 Copyright 2015 Coursera Inc.

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

package com.linkedin.pegasus.generator.test.pdl;

import com.linkedin.pegasus.generator.test.idl.fixed.Fixed8;
import com.linkedin.pegasus.generator.test.idl.fixed.WithFixed8;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class FixedGeneratorTest extends GeneratorTest
{

  @Test
  public void testFixed()
      throws Throwable
  {
    WithFixed8 original = new WithFixed8();
    Fixed8 fixed8 = new Fixed8(SchemaFixtures.bytesFixed8);
    original.setFixed(fixed8);
    WithFixed8 roundTripped = new WithFixed8(roundTrip(original.data()));

    assertEquals(roundTripped.getFixed().bytes(), SchemaFixtures.bytesFixed8);
  }
}
