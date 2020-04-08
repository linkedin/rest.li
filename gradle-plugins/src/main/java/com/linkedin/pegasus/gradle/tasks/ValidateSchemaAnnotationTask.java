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

import com.linkedin.pegasus.gradle.internal.ArgumentFileGenerator;
import com.linkedin.pegasus.gradle.SchemaAnnotationHandlerUtil;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
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
  private boolean _enableArgFile;

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

    int expectedHandlersNumber = ((DefaultConfiguration) _handlerJarPath).getAllDependencies().size();
    // skip if no handlers configured
    if (expectedHandlersNumber == 0)
    {
      getProject().getLogger()
                  .info("no schema annotation handlers configured, will skip schema annotation validation.");
      return;
    }
    List<String> foundClassNames = SchemaAnnotationHandlerUtil.getSchemaAnnotationHandlerClassNames(_handlerJarPath, expectedHandlersNumber, getClass().getClassLoader());

    getProject().javaexec(javaExecSpec ->
                          {
                            String resolverPathArg = _resolverPath.getAsPath();
                            if (isEnableArgFile())
                            {
                              resolverPathArg = ArgumentFileGenerator.getArgFileSyntax(ArgumentFileGenerator.createArgFile(
                                  "validateSchemaAnnotation_resolverPath", Collections.singletonList(resolverPathArg), getTemporaryDir()));
                            }
                            javaExecSpec.setMain(
                                "com.linkedin.restli.tools.annotation.SchemaAnnotationValidatorCmdLineApp");
                            javaExecSpec.setClasspath(_classPath);
                            javaExecSpec.args(_inputDir.getAbsolutePath());
                            javaExecSpec.args("--handler-jarpath");
                            javaExecSpec.args(_handlerJarPath.getAsPath());
                            javaExecSpec.args("--handler-classnames");
                            javaExecSpec.args(String.join(File.pathSeparator, foundClassNames));
                            javaExecSpec.args("--resolverPath");
                            javaExecSpec.args(resolverPathArg);
                          });
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

  @Input
  public boolean isEnableArgFile()
  {
    return _enableArgFile;
  }

  public void setEnableArgFile(boolean enable)
  {
    _enableArgFile = enable;
  }
}
