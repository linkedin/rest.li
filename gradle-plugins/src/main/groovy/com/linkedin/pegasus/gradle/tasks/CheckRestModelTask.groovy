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

  @TaskAction
  protected void check()
  {

    final String modelCompatLevel = PropertyUtil.findCompatLevel(project, FileCompatibilityType.SNAPSHOT)

    project.logger.info('Checking interface compatibility with API ...')

    List<String> argFiles = []
    argFiles.addAll(findMatchingFiles(PegasusPlugin.SNAPSHOT_FILE_SUFFIX, currentSnapshotFiles,
                                      project.files(previousSnapshotDirectory)))
    argFiles.addAll(findMatchingFiles(PegasusPlugin.IDL_FILE_SUFFIX, currentIdlFiles, project.files(previousIdlDirectory)))

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

    if (!isModelCompatible && !isRestSpecCompatible)
    {
      throw new GradleException("See output for " + getPath())
    }
  }

  /**
   * Given two files collections and an extension, the matching files from the previous and current files. In the case that
   * either the current or previous is missing, it will be <pre>""</pre>.
   *
   * @return A list of filepath pairs. The one is the current file, the right one is the previous one.
   */
  public List<String> findMatchingFiles(String ext, FileCollection currentFiles, FileCollection previousFiles)
  {
    Map<String, String> currentFilenameToAbsolutePath = createMapFromFiles(currentFiles, ext)
    Map<String, String> previousFilenameToAbsolutePath = createMapFromFiles(previousFiles, ext)

    List<String> filePairs = []

    currentFilenameToAbsolutePath.each { filename, absolutePath ->
      if (previousFilenameToAbsolutePath.containsKey(filename))
      {
        filePairs.addAll([absolutePath, previousFilenameToAbsolutePath.get(filename)])  //Add both files

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