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

package com.linkedin.data.collections;

public class CommonUtil
{
  // Static singleton, so don't allow instances / subclasses.
  private CommonUtil()
  {
  }

  /**
   * Convenience method for copying a {@link Common} assuming that the object is {@link Cloneable}.
   * If the object is not cloneable then a RuntimeException, which contains the underlying
   * {@link CloneNotSupportedException}, will be thrown.
   *
   * @param obj the object to clone
   * @return a copy of the given object
   */
  public static <T extends Common> T unsafeClone(final T obj)
  {
    try
    {
      @SuppressWarnings("unchecked")
      final T clone = (T)obj.clone();
      return clone;
    }
    catch (CloneNotSupportedException e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * Convenience method to make an immutable copy of a {@link Common}. Changes to the original
   * object are not reflected in the cloned object and vice-versa. The returned object is set to
   * read-only.
   *
   * @param obj the object to clone
   * @return a copy of the given object that has also been set to read-only
   */
  public static <T extends Common> T unsafeCloneSetReadOnly(final T obj)
  {
    final T clone = unsafeClone(obj);
    clone.setReadOnly();
    return clone;
  }
}
