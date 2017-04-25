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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.linkedin.data.schema.DataSchemaConstants.FIELD_NAME_PATTERN;


/**
 * {@link DataSchema} for union.
 *
 * @author slim
 * @author Arun Ponnniah Sethuramalingam
 */
public final class UnionDataSchema extends ComplexDataSchema
{
  /**
   * Class for representing a member inside a Union
   */
  public static class Member
  {
    /**
     * Constructor
     *
     * @param type of the member.
     */
    public Member(DataSchema type)
    {
      setType(type);
    }

    /**
     * Set the alias of the member.
     *
     * @param alias of the member.
     * @param errorMessageBuilder to append error message to.
     * @return false if the name is not a valid field name.
     */
    public boolean setAlias(String alias, StringBuilder errorMessageBuilder)
    {
      boolean ok = true;
      if (!FIELD_NAME_PATTERN.matcher(alias).matches())
      {
        errorMessageBuilder.append("\"").append(alias).append("\" is an invalid member alias.\n");
        ok = false;
      }
      _alias = alias;
      _hasError |= !ok;
      return ok;
    }

    /**
     * Set the type of the member.
     *
     * @param type of the member.
     */
    public void setType(DataSchema type)
    {
      if (type == null)
      {
        _type = DataSchemaConstants.NULL_DATA_SCHEMA;
        _hasError = true;
      }
      else
      {
        _type = type;
      }
    }

    /**
     * Set the documentation of the member.
     *
     * @param documentation of the member.
     */
    public void setDoc(String documentation)
    {
      _doc = documentation;
    }

    /**
     * Set the properties of the member.
     *
     * @param properties of the member.
     */
    public void setProperties(Map<String, Object> properties)
    {
      _properties = Collections.unmodifiableMap(properties);
    }

    /**
     * Sets if the union member type is declared inline in the schema.
     *
     * @param declaredInline true if the union member type is declared inline, false if it is referenced by name.
     */
    public void setDeclaredInline(boolean declaredInline)
    {
      _declaredInline = declaredInline;
    }

    /**
     * Return the alias of the member.
     *
     * @return the alias of the member.
     */
    public String getAlias()
    {
      return _alias;
    }

    /**
     * Return the {@link DataSchema} of the member.
     *
     * @return the {@link DataSchema} of the member.
     */
    public DataSchema getType()
    {
      return _type;
    }

    /**
     * Return the documentation of the member.
     *
     * @return the documentation of the member.
     */
    public String getDoc()
    {
      return _doc;
    }

    /**
     * Return the properties of the member.
     *
     * @return the properties of the member.
     */
    public Map<String, Object> getProperties()
    {
      return _properties;
    }

    /**
     * Checks if the union member type is declared inline.
     *
     * @return true if the union member type is declared inline, false if it is referenced by name.
     */
    public boolean isDeclaredInline()
    {
      return _declaredInline;
    }

    /**
     * Checks if the union member has an alias.
     *
     * @return True if the union member has an explicit alias specified, false otherwise.
     */
    public boolean hasAlias()
    {
      return _alias != null;
    }

    /**
     * Returns the key that will be used for this member while serializing the Union. If an alias
     * is available for this member, the alias will be returned else the default union member key
     * of the member's type will be returned.
     *
     * @return The union member key for this member
     */
    public String getUnionMemberKey()
    {
      return hasAlias() ? _alias : _type.getUnionMemberKey();
    }

    /**
     * Return whether the field has an error.
     *
     * @return if the field has an error.
     */
    public boolean hasError()
    {
      return _hasError;
    }

    private String _alias = null;
    private DataSchema _type = DataSchemaConstants.NULL_DATA_SCHEMA;
    private String _doc = "";
    private Map<String, Object> _properties = Collections.emptyMap();
    private boolean _declaredInline = false;
    private boolean _hasError = false;
  }

  public UnionDataSchema()
  {
    super(DataSchema.Type.UNION);
  }

