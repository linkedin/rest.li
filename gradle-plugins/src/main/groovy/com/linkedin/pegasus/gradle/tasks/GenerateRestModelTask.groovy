package com.linkedin.pegasus.gradle.tasks


import com.linkedin.pegasus.gradle.PegasusOptions
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
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
 * </pre>*/
class GenerateRestModelTask extends DefaultTask
{
  @InputFiles
  @SkipWhenEmpty
  Set<File> inputDirs
  @OutputDirectory
  File snapshotDestinationDir
  @OutputDirectory
  File idlDestinationDir
  @InputFiles
  FileCollection resolverPath

  @InputFiles
  FileCollection codegenClasspath

  PegasusOptions.IdlOptions idlOptions

  File localClasspath

  @TaskAction
  protected void generate()
  {
    final String[] inputDirPaths = inputDirs.collect { it.path }
    project.logger.debug("GenerateRestModel using input directories ${inputDirPaths}")
    project.logger.debug("GenerateRestModel using destination dir ${idlDestinationDir.path}")
    snapshotDestinationDir.mkdirs()
    idlDestinationDir.mkdirs()

    localClasspath = new File(project.buildDir, getName() + 'Classpath')
    project.copy {
      from codegenClasspath
      include '*.jar'
      into localClasspath
    }

    // handle multiple idl generations in the same project, see pegasus rest-framework-server-examples
    // for example.
    // by default, scan in all source files for annotated java classes.
    // specifically, to scan in certain packages, use
    //   pegasus.<sourceSet>.idlOptions.addIdlItem(['<packageName>'])
    // where [<packageName>] is the array of packages that should be searched for annotated java classes.
    // for example:
    // pegasus.main.idlOptions.addIdlItem(['com.linkedin.groups.server.rest.impl', 'com.linkedin.greetings.server.rest.impl'])
    // they will still be placed in the same jar, though

    def loadAdditionalDocProviders = project.tasks.findByName("scaladoc") != null

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
      it.classpath localClasspath.absolutePath + '/*'
      it.classpath codegenClasspath.files.findAll { !it.file || !it.name.endsWith('.jar') }
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
      it.classpath localClasspath.absolutePath + '/*'
      it.classpath codegenClasspath.files.findAll { !it.file || !it.name.endsWith('.jar') }
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
}