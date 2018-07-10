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

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Data structure that expresses adjacency in the relationship {@link Graph}.
 *
 * @author dellamag
 */
public class Node<T>
{
  /**
   * @param object object whose adjacency is to be stored in this Node
   */
  public Node(T object)
  {
    _object = object;
  }

  /**
   * @param node adjacent Node of the current Node
   */
  public synchronized void addAdjacentNode(Node<?> node)
  {
    _neighbors.add(node);
  }

  /**
   * @return iterable adjacent Nodes of the current Node
   */
  public Iterable<Node<?>> getAdjacency()
  {
    return _neighbors;
  }

  /**
   * @param type restrict the returned adjacent Nodes to this type
   * @param <O> restriction type
   * @return iterable adjacent Nodes of the current Node with the specified type
   */
  public <O> Iterable<Node<O>> getAdjacency(final Class<O> type)
  {
    return new Iterable<Node<O>>()
    {
      @Override
      public Iterator<Node<O>> iterator()
      {
        return new Iterator<Node<O>>()
        {
          private Iterator<Node<?>> _itr = _neighbors.iterator();
          private Node<O> _next;

          @SuppressWarnings("unchecked")
          @Override
          public boolean hasNext()
          {
            if (_next != null)
            {
              return true;
            }
            else
            {
              while (_itr.hasNext() && _next == null)
              {
                final Node<?> next = _itr.next();
                if (type.isAssignableFrom(next.getObject().getClass()))
                {
                  _next = (Node<O>)next;
                }
              }
            }
            return _next != null;
          }

          @Override
          public Node<O> next()
          {
            if (hasNext())
            {
              final Node<O> next = _next;
              _next = null;
              return next;
            }
            else
            {
              throw new NoSuchElementException();
            }
          }

          @Override
          public void remove()
          {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  /**
   * @return subject object of the current Node
   */
  public Object getObject()
  {
    return _object;
  }

  @Override
  public int hashCode()
  {
    return _object.hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof Node)
    {
      return obj.equals(_object);
    }
    return false;
  }

  private final T _object;
  private final Set<Node<?>> _neighbors = new HashSet<Node<?>>();
}
