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
package com.linkedin.pegasus.gradle.tasks;


import com.linkedin.pegasus.gradle.PathingJarUtil;
import com.linkedin.pegasus.gradle.SchemaAnnotationHandlerClassUtil;
import com.linkedin.pegasus.gradle.internal.CompatibilityLogChecker;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
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


@CacheableTask
public class CheckPegasusSnapshotTask extends DefaultTask
{
  private File _currentSnapshotDirectory;
  private File _previousSnapshotDirectory;
  private String _compatibilityLevel;
  private FileCollection _codegenClasspath;
  private String _compatibilityMode;
  private FileCollection _handlerJarPath;
  private List<String> _handlerClassNames;
  private boolean _isExtensionSchema = false;
  private static String PEGASUS_SCHEMA_COMPATIBILITY_SUMMARY_FILE = "reports/checkPegasusSchema/compatibilityReport.txt";
  private static String PEGASUS_EXTENSION_SCHEMA_COMPATIBILITY_SUMMARY_FILE = "reports/checkPegasusExtensionSchema/compatibilityReport.txt";

  @TaskAction
  public void checkPegasusSnapshot()
  {
    getLogger().info("Checking pegasus schema compatibility ...");

    FileCollection pathedCodegenClasspath;
    try
    {
      pathedCodegenClasspath = PathingJarUtil.generatePathingJar(getProject(), getName(),
          _codegenClasspath, false);
    }
    catch (IOException e)
    {
      throw new GradleException("Error occurred generating pathing JAR.", e);
    }

    File reportOutput = new File(getProject().getBuildDir(), _isExtensionSchema ? PEGASUS_EXTENSION_SCHEMA_COMPATIBILITY_SUMMARY_FILE :
        PEGASUS_SCHEMA_COMPATIBILITY_SUMMARY_FILE);
    reportOutput.getParentFile().mkdirs();

    getProject().javaexec(javaExecSpec ->
    {
      javaExecSpec.setMain("com.linkedin.restli.tools.snapshot.check.PegasusSchemaSnapshotCompatibilityChecker");
      javaExecSpec.setClasspath(pathedCodegenClasspath);
      javaExecSpec.args("--compatLevel", _compatibilityLevel);
      javaExecSpec.args("--compatMode", _compatibilityMode);
      javaExecSpec.args("--report", reportOutput);
      javaExecSpec.args(_previousSnapshotDirectory);
      javaExecSpec.args(_currentSnapshotDirectory);
      if (_isExtensionSchema)
      {
        javaExecSpec.args("--extensionSchema");
      }
      else if(hasSchemaAnnotationHandler())
      {
        javaExecSpec.args("--handlerJarPath", _handlerJarPath);
        javaExecSpec.args("--handlerClassName", String.join(File.pathSeparator, _handlerClassNames));
      }
    });

    CompatibilityLogChecker logChecker = new CompatibilityLogChecker();
    try
    {
      logChecker.write(Files.readAllBytes(reportOutput.toPath()));
    }
    catch (IOException e)
    {
      throw new GradleException("Error while processing compatibility report: " + e.getMessage());
    }
    if (!logChecker.isModelCompatible() || !logChecker.isAnnotationCompatible())
    {
      throw new GradleException("There are incompatible changes, find details in " + reportOutput.getAbsolutePath());
    }
  }

  @InputDirectory
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getCurrentSnapshotDirectory()
  {
    return _currentSnapshotDirectory;
  }

  public void setCurrentSnapshotDirectory(File currentSnapshotDirectory)
  {
    _currentSnapshotDirectory = currentSnapshotDirectory;
  }

  @InputDirectory
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getPreviousSnapshotDirectory()
  {
    return _previousSnapshotDirectory;
  }

  public void setPreviousSnapshotDirectory(File previousSnapshotDirectory)
  {
    _previousSnapshotDirectory = previousSnapshotDirectory;
  }

  @Input
  public String getCompatibilityLevel()
  {
    return _compatibilityLevel;
  }

  public void setCompatibilityLevel(String compatibilityLevel)
  {
    _compatibilityLevel = compatibilityLevel;
  }

  @Input
  public String getCompatibilityMode()
  {
    return _compatibilityMode;
  }

  public void setCompatibilityMode(String compatibilityMode)
  {
    _compatibilityMode = compatibilityMode;
  }

  @Classpath
  public FileCollection getCodegenClasspath()
  {
    return _codegenClasspath;
  }

  public void setCodegenClasspath(FileCollection codegenClasspath)
  {
    _codegenClasspath = codegenClasspath;
  }

  @Input
  public boolean isExtensionSchema()
  {
    return _isExtensionSchema;
  }

  public void setExtensionSchema(boolean isExtensionSchema)
  {
    _isExtensionSchema = isExtensionSchema;
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

  private boolean hasSchemaAnnotationHandler()
  {
    int expectedHandlersNumber = ((DefaultConfiguration) _handlerJarPath).getAllDependencies().size();
    // skip if no handlers configured
    if (expectedHandlersNumber == 0)
    {
      getLogger().info("no schema annotation handlers configured for schema annotation compatibility check");
      return false;
    }

    List<URL> handlerJarPathUrls = SchemaAnnotationHandlerClassUtil.getAnnotationHandlerJarPathUrls(_handlerJarPath);

    ClassLoader classLoader = new URLClassLoader(handlerJarPathUrls.toArray(new URL[handlerJarPathUrls.size()]), getClass().getClassLoader());

    try
    {
      _handlerClassNames = SchemaAnnotationHandlerClassUtil.getAnnotationHandlerClassNames(_handlerJarPath, classLoader, getProject());
    }
    catch (IOException e)
    {
      throw new GradleException("Annotation compatibility check: could not get annotation handler class name. " + e.getMessage());
    }

    SchemaAnnotationHandlerClassUtil.checkAnnotationClassNumber(_handlerClassNames, expectedHandlersNumber);
    return true;
  }
}
