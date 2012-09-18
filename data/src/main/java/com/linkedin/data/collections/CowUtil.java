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
package com.linkedin.data.collections;


/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class CowUtil
{
  private static final CowMap EMPTY_MAP;
  private static final CowSet EMPTY_SET;
  private static final CowList EMPTY_LIST;

  static
  {
    EMPTY_MAP = new CowMap();
    EMPTY_MAP.setReadOnly();

    EMPTY_SET = new CowSet();
    EMPTY_SET.setReadOnly();

    EMPTY_LIST = new CowList();
    EMPTY_LIST.setReadOnly();
  }

  // Static singleton, so don't allow instances / subclasses.
  private CowUtil()
  {
  }

  /**
   * Convenience method for getting an immutable, empty {@link CowMap}.
   */
  @SuppressWarnings("unchecked")
  public static <K, V> CowMap<K, V> emptyMap()
  {
    return EMPTY_MAP;
  }

  /**
   * Convenience method for getting an immutable, empty {@link CowSet}.
   */
  @SuppressWarnings("unchecked")
  public static <K> CowSet<K> emptySet()
  {
    return EMPTY_SET;
  }

  /**
   * Convenience method for getting an immutable, empty {@link CowList}.
   */
  @SuppressWarnings("unchecked")
  public static <V> CowList<V> emptyList()
  {
    return EMPTY_LIST;
  }
}
