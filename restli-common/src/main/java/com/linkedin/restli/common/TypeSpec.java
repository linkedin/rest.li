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

package com.linkedin.restli.common;

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.TemplateRuntimeException;


/**
 * Runtime representation of a rest.li type within the context of a ResourceSpec.
 *
 * @author jbetz@linkedin.com
 */
public class TypeSpec<T>
{
  /**
   * If the provided type is null, null is returned, otherwise the type is wrapped as a TypeSpec.
   * @param type A class, or null.
   * @param <T> The type of the class.
   * @return A TypeSpec for the class or null.
   */
  public static <T> TypeSpec<T> forClassMaybeNull(Class<T> type)
  {
    if(type == null)
    {
      return null;
    }
    else
    {
      return new TypeSpec<>(type);
    }
  }

  private final Class<T> _type;
  private final DataSchema _schema;

  /**
   * Creates a TypeSpec, backfilling the schema information using DataTemplateUtil.getSchema.  Please avoid this in
   * favor of the TypeSpec(Class, DataSchema) constructor..
   *
   * @param type
   */
  public TypeSpec(Class<T> type)
  {
    this(type, backfillSchemaIfPossible(type));
  }

  // if a data schema is not provided, we attempt to get one using the legacy approach of calling
  // DataTemplateUtil.getSchema.  In the future we want to eliminate use of DataTemplateUtil.getSchema entirely from
  // restli-client by having all TypeSpec instances constructed with a data schema provided.
  private static DataSchema backfillSchemaIfPossible(Class<?> type)
  {
    // These are all the classes used for type specs that are "schema-less".
    if (type == CompoundKey.class || type == ComplexResourceKey.class || type == Void.class)
    {
      return null;
    }

    try
    {
      return DataTemplateUtil.getSchema(type); // could be a primitive, DataTemplate, TyperefInfo, or enum
    }
    catch (TemplateRuntimeException e)
    {
      // can happen in case of custom types
      return null;
    }
  }

  public TypeSpec(Class<T> type, DataSchema schema)
  {
    if(type == null) throw new IllegalArgumentException("type must be non-null.");
    _type = type;
    _schema = schema;
  }

  public Class<T> getType()
  {
    return _type;
  }

  public boolean hasSchema() { return _schema != null; }

  public DataSchema getSchema()
  {
    return _schema;
  }

  @Override
  public String toString()
  {
    return "TypeSpec{" +
        "_type=" + _type +
        ", _schema=" + _schema +
        '}';
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object o)
  {
    if (this == o) return true;

    if (!(o instanceof TypeSpec)) return false;
    TypeSpec<T> typeSpec = (TypeSpec<T>) o;
    if (!_type.equals(typeSpec._type)) return false;
    if (_schema != null ? !_schema.equals(typeSpec._schema) : typeSpec._schema != null) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = _type.hashCode();
    result = 31 * result + (_schema != null ? _schema.hashCode() : 0);
    return result;
  }
}
