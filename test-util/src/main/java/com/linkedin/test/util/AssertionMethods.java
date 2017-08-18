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

package com.linkedin.test.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AssertionMethods
{
  public static void assertWithinRange(String msg, long min, long max, long value)
  {
    if (value < min)
    {
      throw new AssertionError(nonNullMessage(msg) + "actual value of <" + value
        + "> is below the minimum of <" + min + ">");
    }
    if (value > max)
    {
      throw new AssertionError(nonNullMessage(msg) + "actual value of <" + value
        + "> is above the maximum of <" + max + ">");
    }
  }

  public static void assertWithTimeout(long timeout, RetryableAssertion assertion) throws Exception
  {
    long sleepingTime = 20;
    long end = System.currentTimeMillis() + timeout;

    while (true)
    {
      try
      {
        assertion.doAssertion();
        return;
      }
      catch (AssertionError e)
      {
        if (end > System.currentTimeMillis())
        {
          try
          {
            Thread.sleep(sleepingTime);
            sleepingTime *= 2;
          }
          catch (InterruptedException e1)
          {
            throw new RuntimeException(e1);
          }
        }
        else
        {
          throw e;
        }
      }
    }
  }

  public static void assertByteArrayEquals(String msg, byte[] a1, byte[] a2)
  {
    if (a1 == null)
    {
      assertNull("a1 is null but a2 is not null: " + msg, a2);
      return;
    }
    else
    {
      assertNotNull("a1 is not null but a2 is null: " + msg, a2);
    }

    assertEquals("array length mismatch: " + msg, a1.length, a2.length);

    for (int i = 0; i < a1.length; i++)
    {
      assertEquals("mismatch at index: " + i + ": " + msg, a1[i], a2[i]);
    }
  }

  public static void assertIntArrayEquals(int[] a1, int[] a2)
  {
    assertIntArrayEquals("", a1, a2);
  }

  public static void assertIntArrayEquals(String msg, int[] a1, int[] a2)
  {
    if (a1 == null)
    {
      assertNull(msg, a2);
      return;
    }
    else
      assertNotNull(msg, a2);

    assertEquals(msg, a1.length, a2.length);

    for (int i = 0; i < a1.length; i++)
    {
      assertEquals(msg, a1[i], a2[i]);
    }
  }

  public static void assertObjectArrayEquals(Object[] a1, Object[] a2)
  {
    assertObjectArrayEquals("", a1, a2);
  }

  public static void assertObjectArrayEquals(String msg, Object[] a1, Object[] a2)
  {
    if (a1 == null)
    {
      assertNull(msg, a2);
      return;
    }
    else
      assertNotNull(msg, a2);

    assertEquals(msg, a1.length, a2.length);

    for (int i = 0; i < a1.length; ++i)
      assertEquals(msg, a1[i], a2[i]);
  }

  public static void assertContains(String message, String source, String substring)
  {
    String nonNullMessage = message == null ? "" : message;
    assertNotNull(nonNullMessage, source);
    if (!source.contains(substring))
    {
      throw new AssertionError("\"" + source + "\" "
        + "does not contain the expected substring: \"" + substring + "\"\n"
        + nonNullMessage);
    }
  }

  public static void assertContains(String source, String substring)
  {
    assertContains("", source, substring);
  }

  private static String nonNullMessage(String msg)
  {
    return msg == null ? "" : msg + " ";
  }

  private static void assertNull(String message, Object a)
  {
    if (null != a)
    {
      throw new AssertionError(nonNullMessage(message));
    }
  }

  private static void assertNotNull(String message, Object a)
  {
    if (null == a)
    {
      throw new AssertionError(nonNullMessage(message));
    }
  }

  private static void assertEquals(String message, int a1, int a2)
  {
    if (a1 != a2)
    {
      throw new AssertionError(nonNullMessage(message));
    }
  }

  private static void assertEquals(String message, byte a1, byte a2)
  {
    if (a1 != a2)
    {
      throw new AssertionError(nonNullMessage(message));
    }
  }

  private static void assertEquals(String message, Object a1, Object a2)
  {
    if (a1 == null && a2 == null)
    {
      return;
    }

    if (a1 != null && a1.equals(a2))
    {
      return;
    }

    throw new AssertionError(nonNullMessage(message));
  }

  public static void assertEqualsNoOrder(String message, Collection<?> a, Collection<?> b)
  {
    if (a != null && b != null)
    {
      if (!collectionToOccurenceMap(a).equals(collectionToOccurenceMap(b)))
      {
        throw new AssertionError("Collection " + a + " differs without respect to order from " + b + ": " + message);
      }
    }
    else if (a != b)
    {
      throw new AssertionError("Collection " + a + " differs without respect to order from " + b + ": " + message);
    }
  }

  public static <T> Map<T, Integer> collectionToOccurenceMap(Collection<T> collection)
  {
    Map<T, Integer> map = new HashMap<>(collection.size());
    for (T t : collection)
    {
      Integer count = map.get(t);
      if (count == null)
      {
        count = 0;
      }
      map.put(t, count + 1);
    }
    return map;
  }


}
