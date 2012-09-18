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
import com.linkedin.data.transforms.Transform;
import com.linkedin.data.transforms.Transforms;
import java.util.ArrayList;

/**
 * Transforms Data objects returned by a {@link DataIterator}.
 * 
 * @author "Joe Betz<jbetz@linkedin.com>"
 */
public class Transformer
{
  private static class ToTransform
  {
    private ToTransform(DataElement element)
    {
      _value = element.getValue();
      DataElement parentElement = element.getParent();
      _parent = parentElement == null ? null : parentElement.getValue();
      _name = element.getName();
    }

    private boolean isRoot()
    {
      return _parent == null;
    }

    /**
     * Perform actual transform.
     *
     * The Data object to transform must not be the root object.
     */
    private void transform(Transform<Object, Object> transform)
    {
      Object replacementValue = transform.apply(_value);
      
      Class<?> nameClass = _name.getClass();
      Class<?> parentClass = _parent.getClass();
      if (nameClass == String.class)
      {
        assert(parentClass == DataMap.class);
        DataMap map = (DataMap) _parent;
        String key = (String)_name;
        map.put(key, replacementValue);
      }
      else if (nameClass == Integer.class)
      {
        int index = (Integer) _name;
        assert(parentClass == DataList.class);
        DataList list = (DataList) _parent;
        list.set(index, replacementValue);
      }
      else
      {
        // should never happen
        throw new IllegalStateException("DataElement's name is not a String or Integer");
      }
    }

    private final Object _value;
    private final Object _parent;
    private final Object _name;
  }
  /**
   * Transforms the Data objects returned by the {@link DataIterator}.
   * This method mutates the Data object and it's descendants.
   * 
   * @param root provides the root of the Data objects that will be transformed.
   * @param it provides the iterator of Data objects to be transformed.
   * @param transform used to provide a replacement value.
   * @return the transformed of root Data object if it was transformed, otherwise the input root with the transformations applied.
   */
  public static Object transform(Object root, DataIterator it, Transform<Object,Object> transform)
  {
    DataElement element;
    
    // don't transform in place because iterator behavior with replacements (which behave like a remove and an add) while iterating is undefined
    ArrayList<ToTransform> transformList = new ArrayList<ToTransform>();
    while ((element = it.next()) != null)
    {
      transformList.add(new ToTransform(element));
    }

    for (ToTransform toTransform : transformList)
    {
      if (toTransform.isRoot())
      {
        root = transform.apply(toTransform._value);
      }
      else
      {
        toTransform.transform(transform);
      }
    }
    return root;
  }
  
  /**
   * Replaces the Data objects returned by the {@link DataIterator}.
   * This method mutates the Data object and it's descendants.
   * 
   * @param root provides the root containing the Data objects that will be transformed.
   * @param it provides the iterator of Data objects to be transformed.
   * @param value provides the replacement value.
   * @return the replacement of root object if it was replaced, otherwise the input root with the replacements applied.
   */
  public static Object replace(Object root, DataIterator it, Object value)
  {
    return transform(root, it, Transforms.constantValue(value));
  }
}
