/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.internal.common.util;


public class CollectionUtils
{
  /**
   * Returns a capacity value that can be used to construct a {@link java.util.HashMap}
   * or a {@link java.util.concurrent.ConcurrentHashMap} to prevent resizing of the map.
   * @param numberOfItems the number of items which will be put into the map
   * @param loadFactor the load factor the map will be created with
   * @return capacity value
   */
  public static int getMapInitialCapacity(int numberOfItems, float loadFactor)
  {
    return (int)(numberOfItems / loadFactor) + 1;
  }
}
