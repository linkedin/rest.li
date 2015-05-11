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


import javax.annotation.Generated;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Java specific utility functions for data template code generation.
 *
 * @author Keren Jin
 */
public class JavaCodeUtil
{
  private static final Logger _log = LoggerFactory.getLogger(JavaCodeUtil.class);

  public interface PersistentClassChecker
  {
    boolean isPersistent(JDefinedClass clazz);
  }

  /**
   * Create Java {@link Generated} annotation for a class.
   *
   * @param cls CodeModel class to annotate
   * @param classType type of the specified class
   * @param location location of where the specified class is generated from
   */
  public static void annotate(JDefinedClass cls, String classType, String location)
  {
    final JAnnotationUse generatedAnnotation = cls.annotate(Generated.class);
    generatedAnnotation.param("value", JavaCodeUtil.class.getName());
    String comments = "Rest.li " + classType;

    if (location != null)
    {
      comments += ". Generated from " + location + '.';
    }

    generatedAnnotation.param("comments", comments);
    generatedAnnotation.param("date", new Date().toString());
  }

  /**
   * Create getter function name for a variable.
   *
   * @param codeModel {@link JCodeModel} instance
   * @param type CodeModel type of the variable
   * @param capitalizedName name of the variable
   * @return getter function name
   */
  public static String getGetterName(JCodeModel codeModel, JType type, String capitalizedName)
  {
    final String getterPrefix = type.unboxify() == codeModel.BOOLEAN ? "is" : "get";
    return getterPrefix + capitalizedName;
  }

  /**
   * Build the list of files need to be written from CodeModel, with the targetDirectory as base directory.
   *
   * @param targetDirectory directory for the target files
   * @param codeModel {@link JCodeModel} instance
   * @param classLoader Java {@link ClassLoader} to check if a class for the potential target file already exist
   * @param checker custom closure to check if a class should be persistent
   * @return target files to be written
   */
  public static List<File> targetFiles(File targetDirectory, JCodeModel codeModel, ClassLoader classLoader, PersistentClassChecker checker)
  {
    final List<File> generatedFiles = new ArrayList<File>();

    for (Iterator<JPackage> packageIterator = codeModel.packages(); packageIterator.hasNext(); )
    {
      for (Iterator<JDefinedClass> classIterator = packageIterator.next().classes(); classIterator.hasNext(); )
      {
        final JDefinedClass definedClass = classIterator.next();
        boolean classFound;
        try
        {
          final Class<?> clazz = classLoader.loadClass(definedClass.fullName());
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
        else if (!checker.isPersistent(definedClass))
        {
          definedClass.hide();
        }
        else if (definedClass.outer() == null)
        {
          final File file = new File(targetDirectory, definedClass.fullName().replace('.', File.separatorChar) + ".java");
          generatedFiles.add(file);
        }
      }
    }

    return generatedFiles;
  }

  public static ClassLoader classLoaderFromResolverPath(String resolverPath)
  {
    final ClassLoader classLoader;
    if (resolverPath == null)
    {
      classLoader = JavaCodeUtil.class.getClassLoader();
    }
    else
    {
      final List<URL> list = new ArrayList<URL>();
      final StringTokenizer tokenizer = new StringTokenizer(resolverPath, File.pathSeparator);
      while (tokenizer.hasMoreTokens())
      {
        final String s = tokenizer.nextToken();
        final File file = new File(s);
        if (file.exists() == false || file.canRead() == false)
        {
          _log.info("Path " + file + " does not exist or is not readable");
          continue;
        }
        final URI uri = file.toURI();
        try
        {
          list.add(uri.toURL());
        }
        catch (MalformedURLException e)
        {
          throw new IllegalStateException("URI " + uri + " derived from " + file + " should never be malformed");
        }
      }
      classLoader = URLClassLoader.newInstance(list.toArray(new URL[0]), JavaCodeUtil.class.getClassLoader());
    }
    return classLoader;
  }
}
