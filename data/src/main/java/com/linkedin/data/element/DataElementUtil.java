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

package com.linkedin.data.element;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Utility functions.
 */
public class DataElementUtil
{
  /**
   * Get the {@link DataElement} by following the specified path starting from
   * the provided Data object and {@link DataSchema} as the root.
   *
   * The format path is path components separated by {@link DataElement#SEPARATOR}.
   *
   * @param value provides the value of the root Data object.
   * @param schema provides the {@link DataSchema} of the root Data object, may be null if unknown.
   * @param path provides the string representation of the path.
   * @return the {@link DataElement} if the path can be followed, else return null.
   * @throws IllegalArgumentException if the provided path's syntax is incorrect, Data object does not match provided
   *                                  {@link DataSchema}.
   */
  public static DataElement element(Object value, DataSchema schema, String path) throws IllegalArgumentException
  {
    return element(new SimpleDataElement(value, schema), path);
  }

  /**
   * Get the {@link DataElement} by following the specified path starting from
   * the provided Data object and {@link DataSchema} as the root.
   *
   * @param value provides the value of the root Data object.
   * @param schema provides the {@link DataSchema} of the root Data object, may be null if unknown.
   * @param path provides the string representation of the path.
   * @param separator provides the character used to separate path components in the path string.
   * @return the {@link DataElement} if the path can be followed, else return null.
   * @throws IllegalArgumentException if the provided path's syntax is incorrect, Data object does not match provided
   *                                  {@link DataSchema}.
   */
  public static DataElement element(Object value, DataSchema schema, String path, char separator) throws IllegalArgumentException
  {
    return element(new SimpleDataElement(value, schema), path, separator);
  }

  /**
   * Get the {@link DataElement} by following the specified path starting from
   * the provided Data object and {@link DataSchema} as the root.
   *
   * @param value provides the value of the root Data object.
   * @param schema provides the {@link DataSchema} of the root Data object, may be null if unknown.
   * @param path provides the path components through an array.
   * @return the {@link DataElement} if the path can be followed, else return null.
   * @throws IllegalArgumentException if the provided path's syntax is incorrect, Data object does not match provided
   *                                  {@link DataSchema}.
   */
  public static DataElement element(Object value, DataSchema schema, Object[] path) throws IllegalArgumentException
  {
    return element(new SimpleDataElement(value, schema), path);
  }

  /**
   * Get the {@link DataElement} by following the specified path starting from
   * the provided Data object and {@link DataSchema} as the root.
   *
   * @param value provides the value of the root Data object.
   * @param schema provides the {@link DataSchema} of the root Data object, may be null if unknown.
   * @param path provides the path components through an {@link Iterable}.
   * @return the {@link DataElement} if the path can be followed, else return null.
   * @throws IllegalArgumentException if the provided path's syntax is incorrect, Data object does not match provided
   *                                  {@link DataSchema}.
   */
  public static DataElement element(Object value, DataSchema schema, Iterable<Object> path) throws IllegalArgumentException
  {
    return element(new SimpleDataElement(value, schema), path);
  }

  /**
   * Get the {@link DataElement} by following the specified path starting from
   * the provided {@link DataElement}.
   *
   * The format path is path components separated by {@link DataElement#SEPARATOR}.
   *
   * @param element provides the {@link DataElement} that is the starting point.
   * @param path provides the string representation of the path.
   * @return the {@link DataElement} if the path can be followed, else return null.
   * @throws IllegalArgumentException if the provided path's syntax is incorrect, Data object does not match provided
   *                                  {@link DataSchema}.
   */
  public static DataElement element(DataElement element, String path) throws IllegalArgumentException
  {
    return element(element, pathToList(path, DataElement.SEPARATOR));
  }

  /**
   * Get the {@link DataElement} by following the specified path starting from
   * the provided {@link DataElement}.
   *
   * @param element provides the {@link DataElement} that is the starting point.
   * @param path provides the string representation of the path.
   * @param separator provides the character used to separate path components in the path string.
   * @return the {@link DataElement} if the path can be followed, else return null.
   * @throws IllegalArgumentException if the provided path's syntax is incorrect, Data object does not match provided
   *                                  {@link DataSchema}.
   */
  public static DataElement element(DataElement element, String path, char separator) throws IllegalArgumentException
  {
    return element(element, pathToList(path, separator));
  }

