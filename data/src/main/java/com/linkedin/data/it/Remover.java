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

package com.linkedin.data.it;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import java.util.ArrayList;


/**
 * Removes Data objects returned by a {@link DataIterator}.
 */
public class Remover
{
  private static class ToRemove
  {
    private ToRemove(DataElement element)
    {
      DataElement parentElement = element.getParent();
      _parent = parentElement == null ? null : parentElement.getValue();
      _name = element.getName();
    }

    private boolean isRoot()
    {
      return _parent == null;
    }

    /**
     * Perform actual removal.
     *
     * The Data object to remove must not be the root object.
     */
    private void remove()
    {
      Class<?> nameClass = _name.getClass();
      Class<?> parentClass = _parent.getClass();
      if (nameClass == String.class)
      {
        assert(parentClass == DataMap.class);
        DataMap map = (DataMap) _parent;
        map.remove(_name);

      }
      else if (nameClass == Integer.class)
      {
        int index = (Integer) _name;
        assert(parentClass == DataList.class);
        DataList list = (DataList) _parent;
        list.remove(index);
      }
      else
      {
        // should never happen
        throw new IllegalStateException("DataElement's name is not a String or Integer");
      }
    }

    private Object _parent;
    private Object _name;
  }

  /**
   * Removes the Data objects returned by the {@link DataIterator}.
   * This method mutates the Data object and it's descendants.
   *
   * @param root provides the root containing the Data objects that will be removed.
   * @param it provides the iterator of Data objects to be removed.
   * @return null if the input Data object is removed, else return the input root.
   */
  public static Object remove(Object root, DataIterator it)
  {
    DataElement element;

    // construct the list of Data objects to remove
    // don't remove in place because iterator behavior with removals while iterating is undefined
    ArrayList<ToRemove> removeList = new ArrayList<ToRemove>();
    while ((element = it.next()) != null)
    {
      ToRemove toRemove = new ToRemove(element);
      removeList.add(toRemove);
    }

    // perform actual removal in reverse order to make sure deleting array elements starts with higher indices
    for (int i = removeList.size() - 1; i >= 0; i--)
    {
      ToRemove toRemove = removeList.get(i);
      if (toRemove.isRoot())
      {
        root = null;
      }
      else
      {
        toRemove.remove();
      }
    }
    return root;
  }

}
