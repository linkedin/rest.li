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

package com.linkedin.restli.docgen;

import java.util.HashMap;
import java.util.Map;

/**
 * Directed graph for capturing relationships between resource endpoints and data models.
 *
 * @author dellamag
 */
public class Graph
{
  /**
   * Retrieve the corresponding {@link Node} for the specified object.
   *
   * @param o the target object
   * @param <T> type of the target object
   * @return {@link Node} for the specified object
   */
  @SuppressWarnings("unchecked")
  public <T> Node<T> get(T o)
  {
    Node<T> node = (Node<T>) _nodes.get(o);
    if (node == null)
    {
      node = new Node<>(o);
      _nodes.put(o, node);
    }
    return node;
  }

  private final Map<Object, Node<?>> _nodes = new HashMap<>();
}
