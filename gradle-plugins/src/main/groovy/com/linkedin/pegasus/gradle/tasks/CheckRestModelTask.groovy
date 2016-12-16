package com.linkedin.pegasus.gradle.tasks


import com.linkedin.pegasus.gradle.FileCompatibilityType
import com.linkedin.pegasus.gradle.PegasusPlugin
import com.linkedin.pegasus.gradle.PropertyUtil
import com.linkedin.pegasus.gradle.internal.CompatibilityLogChecker
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.JavaExecAction

public class CheckRestModelTask extends DefaultTask
{
  @InputFiles
  @SkipWhenEmpty
  FileCollection currentSnapshotFiles

  @InputDirectory
  @SkipWhenEmpty
  File previousSnapshotDirectory

  @InputFiles
  @SkipWhenEmpty
  FileCollection currentIdlFiles

  @InputDirectory
  @SkipWhenEmpty
  File previousIdlDirectory

  @InputFiles
  FileCollection codegenClasspath

  boolean isModelCompatible = true
  boolean isRestSpecCompatible = true
  boolean isEquivalent = true
  boolean isRestSpecEquivalent = true
  String wholeMessage = "";

  @TaskAction
  protected void check()
  {

    final String modelCompatLevel = PropertyUtil.findCompatLevel(project, FileCompatibilityType.SNAPSHOT)

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

    if (!isModelCompatible || !isRestSpecCompatible)
    {
      throw new GradleException("See output for " + getPath())
    }
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
  public List<String> findMatchingFiles(String ext, FileCollection currentFiles, FileCollection previousFiles, boolean diffOnly)
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