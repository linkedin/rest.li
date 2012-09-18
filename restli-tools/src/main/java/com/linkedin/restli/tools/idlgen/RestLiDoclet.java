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

package com.linkedin.restli.tools.idlgen;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.tools.javadoc.Main;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Custom Javadoc processor that merges documentation into the restspec.json. The embedded Javadoc
 * generator is basically a commandline tool wrapper and it runs in complete isolation from the rest
 * of the application. Due to the fact that the Javadoc tool instantiates RestLiDoclet, we cannot
 * cleanly integrate the output into the {@link RestLiResourceModelExporter} tool. Thus, we're just
 * dumping the docs into a static Map which can be accessed by {@link RestLiResourceModelExporter}.
 *
 * <p>This class is NOT thread-safe
 *
 * @author dellamag
 * @see Main#execute(String[])
 */
public class RestLiDoclet
{
  /**
   * Entry point for Javadoc Doclet.
   *
   * @param root {@link RootDoc} passed in by Javadoc
   * @return is successful or not
   */
  public static boolean start(RootDoc root)
  {
    _nameToClassDoc.clear();
    _identityToMethodDoc.clear();

    for (ClassDoc classDoc : root.classes())
    {
      _nameToClassDoc.put(classDoc.qualifiedName(), classDoc);

      for (MethodDoc methodDoc : classDoc.methods())
      {
        _identityToMethodDoc.put(MethodIdentity.create(methodDoc), methodDoc);
      }
    }

    return true;
  }

  /**
   * Query Javadoc {@link ClassDoc} for the specified resource class.
   *
   * @param resourceClass resource class to be queried
   * @return corresponding {@link ClassDoc}
   */
  public static ClassDoc getClassDoc(Class<?> resourceClass)
  {
    return _nameToClassDoc.get(resourceClass.getCanonicalName());
  }

  /**
   * Query Javadoc {@link MethodDoc} for the specified Java method.
   *
   * @param method Java method to be queried
   * @return corresponding {@link MethodDoc}
   */
  public static MethodDoc getMethodDoc(Method method)
  {
    return _identityToMethodDoc.get(MethodIdentity.create(method));
  }

  private static class MethodIdentity
  {
    public static MethodIdentity create(Method method)
    {
      final List<String> parameterTypeNames = new ArrayList<String>();

      // type parameters are not included in identity because of differences between reflection and Doclet:
      // e.g. for Collection<Void>:
      //   reflection Type.toString() -> Collection<Void>
      //   Doclet Type.toString() -> Collection
      for (Class<?> paramClass: method.getParameterTypes())
      {
        parameterTypeNames.add(paramClass.getCanonicalName());
      }

      return new MethodIdentity(method.getDeclaringClass().getName() + "." + method.getName(), parameterTypeNames);
    }

    public static MethodIdentity create(MethodDoc method)
    {
      final List<String> parameterTypeNames = new ArrayList<String>();
      for (Parameter param: method.parameters())
      {
        parameterTypeNames.add(param.type().qualifiedTypeName());
      }

      return new MethodIdentity(method.qualifiedName(), parameterTypeNames);
    }

    private MethodIdentity(String methodQualifiedName, List<String> parameterTypeNames)
    {
      _methodQualifiedName = methodQualifiedName;
      _parameterTypeNames = parameterTypeNames;
    }

    @Override
    public int hashCode()
    {
      return new HashCodeBuilder(17, 29).
          append(_methodQualifiedName).
          append(_parameterTypeNames).
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

      final MethodIdentity other = (MethodIdentity) obj;
      return new EqualsBuilder().
          append(_methodQualifiedName, other._methodQualifiedName).
          append(_parameterTypeNames, other._parameterTypeNames).
          isEquals();
    }

    private final String _methodQualifiedName;
    private final List<String> _parameterTypeNames;
  }

  private static final Map<String, ClassDoc> _nameToClassDoc = new HashMap<String, ClassDoc>();
  private static final Map<MethodIdentity, MethodDoc> _identityToMethodDoc = new HashMap<MethodIdentity, MethodDoc>();
}