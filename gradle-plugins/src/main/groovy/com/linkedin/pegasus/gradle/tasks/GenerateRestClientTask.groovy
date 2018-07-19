package com.linkedin.pegasus.gradle.tasks


import com.linkedin.pegasus.gradle.PegasusOptions
import com.linkedin.pegasus.gradle.PegasusPlugin
import com.linkedin.pegasus.gradle.SharedFileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec


/**
 * This task will generate the rest client source files.
 *
 * As pre-requisite of this task,, add these lines to your build.gradle:
 * <pre>
 * apply plugin: 'li-pegasus2'
 * </pre>
 *
 * Optionally, you can specify certain resource classes to be generated idl
 * <pre>
 * pegasus.<sourceSet>.clientOptions.addClientItem('<restModelFilePath>', '<defaultPackage>', <keepDataTemplates>)
 * </pre>
 * keepDataTemplates is a boolean that isn't used right now, but might be implemented in the future.
 */
@CacheableTask
class GenerateRestClientTask extends DefaultTask
{
  @InputDirectory
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  File inputDir

  @Classpath
  FileCollection resolverPath

  @Classpath
  FileCollection runtimeClasspath

  @Classpath
  FileCollection codegenClasspath

  @OutputDirectory
  File destinationDir

  boolean isRestli2FormatSuppressed
  boolean _isRestli1BuildersDeprecated = true

  @TaskAction
  protected void generate()
  {
    PegasusOptions.ClientOptions pegasusClientOptions = new PegasusOptions.ClientOptions()

    // idl input could include rest model jar files
    project.files(inputDir).each { input ->
      if (input.isDirectory())
      {
        for (File f : SharedFileUtils.getSuffixedFiles(project, input, PegasusPlugin.IDL_FILE_SUFFIX))
        {
          if (!pegasusClientOptions.hasRestModelFileName(f.name))
          {
            pegasusClientOptions.addClientItem(f.name, '', false)
            project.logger.lifecycle("Add interface file: ${f.path}")
          }
        }
      }
    }
    if (pegasusClientOptions.clientItems.empty)
    {
      return
    }

    project.logger.info('Generating REST client builders ...')

    final String resolverPathStr = resolverPath.asPath
    destinationDir.mkdirs()

    Map<String, List<String>> version1Files = [:].withDefault { [] }
    Map<String, List<String>> version2Files = [:].withDefault { [] }
    project.logger.lifecycle("Destination directory: ${destinationDir}")

    for (PegasusOptions.ClientItem clientItem : pegasusClientOptions.clientItems)
    {
      project.logger.lifecycle("Generating rest client source files for: ${clientItem.restModelFileName}")

      final String defaultPackage
      if (clientItem.defaultPackage.equals("") && project.hasProperty('idlDefaultPackage') && project.idlDefaultPackage)
      {
        defaultPackage = project.idlDefaultPackage
      }
      else
      {
        defaultPackage = clientItem.defaultPackage
      }


      final String restModelFilePath = "${inputDir}${File.separatorChar}${clientItem.restModelFileName}".toString()
      version1Files.get(defaultPackage).add(restModelFilePath)

      if (!isRestli2FormatSuppressed)
      {
        version2Files.get(defaultPackage).add(restModelFilePath)
      }
    }

    String deprecatedVersion = _isRestli1BuildersDeprecated ? '2.0.0' : null

    version1Files.each { defaultPackage, files ->
      project.javaexec { JavaExecSpec it ->
        it.classpath = runtimeClasspath + codegenClasspath
        it.main = 'com.linkedin.restli.tools.clientgen.RestRequestBuilderGenerator'
        it.jvmArgs '-Dgenerator.resolver.path=' + resolverPathStr  //RestRequestBuilderGenerator.run(resolverPath)
        it.jvmArgs '-Dgenerator.default.package=' + defaultPackage //RestRequestBuilderGenerator.run(defaultPackage)
        it.jvmArgs '-Dgenerator.generate.imported=false'  //RestRequestBuilderGenerator.run(generateImported)
        it.jvmArgs '-Dgenerator.rest.generate.datatemplates=false'
        //RestRequestBuilderGenerator.run(generateDataTemplates)
        it.jvmArgs '-Dgenerator.rest.generate.version=1.0.0'  //RestRequestBuilderGenerator.run(version)
        it.jvmArgs '-Dgenerator.rest.generate.deprecated.version=' + deprecatedVersion
        it.jvmArgs '-Droot.path=' + project.rootDir.path
        //RestRequestBuilderGenerator.run(deprecatedByVersion)
        it.args destinationDir.absolutePath
        it.args files
      }.assertNormalExitValue()
    }

    if (!version2Files.isEmpty())
    {
      version2Files.each { defaultPackage, files ->
        project.javaexec { JavaExecSpec it ->
          it.classpath = runtimeClasspath + codegenClasspath
          it.main = 'com.linkedin.restli.tools.clientgen.RestRequestBuilderGenerator'
          it.jvmArgs '-Dgenerator.resolver.path=' + resolverPathStr  //RestRequestBuilderGenerator.run(resolverPath)
          it.jvmArgs '-Dgenerator.default.package=' + defaultPackage //RestRequestBuilderGenerator.run(defaultPackage)
          it.jvmArgs '-Dgenerator.generate.imported=false'        //RestRequestBuilderGenerator.run(generateImported)
          it.jvmArgs '-Dgenerator.rest.generate.datatemplates=false'
          //RestRequestBuilderGenerator.run(generateDataTemplates)
          it.jvmArgs '-Dgenerator.rest.generate.version=2.0.0'  //RestRequestBuilderGenerator.run(version)
          it.jvmArgs '-Droot.path=' + project.rootDir.path
          it.args destinationDir.absolutePath
          it.args files
        }.assertNormalExitValue()
      }
    }
  }
}