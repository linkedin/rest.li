/*
 * Copyright (c) 2020 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.pegasus.gradle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/**
 * A utility for getting SchemaAnnotationHandler classes.
 */
public class SchemaAnnotationHandlerClassUtil
{
  private final static Logger LOGGER = Logging.getLogger(SchemaAnnotationHandlerClassUtil.class);
  private static final String SCHEMA_HANDLER_JAVA_ANNOTATION = "RestLiSchemaAnnotationHandler";
  private static final char FILE_SEPARATOR = File.separatorChar;
  private static final char UNIX_FILE_SEPARATOR = '/';
  private static final char PACKAGE_SEPARATOR = '.';
  private static final String CLASS_SUFFIX = ".class";
  private static ClassLoader _classLoader;

  /**
   * Based on the handlerJarPath, provide a list of schema annotation handler names.
   * @param handlerJarPath FileCollection
   * @param expectedHandlersNumber int
   * @param taskClassLoader ClassLoader
   * @return list of schema annotation handler names.
   * @throws IOException
   */
  public static List<String> getSchemaAnnotationHandlerClassNames(FileCollection handlerJarPath, int expectedHandlersNumber, ClassLoader taskClassLoader)
      throws IOException
  {
    List<URL> handlerJarPathUrls = new ArrayList<>();

    for (File f : handlerJarPath)
    {
      handlerJarPathUrls.add(f.toURI().toURL());
    }

    _classLoader = new URLClassLoader(handlerJarPathUrls.toArray(new URL[handlerJarPathUrls.size()]),
        taskClassLoader);

    LOGGER.info("search for schema annotation handlers...");

    List<String> foundClassNames = new ArrayList<>();
    Set<String> scannedClass = new HashSet<>(); // prevent duplicate scans
    for (File f : handlerJarPath)
    {
      scanHandlersInClassPathJar(f, foundClassNames, scannedClass);
    }

    // For now, every schema annotation handler should be in its own module
    // if the number of found handlers doesn't match number of configured modules, will throw exception
    if (foundClassNames.size() != expectedHandlersNumber)
    {
      String errorMsg = String.format("Encountered errors when searching for annotation handlers: total %s dependencies configured, but %s handlers found: [%s].",
          expectedHandlersNumber,
          foundClassNames.size(),
          String.join(",", foundClassNames));
      LOGGER.error(errorMsg);
      throw new GradleException("Failed during search for annotation handlers.");
    }

    LOGGER.info("Found Schema annotation processing handlers: " + Arrays.toString(foundClassNames.toArray()));

    return foundClassNames;
  }

  private static void scanHandlersInClassPathJar(File f, List<String> foundClasses, Set<String> scannedClass)
      throws IOException
  {
    if (!f.toURI().toString().toLowerCase().endsWith("jar"))
    {
      return;
    }

    try(
        InputStream in = f.toURI().toURL().openStream();
        JarInputStream jarIn = new JarInputStream(in);
    )
    {

      for (JarEntry e = jarIn.getNextJarEntry(); e != null; e = jarIn.getNextJarEntry())
      {
        if (!e.isDirectory() && !scannedClass.contains(e.getName()))
        {
          scannedClass.add(e.getName());
          checkHandlerAnnotation(toNativePath(e.getName()), foundClasses);
        }
        jarIn.closeEntry();
      }
    } catch (FileNotFoundException e)
    {
      // Skip if file not found
      LOGGER.error("Encountered unexpected File not found error", e);
    }
  }

  private static String toNativePath(final String path)
  {
    return path.replace(UNIX_FILE_SEPARATOR, FILE_SEPARATOR);
  }

  /**
   * This method will check the java class annotation on the class instantiated by a className;
   * It will search if that class has an annotation matching SCHEMA_HANDLER_JAVA_ANNOTATION,
   * if found, this class name will be added to "foundClasses" list.
   * if exceptions or errors detected during instantiation of the class, this method will return without doing anything.
   *
   * @param name the name of class to be search annotation from.
   * @param foundClasses a list of class names of the classes that contains SCHEMA_HANDLER_JAVA_ANNOTATION
   */
  private static void checkHandlerAnnotation(String name, List<String> foundClasses)
  {
    if (name.endsWith(CLASS_SUFFIX))
    {
      int end = name.lastIndexOf(CLASS_SUFFIX);
      String clazzPath = name.substring(0, end);
      String clazzName = pathToName(clazzPath);

      Class<?> clazz = null;
      try
      {
        clazz = classForName(clazzName);
      }
      catch (Exception | Error e)
      {
        LOGGER.info("During search for annotation handlers, encountered an unexpected exception or error [{}] when instantiating the class, " +
            "will skip checking this class: [{}]", e.getClass(), clazzName);
        LOGGER.debug("Unexpected exceptions or errors found during instantiating the class [{}], detailed error: ", clazzName, e);
        return;
      }
      for (Annotation a : clazz.getAnnotations())
      {
        if (a.annotationType().getName().contains(SCHEMA_HANDLER_JAVA_ANNOTATION))
        {
          foundClasses.add(clazzName);
          return;
        }
      }
    }
  }

  private static String pathToName(final String path)
  {
    return path.replace(FILE_SEPARATOR, PACKAGE_SEPARATOR);
  }

  public static Class<?> classForName(final String name) throws ClassNotFoundException
  {
    return Class.forName(name, false, _classLoader);
  }
}
