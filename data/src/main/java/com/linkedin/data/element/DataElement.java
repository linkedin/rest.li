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
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import java.util.List;


/**
 * Provides information about a Data object being iterated.
 * <p>
 * It provides the Data object's name within its parent Data object,
 * the Data object itself, the {@link DataSchema} of the Data object,
 * how depth of the Data object within the object graph being
 * iterated, and a reference to {@link DataElement} providing the
 * same information about the parent Data object.
 *
 * @author slim
 */
public interface DataElement
{
  /**
   * The name of the root Data object.
   */
  String ROOT_NAME = new String();

  /**
   * The default separator for path components within a fully qualified path.
   */
  Character SEPARATOR = '/';

  /**
   * Value of the Data object.
   *
   * @return the value of the Data object.
   */
  Object getValue();

  /**
   * The name used to address the the Data object within
   * a complex Data object.
   * <p>
   *
   * If the Data object is contained within a {@link DataList},
   * then it's name is an {@link Integer} whose
   * value is the Data object's index in the {@link DataList}.
   * <p>
   *
   * If the Data object is contained within a {@link DataMap},
   * then the name is a {@link String} whose value
   * is the key of the Data object in the {@link DataMap}.
   * <p>
   *
   * One special case is the root Data object, its name is
   * always {@link #ROOT_NAME}. The root Data object is one
   * for which the object containing the root is unknown,
   * i.e. {@link #getParent()} returns null.
   *
   * @return the name of the Data object.
   */
  Object getName();

  /**
   * The {@link DataSchema} of the Data object.
   *
   * The schema is not always known or available. For example,
   * the schema will not be available:
   * <ul>
   * <li> if a {@link DataSchema} is not provided to the {@link com.linkedin.data.it.ObjectIterator},
   * <li> if a Data object has no name that does not have a corresponding
   *      field defined in the parent's {@link RecordDataSchema}.
   * </ul>
   *
   * @return the {@link DataSchema} of the Data object, or
   *         return null if the {@link DataSchema} is not
   *         available or unknown.
   */
  DataSchema getSchema();

  /**
   * Return the {@link DataElement} that represents the
   * parent Data object of this {@link DataElement}'s
   * Data object.
   *
   * @return the {@link DataElement} of the parent Data object
   *         of the Data object represented by this
   *         {@link DataElement}.
   */
  DataElement getParent();

  /**
   * The level of the {@link DataElement} provides the depth of the
   * Data object within the object graph being iterated.
   *
   * The level of the root Data object is always 0.
   * The level of the children of root Data object is 1.
   *
   * @return the level of the Data object.
   */
  int level();

  /**
   * Lookup a child Data object by name.
   * <p>
   *
   * The primary intent of this method is to provide a single
   * method to lookup a child by name without explicitly having
   * to know the current Data object's type and calling
   * either {@link DataMap#get(Object)} or {@link DataList#get(int)}
   * based on the Data object's class.
   * <p>
   *
   * If the current Data object is a {@link DataMap}, the name's
   * class should be {@link String}. If the current Data object is
   * a {@link DataList}, the name's class should be a {@link Integer}.
   * If the name is not of the expected class, the lookup will not find a match.
   * Similarly, the lookup will not find a child if the current value
   * is not a {@link DataMap} or {@link DataList}.
   * <p>
   *
   * @param childName the name to lookup.
   * @return the child Data object if found, else return null if not found.
   */
  Object getChild(Object childName);

  /**
   * Output path as an array of path components.
   *
   * @return a array of path components.
   */
  Object[] path();

  /**
   * Output path as an array of path components with the provided paths appended.
   *
   * @param append provides additional components to be appended to the array pf
   *               path components.
   * @return a array of path components.
   */
  Object[] path(Object... append);

  /**
   * Output path to the specified {@link List}.
   *
   * @param path provides the {@link List} be initialized with the path.
   */
  void pathAsList(List<Object> path);

  /**
   * Output the fully qualified path.
   * <p>
   *
   * The fully qualified path is the list of names (also known
   * as path components) of the Data object's traversed
   * from the root Data object to reach this Data object. If the
   * Data object is an array item, the path component will be its
   * index within the array and if the data object is a map value,
   * the path component will be its map entry key.
   *
   * The root Data object's name is not included in this list since
   * it is always the same and present.
   *
   * <p>
   *
   * @param separator provides the character that will be used to
   *                  separate the path components in the output string.
   * @return the fully qualified path.
   */
  String pathAsString(Character separator);

  /**
   * Output the fully qualified path name with the default separator.
   *
   * @return the fully qualified path separated by the default separator.
   */
  String pathAsString();

  /**
   * Copy chain of {@link DataElement}'s starting from this {@link DataElement}
   * to the root, i.e. the {@link DataElement} whose parent is null.
   */
  DataElement copyChain();

  /**
   * Output this element's corresponding schema field's
   * {@link com.linkedin.data.schema.PathSpec}, if DataSchemas needed to construct it are available
   *
   * Because DataElement does not necessarily associate with DataSchemas, therefore for a dataElement with no
   * DataSchema, or its parents or ancestors don't have one, there will not be PathSpec for it.
   *
   * Note that the path component representing the array index and map key entry will be
   * {@link com.linkedin.data.schema.PathSpec#WILDCARD} inside PathSpec.
   *
   * @return schema's {@link PathSpec} of this dataElement's corresponding field
   *   if there is no PathSpec,  will return null
   */
   PathSpec getSchemaPathSpec();
}
