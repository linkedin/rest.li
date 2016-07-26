package com.linkedin.pegasus.gradle.tasks


import com.linkedin.pegasus.gradle.FileCompatibilityType
import com.linkedin.pegasus.gradle.PegasusPlugin
import com.linkedin.pegasus.gradle.PropertyUtil
import com.linkedin.pegasus.gradle.internal.CompatibilityLogChecker
import com.linkedin.pegasus.gradle.internal.FileExtensionFilter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.JavaExecAction

class CheckIdlTask extends DefaultTask
{
  @InputFiles
  @SkipWhenEmpty
  FileCollection currentIdlFiles

  @InputDirectory
  @SkipWhenEmpty
  File previousIdlDirectory
  @InputFiles
  FileCollection resolverPath
  @InputFiles
  FileCollection codegenClasspath

  boolean isModelCompatible = true
  boolean isRestSpecCompatible = true
  boolean isEquivalent = true

  private FileExtensionFilter _idlFilter = new FileExtensionFilter(PegasusPlugin.IDL_FILE_SUFFIX)

  @TaskAction
  protected void check()
  {
    final String idlCompatLevel = PropertyUtil.findCompatLevel(project, FileCompatibilityType.IDL)

    project.logger.info('Checking interface compatibility with API ...')

    List<String> errorFilePairs = findErrorFilePairs()

    def logChecker = new CompatibilityLogChecker()

    project.javaexec { JavaExecAction it ->
      it.main = 'com.linkedin.restli.tools.idlcheck.RestLiResourceModelCompatibilityChecker'
      it.classpath = codegenClasspath
      it.jvmArgs '-Dgenerator.resolver.path=' + resolverPath.asPath
      it.args '--compat', idlCompatLevel
      it.args '--report'
      it.args errorFilePairs
      it.standardOutput = logChecker
    }

    isModelCompatible = logChecker.isModelCompatible()
    isRestSpecCompatible = logChecker.isRestSpecCompatible()
    isEquivalent = logChecker.getModelCompatibility().isEmpty() && logChecker.getRestSpecCompatibility().isEmpty()

    if (!isModelCompatible && !isRestSpecCompatible)
    {
      throw new GradleException("See output for " + getPath())
    }
  }

  private List<String> findErrorFilePairs()
  {
    List<String> nonEquivExpectedFiles = new ArrayList<String>()

    final List<String> errorFilePairs = []
    final Set<String> apiExistingFilePaths = previousIdlDirectory.listFiles(_idlFilter).collect { it.absolutePath }
    currentIdlFiles.each {
      String expectedOldFilePath = "${previousIdlDirectory.path}${File.separatorChar}${it.name}"
      final File expectedFile = project.file(expectedOldFilePath)
      if (expectedFile.exists())
      {
        apiExistingFilePaths.remove(expectedOldFilePath)
        errorFilePairs.addAll([expectedFile.absolutePath, it.path])
      }
      else
      {
        // found new file that has no matching old file
        errorFilePairs.addAll(["", it.path])
        nonEquivExpectedFiles.add(expectedFile.absolutePath)
      }
    }

    apiExistingFilePaths.each {
      // found old file that has no matching new file
      errorFilePairs.addAll([it, ""])
    }

    return errorFilePairs
  }
}