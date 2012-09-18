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

public interface Common extends Cloneable
{
  /**
   * Shallow clone.
   *
   * The contained values are not recursively cloned.
   * The clone will be read-write enabled.
   *
   * @return the clone.
   */
  public Object clone() throws CloneNotSupportedException;

  /**
   * Make the instance read-only.
   *
   * The instance can be marked read-only to disallow mutation,
   * it can also be used avoid copy-on-write by disallowing writes.
   */
  public void setReadOnly();

  /**
   * Return whether instance is read-only.
   *
   * @return whether instance is read-only.
   */
  public boolean isReadOnly();

  /**
   * Invalidate this instance by releasing reference to underlying instance.
   * After invalidate returns, calling methods of this instance will result in undefined behavior.
   */
  public void invalidate();
}
