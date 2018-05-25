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

/* $Id$ */
package com.linkedin.util;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class ArgumentUtil
{
  /**
   * Verifies that the given argument is not {@code null}. If it is {@code null} then a
   * {@link NullPointerException} is raised.
   *
   * @param obj the object to test for null
   * @param name the name of the argument that is being tested (used if an NPE is thrown).
   * @throws NullPointerException if {@code obj == null}
   */
  public static void notNull(Object obj, String name)
  {
    if (obj == null)
    {
      throw new NullPointerException(name + " is null");
    }
  }

  /**
   * Verifies that {@code str != null && str.isEmpty() == false}. If either condition does not
   * hold an exception is thrown.
   *
   * @param name the name of the argument that is being tested (used if an exception is thrown).
   * @throws NullPointerException if str is null
   * @throws IllegalArgumentException is str is empty
   */
  public static void notEmpty(String str, String name)
  {
    notNull(str, name);
    if (str.isEmpty())
    {
      throw new IllegalArgumentException(name + " is an empty string");
    }
  }

  /**
   * Verifies that the given argument is not {@code null}. If it is {@code null}, then a
   * {@link NullPointerException} is raised.
   *
   * @param obj the object to test for null
   * @param name the name of the argument that is being tested (used if an NPE is thrown).
   * @return obj if obj is not null
   * @throws NullPointerException if {@code obj == null}
   */
  public static <T> T ensureNotNull(T obj, String name)
  {
    notNull(obj, name);
    return obj;
  }

  /**
   * Checks the length of an array based on an offset and the actual array length. An
   * {@link ArrayIndexOutOfBoundsException} is thrown if {@code offset + length > arrayLength}.
   * @param arrayLength Actual length of the array
   * @param offset Starting offset of the array
   * @param length Required length starting from the offset
   */
  public static void checkBounds(int arrayLength, int offset, int length)
  {
    if (offset < 0)
    {
      throw new IndexOutOfBoundsException("offset cannot be negative: " + offset);
    }

    if (length < 0)
    {
      throw new IndexOutOfBoundsException("length cannot be negative: " + length);
    }

    if (offset + length > arrayLength)
    {
      throw new IndexOutOfBoundsException("index out of bound: " + (offset + length));
    }
  }

  /**
   * Checks the validity of an argument based on an expression. An {@link IllegalArgumentException}
   * is thrown if {@code expression == false}.
   * @param expression Expression to evaluate
   * @param name Name of the argument
   */
  public static void checkArgument(boolean expression, String name)
  {
    if (!expression)
    {
      throw new IllegalArgumentException("Argument " + name + " value is invalid");
    }
  }
}
