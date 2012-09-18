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


import static com.linkedin.data.schema.DataSchemaConstants.NAMESPACE_PATTERN;
import static com.linkedin.data.schema.DataSchemaConstants.NAME_PATTERN;
import static com.linkedin.data.schema.DataSchemaConstants.UNQUALIFIED_NAME_PATTERN;

public final class Name
{
  /**
   * Construct empty {@link Name}.
   */
  public Name()
  {
  }

  /**
   * Construct a new {@link Name} with the specified full name. No error message returned.
   *
   * @param fullName provides the full name.
   * @throws IllegalArgumentException if there's an error setting the name.
   */
  public Name(String fullName)
  {
    StringBuilder errorMessageBuilder = new StringBuilder();
    setName(fullName, errorMessageBuilder);
    if (errorMessageBuilder.length() > 0)
    {
      throw new IllegalArgumentException(errorMessageBuilder.toString());
    }
  }

  /**
   * Construct a new {@link Name} with the specified full name and
   * append errors in the specified {@link StringBuilder}.
   *
   * @param fullName provides the full name.
   * @param errorMessageBuilder provides the {@link StringBuilder} to append 
   *                            error messages to.
   */
  public Name(String fullName, StringBuilder errorMessageBuilder)
  {
    setName(fullName, errorMessageBuilder);
  }

  /**
   * Construct a new {@link Name} with the specified name and namespace, 
   * and append errors in the specified {@link StringBuilder}.
   *
   * @param name provides the name.
   * @param namespace provides the namespace.            
   * @param errorMessageBuilder provides the {@link StringBuilder} to append 
   *                            error messages to.
   */
  public Name(String name, String namespace, StringBuilder errorMessageBuilder)
  {
    setName(name, namespace, errorMessageBuilder);
  }

  /**
   * Sets this {@link Name} with the specified full name and
   * append errors in the specified {@link StringBuilder}.
   *
   * @param fullName provides the full name.
   * @param errorMessageBuilder provides the {@link StringBuilder} to append 
   *                            error messages to.
   */
  public boolean setName(String fullName, StringBuilder errorMessageBuilder)
  {
    boolean ok = true;
    String name;
    String namespace;
    int index = fullName.lastIndexOf('.');
    if (index == -1)
    {
      namespace = "";
      name = fullName;
    }
    else
    {
      namespace = fullName.substring(0, index);
      name = (index + 1) < fullName.length() ? fullName.substring(index + 1) : "";
    }
    _name = name;
    _namespace = namespace;
    _fullName = fullName;
    _isEmpty = false;
    if (isValidName(fullName) == false)
    {
      errorMessageBuilder.append("\"").append(fullName).append("\" is an invalid name.\n");
      ok = false;
    }
    _hasError |= !ok;
    return ok;
  }

  /**
   * Sets this {@link Name} with the specified name and namespace, 
   * and append errors in the specified {@link StringBuilder}.
   *
   * @param name provides the name.
   * @param namespace provides the namespace.            
   * @param errorMessageBuilder provides the {@link StringBuilder} to append 
   *                            error messages to.
   */
  public boolean setName(String name, String namespace, StringBuilder errorMessageBuilder)
  {
    boolean ok = true;
    _isEmpty = false;
    _name = name;
    _namespace = namespace;
    _fullName = namespace.isEmpty() ? _name : _namespace + "." + _name;
    if (isValidName(name) == false)
    {
      errorMessageBuilder.append("\"").append(name).append("\" is an invalid name.\n");
      ok = false;
    }
    if (isValidNamespace(namespace) == false)
    {
      errorMessageBuilder.append("\"").append(namespace).append("\" is an invalid namespace.\n");
      ok = false;
    }
    _hasError |= !ok;
    return ok;
  }

  public boolean isEmpty()
  {
    return _isEmpty;
  }

  public boolean hasError()
  {
    return _hasError;
  }

  public String getName()
  {
    return _name;
  }

  public String getNamespace()
  {
    return _namespace;
  }

  public String getFullName()
  {
    return _fullName;
  }

  public boolean equals(Object object)
  {
    if (object != null && object.getClass() == Name.class)
    {
      Name other = (Name) object;
      return _fullName.equals(other._fullName);
    }
    return false;
  }

  /**
   * Return the fullname.
   *
   * @return the fullname.
   */
  @Override
  public String toString()
  {
    return getFullName();
  }

  public int hashCode()
  {
    return _fullName.hashCode();
  }

  public static boolean isFullName(String name)
  {
    return name.indexOf('.') >= 0;
  }

  public static boolean isValidName(String name)
  {
    return NAME_PATTERN.matcher(name).matches();
  }

  public static boolean isValidNamespace(String namespace)
  {
    return NAMESPACE_PATTERN.matcher(namespace).matches();
  }

  public static boolean isValidUnqualifiedName(String name)
  {
    return UNQUALIFIED_NAME_PATTERN.matcher(name).matches();
  }

  private boolean _isEmpty = true;
  private boolean _hasError = false;
  private String _name = "";
  private String _namespace = "";
  private String _fullName = "";
}
