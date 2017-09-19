package com.linkedin.pegasus.gradle.tasks


import com.linkedin.pegasus.gradle.PathingJarUtil
import com.linkedin.pegasus.gradle.PegasusOptions
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.JavaExecAction


/**
 * Generate the idl file from the annotated java classes. This also requires access to the
 * classes that were used to compile these java classes.
 * Projects with no IdlItem will be excluded from this task
 *
 * As prerequisite of this task, add these lines to your build.gradle:
 * <pre>
 * apply plugin: 'li-pegasus2'
 * </pre>
 *
 * Optionally, to generate idl for specific packages, add
 * <pre>
 * pegasus.&lt;sourceSet&gt;.idlOptions.addIdlItem(['&lt;packageName&gt;'])
 * </pre>
 */
@CacheableTask
class GenerateRestModelTask extends DefaultTask
{
  @Classpath
  FileCollection watchedCodegenClasspath

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  Set<File> watchedInputDirs

  @Classpath
  FileCollection resolverPath

  @OutputDirectory
  @PathSensitive(PathSensitivity.NAME_ONLY)
  File idlDestinationDir

  @OutputDirectory
  File snapshotDestinationDir

  PegasusOptions.IdlOptions idlOptions

  FileCollection pathedCodegenClasspath

  // we make a separation between watched and unwatched variables to create a stricter definition for incremental builds.
  // In this case, the unwatched directories and classes include all of the main source sets for the application. This
  // creates a nasty use case where the user updates a utility class, but the rest models still regenerate. In Gradle 4.0
  // this will no longer be needed with the introduction of normalizing filters.
  private FileCollection _codegenClasspath
  private Set<File> _inputDirs

  public static final String INCLUDED_SOURCE_TYPES_PROPERTY = "pegasus.generateRestModel.includedSourceTypes"

  @TaskAction
  protected void generate()
  {
    final String[] inputDirPaths = getInputDirs().collect { it.path }
    project.logger.debug("GenerateRestModel using input directories ${inputDirPaths}")
    project.logger.debug("GenerateRestModel using destination dir ${idlDestinationDir.path}")
    snapshotDestinationDir.mkdirs()
    idlDestinationDir.mkdirs()

    boolean ignoreNonJavaFiles = false

    Set<String> includedSourceTypes = project.findProperty(INCLUDED_SOURCE_TYPES_PROPERTY) ?: ["ALL"]

    if (includedSourceTypes.size() == 1 && includedSourceTypes.contains("JAVA")){
      ignoreNonJavaFiles = true
    }

    pathedCodegenClasspath =  PathingJarUtil.generatePathingJar(project, "generateRestModel", getCodegenClasspath(),
      ignoreNonJavaFiles)

    // handle multiple idl generations in the same project, see pegasus rest-framework-server-examples
    // for example.
    // by default, scan in all source files for annotated java classes.
    // specifically, to scan in certain packages, use
    //   pegasus.<sourceSet>.idlOptions.addIdlItem(['<packageName>'])
    // where [<packageName>] is the array of packages that should be searched for annotated java classes.
    // for example:
    // pegasus.main.idlOptions.addIdlItem(['com.linkedin.groups.server.rest.impl', 'com.linkedin.greetings.server.rest.impl'])
    // they will still be placed in the same jar, though

    def loadAdditionalDocProviders = !ignoreNonJavaFiles && project.tasks.findByName("scaladoc") != null

    if (idlOptions.idlItems.empty)
    {
      executeSnapshotExporter(inputDirPaths, snapshotDestinationDir.path, loadAdditionalDocProviders)
      executeResourceExporter(inputDirPaths, idlDestinationDir.path, loadAdditionalDocProviders)
    }
    else
    {
      for (PegasusOptions.IdlItem idlItem : idlOptions.idlItems)
      {
        final String apiName = idlItem.apiName
        if (apiName.length() == 0)
        {
          project.logger.info('Generating interface for unnamed api ...')
        }
        else
        {
          project.logger.info("Generating interface for api: ${apiName} ...")
        }

        executeSnapshotExporter(apiName, inputDirPaths, idlItem.packageNames, snapshotDestinationDir.path,
                                loadAdditionalDocProviders)
        executeResourceExporter(apiName, inputDirPaths, idlItem.packageNames, idlDestinationDir.path,
                                loadAdditionalDocProviders)
      }
    }
  }

  private void executeSnapshotExporter(String[] inputDirs, String destinationPath, boolean additionalDocProviders)
  {
    executeSnapshotExporter(null, inputDirs, null, destinationPath, additionalDocProviders)
  }

  private void executeSnapshotExporter(String name, String[] inputDirs, String[] packages, String destinationPath,
                                       boolean additionalDocProviders)
  {
    project.javaexec { JavaExecAction it ->
      it.main = 'com.linkedin.restli.tools.snapshot.gen.RestLiSnapshotExporterCmdLineApp'
      it.classpath pathedCodegenClasspath
      it.jvmArgs '-Dgenerator.resolver.path=' + resolverPath.asPath
      it.systemProperty "scala.usejavacp", "true"
      if (name != null)
      {
        it.args '-name', name
      }
      it.args addPrefixToArray('-sourcepath', inputDirs)
      it.args '-outdir', destinationPath
      if (packages != null)
      {
        it.args addPrefixToArray('-resourcepackages', packages)
      }
      if (additionalDocProviders)
      {
        it.args '-loadAdditionalDocProviders'
      }
    }
  }

  private void executeResourceExporter(String[] inputDirs, String destinationPath, boolean additionalDocProviders)
  {
    executeResourceExporter(null, inputDirs, null, destinationPath, additionalDocProviders)
  }

  private void executeResourceExporter(String name, String[] inputDirs, String[] packages, String destinationPath,
                                       boolean additionalDocProviders)
  {
    def sourcePath = ['-sourcepath']
    sourcePath.addAll(inputDirs)

    project.javaexec { JavaExecAction it ->
      it.main = 'com.linkedin.restli.tools.idlgen.RestLiResourceModelExporterCmdLineApp'
      it.classpath pathedCodegenClasspath
      it.systemProperty "scala.usejavacp", "true"
      if (name != null)
      {
        it.args '-name', name
      }
      it.args addPrefixToArray('-sourcepath', inputDirs)
      it.args '-outdir', destinationPath
      if (packages != null)
      {
        it.args addPrefixToArray('-resourcepackages', packages)
      }
      if (additionalDocProviders)
      {
        it.args '-loadAdditionalDocProviders'
      }
    }
  }

  static List<String> addPrefixToArray(String prefix, String[] args)
  {
    def sourcePath = [prefix]
    sourcePath.addAll(args)
    return sourcePath
  }

  void setCodegenClasspath(FileCollection codegenClasspath) {
    _codegenClasspath = codegenClasspath
  }

  void setInputDirs(Set<File> inputDirs) {
    _inputDirs = inputDirs
  }

  FileCollection getCodegenClasspath() {
    _codegenClasspath
  }

  Set<File> getInputDirs() {
    _inputDirs
  }
}