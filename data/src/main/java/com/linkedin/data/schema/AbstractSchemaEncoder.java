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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public abstract class AbstractSchemaEncoder {
  protected TypeReferenceFormat _typeReferenceFormat = TypeReferenceFormat.DENORMALIZE;
  private final Set<String> _alreadyEncountered = new HashSet<>();

  public AbstractSchemaEncoder()
  {
  }

  public AbstractSchemaEncoder(TypeReferenceFormat typeReferenceFormat)
  {
    _typeReferenceFormat = typeReferenceFormat;
  }

  /**
   * Encode the specified {@link DataSchema}.
   * @param schema to encode.
   * @throws IOException if there is an error while encoding.
   */
  abstract public void encode(DataSchema schema) throws IOException;

  /**
   * The different ways type references can be formatted.
   */
  public enum TypeReferenceFormat
  {
    /**
     * Format with all dependent types declared inline at their first lexical appearance, and referenced by name in
     * all subsequent appearances.
     *
     * This format produces a single JSON object representation of this schema with all of the schemas it
     * transitively depends on inlined.
     */
    DENORMALIZE,

    /**
     * Format with all dependent types either declared inline or referenced in the exact same way they were in the
     * original schema declaration.
     */
    PRESERVE,

    /**
     * Format with all dependent types referenced by name.
     */
    MINIMIZE
  }

  /**
   * Gets how type references are formatted.
   */
  public TypeReferenceFormat getTypeReferenceFormat()
  {
    return _typeReferenceFormat;
  }

  /**
   * Set how type references are formatted.
   */
  public void setTypeReferenceFormat(TypeReferenceFormat typeReferenceFormat)
  {
    _typeReferenceFormat = typeReferenceFormat;
  }

  /**
   * Determines how a type from the original schema should be encoded.
   *
   * @param originallyInlined identifies if the provided type was originally inlined.
   * @return the {@link TypeRepresentation} to use when encoding a type.
   */
  protected TypeRepresentation selectTypeRepresentation(DataSchema schema, boolean originallyInlined)
  {
    boolean firstEncounter = true;
    if (schema instanceof NamedDataSchema)
    {
      String fullName = ((NamedDataSchema) schema).getFullName();
      firstEncounter = !_alreadyEncountered.contains(fullName);
    }
    else if (schema instanceof PrimitiveDataSchema)
    {
      return TypeRepresentation.DECLARED_INLINE;
    }
    switch (_typeReferenceFormat)
    {
      case PRESERVE:
        return originallyInlined ? TypeRepresentation.DECLARED_INLINE : TypeRepresentation.REFERENCED_BY_NAME;
      case DENORMALIZE:
        return firstEncounter ? TypeRepresentation.DECLARED_INLINE : TypeRepresentation.REFERENCED_BY_NAME;
      case MINIMIZE:
        return TypeRepresentation.REFERENCED_BY_NAME;
      default:
        throw new IllegalArgumentException("Unrecognized enum symbol: " + _typeReferenceFormat);
    }
  }

  public void markEncountered(DataSchema schema)
  {
    if (schema instanceof NamedDataSchema)
    {
      _alreadyEncountered.add(((NamedDataSchema) schema).getFullName());
    }
  }

  /**
   * Possible serialization formats of a particular dependant type.
   */
  protected enum TypeRepresentation
  {
    /**
     * The type declaration is inlined.
     */
    DECLARED_INLINE,

    /**
     * The type is referenced by name.
     */
    REFERENCED_BY_NAME
  }
}
