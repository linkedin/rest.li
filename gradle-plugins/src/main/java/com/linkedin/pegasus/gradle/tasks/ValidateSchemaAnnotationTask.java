/*
   Copyright (c) 2019 LinkedIn Corp.

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
package com.linkedin.pegasus.gradle.tasks;

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
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;


/**
 * This task triggers {@link com.linkedin.restli.tools.annotation.SchemaAnnotationValidatorCmdLineApp} to validate Schema Annotation
 *
 * This task will fail if exception was thrown during validation, or validation result returned by the SchemaProcessor is not successful
 *
 * This task will be generated and added to gradle build task by {@link com.linkedin.pegasus.gradle.PegasusPlugin}
 *
 * This task depends on GenerateDataTemplateTask to get correct paths and dependencies. In case some other plugins modify its value
 * so GenerateDataTemplateTask needs to be passed and the parameters are read during runtime.
 *
 * This task is cacheable because GenerateDataTemplateTask is cacheable and other inputs fixed.
 *
 *
 */
@CacheableTask
public class ValidateSchemaAnnotationTask extends DefaultTask
{
  public static final String SCHEMA_HANDLER_JAVA_ANNOTATION = "RestLiSchemaAnnotationHandler";
  private static final char FILE_SEPARATOR = File.separatorChar;
  private static final String DEFAULT_PATH_SEPARATOR = File.pathSeparator;
  private static final char UNIX_FILE_SEPARATOR = '/';
  private static final char PACKAGE_SEPARATOR = '.';
  private static final String CLASS_SUFFIX = ".class";
  private ClassLoader _classLoader;
  /**
   * Pass in parameters:
   * _inputDir: Directory containing the data schema file. This value can be got from GenerateDataTemplateTask
   * _resolverPath: resolver path for parsing schema.  This value can be got from GenerateDataTemplateTask
   * _classPath: classPath used for triggering the command line tool
   * _handlerJarPath: jar paths which contains the handlers
   *
   */
  private FileCollection _classPath;
  private FileCollection _handlerJarPath;

  // Below fields needs to retrieve from GenerateDataTemplateTask
  // models location
  private File _inputDir;
  // resolver path to parse the models
  private FileCollection _resolverPath;

  @TaskAction
  public void validateSchemaAnnotation() throws IOException
  {
    // need to Update resolver path
    _resolverPath = _resolverPath.plus(getProject().files(_inputDir));

    getProject().getLogger().info("started schema annotation validation");

    List<URL> handlerJarPathUrls = new ArrayList<>();

    for (File f : _handlerJarPath)
    {
      handlerJarPathUrls.add(f.toURI().toURL());
    }

    _classLoader = new URLClassLoader(handlerJarPathUrls.toArray(new URL[handlerJarPathUrls.size()]),
                                      getClass().getClassLoader());

    getProject().getLogger().info("search for schema annotation handlers...");

    List<String> foundClassNames = new ArrayList<>();
    Set<String> scannedClass = new HashSet<>(); // prevent duplicate scans
    for (File f : _handlerJarPath)
    {
      scanHandlersInClassPathJar(f, foundClassNames, scannedClass);
    }

    // skip if no handlers configured or found
    if (handlerJarPathUrls.size() == 0 || foundClassNames.size() == 0)
    {
      getProject().getLogger()
                  .info("no schema annotation handlers configured or found, will skip schema annotation validation.");
      return;
    }

    getProject().getLogger()
                .info("Found Schema annotation processing handlers: " + Arrays.toString(foundClassNames.toArray()));

    getProject().javaexec(javaExecSpec ->
                          {
                            javaExecSpec.setMain(
                                "com.linkedin.restli.tools.annotation.SchemaAnnotationValidatorCmdLineApp");
                            javaExecSpec.setClasspath(_classPath);
                            javaExecSpec.args(_inputDir.getAbsolutePath());
                            javaExecSpec.args("--handler-jarpath");
                            javaExecSpec.args(_handlerJarPath.getAsPath());
                            javaExecSpec.args("--handler-classnames");
                            javaExecSpec.args(String.join(DEFAULT_PATH_SEPARATOR, foundClassNames));
                            javaExecSpec.args("--resolverPath");
                            javaExecSpec.args(_resolverPath.getAsPath());
                          });
  }

  private void scanHandlersInClassPathJar(File f, List<String> foundClasses, Set<String> scannedClass)
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
      getProject().getLogger().error("Encountered unexpected File not found error", e);
    }
  }

  private String toNativePath(final String path)
  {
    return path.replace(UNIX_FILE_SEPARATOR, FILE_SEPARATOR);
  }

  /**
   * This method will check the java class annotation on the class instantiated by a className;
   * It will search if that class has an annotation matching {@link SCHEMA_HANDLER_JAVA_ANNOTATION},
   * if found, this class name will be added to "foundClasses" list.
   *
   * @param name the name of class to be search annotation from.
   * @param foundClasses a list of class names of the classes that contains {@link SCHEMA_HANDLER_JAVA_ANNOTATION}
   * @return whether this
   * @throws IOException
   */
  private void checkHandlerAnnotation(String name, List<String> foundClasses) throws IOException
  {
    if (name.endsWith(CLASS_SUFFIX))
    {
      int end = name.lastIndexOf(CLASS_SUFFIX);
      String clazzPath = name.substring(0, end);
      String clazzName = pathToName(clazzPath);

      try
      {
        Class<?> clazz = classForName(clazzName);
        for (Annotation a : clazz.getAnnotations())
        {
          if (a.annotationType().getName().contains(SCHEMA_HANDLER_JAVA_ANNOTATION))
          {
            foundClasses.add(clazzName);
            return;
          }
        }
      }
      catch (ClassNotFoundException e)
      {
        throw new IOException("Failed to load class while scanning classes", e);
      }
      catch (Exception | Error e)
      {
        getProject().getLogger()
                    .info("Unexpected exceptions or errors found during scanning class, ok to skip", e);
      }
    }
  }

  private String pathToName(final String path)
  {
    return path.replace(FILE_SEPARATOR, PACKAGE_SEPARATOR);
  }

  private String toUnixPath(final String path)
  {
    return path.replace(FILE_SEPARATOR, UNIX_FILE_SEPARATOR);
  }

  public Class<?> classForName(final String name) throws ClassNotFoundException
  {
    return Class.forName(name, false, _classLoader);
  }

  @Classpath
  public FileCollection getClassPath()
  {
    return _classPath;
  }

  public void setClassPath(FileCollection classPath)
  {
    _classPath = classPath;
  }

  @Classpath
  public FileCollection getHandlerJarPath()
  {
    return _handlerJarPath;
  }

  public void setHandlerJarPath(FileCollection handlerJarPath)
  {
    _handlerJarPath = handlerJarPath;
  }

  @InputDirectory
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getInputDir()
  {
    return _inputDir;
  }

  public void setInputDir(File inputDir)
  {
    _inputDir = inputDir;
  }

  @Classpath
  public FileCollection getResolverPath()
  {
    return _resolverPath;
  }

  public void setResolverPath(FileCollection resolverPath)
  {
    _resolverPath = resolverPath;
  }

}
