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

/**
 * {@link DataSchema} for typeref.
 *
 * A {@link TyperefDataSchema} is used to provide an alternate name
 * and additional metadata on another type. This is especially useful
 * for creating types to differentiate different uses of the same
 * primitive types. For example, string may be used to store an
 * Uniform Resource Name or a locale. It is desirable to use
 * a "urn" or "locale" instead of "string" for these uses.
 */
public class TyperefDataSchema extends NamedDataSchema
{
  private DataSchema _referencedType = DataSchemaConstants.NULL_DATA_SCHEMA;

  public TyperefDataSchema(Name name)
  {
    super(Type.TYPEREF, name);
  }

  /**
   * Set the referenced type.
   *
   * @param schema of referenced type.
   */
  public void setReferencedType(DataSchema schema)
  {
    if (schema == null)
    {
      _referencedType = DataSchemaConstants.NULL_DATA_SCHEMA;
      setHasError();
    }
    else
    {
      _referencedType = schema;
    }
  }

  /**
   * Get the referenced type.
   *
   * @return the referenced type.
   */
  public DataSchema getRef()
  {
    return _referencedType;
  }

  @Override
  public Type getDereferencedType()
  {
    return _referencedType.getDereferencedType();
  }

  @Override
  public DataSchema getDereferencedDataSchema()
  {
    return _referencedType.getDereferencedDataSchema();
  }

  @Override
  public String getUnionMemberKey()
  {
    return _referencedType.getUnionMemberKey();
  }

  @Override
  public boolean equals(Object object)
  {
    if (this == object)
    {
      return true;
    }
    if (object != null && object.getClass() == TyperefDataSchema.class)
    {
      TyperefDataSchema other = (TyperefDataSchema) object;
      return super.equals(other) && _referencedType.equals(other._referencedType);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return super.hashCode() ^ _referencedType.hashCode();
  }
}