  /**
   * Sets the members of the union.
   *
   * @param members A list of {@link Member} instances that were defined in this Union
   * @param errorMessageBuilder {@link StringBuilder} to append error messages to
   * @return True if the members were set successfully, false otherwise
   */
  public boolean setMembers(List<Member> members, StringBuilder errorMessageBuilder)
  {
    boolean ok = true;

    List<DataSchema> types = new ArrayList<>(members.size());
    Set<DataSchema> typesDeclaredInline = new HashSet<>(members.size());

    Map<String, Integer> typeMap = new HashMap<>(members.size());
    Map<String, Integer> nameMap = new HashMap<>(members.size());
    Map<String, Integer> memberKeyMap = new HashMap<>(members.size());

    Optional<Boolean> areMembersAliased = Optional.empty();

    int index = 0;
    for (Member member: members)
    {
      DataSchema memberType = member.getType();

      boolean memberHasAlias = member.hasAlias();
      if (memberType.getDereferencedType() != Type.NULL)
      {
        // "All or none" alias check is only for non-null member types
        if (!areMembersAliased.isPresent())
        {
          areMembersAliased = Optional.of(memberHasAlias);
        }
        else if (areMembersAliased.get() != memberHasAlias)
        {
          errorMessageBuilder.append("Union definition should have aliases specified for either all or zero members.\n");
          ok = false;
        }
      }
      else if (memberHasAlias)
      {
        // Aliasing "null" member is not allowed
        errorMessageBuilder.append(memberType).append(" member should not have an alias.\n");
        ok = false;
      }

      if (memberType.getDereferencedType() == Type.UNION)
      {
        errorMessageBuilder.append(memberType).append(" union cannot be inside another union.\n");
        ok = false;
      }

      Integer existing = memberKeyMap.put(member.getUnionMemberKey(), index);
      if (existing != null)
      {
        errorMessageBuilder.append(memberHasAlias ? "alias " : "").append(member.getUnionMemberKey()).append(" appears more than once in a union.\n");
        ok = false;
      }
      else
      {
        String name = avroUnionMemberKey(memberType);
        existing = nameMap.put(name, index);
        if (existing != null && !memberHasAlias)
        {
          errorMessageBuilder.append(memberType).append(" has name ").append(name).append(" that appears more than once in a union, this may cause compatibility problems with Avro.\n");
          ok = false;
        }
      }

      types.add(memberType);
      typeMap.put(memberType.getUnionMemberKey(), index);

      if (member.isDeclaredInline())
      {
        typesDeclaredInline.add(memberType);
      }

      index++;
    }

    setTypesDeclaredInline(typesDeclaredInline);

    _members = Collections.unmodifiableList(members);
    _types = Collections.unmodifiableList(types);
    _typesToIndexMap = Collections.unmodifiableMap(typeMap);
    _namesToIndexMap = Collections.unmodifiableMap(nameMap);
    _memberKeyToIndexMap = Collections.unmodifiableMap(memberKeyMap);
    _membersAliased = areMembersAliased.orElse(false);

    if (!ok)
    {
      setHasError();
    }

    return ok;
  }

  /**
   * Sets the types of the union.
   *
   * @param types that may be members of the union in the order they are defined.
   * @param errorMessageBuilder to append error message to.
   * @return true if types were set successfully, false otherwise.
   *
   * TODO (aponniah): Mark this method as deprecated.
   * (@deprecated) Deprecated when the support for aliasing Union members was introduced.
   * Use {@link #setMembers(List, StringBuilder)} ()} instead.
   */
  public boolean setTypes(List<DataSchema> types, StringBuilder errorMessageBuilder)
  {
    List<Member> members = types.stream()
        .map(Member::new)
        .collect(Collectors.toList());
    return setMembers(members, errorMessageBuilder);
  }

  /**
   * Union members in the order declared.
   *
   * @return union members in the the order declared.
   *
   * TODO (aponniah): Mark this method as deprecated.
   * (@deprecated) Deprecated when the support for aliasing Union members was introduced. Use
   * {@link #getMembers()} instead.
   */
  public List<DataSchema> getTypes()
  {
    return _types;
  }

  /**
   * Union members in the order declared.
   *
   * @return union members in the the order declared.
   */
  public List<Member> getMembers()
  {
    return _members;
  }

  /**
   * Returns the index of a member.
   *
   * @param type to obtain index for.
   * @return positive integer which is the index of the member if found else return -1.
   *
   * TODO (aponniah): Mark this method as deprecated.
   * (@deprecated) Deprecated when the support for aliasing Union members was introduced. For Unions declared with more
   * than one member of the same type this method will return a wrong index.
   */
  public int index(String type)
  {
    Integer index = _typesToIndexMap.get(type);
    return (index == null ? -1 : index);
  }

