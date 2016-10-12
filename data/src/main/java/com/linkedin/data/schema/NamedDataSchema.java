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
import java.util.List;

/**
 * Interface for named types, e.g. enum, fixed, record.
 *
 * @author slim
 */
public abstract class NamedDataSchema extends ComplexDataSchema implements Named, BindingInfo
{
  protected NamedDataSchema(Type type, Name name)
  {
    super(type);
    _name = name;
  }

  /**
   * Set the aliases of the {@link DataSchema}.
   *
   * @param aliases of the {@link DataSchema}.
   */
  public void setAliases(List<Name> aliases)
  {
    _aliases = Collections.unmodifiableList(aliases);
  }

  /**
   * Set the documentation of the {@link DataSchema}.
   *
   * @param documentation of the {@link DataSchema}.
   */
  public void setDoc(String documentation)
  {
    _doc = documentation;
  }

  /**
   * Set the binding package of the {@link DataSchema}.
   *
   * @param packageName of the {@link DataSchema}.
   */
  public void setPackage(String packageName)
  {
    _package = packageName;
  }

  /**
   * Return the {@link DataSchema}'s unqualified name.
   *
   * @return the {@link DataSchema}'s unqualified name.
   */
  @Override
  public String getName()
  {
    return _name.getName();
  }

  /**
   * Return the {@link DataSchema}'s fully qualified name.
   * This name is used in the over-the-wire protocol.
   *
   * @return the {@link DataSchema}'s fully qualified name.
   */
  @Override
  public String getFullName()
  {
    return _name.getFullName();
  }

  /**
   * Return the {@link DataSchema}'s language binding name.
   * This is the fully qualified name for the generated data model to resolve potential name conflict.
   *
   * @return the {@link DataSchema}'s language binding name.
   */
  @Override
  public String getBindingName()
  {
    return (_package == null || _package.isEmpty()) ? getFullName() : _package + "." + getName();
  }

  /**
   * Return the {@link DataSchema}'s binding package.
   *
   * @return the {@link DataSchema}'s binding package.
   */
  @Override
  public String getPackage()
  {
    return _package;
  }

  /**
   * Return the {@link DataSchema}'s namespace.
   *
   * @return the {@link DataSchema}'s namespace.
   */
  @Override
  public String getNamespace()
  {
    return _name.getNamespace();
  }

  /**
   * Return the {@link DataSchema}'s fully scoped aliases.
   *
   * @return the {@link DataSchema}'s fully scoped aliases.
   */
  public List<Name> getAliases()
  {
    return _aliases;
  }

  /**
   * Return the documentation.
   *
   * @return the documentation.
   */
  public String getDoc()
  {
    return _doc;
  }

  @Override
  public String getUnionMemberKey()
  {
    return getFullName();
  }

  @Override
  public boolean equals(Object object)
  {
    if (object != null && object instanceof NamedDataSchema)
    {
      // Location is not used for comparison.
      // Equal if the schema is the same.
      NamedDataSchema other = (NamedDataSchema) object;
      return super.equals(object) && _name.equals(other._name) && _doc.equals(other._doc) && _aliases.equals(other._aliases);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    // Location is not used in hashCode
    return super.hashCode() ^ _name.hashCode() ^ _doc.hashCode() ^ _aliases.hashCode();
  }

  private final Name _name;
  /**
   * Package override (if specified) for this data schema. This is used in  {@link #getBindingName()} to generate the
   * language binding class name for generated data model. It is optional, if not specified, we will use schema namespace
   * as the default package name.
   */
  private String _package = "";
  private String _doc = "";
  private List<Name> _aliases = _emptyAliases;

  private static final List<Name> _emptyAliases = Collections.emptyList();
}