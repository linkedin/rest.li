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

package com.linkedin.data.transform;


import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.linkedin.data.DataMap;


public class TestScheduler
{
  private static final Instruction a = new Instruction("a", new DataMap(), ImmutableList.empty());
  private static final Instruction b = new Instruction("b", new DataMap(), ImmutableList.empty());
  private static final Instruction c = new Instruction("c", new DataMap(), ImmutableList.empty());
  private static final Instruction d = new Instruction("d", new DataMap(), ImmutableList.empty());
  private static final Instruction e = new Instruction("e", new DataMap(), ImmutableList.empty());

  @Test
  public void testFiloScheduler()
  {
    FILOScheduler scheduler = new FILOScheduler();
    scheduler.scheduleInstructions(Arrays.asList(a, b));
    assertTrue(scheduler.hasNext());
    assertSame(scheduler.next(), b);
    assertTrue(scheduler.hasNext());
    assertSame(scheduler.next(), a);
    scheduler.scheduleInstructions(Arrays.asList(a, b));
    scheduler.scheduleInstructions(Arrays.asList(c));
    scheduler.scheduleInstructions(Arrays.asList(d, e));
    assertTrue(scheduler.hasNext());
    assertSame(scheduler.next(), e);
    assertTrue(scheduler.hasNext());
    assertSame(scheduler.next(), d);
    assertTrue(scheduler.hasNext());
    assertSame(scheduler.next(), c);
    assertTrue(scheduler.hasNext());
    assertSame(scheduler.next(), b);
    assertTrue(scheduler.hasNext());
    assertSame(scheduler.next(), a);
  }
}
