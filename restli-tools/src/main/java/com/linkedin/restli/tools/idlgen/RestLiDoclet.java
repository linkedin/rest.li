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
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Custom Javadoc processor that merges documentation into the restspec.json. The embedded Javadoc
 * generator is basically a commandline tool wrapper and it runs in complete isolation from the rest
 * of the application. Due to the fact that the Javadoc tool instantiates RestLiDoclet, we cannot
 * cleanly integrate the output into the {@link RestLiResourceModelExporter} tool. Thus, we're just
 * dumping the docs into a static Map which can be accessed by {@link RestLiResourceModelExporter}.
 *
 * This class supports multiple runs of Javadoc Doclet API {@link Main#execute(String[])}.
 * Each run will be assigned an unique "Doclet ID", returned by
 * {@link #generateJavadoc(String, java.io.PrintWriter, java.io.PrintWriter, java.io.PrintWriter, String[])}.
 * The Doclet ID should be subsequently used to initialize {@link DocletDocsProvider}.
 *
 * This class is thread-safe. However, #generateJavadoc() will be synchronized.
 *
 * @author dellamag
 * @see {@link Main#execute(String, java.io.PrintWriter, java.io.PrintWriter, java.io.PrintWriter, String, String[])}
 */
public class RestLiDoclet
{
  /**
   * Generate Javadoc and return associated Doclet ID.
   * This method is synchronized.
   *
   * @param programName Name of the program (for error messages).
   * @param errWriter PrintWriter to receive error messages.
   * @param warnWriter PrintWriter to receive warning messages.
   * @param noticeWriter PrintWriter to receive notice messages.
   * @param args The command line parameters.
   * @return an unique doclet ID which represent the subsequent Main#execute() run.
   * @throws IllegalStateException if the generated doclet ID is already used. Try again.
   * @throws IllegalArgumentException if Javadoc fails to generate docs.
   */
  public static synchronized long generateJavadoc(String programName,
                                                  PrintWriter errWriter,
                                                  PrintWriter warnWriter,
                                                  PrintWriter noticeWriter,
                                                  String[] args)
  {
    if (_docInfo.containsKey(_id))
    {
      throw new IllegalStateException("Doclet information with ID " + _id + " already exists");
    }

    final int javadocRetCode = Main.execute(programName, errWriter, warnWriter, noticeWriter, RestLiDoclet.class.getName(), args);
    if (javadocRetCode != 0)
    {
      throw new IllegalArgumentException("Javadoc failed with return code " + javadocRetCode);
    }

    final long currentId = _id;
    ++_id;
    return currentId;
  }

  /**
   * Entry point for Javadoc Doclet.
   *
   * @param root {@link RootDoc} passed in by Javadoc
   * @return is successful or not
   */
  public static boolean start(RootDoc root)
  {
    final DocInfo docInfo = new DocInfo();

    for (ClassDoc classDoc : root.classes())
    {
      docInfo.setClassDoc(classDoc.qualifiedName(), classDoc);

      for (MethodDoc methodDoc : classDoc.methods())
      {
        docInfo.setMethodDoc(MethodIdentity.create(methodDoc), methodDoc);
      }
    }

    _docInfo.put(_id, docInfo);

    return true;
  }

  /**
   * Query Javadoc {@link ClassDoc} for the specified resource class.
   *
   * @param resourceClass resource class to be queried
   * @return corresponding {@link ClassDoc}
   */
  public static ClassDoc getClassDoc(long docletId, Class<?> resourceClass)
  {
    final DocInfo docInfo = _docInfo.get(docletId);
    if (docInfo == null)
    {
      return null;
    }

    return docInfo.getClassDoc(resourceClass.getCanonicalName());
  }

  /**
   * Query Javadoc {@link MethodDoc} for the specified Java method.
   *
   * @param method Java method to be queried
   * @return corresponding {@link MethodDoc}
   */
  public static MethodDoc getMethodDoc(long docletId, Method method)
  {
    final DocInfo docInfo = _docInfo.get(docletId);
    if (docInfo == null)
    {
      return null;
    }

    final MethodIdentity methodId = MethodIdentity.create(method);
    return docInfo.getMethodDoc(methodId);
  }

  private static class DocInfo
  {
    public ClassDoc getClassDoc(String className)
    {
      return _classNameToClassDoc.get(className);
    }

    public MethodDoc getMethodDoc(MethodIdentity methodId)
    {
      return _methodIdToMethodDoc.get(methodId);
    }

    public void setClassDoc(String className, ClassDoc classDoc)
    {
      _classNameToClassDoc.put(className, classDoc);
    }

    public void setMethodDoc(MethodIdentity methodId, MethodDoc methodDoc)
    {
      _methodIdToMethodDoc.put(methodId, methodDoc);
    }

    private final Map<String, ClassDoc> _classNameToClassDoc = new HashMap<String, ClassDoc>();
    private final Map<MethodIdentity, MethodDoc> _methodIdToMethodDoc = new HashMap<MethodIdentity, MethodDoc>();
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

  private static final ConcurrentMap<Long, DocInfo> _docInfo = new ConcurrentHashMap<Long, DocInfo>();
  private static long _id = 0;
}
