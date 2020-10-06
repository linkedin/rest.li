/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.pegasus.generator;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.SetMode;

import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JPackage;


/**
 * Base class for Java code generators. Host CodeModel and define Java specific language rules.
 *
 * @author Keren Jin
 */
public class JavaCodeGeneratorBase
{
  /**
   * Package to be used when a {@link NamedDataSchema} does not specify a namespace
   */
  public static final String GENERATOR_DEFAULT_PACKAGE = "generator.default.package";
  public static final String ROOT_PATH = "root.path";

  protected static final String SUPER = "super";
  protected static final String THIS = "this";

  private static final int MAX_STRING_LITERAL_LENGTH = 32000;

  private static final Set<String> _reserved = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
      "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
      "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
      "interface", "long", "native", "new", "null", "package", "private", "protected", "public",
      "return", "short", "static", "strictfp", "super", "switch", "synchronized",
      "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
  )));

  /**
   * Useful type references
   */
  protected final JClass _byteStringClass;
  protected final JClass _collectionClass;
  protected final JClass _arraysClass;
  protected final JClass _checkedUtilClass;
  protected final JClass _customClass;
  protected final JClass _dataListClass;
  protected final JClass _dataMapClass;
  protected final JClass _dataTemplateUtilClass;
  protected final JClass _getModeClass;
  protected final JClass _mapClass;
  protected final JClass _objectClass;
  protected final JClass _pathSpecClass;
  protected final JClass _setModeClass;
  protected final JClass _stringBuilderClass;
  protected final JClass _stringClass;

  protected final JFieldRef _disallowNullSetMode;
  protected final JFieldRef _strictGetMode;
  /**
   * The main code generator
   */
  private final JCodeModel _codeModel = new JCodeModel();
  /**
   * Package for generated classes
   */
  private JPackage _package;

  public JavaCodeGeneratorBase(String defaultPackage)
  {
    _byteStringClass = getCodeModel().ref(ByteString.class);
    _collectionClass = getCodeModel().ref(Collection.class);
    _arraysClass = getCodeModel().ref(Arrays.class);
    _checkedUtilClass = getCodeModel().ref(CheckedUtil.class);
    _customClass = getCodeModel().ref(Custom.class);
    _dataListClass = getCodeModel().ref(DataList.class);
    _dataMapClass = getCodeModel().ref(DataMap.class);
    _dataTemplateUtilClass = getCodeModel().ref(DataTemplateUtil.class);
    _getModeClass = getCodeModel().ref(GetMode.class);
    _mapClass = getCodeModel().ref(Map.class);
    _objectClass = getCodeModel().ref(Object.class);
    _pathSpecClass = getCodeModel().ref(PathSpec.class);
    _setModeClass = getCodeModel().ref(SetMode.class);
    _stringBuilderClass = getCodeModel().ref(StringBuilder.class);
    _stringClass = getCodeModel().ref(String.class);

    _disallowNullSetMode = getCodeModel().ref(SetMode.class).staticRef("DISALLOW_NULL");
    _strictGetMode = getCodeModel().ref(GetMode.class).staticRef("STRICT");

    _package = getCodeModel()._package(defaultPackage == null ? "" : defaultPackage);
  }

  protected static boolean isReserved(String name)
  {
    return _reserved.contains(name);
  }

  protected static String escapeReserved(String name)
  {
    if (_reserved.contains(name))
    {
      return name + '_';
    }
    return name;
  }

  /**
   * The main code generator
   */
  public JCodeModel getCodeModel()
  {
    return _codeModel;
  }

  protected JPackage getPackage()
  {
    return _package;
  }

  protected JPackage getPackage(String namespace)
  {
    return namespace.isEmpty() ? getPackage() : _codeModel._package(namespace);
  }

  /**
   * Generates an expression that's semantically equivalent to a string literal, yet avoids generating string literals
   * that exceed some predefined size bound. This is needed to ensure compiler string literal size limits are not hit.
   *
   * @param text string literal text
   * @return expression which is semantically equivalent to a string literal
   */
  protected JExpression getSizeBoundStringLiteral(String text)
  {
    if (text.length() < MAX_STRING_LITERAL_LENGTH)
    {
      return JExpr.lit(text);
    }
    else
    {
      JInvocation stringBuilderInvocation = JExpr._new(_stringBuilderClass);
      for (int index = 0; index < text.length(); index += MAX_STRING_LITERAL_LENGTH)
      {
        stringBuilderInvocation = stringBuilderInvocation.
            invoke("append").
            arg(text.substring(index, Math.min(text.length(), index + MAX_STRING_LITERAL_LENGTH)));
      }
      return stringBuilderInvocation.invoke("toString");
    }
  }
}
