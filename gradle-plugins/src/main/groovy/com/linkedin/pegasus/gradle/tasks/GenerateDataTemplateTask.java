package com.linkedin.pegasus.gradle.tasks;

import com.linkedin.pegasus.gradle.PegasusPlugin;
import com.linkedin.pegasus.gradle.internal.DataTemplateArgumentProvider;
import com.linkedin.pegasus.gradle.internal.DataTemplateJvmArgumentProvider;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GradleVersion;

import static com.linkedin.pegasus.gradle.SharedFileUtils.getSuffixedFiles;


/**
 * Generate the data template source files from data schema files.
 * Adding this task automatically adds a corresponding 'config task' that will be responsible for configuring this task.
 * Config task approach is needed to keep this task neatly incremental
 * while offering the configurability and robustness of {@link JavaExec} API.
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
public class GenerateDataTemplateTask extends JavaExec
{
  private File _destinationDir;
  private File _inputDir;
  private FileCollection _resolverPath;
  private FileCollection _codegenClasspath;

  public GenerateDataTemplateTask()
  {
    //We need config task to add robustness and configurability to the JavaExec task such as this one.
    //Config task is not typical but it is one of the fundamental design patterns in Gradle.
    //We need config task here because the inputs to the JavaExec task such as 'jvmArgs'
    // need to be set during Gradle's execution time because they require dependencies to be resolved/downloaded.
    //'jvmArgs' value is driven from 'resolverPath' input property which should evaluated during execution time.
    GenerateDataTemplateConfigurerTask configTask = getProject().getTasks()
        .create(getName() + "Configuration", GenerateDataTemplateConfigurerTask.class);

    configTask.setDescription("Configures " + getName() + " task and always runs before that task.");
    configTask.setTarget(this);

    dependsOn(configTask);
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

  /**
   * Configures data template generation task.
   * Should never be incremental because it is a 'config task' for target task.
   * The target task will be neatly incremental.
   */
  public static class GenerateDataTemplateConfigurerTask extends DefaultTask
  {
    private static final Logger LOG = Logging.getLogger(GenerateDataTemplateConfigurerTask.class);

    private GenerateDataTemplateTask _target;

    @TaskAction
    public void configureTask()
    {
      FileTree inputDataSchemaFiles = getSuffixedFiles(_target.getProject(), _target.getInputDir(),
          PegasusPlugin.DATA_TEMPLATE_FILE_SUFFIXES);

      List<String> inputDataSchemaFilenames = StreamSupport.stream(inputDataSchemaFiles.spliterator(), false)
          .map(File::getPath)
          .collect(Collectors.toList());

      if (inputDataSchemaFilenames.isEmpty())
      {
        LOG.info("{} - disabling task {} because no input data data schema files found in {}",
            getPath(), _target.getPath(), _target.getInputDir());
        _target.setEnabled(false);
      }
      else
      {
        LOG.lifecycle("There are {} data schema input files. Using input root folder: {}",
            inputDataSchemaFilenames.size(), _target.getInputDir());
      }

      String resolverPathStr = _target.getResolverPath().plus(getProject().files(_target.getInputDir())).getAsPath();

      _target.setMain("com.linkedin.pegasus.generator.PegasusDataTemplateGenerator");

      //needed for backwards compatibility, we need to keep the existing API (codegenClasspath method)
      _target.setClasspath(_target.getCodegenClasspath());

      if (GradleVersion.current().compareTo(GradleVersion.version("4.6")) < 0)
      {
        //Maintain backwards compatibility
        _target.jvmArgs("-Dgenerator.resolver.path=" + resolverPathStr);
        _target.jvmArgs("-Droot.path=" + getProject().getRootDir().getPath());
        _target.args(_target.getDestinationDir().getPath());
        _target.args(_target.getInputDir());
      }
      else
      {
        //Use the new provider API to better leverage the build cache
        _target.getJvmArgumentProviders().add(new DataTemplateJvmArgumentProvider(
            resolverPathStr, getProject().getRootDir()));

        _target.getArgumentProviders().add(new DataTemplateArgumentProvider(
            Arrays.asList(_target.getDestinationDir().getPath(), _target.getInputDir().getPath())));
      }

      LOG.info("{} - finished configuring task {}: \n"
          + "  - main: {}\n"
          + "  - classpath: {}\n"
          + "  - jvmArgs: {}\n"
          + "  - args: {}",
          getPath(), _target.getPath(),
          _target.getMain(),
          _target.getClasspath(),
          _target.getJvmArgs(),
          _target.getArgs());
    }

    @Internal
    public GenerateDataTemplateTask getTarget()
    {
      return _target;
    }

    public void setTarget(GenerateDataTemplateTask target)
    {
      _target = target;
    }
  }
}
