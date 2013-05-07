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

package com.linkedin.restli.tools.idlcheck;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class CompatibilityInfo
{
  enum Level
  {
    INCOMPATIBLE,
    COMPATIBLE
  }

  enum Type
  {
    FINDER_ASSOCKEYS_DOWNGRADE(Level.INCOMPATIBLE, "Finder AssocKeys may not be downgraded to AssocKey"),
    ARRAY_NOT_CONTAIN(Level.INCOMPATIBLE, "Current field must contain these values: %s"),
    ARRAY_NOT_EQUAL(Level.INCOMPATIBLE, "Current field must be these values: %s"),
    ARRAY_MISSING_ELEMENT(Level.INCOMPATIBLE, "\"%s\" has been removed from the current field"),
    OTHER_ERROR(Level.INCOMPATIBLE, "%s"),
    PARAMETER_NEW_REQUIRED(Level.INCOMPATIBLE, "Unable to add the new required parameter \"%s\""),
    PARAMETER_WRONG_OPTIONALITY(Level.INCOMPATIBLE, "Unable to change the previous optional parameter to currently required"),
    RESOURCE_MISSING(Level.INCOMPATIBLE, "Missing resource class for idl \"%s\""),
    TYPE_INCOMPATIBLE(Level.INCOMPATIBLE, "Previous type \"%s\" and current type \"%s\" are incompatible"),
    TYPE_MISSING(Level.INCOMPATIBLE, "Type is required but missing"),
    TYPE_UNKNOWN(Level.INCOMPATIBLE, "Type cannot be resolved: %s"),
    VALUE_NOT_EQUAL(Level.INCOMPATIBLE, "Current value \"%2$s\" does not match the previous value \"%1$s\""),
    VALUE_WRONG_OPTIONALITY(Level.INCOMPATIBLE, "\"%s\" may not be removed because it exists in the previous version"),
    PARAMETER_NEW_OPTIONAL(Level.COMPATIBLE, "New optional parameter \"%s\" is added"),
    OPTIONAL_PARAMETER(Level.COMPATIBLE, "Previous optional parameter is changed to currently required"),
    OPTIONAL_VALUE(Level.COMPATIBLE, "Optional field \"%s\" was previously missing but currently present"),
    RESOURCE_NEW(Level.COMPATIBLE, "Adding new resource defined in \"%s\""),
    SUPERSET(Level.COMPATIBLE, "Current values have these extra values: %s"),
    VALUE_DIFFERENT(Level.COMPATIBLE, "Previous value \"%s\" is changed to \"%s\"");

    public String getDescription(Object[] parameters)
    {
      return String.format(_description, parameters);
    }

    Level getLevel()
    {
      return _level;
    }

    private Type(Level level, String description)
    {
      _level = level;
      _description = description;
    }

    private Level _level;
    private String _description;
  }

  /**
   * @param path path to the subject node in the idl (JSON) file, delimited by slash "/"
   * @param type type of the information
   */
  public CompatibilityInfo(List<Object> path, Type type)
  {
    _path = StringUtils.join(path, _pathSeparator);
    _type = type;
  }

  /**
   * @param path path to the subject node in the idl (JSON) file, delimited by slash "/"
   * @param type type of the information
   * @param parameters parameters used to construct the information message
   */
  public CompatibilityInfo(List<Object> path, Type type, Object... parameters)
  {
    this(path, type);

    if (parameters.length > 0)
    {
      _parameters = parameters;
    }
  }

  @Override
  public int hashCode()
  {
    return new HashCodeBuilder(17, 29).
        append(_type).
        append(_path).
        append(_parameters).
        toHashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }

    if (obj == null)
    {
      return false;
    }

    if (getClass() != obj.getClass())
    {
      return false;
    }

    final CompatibilityInfo other = (CompatibilityInfo) obj;
    return new EqualsBuilder().
        append(_type, other._type).
        append(_path, other._path).
        append(_parameters, other._parameters).
        isEquals();
  }

  @Override
  public String toString()
  {
    final StringBuilder output = new StringBuilder();

    if (!_path.isEmpty())
    {
      output.append(_path).append(": ");
    }

    output.append(_type.getDescription(_parameters));

    return output.toString();
  }

  protected String _path;
  protected Type _type;
  protected Object[] _parameters;

  private final static String _pathSeparator = "/";
}
