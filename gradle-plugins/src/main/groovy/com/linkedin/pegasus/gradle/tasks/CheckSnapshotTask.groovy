package com.linkedin.pegasus.gradle.tasks


import com.linkedin.pegasus.gradle.FileCompatibilityType
import com.linkedin.pegasus.gradle.PegasusPlugin
import com.linkedin.pegasus.gradle.PropertyUtil
import com.linkedin.pegasus.gradle.internal.CompatibilityLogChecker
import com.linkedin.pegasus.gradle.internal.FileExtensionFilter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.JavaExecAction

class CheckSnapshotTask extends DefaultTask
{
  @InputFiles
  FileCollection currentSnapshotFiles

  @InputDirectory
  File previousSnapshotDirectory

  @InputFiles
  FileCollection codegenClasspath

  boolean isModelCompatible = true
  boolean isRestSpecCompatible = true
  boolean isEquivalent = true
  boolean isRestSpecEquivalent = true
  String wholeMessage = "";

  private static _snapshotFilter = new FileExtensionFilter(PegasusPlugin.SNAPSHOT_FILE_SUFFIX)

  @TaskAction
  protected void check()
  {
    final String snapshotCompatLevel = PropertyUtil.findCompatLevel(project, FileCompatibilityType.SNAPSHOT)

    project.logger.info('Checking interface compatibility with API ...')

    List<String> argFiles = []

    checkSnapshotCompatibility(project, currentSnapshotFiles, previousSnapshotDirectory, _snapshotFilter, argFiles)

    if (argFiles.isEmpty())
    {
      return
    }

    def logChecker = new CompatibilityLogChecker()

    project.javaexec { JavaExecAction it ->
      it.main = 'com.linkedin.restli.tools.snapshot.check.RestLiSnapshotCompatibilityChecker'
      it.classpath = codegenClasspath
      it.args '--compat', snapshotCompatLevel
      it.args '--report'
      it.args argFiles
      it.standardOutput = logChecker
    }

    isModelCompatible = logChecker.isModelCompatible()
    isRestSpecCompatible = logChecker.isRestSpecCompatible()
    isEquivalent = logChecker.getModelCompatibility().isEmpty() && logChecker.getRestSpecCompatibility().isEmpty()
    isRestSpecEquivalent = logChecker.getRestSpecCompatibility().isEmpty()
    wholeMessage = logChecker.getWholeText()

    if (!isModelCompatible || !isRestSpecCompatible)
    {
      throw new GradleException("See output for " + getPath())
    }
  }

  private void checkSnapshotCompatibility(Project project,
                                          FileCollection currentFiles,
                                          File previousDirectory,
                                          FileExtensionFilter filter,
                                          List<String> fileArgs)
  {

    final boolean isCheckRestSpecVsSnapshot = filter.suffix.equals(PegasusPlugin.IDL_FILE_SUFFIX)

    currentFiles.each {
      project.logger.info('Checking interface file: ' + it.path)

      final String apiFilename
      if (isCheckRestSpecVsSnapshot)
      {
        apiFilename = it.name.substring(0, it.name.length() - PegasusPlugin.SNAPSHOT_FILE_SUFFIX.length()) + PegasusPlugin.IDL_FILE_SUFFIX
      }
      else
      {
        apiFilename = it.name
      }
      final String apiFilePath = "${previousDirectory.path}${File.separatorChar}${apiFilename}"
      final File apiFile = project.file(apiFilePath)
      if (apiFile.exists())
      {
        fileArgs.addAll([apiFilePath, it.path])
      }
    }
  }
}
