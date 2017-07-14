package com.linkedin.pegasus.gradle.tasks


import com.linkedin.pegasus.gradle.IOUtil
import com.linkedin.pegasus.gradle.PegasusPlugin
import com.linkedin.pegasus.gradle.internal.CompatibilityLogChecker
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.JavaExecAction

@CacheableTask
public class CheckRestModelTask extends DefaultTask
{
  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  FileCollection currentSnapshotFiles

  @InputDirectory
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  File previousSnapshotDirectory

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  FileCollection currentIdlFiles

  @InputDirectory
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  File previousIdlDirectory

  @Classpath
  FileCollection codegenClasspath

  @Input
  String modelCompatLevel

  @OutputFile
  File getSummaryTarget()
  {
    return summaryTarget
  }

  boolean isModelCompatible = true
  boolean isRestSpecCompatible = true
  boolean isEquivalent = true
  boolean isRestSpecEquivalent = true
  String wholeMessage = ""

  private File summaryTarget = new File(project.buildDir, "reports/checkRestModel/summary.txt")

  @TaskAction
  protected void check()
  {
    project.logger.info('Checking interface compatibility with API ...')
    List<String> argFiles = []
    argFiles.addAll(findMatchingFiles(PegasusPlugin.SNAPSHOT_FILE_SUFFIX, currentSnapshotFiles,
                                      project.fileTree(previousSnapshotDirectory), false))
    // We don't pass matching IDL files to RestLiSnapshotCompatibilityChecker. We only specify added or deleted IDL
    // files, for which the checker will generate appropriate message.
    argFiles.addAll(findMatchingFiles(PegasusPlugin.IDL_FILE_SUFFIX, currentIdlFiles,
                                      project.fileTree(previousIdlDirectory), true))

    if (argFiles.isEmpty())
    {
      return
    }

    def logChecker = new CompatibilityLogChecker()
    project.javaexec { JavaExecAction it ->
      it.main = 'com.linkedin.restli.tools.snapshot.check.RestLiSnapshotCompatibilityChecker'
      it.classpath = codegenClasspath
      it.args '--compat', modelCompatLevel.toLowerCase()
      it.args '--report'
      it.args argFiles
      it.standardOutput = logChecker
    }

    isModelCompatible = logChecker.isModelCompatible()
    isRestSpecCompatible = logChecker.isRestSpecCompatible()
    isEquivalent = logChecker.getModelCompatibility().isEmpty() && logChecker.getRestSpecCompatibility().isEmpty()
    isRestSpecEquivalent = logChecker.getRestSpecCompatibility().isEmpty()
    wholeMessage = logChecker.getWholeText()
    IOUtil.writeText(getSummaryTarget(), wholeMessage)

    if (!isModelCompatible || !isRestSpecCompatible)
    {
      throw new GradleException("See output for " + getPath() + ". Summary written to " + getSummaryTarget().absolutePath)
    }
  }

  void setSummaryTarget(File summaryTarget)
  {
    this.summaryTarget = summaryTarget
  }

  /**
   * Given two file collections and an extension, find files with the same names from the previous and current files.
   * In the case that either the current or the previous is missing, it will be <pre>""</pre>.
   *
   * @param ext           The file extension.
   * @param currentFiles  Current files which are newly generated IDL or snapshot files.
   * @param previousFiles Previous files which are existing IDL or snapshot files.
   * @param diffOnly      Return only the difference between current files and previous files without files with
   *                      matching names.
   *
   * @return A list of filepath which are pairs of current file and previous file concatenated together.
   */
  List<String> findMatchingFiles(String ext, FileCollection currentFiles, FileCollection previousFiles, boolean diffOnly)
  {
    Map<String, String> currentFilenameToAbsolutePath = createMapFromFiles(currentFiles, ext)
    Map<String, String> previousFilenameToAbsolutePath = createMapFromFiles(previousFiles, ext)

    List<String> filePairs = []

    currentFilenameToAbsolutePath.each { filename, absolutePath ->
      if (previousFilenameToAbsolutePath.containsKey(filename))
      {
        if (!diffOnly)
        {
          filePairs.addAll([previousFilenameToAbsolutePath.get(filename), absolutePath])  //Add both files (prev, current)
        }

        previousFilenameToAbsolutePath.remove(filename)
        //remove it from the map, so that we can loop over everything left
      }
      else
      {
        filePairs.addAll(['', absolutePath])  // Add missing file
      }
    }

    previousFilenameToAbsolutePath.each { filename, absolutePath -> filePairs.addAll([absolutePath, ''])  //Add missing file
    }

    return filePairs
  }

  private static Map<String, String> createMapFromFiles(FileCollection currentFiles, String ext)
  {
    Map<String, String> filenameToAbsolutePath = new HashMap<>()
    currentFiles.filter(new Spec<File>() {
      @Override
      boolean isSatisfiedBy(File file)
      {
        file.getName().endsWith(ext)
      }
    }).each { filenameToAbsolutePath.put(it.getName(), it.getAbsolutePath()) }
    return filenameToAbsolutePath;
  }
}