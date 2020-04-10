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
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import static com.linkedin.pegasus.gradle.SharedFileUtils.getSuffixedFiles;


/**
 * Generate the data template source files from data schema files.
 *
 * To use this plugin, add these three lines to your build.gradle:
 * <pre>
 * apply plugin: 'li-pegasus2'
 * </pre>
 *
 * The plugin will scan the source set's pegasus directory, e.g. "src/main/pegasus"
 * for data schema (.pdsc) files.
 */
@CacheableTask
public class GenerateDataTemplateTask extends DefaultTask
{
  private File _destinationDir;
  private File _inputDir;
  private FileCollection _resolverPath;
  private FileCollection _codegenClasspath;
  private boolean _enableArgFile;

  public GenerateDataTemplateTask()
  {
  }

  /**
   * Directory containing the data schema files.
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

  /**
   * Classpath of the java process that generates data template.
   * The value will be automatically copied over to 'classpath' property.
   * It is kept here for backwards compatibility.
   */
  @Classpath
  public FileCollection getCodegenClasspath()
  {
    return _codegenClasspath;
  }

  public void setCodegenClasspath(FileCollection codegenClasspath)
  {
    _codegenClasspath = codegenClasspath;
  }

  /**
   * Directory to write the generated data template source files.
   */
  @OutputDirectory
  public File getDestinationDir()
  {
    return _destinationDir;
  }

  public void setDestinationDir(File destinationDir)
  {
    _destinationDir = destinationDir;
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
  public void generate()
  {
    FileTree inputDataSchemaFiles = getSuffixedFiles(getProject(), _inputDir,
        PegasusPlugin.DATA_TEMPLATE_FILE_SUFFIXES);

    List<String> inputDataSchemaFilenames = StreamSupport.stream(inputDataSchemaFiles.spliterator(), false)
        .map(File::getPath)
        .collect(Collectors.toList());

    if (inputDataSchemaFilenames.isEmpty())
    {
      getLogger().lifecycle("There are no data schema input files. Skip generating data template.");
      return;
    }

    getLogger().lifecycle("There are {} data schema input files. Using input root folder: {}",
        inputDataSchemaFilenames.size(), _inputDir);

    _destinationDir.mkdirs();

    String resolverPathStr = _resolverPath.plus(getProject().files(_inputDir)).getAsPath();

    FileCollection _pathedCodegenClasspath;
    try
    {
      _pathedCodegenClasspath = PathingJarUtil.generatePathingJar(
          getProject(), "generateDataTemplate", _codegenClasspath, true);
    }
    catch (IOException e)
    {
      throw new GradleException("Error occurred generating pathing JAR.", e);
    }

    getProject().javaexec(javaExecSpec ->
    {
      String resolverPathArg = resolverPathStr;
      if (isEnableArgFile())
      {
        resolverPathArg = ArgumentFileGenerator.getArgFileSyntax(ArgumentFileGenerator.createArgFile(
            "generateDataTemplate_resolverPath", Collections.singletonList(resolverPathArg), getTemporaryDir()));
      }

      javaExecSpec.setMain("com.linkedin.pegasus.generator.PegasusDataTemplateGenerator");
      javaExecSpec.setClasspath(_pathedCodegenClasspath);
      javaExecSpec.jvmArgs("-Dgenerator.resolver.path=" + resolverPathArg);
      javaExecSpec.jvmArgs("-Droot.path=" + getProject().getRootDir().getPath());
      javaExecSpec.args(_destinationDir.getPath());
      javaExecSpec.args(_inputDir);
    });
  }
}
