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

package com.linkedin.pegasus.generator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.Generated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.data.schema.generator.AbstractGenerator;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;


/**
 * Generic abstract code generator.
 *
 * @author Eran Leshem
 */
public abstract class CodeGenerator extends AbstractGenerator
{
  private static final Logger _log = LoggerFactory.getLogger(CodeGenerator.class);

  /**
   * The main code generator
   */
  private final JCodeModel _codeModel = new JCodeModel();

  private Set<String> _reserved = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
    "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
    "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
    "interface", "long", "native", "new", "null", "package", "private", "protected", "public",
    "return", "short", "static", "strictfp", "super", "switch", "synchronized",
    "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
  )));

  /**
   * Package for generated classes
   */
  private JPackage _package;

  protected void annotate(JDefinedClass cls, String classType, String location)
  {
    JAnnotationUse generatedAnnotation = cls.annotate(Generated.class);
    generatedAnnotation.param("value", getClass().getName());
    String comments = "LinkedIn " + classType;

    if (location != null)
    {
      comments += ". Generated from " + location + '.';
    }

    generatedAnnotation.param("comments", comments);
    generatedAnnotation.param("date", new Date().toString());
  }

  protected static String capitalize(String name)
  {
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }

  protected void initializeDefaultPackage()
  {
    String defaultPackage = System.getProperty("generator.default.package");
    if (defaultPackage == null)
    {
      defaultPackage = "";
    }
    setPackage(getCodeModel()._package(defaultPackage));
  }

  protected void setPackage(JPackage aPackage)
  {
    _package = aPackage;
  }

  protected JPackage getPackage()
  {
    return _package;
  }

  protected JPackage getPackage(String namespace)
  {
    return namespace.isEmpty() ? getPackage() : _codeModel._package(namespace);
  }

  protected String getGetterName(JType type, String capitalizedName)
  {
    String getterPrefix = type.unboxify() == _codeModel.BOOLEAN ? "is" : "get";
    return getterPrefix + capitalizedName;
  }


  /**
   * Temporary logging facility, until we figure out the gradle-log4j combination
   */
  protected void log(String message)
  {
    _log.info(message);
    System.out.println(message);
  }

  protected boolean isReserved(String name)
  {
    return _reserved.contains(name);
  }

  protected String escapeReserved(String name)
  {
    if (_reserved.contains(name))
    {
      return name + '_';
    }
    return name;
  }

  protected ClassLoader getClassLoader()
  {
    String path = getResolverPath();
    ClassLoader classLoader;
    if (path == null)
    {
      classLoader = getClass().getClassLoader();
    }
    else
    {
      List<URL> list = new ArrayList<URL>();
      StringTokenizer tokenizer = new StringTokenizer(path, File.pathSeparator);
      while (tokenizer.hasMoreTokens())
      {
        String s = tokenizer.nextToken();
        File file = new File(s);
        if (file.exists() == false || file.canRead() == false)
        {
          _log.info("Path " + file + " does not exist or is not readable");
          continue;
        }
        URI uri = file.toURI();
        try
        {
          list.add(uri.toURL());
        }
        catch (MalformedURLException e)
        {
          throw new IllegalStateException("URI " + uri + " derived from " + file + " should never be malformed");
        }
      }
      classLoader = URLClassLoader.newInstance(list.toArray(new URL[0]), getClass().getClassLoader());
    }
    return classLoader;
  }

  protected List<File> targetFiles(File targetDirectory)
  {
    ClassLoader classLoader = getClassLoader();

    List<File> generatedFiles = new ArrayList<File>();

    for (Iterator<JPackage> packageIterator = _codeModel.packages(); packageIterator.hasNext();)
    {
      for (Iterator<JDefinedClass> classIterator = packageIterator.next().classes(); classIterator.hasNext();)
      {
        JDefinedClass definedClass = classIterator.next();
        boolean classFound;
        try
        {
          Class<?> clazz = classLoader.loadClass(definedClass.fullName());
          classFound = true;
        }
        catch (ClassNotFoundException e)
        {
          classFound = false;
        }
        if (classFound)
        {
          _log.debug(definedClass.fullName() + " found in resolver path");
          definedClass.hide();
        }
        else if (hideClass(definedClass))
        {
          definedClass.hide();
        }
        else if (definedClass.outer() == null)
        {
          File file = new File(targetDirectory, definedClass.fullName().replace('.', File.separatorChar) + ".java");
          generatedFiles.add(file);
        }
      }
    }

    return generatedFiles;
  }

  /**
   * Determine whether a class defined by the generator should be hidden.
   * Hiding a class suppresses generation of the corresponding .java file.
   * Subclasses should override this method to customize the set of generated classes.
   *
   * @param clazz The class under consideration
   * @return true if the class should be hidden
   */
  protected boolean hideClass(JDefinedClass clazz)
  {
    return false;
  }

  /**
   * The main code generator
   */
  protected JCodeModel getCodeModel()
  {
    return _codeModel;
  }
}