  /**
   * Returns whether the passed in member key maps to one of the members of the union. The key will be matched
   * against the contained member's key returned from {@link Member#getUnionMemberKey()}.
   *
   * @param memberKey to check.
   * @return true if maps to an existing member of the union, false otherwise.
   */
  public boolean contains(String memberKey)
  {
    return _memberKeyToIndexMap.containsKey(memberKey);
  }

  /**
   * Returns the {@link DataSchema} for a member. For {@link NamedDataSchema} types, the method expects
   * its fully qualified name (with namespace) and for others, the default union member key returned from
   * {@link DataSchema#getUnionMemberKey()}.
   *
   * @param type Fully qualified name of the member's type to return.
   * @return the {@link DataSchema} if type is a member of the union, else return null.
   *
   * TODO (aponniah): Mark this method as deprecated.
   * (@deprecated) Deprecated when the support for aliasing Union members was introduced.
   * Use {@link #getTypeByMemberKey(String)} instead.
   */
  public DataSchema getType(String type)
  {
    Integer index = _typesToIndexMap.get(type);
    return (index != null ? _types.get(index) : null);
  }

  /**
   * Returns the {@link DataSchema} for a member identified by its member key returned
   * from {@link Member#getUnionMemberKey()}.
   *
   * @param memberKey Union member key of the member.
   * @return the {@link DataSchema} if type is a member of the union, else return null.
   */
  public DataSchema getTypeByMemberKey(String memberKey)
  {
    Integer index = _memberKeyToIndexMap.get(memberKey);
    return (index != null ? _types.get(index) : null);
  }

  /**
   * Returns the {@link DataSchema} for a member. For {@link NamedDataSchema} types, the method expects
   * its simple name (without namespace) and for others, the default union member key returned from
   * {@link DataSchema#getUnionMemberKey()}.
   *
   * @param typeName Simple name of the member's type to return
   * @return the {@link DataSchema} if type is a member of the union, else return null.
   *
   * @see #avroUnionMemberKey(DataSchema)
   *
   * TODO (aponniah): Mark this method as deprecated.
   * (@deprecated) Deprecated when the support for aliasing Union members was introduced.
   * Use {@link #getTypeByMemberKey(String)} instead.
   */
  public DataSchema getTypeByName(String typeName)
  {
    Integer index = _namesToIndexMap.get(typeName);
    return (index != null ? _types.get(index) : null);
  }

  /**
   * Sets the union member types that are declared inline in the schema.
   *
   * @param typesDeclaredInline provides a set of member type that are declared inline.
   *
   * TODO (aponniah): Mark this method as deprecated.
   * (@deprecated) Deprecated when the support for aliasing Union members was introduced.
   * Replaced by {@link #setMembers(List, StringBuilder)} to set the union members.
   */
  public void setTypesDeclaredInline(Set<DataSchema> typesDeclaredInline)
  {
    _typesDeclaredInline = Collections.unmodifiableSet(typesDeclaredInline);
  }

  /**
   * Checks if a union member type is declared inline.
   *
   * @return true if the union member type is declared inline, false if it is referenced by name.
   *
   * TODO (aponniah): Mark this method as deprecated.
   * (@deprecated) Deprecated when the support for aliasing Union members was introduced. This method can potentially
   * return a wrong value on Unions declared with aliases. Use {@link Member#isTypeDeclaredInline(DataSchema)} instead.
   */
  public boolean isTypeDeclaredInline(DataSchema type)
  {
    return _typesDeclaredInline.contains(type);
  }

  /**
   * Checks if the union members have aliases specified. Since either all or none of the members can be aliased
   * in a union, a return value of true from this method means all the members (excluding null member, if present)
   * have been aliased and none otherwise.
   *
   * @return True if all the members (excluding null member, if present) have aliases, false otherwise.
   */
  public boolean areMembersAliased()
  {
    return _membersAliased;
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

  private List<Member> _members = Collections.emptyList();
  private List<DataSchema> _types = Collections.emptyList();
  private Map<String, Integer> _typesToIndexMap = _emptyTypesToIndexMap;
  private Map<String, Integer> _namesToIndexMap = _emptyTypesToIndexMap;
  private Map<String, Integer> _memberKeyToIndexMap = _emptyTypesToIndexMap;
  private Set<DataSchema> _typesDeclaredInline = Collections.emptySet();
  private boolean _membersAliased = false;

  private static final Map<String, Integer> _emptyTypesToIndexMap = Collections.emptyMap();
}
