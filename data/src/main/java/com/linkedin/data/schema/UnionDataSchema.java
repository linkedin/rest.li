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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link DataSchema} for union.
 *
 * @author slim
 */
public final class UnionDataSchema extends ComplexDataSchema
{
  public UnionDataSchema()
  {
    super(DataSchema.Type.UNION);
  }

  /**
   * Sets the types of the union.
   *
   * @param types that may be members of the union in the order they are defined.
   * @param errorMessageBuilder to append error message to.
   * @return true if types were set successfully, false otherwise.
   */
  public boolean setTypes(List<DataSchema> types, StringBuilder errorMessageBuilder)
  {
    boolean ok = false;
    Map<String, Integer> typeMap = new HashMap<String, Integer>(types.size() * 2);
    Map<String, Integer> nameMap = new HashMap<String, Integer>(types.size() * 2);
    int index = 0;
    for (DataSchema type : types)
    {
      if (type.getDereferencedType() == DataSchema.Type.UNION)
      {
        errorMessageBuilder.append(type).append(" union cannot be inside another union.\n");
        ok = false;
      }
      String member = type.getUnionMemberKey();
      Integer existing = typeMap.put(member, index);
      if (existing != null)
      {
        errorMessageBuilder.append(type).append(" appears more than once in a union.\n");
        ok = false;
      }
      else
      {
        String name = avroUnionMemberKey(type);
        existing = nameMap.put(name, index);
        if (existing != null)
        {
          errorMessageBuilder.append(type).append(" has name " + name + " that appears more than once in a union, this may cause compatibility problems with Avro.\n");
          ok = false;
        }
      }
      index++;
    }
    _types = Collections.unmodifiableList(types);
    _typesToIndexMap = Collections.unmodifiableMap(typeMap);
    _namesToIndexMap = Collections.unmodifiableMap(nameMap);
    if (ok == false)
    {
      setHasError();
    }
    return ok;
  }

  /**
   * Union members in the order declared.
   *
   * @return union members in the the order declared.
   */
  public List<DataSchema> getTypes()
  {
    return _types;
  }

  /**
   * Returns the index of a member.
   *
   * @param type to obtain index for.
   * @return positive integer which is the index of the member if found else return -1.
   */
  public int index(String type)
  {
    Integer index = _typesToIndexMap.get(type);
    return (index == null ? -1 : index);
  }

  /**
   * Returns whether the type is a member of the union.
   *
   * @param type to check.
   * @return true if type is a member of the union.
   */
  public boolean contains(String type)
  {
    return _typesToIndexMap.containsKey(type);
  }

  /**
   * Returns the {@link DataSchema} for a member.
   *
   * @param type to obtain index for.
   * @return the {@link DataSchema} if type is a member of the union, else return null.
   */
  public DataSchema getType(String type)
  {
    Integer index = _typesToIndexMap.get(type);
    return (index != null ? _types.get(index) : null);
  }

  /**
   * Returns the {@link DataSchema} for a member.
   *
   * @param typeName provides the name of type to obtain index for.
   * @return the {@link DataSchema} if type is a member of the union, else return null.
   */
  public DataSchema getTypeByName(String typeName)
  {
    Integer index = _namesToIndexMap.get(typeName);
    return (index != null ? _types.get(index) : null);
  }

  @Override
  public String getUnionMemberKey()
  {
    return "union";
  }

  @Override
  public boolean equals(Object object)
  {
    if (this == object)
    {
      return true;
    }
    if (object != null && object.getClass() == UnionDataSchema.class)
    {
      UnionDataSchema other = (UnionDataSchema) object;
      return super.equals(other) && _types.equals(other._types);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return super.hashCode() ^ _types.hashCode();
  }

  /**
   * Return the Avro-compatible union member key (discriminator) for the provided {@link DataSchema}.
   *
   * The Avro-compatible key does not include the namespace in the key, e.g. the key for
   * "com.linkedin.foo.Bar" is "Bar".
   *
   * @param schema to return the Avro-compatible union member key for.
   * @return the Avro-compatible union member key for provided {@link DataSchema}.
   */
  public static String avroUnionMemberKey(DataSchema schema)
  {
    DataSchema dereferencedSchema = schema.getDereferencedDataSchema();
    String name;

    // Avro union member type discriminator names
    if (dereferencedSchema instanceof NamedDataSchema)
    {
      name = ((NamedDataSchema) dereferencedSchema).getName();
    }
    else
    {
      name = dereferencedSchema.getUnionMemberKey();
    }
    return name;
  }

  private List<DataSchema> _types = _emptyTypes;
  private Map<String, Integer> _typesToIndexMap = _emptyTypesToIndexMap;
  private Map<String, Integer> _namesToIndexMap = _emptyTypesToIndexMap;

  private static final List<DataSchema> _emptyTypes = Collections.emptyList();
  private static final Map<String, Integer> _emptyTypesToIndexMap = Collections.emptyMap();
}