  /**
   * Get the {@link DataElement} by following the specified path starting from
   * the provided {@link DataElement}.
   *
   * @param element provides the {@link DataElement} that is the starting point.
   * @param path provides the path components through an array.
   * @return the {@link DataElement} if the path can be followed, else return null.
   * @throws IllegalArgumentException if the provided path's syntax is incorrect, Data object does not match provided
   *                                  {@link DataSchema}.
   */
  public static DataElement element(DataElement element, Object[] path) throws IllegalArgumentException
  {
    return element(element, Arrays.asList(path));
  }

  /**
   * Get the {@link DataElement} by following the specified path starting from
   * the provided {@link DataElement}.
   *
   * @param element provides the {@link DataElement} that is the starting point.
   * @param path provides the path components through an {@link Iterable}.
   * @return the {@link DataElement} if the path can be followed, else return null.
   * @throws IllegalArgumentException if the provided path's syntax is incorrect, Data object does not match provided
   *                                  {@link DataSchema}.
   */
  public static DataElement element(DataElement element, Iterable<Object> path) throws IllegalArgumentException
  {
    DataElement currentElement = element;
    for (Object component : path)
    {
      Object name;
      if (currentElement.getValue().getClass() == DataMap.class && component.getClass() != String.class)
      {
        name = component.toString();
      }
      else if (currentElement.getValue().getClass() == DataList.class && component.getClass() != Integer.class)
      {
        try
        {
          name = Integer.parseInt(component.toString());
        }
        catch (NumberFormatException e)
        {
          return null;
        }
      }
      else
      {
        name = component;
      }

      Object childValue = currentElement.getChild(name);
      if (childValue == null)
      {
        return null;
      }
      DataSchema childSchema = null;
      DataSchema schema = currentElement.getSchema();
      if (schema != null)
      {
        switch (schema.getType())
        {
          case ARRAY:
            childSchema = ((ArrayDataSchema) schema).getItems();
            break;
          case MAP:
            childSchema = ((MapDataSchema) schema).getValues();
            break;
          case UNION:
            childSchema = ((UnionDataSchema) schema).getType((String) name);
            break;
          case RECORD:
            RecordDataSchema.Field field = ((RecordDataSchema) schema).getField((String) name);
            if (field != null)
            {
              childSchema = field.getType();
            }
            break;
          default:
            throw new IllegalArgumentException(currentElement.pathAsString() + " has schema of type " + schema.getType() + " which cannot have child, but has child with name \"" + name + "\"");
        }
      }
      currentElement = new SimpleDataElement(childValue, name, childSchema, currentElement);
    }
    return currentElement;
  }

  /* package scope */
  static List<Object> pathToList(String path, char separator) throws IllegalArgumentException
  {
    if (path.isEmpty())
    {
      return Collections.emptyList();
    }

    List<Object> list = new ArrayList<Object>(path.length() / 4);
    int len = path.length();
    int index = 0;
    if (path.charAt(index) != separator)
    {
      throw new IllegalArgumentException("\"" + separator + "\" expected at index " + index + " of \"" + path + "\"");
    }
    while (index < len)
    {
      int start = index;
      int end = start + 1;
      for (; end < len && path.charAt(end) != separator; end++)
      {
      }
      if ((end - start) == 1)
      {
        throw new IllegalArgumentException("Path component starting at index " + index + " of \"" + path + "\" is empty");
      }
      String component = path.substring(start + 1, end);
      list.add(component);
      index = end;
    }
    return list;
  }

  /**
   * Similar to {@link DataElement#pathAsString}, but without map keys or array indices.
   *
   * @param dataElement data element
   * @return path without keys as a string
   */
  public static String pathWithoutKeysAsString(DataElement dataElement)
  {
    DataElement element = dataElement;
    StringBuilder builder = new StringBuilder();
    while (element.getParent() != null)
    {
      DataSchema.Type parentType = element.getParent().getSchema().getType();
      if (parentType == DataSchema.Type.ARRAY || parentType == DataSchema.Type.MAP)
      {
        element = element.getParent();
        continue;
      }
      builder = new StringBuilder().append(DataElement.SEPARATOR).append(element.getName()).append(builder);
      element = element.getParent();
    }
    return builder.toString();
  }
}