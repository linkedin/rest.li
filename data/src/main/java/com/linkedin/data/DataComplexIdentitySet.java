/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.data;

import java.util.IdentityHashMap;


/**
 * A space and time efficient {@link IdentityHashMap} equivalent optimized for checking {@link DataComplex} by
 * identity. We don't use {@link IdentityHashMap} as-is since {@link System#identityHashCode(Object)} performs
 * significantly worse than {@link DataComplex#dataComplexHashCode()} under multi-threaded conditions.
 *
 * <p>Instances of this class are typically used for cycle detection.</p>
 */
class DataComplexIdentitySet
{
  private static final int CAPACITY = 16;

  private final Node[] _table;

  /**
   * Constructor.
   */
  DataComplexIdentitySet()
  {
    _table = new Node[CAPACITY];
  }

  /**
   * Add the given {@link DataComplex} to the set.
   *
   * @param dataComplex The {@link DataComplex} to add.
   *
   * @return True if the {@link DataComplex} was already present and not added, false otherwise.
   */
  boolean add(DataComplex dataComplex)
  {
    final int index = dataComplex.dataComplexHashCode() & (CAPACITY - 1);
    Node node = _table[index];

    // No entries in bucket, add and return false.
    if (node == null)
    {
      _table[index] = new Node(dataComplex);
      return false;
    }

    Node previous = null;
    while (node != null)
    {
      if (node._dataComplex == dataComplex)
      {
        // Entry found, return true and bail!
        return true;
      }

      previous = node;
      node = node._next;
    }

    // No entry found for given data complex. Add it to the end, and return false.
    previous._next = new Node(dataComplex);
    return false;
  }

  /**
   * Removes the given {@link DataComplex} from the set if it exists.
   *
   * @param dataComplex The {@link DataComplex} to remove.
   */
  void remove(DataComplex dataComplex)
  {
    final int index = dataComplex.dataComplexHashCode() & (CAPACITY - 1);
    Node node = _table[index];

    // If there is no node at the given index there is nothing to do.
    if (node == null)
    {
      return;
    }

    Node previous = null;
    while (node != null)
    {
      if (node._dataComplex == dataComplex)
      {
        // Entry found, remove it.
        if (previous == null)
        {
          // This is the first node, set table index to point to the next node.
          _table[index] = node._next;
        }
        else
        {
          // Make the previous node point to the next node, cutting this node out.
          previous._next = node._next;
        }

        return;
      }

      previous = node;
      node = node._next;
    }
  }

  private static class Node
  {
    private final DataComplex _dataComplex;
    private Node _next;

    public Node(DataComplex dataComplex)
    {
      _dataComplex = dataComplex;
    }
  }
}
