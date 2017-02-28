package com.linkedin.pegasus.gradle.tasks


import com.linkedin.pegasus.gradle.SchemaFileType
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.JavaExecAction


/**
 * Translates files between the .pdsc and .pdl Pegasus schema formats.
 */
class TranslateSchemasTask extends DefaultTask {
  /**
   * Directory to write the translated files.
   */
  @OutputDirectory
  File destinationDir

  /**
   * Directory containing the data schema files to translate.
   */
  @InputDirectory
  @SkipWhenEmpty
  File inputDir

  /**
   * The resolver path.
   */
  @InputFiles
  FileCollection resolverPath

  @InputFiles
  FileCollection codegenClasspath

  SchemaFileType sourceFormat = SchemaFileType.PDSC
  SchemaFileType destinationFormat = SchemaFileType.PDL

  @TaskAction
  protected void translate()
  {
    project.logger.info('Translating data schemas ...')
    destinationDir.mkdirs()

    final String resolverPathStr = (resolverPath + project.files(inputDir)).asPath

    project.javaexec { JavaExecAction it ->
      it.main = 'com.linkedin.restli.tools.data.SchemaFormatTranslator'
      it.classpath codegenClasspath
      it.jvmArgs '-Dgenerator.resolver.path=' + resolverPathStr
      it.args '--source-format'
      it.args sourceFormat.getFileExtension()
      it.args '--destination-format'
      it.args destinationFormat.getFileExtension()
      it.args resolverPathStr
      it.args inputDir.absolutePath
      it.args destinationDir.absolutePath
    }
  }
}
