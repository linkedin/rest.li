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

package com.linkedin.data.schema;

import java.util.HashMap;
import java.util.Map;


/**
 * {@link DataSchema} represents an schema.
 *
 * @author slim
 */
public abstract class DataSchema implements Cloneable
{
  /**
   * Possible types for a DataSchema.
   */
  public enum Type
  {
    BOOLEAN,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    BYTES,
    STRING,
    NULL,
    FIXED,
    ENUM,
    ARRAY,
    RECORD,
    MAP,
    UNION,
    TYPEREF
  }

  /**
   * Constructor.
   *
   * @param type of the {@link DataSchema}.
   */
  protected DataSchema(Type type)
  {
    _type = type;
  }

  /**
   * Return the type of the {@link DataSchema}.
   *
   * @return the type of the {@link DataSchema}.
   */
  public Type getType()
  {
    return _type;
  }

  /**
   * Return the dereferenced native type by following typerefs.
   *
   * Should never return {@link Type#TYPEREF}.
   *
   * @return the dereferenced native type by following typerefs.
   */
  public Type getDereferencedType()
  {
    return _type;
  }

  /**
   * Return the dereferenced {@link DataSchema} by following typerefs.
   *
   * Should never return a {@link TyperefDataSchema}.
   *
   * @return the dereferenced {@link DataSchema} by following typerefs.
   */
  public DataSchema getDereferencedDataSchema()
  {
    return this;
  }

  /**
   * Return whether the {@link DataSchema} has one or more errors.
   *
   * @return whether the {@link DataSchema} has one or more errors.
   */
  public abstract boolean hasError();

  /**
   * Return whether type is a primitive type.
   *
   * @return true if type is a primitive type.
   */
  public abstract boolean isPrimitive();

  /**
   * Return whether type is a complex type.
   *
   * @return true if type is a complex type.
   */
  public boolean isComplex()
  {
    return isPrimitive() == false;
  }

  /**
   * Return the properties of the {@link DataSchema}.
   *
   * Properties will be empty for non-complex types.
   *
   * @return the properties of the {@link DataSchema}.
   */
  public abstract Map<String, Object> getProperties();

  /**
   * <p>Return the resolved properties of the {@link DataSchema}
   *
   * <p>The DataSchema can have some properties associated with it,
   * but other schema who refer to this dataSchema might want to override them by using the annotations.
   * This field is for storing the resulted properties, after resolved those overrides to this schema
   *
   * @see com.linkedin.data.schema.annotation.SchemaAnnotationProcessor
   * @see com.linkedin.data.schema.annotation.SchemaAnnotationHandler
   *
   * @return the properties after resolution for current {@link DataSchema}
   */
  public Map<String, Object> getResolvedProperties()
  {
    return _resolvedProperties;
  }

  public void setResolvedProperties(Map<String, Object> resolvedProperties)
  {
    _resolvedProperties = new HashMap<>(resolvedProperties);
  }

  /**
   * Return the default union member key for this {@link DataSchema}.
   *
   * This key can be used to identify union members following the Avro specification but for unions
   * that contain more than one member of the same type, using this key will not provide uniqueness.
   *
   * @return the default union member key for this {@link DataSchema}.
   */
  public abstract String getUnionMemberKey();

  /**
   * Print the {@link DataSchema} to JSON with space between fields, items, names, values, ... etc.
   *
   * @return JSON representation of {@link DataSchema}.
   */
  @Override
  public String toString()
  {
    return SchemaToJsonEncoder.schemaToJson(this, JsonBuilder.Pretty.SPACES);
  }

  @Override
  public abstract boolean equals(Object other);

  @Override
  public abstract int hashCode();

  @Override
  public DataSchema clone() throws CloneNotSupportedException
  {
    DataSchema dataSchema = (DataSchema) super.clone();
    dataSchema._resolvedProperties = new HashMap<>();
    return dataSchema;
  }

  private final Type _type;
  Map<String, Object> _resolvedProperties = new HashMap<>();
}
