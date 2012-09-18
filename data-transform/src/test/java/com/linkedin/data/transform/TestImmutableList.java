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

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class TestImmutableList
{

  @Test
  public void testEmpty()
  {
    assertEquals(ImmutableList.empty(), ImmutableList.empty());

    assertTrue(ImmutableList.empty() == ImmutableList.empty());

    assertEquals(ImmutableList.empty().size(), 0);

    assertEquals(ImmutableList.empty().toArray().length, 0);
  }

  @Test
  public void testAppend()
  {

    ImmutableList<String> empty = ImmutableList.empty();
    ImmutableList<String> a = empty.append("a");
    ImmutableList<String> ab = a.append("b");
    ImmutableList<String> abc = ab.append("c");

    assertEquals(a.size(), 1);
    assertEquals(ab.size(), 2);
    assertEquals(abc.size(), 3);

    assertEquals(a.toArray(), new Object[] { "a" });
    assertEquals(ab.toArray(), new Object[] { "a", "b" });
    assertEquals(abc.toArray(), new Object[] { "a", "b", "c" });
  }

  @Test
  public void testHashCodeAndEquals()
  {

    ImmutableList<String> empty = ImmutableList.empty();
    ImmutableList<String> a = empty.append("a");
    ImmutableList<String> ab = a.append("b");
    ImmutableList<String> abc = ab.append("c");

    ImmutableList<String> a2 = empty.append("a");
    ImmutableList<String> ab2 = a.append("b");
    ImmutableList<String> abc2 = ab.append("c");

    assertEquals(a, a2);
    assertEquals(ab, ab2);
    assertEquals(abc, abc2);

    assertEquals(a.hashCode(), a2.hashCode());
    assertEquals(ab.hashCode(), ab2.hashCode());
    assertEquals(abc.hashCode(), abc2.hashCode());

    assertFalse(a == a2);
    assertFalse(ab == ab2);
    assertFalse(abc == abc2);
  }

}
