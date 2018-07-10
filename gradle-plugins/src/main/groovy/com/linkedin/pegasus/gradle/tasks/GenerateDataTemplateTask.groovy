package com.linkedin.pegasus.gradle.tasks


import com.linkedin.pegasus.gradle.PegasusPlugin
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

import static com.linkedin.pegasus.gradle.SharedFileUtils.getSuffixedFiles


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
@CompileStatic
class GenerateDataTemplateTask extends JavaExec
{
  /**
   * Directory to write the generated data template source files.
   */
  @OutputDirectory
  File destinationDir

  /**
   * Directory containing the data schema files.
   */
  @InputDirectory
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  File inputDir

  /**
   * The resolver path.
   */
  @Classpath
  FileCollection resolverPath

  /**
   * Classpath of the java process that generates data template.
   * The value will be automatically copied over to 'classpath' property.
   * It is kept here for backwards compatibility.
   */
  @Classpath
  FileCollection codegenClasspath

  GenerateDataTemplateTask()
  {
    //We need config task to add robustness and configurability to the JavaExec task such as this one.
    //Config task is not typical but it is one of the fundamental design patterns in Gradle.
    //We need config task here because the inputs to the JavaExec task such as 'jvmArgs'
    // need to be set during Gradle's execution time because they require dependencies to be resolved/downloaded.
    //'jvmArgs' value is driven from 'resolverPath' input property which should evaluated during execution time.
    def configTask = project.tasks.create("${name}Configuration", GenerateDataTemplateConfigurerTask.class)
    configTask.description = "Configures $name task and always run before that task."
    configTask.target = this
    this.dependsOn configTask
  }

  /**
   * Configures data template generation task.
   * Should never be incremental because it is a 'config task' for target task.
   * The target task will be neatly incremental.
   */
  static class GenerateDataTemplateConfigurerTask extends DefaultTask
  {

    private final static Logger LOG = Logging.getLogger(GenerateDataTemplateConfigurerTask)

    protected GenerateDataTemplateTask target

    @TaskAction configureTask()
    {
      final FileTree inputDataSchemaFiles = getSuffixedFiles(target.project, target.inputDir,
          PegasusPlugin.DATA_TEMPLATE_FILE_SUFFIXES)
      final String[] inputDataSchemaFilenames = inputDataSchemaFiles.collect { File it -> it.path } as String[]
      if (inputDataSchemaFilenames.length == 0)
      {
        LOG.info(
            "{} - disabling task {} because no input data data schema files found in ${target.inputDir}", getPath(), target.getPath())
        target.enabled = false;
      }
      else
      {
        LOG.lifecycle(
            "There are ${inputDataSchemaFilenames.length} data schema input files. Using input root folder: ${target.inputDir}")
      }

      final String resolverPathStr = (target.resolverPath + project.files(target.inputDir)).asPath

      target.main = 'com.linkedin.pegasus.generator.PegasusDataTemplateGenerator'
      target.classpath target.codegenClasspath //needed for backwards compatibility, we need to keep the existing API (codegenClasspath method)
      target.jvmArgs '-Dgenerator.resolver.path=' + resolverPathStr
      target.args target.destinationDir.path
      target.args target.inputDir

      LOG.info("""$path - finished configuring task $target.path:
  - main: $target.main
  - classpath: $target.classpath
  - jvmArgs: $target.jvmArgs
  - args: $target.args""")
    }
  }
}
