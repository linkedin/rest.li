/*
   Copyright (c) 2020 LinkedIn Corp.

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

import com.linkedin.pegasus.gradle.PathingJarUtil;
import com.linkedin.pegasus.gradle.PegasusPlugin;
import com.linkedin.pegasus.gradle.internal.ArgumentFileGenerator;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import static com.linkedin.pegasus.gradle.SharedFileUtils.*;

/**
 * Validate extension schemas.
 *
 * To use this plugin, add these three lines to your build.gradle:
 * <pre>
 * apply plugin: 'li-pegasus'
 * </pre>
 *
 * The plugin will scan the source set's pegasus directory, e.g. "src/main/extensions"
 * for extension schema (.pdl) files.
 */
@CacheableTask
public class ValidateExtensionSchemaTask extends DefaultTask
{
  private File _inputDir;
  private FileCollection _resolverPath;
  private FileCollection _classPath;
  private boolean _enableArgFile;

  /**
   * Directory containing the extension schema files.
   */
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

  /**
   * The resolver path.
   */
  @Classpath
  public FileCollection getResolverPath()
  {
    return _resolverPath;
  }

  public void setResolverPath(FileCollection resolverPath)
  {
    _resolverPath = resolverPath;
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

  @Input
  public boolean isEnableArgFile()
  {
    return _enableArgFile;
  }

  public void setEnableArgFile(boolean enable)
  {
    _enableArgFile = enable;
  }

  @TaskAction
  public void validateExtensionSchema() throws IOException
  {
    FileTree inputDataSchemaFiles = getSuffixedFiles(getProject(), _inputDir,
        PegasusPlugin.DATA_TEMPLATE_FILE_SUFFIXES);

    List<String> inputDataSchemaFilenames = StreamSupport.stream(inputDataSchemaFiles.spliterator(), false)
        .map(File::getPath)
        .collect(Collectors.toList());

    if (inputDataSchemaFilenames.isEmpty())
    {
      getLogger().lifecycle("There are no extension schema input files. Skip validating extension schema");
      return;
    }
    getProject().getLogger().info("Verifying extension schemas ...");

    String resolverPathStr = _resolverPath.plus(getProject().files(_inputDir)).getAsPath();

    FileCollection _pathedClasspath;
    try {
      _pathedClasspath = PathingJarUtil.generatePathingJar(getProject(), getName(),
          _classPath, false);
    }
    catch (IOException e) {
      throw new GradleException("Error occurred generating pathing JAR.", e);
    }

    getProject().javaexec(javaExecSpec -> {
      String resolverPathArg = resolverPathStr;
      if (isEnableArgFile())
      {
        resolverPathArg = ArgumentFileGenerator.getArgFileSyntax(ArgumentFileGenerator.createArgFile(
            "validateExtensionSchema_resolverPath", Collections.singletonList(resolverPathArg), getTemporaryDir()));
      }
      javaExecSpec.setMain("com.linkedin.restli.tools.data.ExtensionSchemaValidationCmdLineApp");
      javaExecSpec.setClasspath(_pathedClasspath);
      javaExecSpec.args(resolverPathArg);
      javaExecSpec.args(_inputDir.getAbsolutePath());
    });
  }
}
