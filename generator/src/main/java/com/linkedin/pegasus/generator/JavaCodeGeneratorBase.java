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
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DirectArrayTemplate;
import com.linkedin.data.template.DirectMapTemplate;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;
import com.linkedin.data.template.UnionTemplate;
import com.linkedin.data.template.WrappingArrayTemplate;
import com.linkedin.data.template.WrappingMapTemplate;

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

  protected static final String SUPER = "super";
  protected static final String THIS = "this";

  private static final Set<String> _reserved = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
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
  protected final JClass _customClass;
  protected final JClass _dataListClass;
  protected final JClass _dataMapClass;
  protected final JClass _dataTemplateUtilClass;
  protected final JClass _directArrayClass;
  protected final JClass _directMapClass;
  protected final JClass _getModeClass;
  protected final JClass _mapClass;
  protected final JClass _pathSpecClass;
  protected final JClass _setModeClass;
  protected final JClass _stringBuilderClass;
  protected final JClass _stringClass;

  protected final JClass _recordClass;
  protected final JClass _unionClass;
  protected final JClass _wrappingArrayClass;
  protected final JClass _wrappingMapClass;

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
    _customClass = getCodeModel().ref(Custom.class);
    _dataListClass = getCodeModel().ref(DataList.class);
    _dataMapClass = getCodeModel().ref(DataMap.class);
    _dataTemplateUtilClass = getCodeModel().ref(DataTemplateUtil.class);
    _directArrayClass = getCodeModel().ref(DirectArrayTemplate.class);
    _directMapClass = getCodeModel().ref(DirectMapTemplate.class);
    _getModeClass = getCodeModel().ref(GetMode.class);
    _mapClass = getCodeModel().ref(Map.class);
    _pathSpecClass = getCodeModel().ref(PathSpec.class);
    _setModeClass = getCodeModel().ref(SetMode.class);
    _stringBuilderClass = getCodeModel().ref(StringBuilder.class);
    _stringClass = getCodeModel().ref(String.class);

    _recordClass = getRecordClass();
    _unionClass = getUnionClass();
    _wrappingArrayClass = getWrappingArrayClass();
    _wrappingMapClass = getWrappingMapClass();

    _disallowNullSetMode = getCodeModel().ref(SetMode.class).staticRef("DISALLOW_NULL");
    _strictGetMode = getCodeModel().ref(GetMode.class).staticRef("STRICT");

    _package = getCodeModel()._package(defaultPackage == null ? "" : defaultPackage);
  }

  protected JClass getRecordClass()
  {
    return getCodeModel().ref(RecordTemplate.class);
  }

  protected JClass getUnionClass()
  {
    return getCodeModel().ref(UnionTemplate.class);
  }

  protected JClass getWrappingArrayClass()
  {
    return getCodeModel().ref(WrappingArrayTemplate.class);
  }

  protected JClass getWrappingMapClass()
  {
    return getCodeModel().ref(WrappingMapTemplate.class);
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
}
