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

import com.linkedin.data.DataMap;
import java.util.HashMap;
import java.util.Map;


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
  private boolean _refDeclaredInline = false;

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
   * This method returns the underlying _referencedType for {@link TyperefDataSchema}
   * Note this method could still return {@link TyperefDataSchema} while {@link #getDereferencedDataSchema()} would not
   * @return the referenced DataSchema
   */
  public DataSchema getRef()
  {
    return _referencedType;
  }

  /**
   * Sets if the ref type is declared inline in the schema.
   * @param refDeclaredInline true if the ref type is declared inline, false if it is referenced by name.
   */
  public void setRefDeclaredInline(boolean refDeclaredInline)
  {
    _refDeclaredInline = refDeclaredInline;
  }

  /**
   * Checks if the ref type is declared inline.
   * @return true if the ref type is declared inline, false if it is referenced by name.
   */
  public boolean isRefDeclaredInline()
  {
    return _refDeclaredInline;
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

  /**
   * Would merge properties of current {@link TyperefDataSchema} with its referenced DataSchema
   * If referenced DataSchema is also a {@link TyperefDataSchema}, then it would merge recursively
   * @return a map of properties which merged properties recursively, up to current {@link TyperefDataSchema}
   */
  public Map<String, Object> getMergedTyperefProperties()
  {
    Map<String, Object> propertiesToBeMerged = null;
    if (getRef().getType() == Type.TYPEREF)
    {
      propertiesToBeMerged = ((TyperefDataSchema) getRef()).getMergedTyperefProperties();
    }
    else if (getRef().isPrimitive())
    {
      propertiesToBeMerged = getRef().getProperties();
    }
    else
    {
      propertiesToBeMerged = new HashMap<>();
    }
    Map<String, Object> mergedMap = new DataMap(getProperties());
    // Merge rule for same name property conflicts:
    //   Outer layer TypeRef's properties would override de-referenced DataSchema's properties.
    propertiesToBeMerged.forEach(mergedMap::putIfAbsent);
    return mergedMap;
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
