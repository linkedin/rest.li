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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.linkedin.data.schema.DataSchemaConstants.FIELD_NAME_PATTERN;
import static com.linkedin.data.schema.DataSchemaConstants.RESTRICTED_UNION_ALIASES;


/**
 * {@link DataSchema} for union.
 *
 * @author slim
 * @author Arun Ponnniah Sethuramalingam
 */
public final class UnionDataSchema extends ComplexDataSchema
{
  /**
   * If this is set to true, this is schema will be  a partial schema
   * created from projection. It is internal use only to represent a
   * subset of members in the union.
   */
  private boolean isPartialSchema = false;

  /**
   * Class for representing a member inside a Union
   */
  public static class Member implements Cloneable
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
      else if (RESTRICTED_UNION_ALIASES.contains(alias))
      {
        errorMessageBuilder.append("\"").append(alias).append("\" is restricted keyword for a member alias.\n");
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

    @Override
    public boolean equals(Object object)
    {
      boolean result = false;
      if (this == object)
      {
        result = true;
      }
      else if (object != null && object.getClass() == Member.class)
      {
        Member other = (Member) object;
        result = ((_alias == null) ? other._alias == null : _alias.equals(other._alias)) &&
            _type.equals(other._type) &&
            _doc.equals(other._doc) &&
            _properties.equals(other._properties) &&
            _hasError == other._hasError;
      }

      return result;
    }

    @Override
    public int hashCode()
    {
      return (_alias == null) ? 0 : _alias.hashCode() ^
          _type.hashCode() ^
          _doc.hashCode() ^
          _properties.hashCode() ^
          (_hasError ? 0xAAAAAAAA : 0x55555555);
    }

    @Override
    public Member clone() throws CloneNotSupportedException
    {
      return (Member) super.clone();
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

    Set<String> avroMemberKeys = new HashSet<>(members.size());
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
        String avroMemberKey = avroUnionMemberKey(memberType);
        boolean unique = avroMemberKeys.add(avroMemberKey);
        if (!unique && !memberHasAlias)
        {
          errorMessageBuilder.append(memberType).append(" has name ").append(avroMemberKey).append(" that appears more than once in a union, this may cause compatibility problems with Avro.\n");
          ok = false;
        }
      }

      index++;
    }

    _members = Collections.unmodifiableList(members);
    _memberKeyToIndexMap = Collections.unmodifiableMap(memberKeyMap);
    _membersAliased = areMembersAliased.orElse(false);

    if (!ok)
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
  public List<Member> getMembers()
  {
    return _members;
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
   * Returns the {@link DataSchema} for a member identified by its member key returned
   * from {@link Member#getUnionMemberKey()}.
   *
   * @param memberKey Union member key of the member.
   * @return the {@link DataSchema} if type is a member of the union, else return null.
   *
   * @deprecated Replaced by {@link #getTypeByMemberKey(String)}. This method exists only to help during the
   * migration phase. It will be removed in the later versions and SHOULD NOT be used for any new use cases.
   */
  @Deprecated
  public DataSchema getType(String memberKey)
  {
    return getTypeByMemberKey(memberKey);
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
    return (index != null ? _members.get(index).getType() : null);
  }

  /**
   * Returns the member identified by its member key.
   *
   * @param memberKey Union member key of the member.
   * @return the {@link Member} if key matches a member of the union, else return null.
   */
  public Member getMemberByMemberKey(String memberKey)
  {
    Integer index = _memberKeyToIndexMap.get(memberKey);
    return (index != null ? _members.get(index) : null);
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
      return super.equals(other) && _members.equals(other._members);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return super.hashCode() ^ _members.hashCode();
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
  private Map<String, Integer> _memberKeyToIndexMap = _emptyTypesToIndexMap;
  private boolean _membersAliased = false;

  private static final Map<String, Integer> _emptyTypesToIndexMap = Collections.emptyMap();

  /**
   * This set/get pair methods are used internal only to specify a boolean flag
   * for partial union schema.
   * @param partialSchema
   */
  public void setPartialSchema(boolean partialSchema)
  {
    this.isPartialSchema = partialSchema;
  }

  public boolean isPartialSchema()
  {
    return this.isPartialSchema;
  }
}
